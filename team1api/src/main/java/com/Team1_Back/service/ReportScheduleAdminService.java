package com.Team1_Back.service;

import com.Team1_Back.constants.ReportTypes;
import com.Team1_Back.dto.ReportGenerateRequestDTO;
import com.Team1_Back.dto.ReportScheduleCreateRequestDTO;
import com.Team1_Back.dto.ReportScheduleResponseDTO;
import com.Team1_Back.dto.ReportScheduleUpsertRequestDTO;
import com.Team1_Back.domain.ReportSchedule;
import com.Team1_Back.repository.ReportScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class ReportScheduleAdminService {

    private final ReportScheduleRepository repo;
    private final ReportService reportService; // ✅ runNow에서 필요
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Transactional
    public ReportScheduleResponseDTO create(ReportScheduleCreateRequestDTO req) {

        // 1) 타입 코드 검증 (ReportTypes에 존재해야 함)
        String typeCode = normalizeTypeCodeOrThrow(req.reportTypeId());

        // 2) enum 검증 (CreateRequest가 String으로 주면 여기서 변환)
        var scope = com.Team1_Back.domain.enums.DataScope.valueOf(req.dataScope());
        var fmt = com.Team1_Back.domain.enums.OutputFormat.valueOf(req.outputFormat());

        // 3) repeat rule -> cronExpr
        String cronExpr = buildCron(req.repeatType(), req.time(), req.daysOfWeek(), req.dayOfMonth());

        // 4) nextRunAt 계산
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime nextRunAt = calcNextRunAtOrThrow(cronExpr, now);

        ReportSchedule s = new ReportSchedule();
        s.setName(req.name());
        s.setReportTypeId(typeCode);   // ✅ 문자열 코드
        s.setDataScope(scope);
        s.setOutputFormat(fmt);
        s.setCronExpr(cronExpr);
        s.setIsEnabled(true);
        s.setRequestedBy((req.requestedBy() == null || req.requestedBy().isBlank()) ? "ADMIN" : req.requestedBy());
        s.setNextRunAt(nextRunAt);

        // (선택) 초기화
        s.setFailCount(0);
        s.setLastError(null);

        ReportSchedule saved = repo.save(s);
        return toResponse(saved);
    }


    private String buildCron(String repeatType, String timeHHmm, java.util.List<String> daysOfWeek, Integer dayOfMonth) {
        if (repeatType == null || repeatType.isBlank()) throw new IllegalArgumentException("repeatType is blank");
        if (timeHHmm == null || !timeHHmm.matches("^\\d{2}:\\d{2}$")) throw new IllegalArgumentException("time must be HH:mm");

        int hour = Integer.parseInt(timeHHmm.substring(0, 2));
        int min = Integer.parseInt(timeHHmm.substring(3, 5));
        if (hour < 0 || hour > 23 || min < 0 || min > 59) throw new IllegalArgumentException("invalid time");

        return switch (repeatType.trim().toUpperCase()) {
            case "DAILY" -> String.format("0 %d %d * * *", min, hour);

            case "WEEKLY" -> {
                if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                    throw new IllegalArgumentException("daysOfWeek required for WEEKLY");
                }
                String dow = String.join(",", daysOfWeek).toUpperCase(); // MON,WED
                yield String.format("0 %d %d * * %s", min, hour, dow);
            }

            case "MONTHLY" -> {
                if (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 28) {
                    throw new IllegalArgumentException("dayOfMonth must be 1~28 for MONTHLY");
                }
                yield String.format("0 %d %d %d * *", min, hour, dayOfMonth);
            }

            default -> throw new IllegalArgumentException("Unknown repeatType: " + repeatType);
        };
    }


    // ====== create()는 네가 이미 구현한 그대로 두고 ======

    // ✅ UPDATE
    @Transactional
    public ReportScheduleResponseDTO update(Long id, ReportScheduleUpsertRequestDTO req) {

        ReportSchedule s = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: id=" + id));

        // 1) reportTypeId(code) 검증
        String typeCode = normalizeTypeCodeOrThrow(req.reportTypeId());

        // 2) cronExpr 유효성 검증 + nextRunAt 재계산
        String cronExpr = req.cronExpr().trim();
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime nextRunAt = calcNextRunAtOrThrow(cronExpr, now);

        // 3) 필드 반영
        s.setName(req.name().trim());
        s.setReportTypeId(typeCode);
        s.setDataScope(req.dataScope());
        s.setOutputFormat(req.outputFormat());
        s.setPeriodRule(req.periodRule());
        s.setCronExpr(cronExpr);
        s.setIsEnabled(Boolean.TRUE.equals(req.enabled()));
        s.setNextRunAt(nextRunAt);

        // (선택) 관리자가 설정을 바꿨으니 실패 상태/에러는 리셋하는 게 보통 더 UX 좋음
        s.setFailCount(0);
        s.setLastError(null);

        ReportSchedule saved = repo.save(s);
        return toResponse(saved);
    }

    // ✅ RUN NOW (즉시 1회 실행 + nextRunAt은 cron 기준으로 재설정)
    @Transactional
    public ReportScheduleResponseDTO runNow(Long id) {

        ReportSchedule s = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: id=" + id));

        // 꺼져 있어도 관리자가 "지금 실행"은 할 수 있게 할지 정책 선택.
        // 여기서는 허용하되, 원하면 아래로 막아도 됨.
        // if (!Boolean.TRUE.equals(s.getIsEnabled())) throw new IllegalStateException("Schedule is disabled");

        // 1) reportTypeId(code) 검증 (DB가 깨져 있으면 여기서 바로 잡힘)
        String typeCode = normalizeTypeCodeOrThrow(s.getReportTypeId());

        // 2) generateInternal 호출용 req 생성
        ReportGenerateRequestDTO genReq = buildGenerateRequestFromSchedule(s, typeCode);

        LocalDateTime now = LocalDateTime.now(ZONE);

        try {
            var result = reportService.generateInternal(genReq);

            // 3) 성공 반영
            s.setLastRunAt(now);
            s.setLastJobId(result.reportId());
            s.setFailCount(0);
            s.setLastError(null);

        } catch (Exception e) {
            int nextFail = (s.getFailCount() == null ? 0 : s.getFailCount()) + 1;
            s.setFailCount(nextFail);
            s.setLastRunAt(now);
            s.setLastError(shortMsg(e));

            // runNow 실패는 "다음 실행"을 굳이 backoff로 미루지 않고 cron대로 두는게 보통 안전
            // (스케줄러 tick 로직에서 backoff를 하려면 거기서 처리)
        }

        // 4) nextRunAt은 cron 기준으로 재계산 (cron이 깨져있으면 update에서 막았어야 함)
        s.setNextRunAt(calcNextRunAtOrThrow(s.getCronExpr(), now));

        ReportSchedule saved = repo.save(s);
        return toResponse(saved);
    }

    // ====== 아래는 헬퍼들 ======

    private String normalizeTypeCodeOrThrow(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("reportTypeId is blank");
        String code = raw.trim();
        if (ReportTypes.find(code) == null) throw new IllegalArgumentException("Invalid reportTypeId(code): " + code);
        return code;
    }

    private LocalDateTime calcNextRunAtOrThrow(String cronExpr, LocalDateTime base) {
        try {
            CronExpression cron = CronExpression.parse(cronExpr);
            ZonedDateTime next = cron.next(base.atZone(ZONE));
            return (next == null) ? base.plusMinutes(5) : next.toLocalDateTime();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cronExpr: " + cronExpr, e);
        }
    }

    private ReportGenerateRequestDTO buildGenerateRequestFromSchedule(ReportSchedule s, String typeCode) {
        ReportGenerateRequestDTO.Filters f = new ReportGenerateRequestDTO.Filters();
        f.setFormat(s.getOutputFormat().name()); // "PDF"/"EXCEL"
        f.setDataScope(s.getDataScope().name()); // "DEPT"/"MY"/"ALL"
        f.setPeriod(resolvePeriodForRun(s));     // 아래 구현 참고

        ReportGenerateRequestDTO req = new ReportGenerateRequestDTO();
        req.setReportTypeId(typeCode); // ✅ 문자열 코드
        req.setFilters(f);
        return req;
    }

    // periodRule을 쓰는 구조면 여기서 결정
    private String resolvePeriodForRun(ReportSchedule s) {
        var rule = s.getPeriodRule();
        LocalDate today = LocalDate.now(ZONE);
        if (rule == null) return ym(today);

        return switch (rule) {
            case CURRENT_MONTH -> ym(today);
            case PREV_MONTH -> ym(today.minusMonths(1));
            case YESTERDAY -> today.minusDays(1).toString();
            case LAST_7_DAYS -> "LAST_7_DAYS";
        };
    }

    private String ym(LocalDate d) {
        return String.format("%04d-%02d", d.getYear(), d.getMonthValue());
    }

    private String shortMsg(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }

    private ReportScheduleResponseDTO toResponse(ReportSchedule s) {
        return new ReportScheduleResponseDTO(
                s.getId(),
                s.getName(),
                s.getReportTypeId(),     // ✅ String이어야 함 (Response DTO도 String으로!)
                s.getDataScope(),
                s.getOutputFormat(),
                s.getPeriodRule(),
                s.getCronExpr(),
                Boolean.TRUE.equals(s.getIsEnabled()),
                s.getLastRunAt(),
                s.getNextRunAt(),
                s.getLastJobId(),
                s.getFailCount() == null ? 0 : s.getFailCount(),
                s.getLastError()
        );
    }
}
