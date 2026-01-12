package com.Team1_Back.service;

import com.Team1_Back.domain.Expense;
import com.Team1_Back.dto.*;
import com.Team1_Back.constants.ReportTypes;
import com.Team1_Back.domain.ReportDownloadLog;
import com.Team1_Back.domain.ReportFile;
import com.Team1_Back.report.entity.ReportJob;
import com.Team1_Back.domain.enums.DataScope;
import com.Team1_Back.domain.enums.OutputFormat;
import com.Team1_Back.domain.enums.ReportStatus;
import com.Team1_Back.generator.ExcelReportGenerator;
import com.Team1_Back.generator.PdfReportGenerator;
import com.Team1_Back.repository.*;
import com.Team1_Back.repository.projection.ReportQueryRepository;
import com.Team1_Back.security.ReportPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ExpenseRepository expenseRepository;
    private final ReportQueryRepository reportQueryRepository;

    private final ReportJobRepository reportJobRepository;
    private final PdfReportGenerator pdfGen;
    private final ExcelReportGenerator excelGen;
    private final ReportFileRepository reportFileRepository;
    private final ReportDownloadLogRepository reportDownloadLogRepository;
    private final ReportScheduleRepository reportScheduleRepository;

    private void insertDownloadLog(ReportFile rf, Long userId) {
        ReportDownloadLog log = new ReportDownloadLog();
        log.setReportFile(rf);
        log.setReportJob(rf.getReportJob());
        log.setDownloadedBy(userId);
        log.setDownloadedAt(LocalDateTime.now());
        reportDownloadLogRepository.saveAndFlush(log);
    }


    /**
     * 로컬 스토리지 루트 (기본값 report-storage)
     * 예: report-storage/2025/12/{reportId}/Report_2025-12_xxx.pdf
     */
    @Value("${com.mallapi.report.storage-path:report-storage}")
    private String storagePath;

    // =========================
    // Public APIs
    // =========================
    public ReportGenerateResult generate(ReportPrincipal principal, ReportGenerateRequestDTO req) {

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        return generateCore(
                principal.userId(),
                principal.role(),
                principal.departmentName(), // 요청자(로그인한 사람) 부서명
                req
        );
    }

    public ReportGenerateResult generateInternal(ReportGenerateRequestDTO req) {
        // SYSTEM 실행 가정
        Long systemUserId = 0L;
        String role = "ADMIN";
        String requesterDeptName = null;

        return generateCore(systemUserId, role, requesterDeptName, req);
    }

    private ReportGenerateResult generateCore(
            Long requestedBy,
            String role,
            String requesterDepartmentName,
            ReportGenerateRequestDTO req
    ) {

        if (req == null || req.getFilters() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request");
        }

        // ✅ filters 변수 선언 (Cannot resolve symbol 'filters' 해결)
        ReportGenerateRequestDTO.Filters filters = req.getFilters();

        // -------------------------
        // 1) 타입 검증
        // -------------------------
        String incoming = req.getReportTypeId();
        log.warn("[GEN] incoming reportTypeId='{}'", incoming);

        ReportTypes.TypeDef type = ReportTypes.find(incoming);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reportTypeId");
        }

        // -------------------------
        // 2) RBAC
        // -------------------------
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (type.adminOnly() && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin-only report");
        }

        // -------------------------
        // 3) format 검증
        // -------------------------
        OutputFormat expectedFormat =
                (filters.getFormat() == null)
                        ? parseFormat(type.format())
                        : parseFormat(filters.getFormat());

        String reqFormat = safeUpper(filters.getFormat());
        if (reqFormat != null && !reqFormat.equals(expectedFormat.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format mismatch");
        }

        // -------------------------
        // 4) scope 확정
        // -------------------------
        DataScope scope = resolveScope(isAdmin, safeUpper(filters.getDataScope()));

        // ✅ DEPT scope면 "조회 대상 부서"는 filters.department로 강제
        String targetDept = null;
        if (scope == DataScope.DEPT) {
            targetDept = (filters.getDepartment() == null) ? null : filters.getDepartment().trim();
            if (targetDept == null || targetDept.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department is required for DEPT scope");
            }
        }

        log.warn("[GEN] scope={}, requesterDept={}, targetDept(filters)={}",
                scope, requesterDepartmentName, targetDept);

        // -------------------------
        // 5) category JSON
        // -------------------------
        String categoryJson = toJsonArray(filters.getCategory());

        // -------------------------
        // 6) job 생성 + snapshot 세팅
        // -------------------------
        ReportJob job = new ReportJob();
        job.setRequestedBy(requestedBy);
        job.setRoleSnapshot(role);
        job.setReportTypeId(type.id());
        job.setPeriod(filters.getPeriod());
        job.setDataScope(scope);
        job.setCategoryJson(categoryJson);
        job.setOutputFormat(expectedFormat);
        job.setStatus(ReportStatus.GENERATING);

        // ✅ snapshot 정책 (여기서만 결정!)
        // - DEPT: targetDept(=filters.department) "개발2팀"
        // - MY  : requesterDepartmentName (있으면)
        // - ALL : null
        String snapshotDept;
        if (scope == DataScope.DEPT) {
            snapshotDept = targetDept; // ✅ 무조건 문자열 부서명
        } else if (scope == DataScope.MY) {
            snapshotDept = (requesterDepartmentName == null) ? null : requesterDepartmentName.trim();
        } else {
            snapshotDept = null;
        }
        job.setDepartmentSnapshot(snapshotDept);

        log.warn("[GEN] filters.department(raw)='{}'", filters.getDepartment());
        log.warn("[GEN] job.departmentSnapshot='{}'", job.getDepartmentSnapshot());
        log.warn("[GEN] filters.period='{}', dataScope(raw)='{}'", filters.getPeriod(), filters.getDataScope());

        LocalDate[] range = toMonthRangeOrNull(job.getPeriod());
        if (range != null) {
            job.setPeriodStart(range[0]);
            job.setPeriodEnd(range[1]);
        }

        // ✅ 저장 + flush (id 확보 & snapshot DB 반영)
        ReportJob saved = reportJobRepository.saveAndFlush(job);
        Long reportId = saved.getId();

        // -------------------------
        // 7) 파일 경로
        // -------------------------
        Path dir = Paths.get(
                storagePath,
                String.valueOf(LocalDate.now().getYear()),
                String.valueOf(LocalDate.now().getMonthValue()),
                String.valueOf(reportId)
        );

        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            saved.setStatus(ReportStatus.FAILED);
            saved.setErrorMessage("Failed to create directories");
            reportJobRepository.save(saved);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Storage error");
        }

        String ext = (expectedFormat == OutputFormat.PDF) ? "pdf" : "xlsx";
        String fileName = buildFileName(saved.getPeriod(), type.id(), ext);
        Path outputFile = dir.resolve(fileName);

        // -------------------------
        // 8) EXPENSE 승인 합계 리포트면 미리 계산해서 DB에 저장
        // -------------------------
        boolean isExpenseApprovedReport =
                ReportTypes.EXPENSE_APPROVED_SUMMARY_PDF.equals(saved.getReportTypeId()) ||
                        ReportTypes.EXPENSE_APPROVED_SUMMARY_EXCEL.equals(saved.getReportTypeId());

        log.warn("[EXP] reportTypeId(saved)={}", saved.getReportTypeId());
        log.warn("[EXP] isExpenseApprovedReport={}", isExpenseApprovedReport);

        if (isExpenseApprovedReport) {

            LocalDate startDate = saved.getPeriodStart(); // 2025-12-01
            LocalDate endDate   = saved.getPeriodEnd();   // 2025-12-31

            ApprovedAgg agg = switch (saved.getDataScope()) {
                case ALL -> reportQueryRepository.approvedSumAll(startDate, endDate);
                case MY  -> reportQueryRepository.approvedSumByUser(saved.getRequestedBy(), startDate, endDate);
                case DEPT -> {
                    String dept = saved.getDepartmentSnapshot();
                    if (dept == null || dept.isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department is required for DEPT scope");
                    }
                    yield reportQueryRepository.approvedSumByDept(dept.trim(), startDate, endDate);
                }
            };

            long total = (agg == null || agg.getTotal() == null) ? 0L : agg.getTotal();
            int count  = (agg == null || agg.getCnt() == null) ? 0 : agg.getCnt().intValue();

            log.warn("[EXP] scope={}, startDt={}, endDt={}, requestedBy={}, deptSnapshot={}",
                    saved.getDataScope(), startDate, endDate, saved.getRequestedBy(), saved.getDepartmentSnapshot());
            log.warn("[EXP] count={}, total={}", count, total);

            saved.setApprovedTotal(total);
            saved.setApprovedCount(count);

            // ✅ 여기서 flush까지 해서 "freshJob"로 파일 생성
            reportJobRepository.saveAndFlush(saved);
        }

        // ✅ DB에서 최신 값 다시 읽어오기 (PDF/EXCEL이 다른 값 보는 문제 방지)
        ReportJob freshJob = reportJobRepository.findById(saved.getId()).orElseThrow();

        log.warn("[GEN] freshJob.departmentSnapshot='{}'", freshJob.getDepartmentSnapshot());
        log.warn("[GEN] freshJob.approvedCount={}, approvedTotal={}",
                freshJob.getApprovedCount(), freshJob.getApprovedTotal());

        // -------------------------
        // 9) 실제 파일 생성 (✅ freshJob 사용)
        // -------------------------
        try {
            if (expectedFormat == OutputFormat.PDF) {
                pdfGen.generate(outputFile, freshJob);
            } else {
                excelGen.generate(outputFile, freshJob);
            }

            long size = Files.size(outputFile);
            String checksum = sha256Hex(outputFile);

            ReportFile rf = saveOrReuseReportFile(
                    freshJob, fileName, outputFile, expectedFormat, size, checksum
            );

            freshJob.setStatus(ReportStatus.READY);
            freshJob.setFileName(rf.getFileName());
            freshJob.setFilePath(outputFile.toString());
            freshJob.setErrorMessage(null);
            reportJobRepository.save(freshJob);

            return new ReportGenerateResult(reportId, freshJob.getStatus().name(), rf.getFileName());

        } catch (Exception e) {
            freshJob.setStatus(ReportStatus.FAILED);
            freshJob.setErrorMessage(e.getMessage());
            freshJob.setFileName(null);
            freshJob.setFilePath(null);
            reportJobRepository.save(freshJob);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Generate failed");
        }
    }



    @Transactional
    public DownloadResult download(ReportPrincipal principal, Long reportId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        ReportJob job = reportJobRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(principal.role());
        if (!isAdmin && !principal.userId().equals(job.getRequestedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your report");
        }

        if (job.getStatus() != ReportStatus.READY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report not ready");
        }

        try {
            ReportFile rf = reportFileRepository.findTopByReportJob_IdOrderByIdDesc(reportId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report file not found"));

            Path file = Paths.get(rf.getFileUrl());
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File missing");


            insertDownloadLog(rf, principal.userId());
            reportDownloadLogRepository.flush();
            return new DownloadResult(resource, rf.getFileName(), job.getOutputFormat());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Download error");
        }
    }


    @Transactional(readOnly = true)
    public ReportFilesResponseDTO getFiles(ReportPrincipal principal, Long reportId) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");

        ReportJob job = reportJobRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        assertCanAccess(principal, job);

        var files = reportFileRepository.findByReportJob_IdOrderByCreatedAtDesc(reportId).stream()
                .map(f -> new ReportFileItemDTO(
                        f.getId(),
                        f.getFileName(),
                        f.getFileType(),
                        f.getFileSize(),
                        f.getChecksum(),
                        f.getFileUrl(),
                        f.getCreatedAt()
                ))
                .toList();



        return new ReportFilesResponseDTO(reportId, files);
    }

    // =========================
    // Private helpers
    // =========================

    private void assertCanAccess(ReportPrincipal principal, ReportJob job) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(principal.role());
        if (!isAdmin && !principal.userId().equals(job.getRequestedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your report");
        }
    }

    private ReportFile saveOrReuseReportFile(
            ReportJob job,
            String fileName,
            Path outputFile,
            OutputFormat expectedFormat,
            long size,
            String checksum
    ) {
        ReportFile rf = new ReportFile();
        rf.setReportJob(job);
        rf.setFileName(fileName);
        rf.setFileUrl(outputFile.toString()); // 지금은 로컬 경로 저장
        rf.setFileType(expectedFormat.name()); // "PDF" / "EXCEL"
        rf.setFileSize(size);
        rf.setChecksum(checksum);

        try {
            return reportFileRepository.save(rf);
        } catch (DataIntegrityViolationException e) {
            // checksum UNIQUE 충돌 → 기존 파일 재사용
            return reportFileRepository.findByChecksum(checksum)
                    .orElseThrow(() -> new IllegalStateException("checksum duplicate but existing ReportFile not found"));
        }
    }

    private String buildFileName(String period, String reportTypeId, String ext) {
        String p = (period == null || period.isBlank()) ? "NA" : period.trim();
        return "Report_" + p + "_" + reportTypeId + "." + ext;
    }

    private String sha256Hex(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) md.update(buf, 0, r);
            byte[] dig = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Checksum failed");
        }
    }

    private OutputFormat parseFormat(String s) {
        String v = safeUpper(s);
        if ("PDF".equals(v)) return OutputFormat.PDF;
        if ("EXCEL".equals(v) || "XLSX".equals(v)) return OutputFormat.EXCEL;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid format in ReportTypes");
    }

    private DataScope resolveScope(boolean isAdmin, String scope) {
        if (!isAdmin) return DataScope.MY;

        // admin 기본값 DEPT (필요 시 ALL로 바꿔도 됨)
        if (scope == null) return DataScope.DEPT;

        return switch (scope) {
            case "DEPT" -> DataScope.DEPT;
            case "ALL" -> DataScope.ALL;
            case "MY" -> DataScope.MY; // 허용/차단은 정책에 따라
            default -> DataScope.DEPT;
        };
    }

    private String safeUpper(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toUpperCase();
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private LocalDate[] toMonthRangeOrNull(String period) {
        if (period == null) return null;
        String p = period.trim();
        if (p.isEmpty()) return null;

        // MVP: "YYYY-MM"
        if (p.matches("^\\d{4}-\\d{2}$")) {
            YearMonth ym = YearMonth.parse(p);
            return new LocalDate[]{ ym.atDay(1), ym.atEndOfMonth() };
        }
        return null;
    }

    @Transactional
    public DownloadResult downloadFileById(ReportPrincipal principal, Long fileId) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");

        ReportFile rf = reportFileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report file not found"));

        ReportJob job = rf.getReportJob(); // LAZY지만 트랜잭션 안이니까 OK
        boolean isAdmin = "ADMIN".equalsIgnoreCase(principal.role());
        if (!isAdmin && !principal.userId().equals(job.getRequestedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your report");
        }

        if (job.getStatus() != ReportStatus.READY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report not ready");
        }

        try {
            Path file = Paths.get(rf.getFileUrl()); // 지금은 로컬 경로
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File missing");


            insertDownloadLog(rf, principal.userId()); // saveAndFlush로 되어 있어야 함

            return new DownloadResult(resource, rf.getFileName(), job.getOutputFormat());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Download error");
        }
    }

    @Transactional(readOnly = true)
    public ReportDownloadLogsResponseDTO getDownloadLogsByReportId(ReportPrincipal principal, Long reportId) {
        assertAdmin(principal);


        List<ReportDownloadLogItemDTO> items = reportDownloadLogRepository
                .findByReportJob_IdOrderByIdDesc(reportId)
                .stream()
                .map(this::toLogItem)
                .toList();

        return new ReportDownloadLogsResponseDTO(reportId, items);
    }



    @Transactional(readOnly = true)
    public ReportFileDownloadLogsResponseDTO getDownloadLogsByFileId(ReportPrincipal principal, Long fileId) {
        assertAdmin(principal);

        List<ReportDownloadLogItemDTO> items = reportDownloadLogRepository
                .findByReportFile_IdOrderByIdDesc(fileId)
                .stream()
                .map(this::toLogItem)
                .toList();

        return new ReportFileDownloadLogsResponseDTO(fileId, items);
    }

    private void assertAdmin(ReportPrincipal p) {
        if (p == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");

        String role = p.role();
        boolean ok = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);

        if (!ok) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }


    private ReportDownloadLogItemDTO toLogItem(ReportDownloadLog log) {
        var rf = log.getReportFile(); // ManyToOne
        return new ReportDownloadLogItemDTO(
                log.getId(),
                rf.getId(),
                rf.getFileName(),
                log.getDownloadedBy(),
                log.getDownloadedAt()
        );
    }

    @Transactional(readOnly = true)
    public ReportSchedulesResponseDTO getSchedules(ReportPrincipal principal) {
        assertAdmin(principal);

        var list = reportScheduleRepository.findAll(); // 일단 simplest
        var items = list.stream()
                .map(s -> new ReportScheduleItemDTO(
                        s.getId(),
                        s.getName(),
                        s.getReportTypeId(),
                        s.getDataScope().name(),
                        s.getOutputFormat().name(),
                        s.getCronExpr(),
                        Boolean.TRUE.equals(s.getIsEnabled()),
                        s.getNextRunAt(),
                        s.getLastRunAt(),
                        s.getLastJobId(),
                        s.getFailCount() == null ? 0 : s.getFailCount(),
                        s.getLastError()
                ))
                .toList();

        return new ReportSchedulesResponseDTO(items);
    }




    // =========================
    // Results
    // =========================

    public record ReportGenerateResult(Long reportId, String status, String fileName) {}
    public record DownloadResult(Resource resource, String fileName, OutputFormat format) {}
}
