package com.Team1_Back.scheduler;

import com.Team1_Back.dto.ReportGenerateRequestDTO;
//import com.demo.report.dto.ReportGenerateRequest.ReportGenerateFilters; // ✅ 너 DTO 실제 클래스명에 맞게 수정
import com.Team1_Back.domain.ReportSchedule;
import com.Team1_Back.repository.ReportScheduleRepository;
import com.Team1_Back.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final ReportScheduleRepository reportScheduleRepository;
    private final ReportService reportService;
    private final RedisLock redisLock;

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private static final int BATCH_SIZE = 20;
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    @Scheduled(fixedDelay = 60000) // 1분마다
    public void tick() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        log.info("[SCHED TICK] now={}", now);


        List<ReportSchedule> targets =
                reportScheduleRepository.findByIsEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
                        now,
                        PageRequest.of(0, BATCH_SIZE)
                );

        if (targets.isEmpty()) return;

        log.info("[SCHED] targets.size={}", targets.size());
        for (ReportSchedule s : targets) {
            log.info("[SCHED] pick id={}, nextRunAt={}, enabled={}", s.getId(), s.getNextRunAt(), s.getIsEnabled());
        }



        for (ReportSchedule s0 : targets) {

            // 최신값 다시 로드
            ReportSchedule s = reportScheduleRepository.findById(s0.getId()).orElse(null);
            if (s == null) continue;

            // 실행 조건 재검사 (관리자가 껐거나 next_run이 바뀌었으면 스킵)
            if (!Boolean.TRUE.equals(s.getIsEnabled())) continue;
            if (s.getNextRunAt() != null && s.getNextRunAt().isAfter(now)) continue;

            String lockKey = "report:schedule:" + s.getId();
            String token = redisLock.tryLock(lockKey, LOCK_TTL);
            if (token == null) continue;

            try {
                ReportGenerateRequestDTO req = reqFrom(s);
                String rt = String.valueOf(s.getReportTypeId());
                log.warn("[SCHED] scheduleId={}, reportTypeId(raw)={}, reportTypeId(str)='{}'",
                        s.getId(), s.getReportTypeId(), rt);

                var result = reportService.generateInternal(req); // 네 시그니처 그대로


                s.setLastRunAt(now);
                s.setLastJobId(result.reportId());
                s.setFailCount(0);
                s.setLastError(null);

                // ✅ 관리자 cronExpr 수정도 반영됨
                s.setNextRunAt(calcNextRunAt(s.getCronExpr(), now));

                reportScheduleRepository.save(s);

            } catch (Exception e) {
                int nextFail = (s.getFailCount() == null ? 0 : s.getFailCount()) + 1;

                s.setFailCount(nextFail);
                s.setLastRunAt(now);
                s.setLastError(shortMsg(e));
                s.setNextRunAt(now.plusMinutes(Math.min(30, nextFail)));

                // (옵션) 5회 이상이면 자동 disable
                if (nextFail >= 5) s.setIsEnabled(false);

                reportScheduleRepository.save(s);

            } finally {
                redisLock.unlock(lockKey, token);
            }
        }
    }


    private ReportGenerateRequestDTO reqFrom(ReportSchedule s) {
        ReportGenerateRequestDTO.Filters filters =
                new ReportGenerateRequestDTO.Filters();

        filters.setFormat(s.getOutputFormat().name());
        filters.setDataScope(s.getDataScope().name());
        filters.setPeriod(resolvePeriod(s));

        ReportGenerateRequestDTO req = new ReportGenerateRequestDTO();
        req.setReportTypeId(String.valueOf(s.getReportTypeId()));
        req.setFilters(filters);

        return req;
    }

    private String resolvePeriod(ReportSchedule s) {
        // periodRule 없으면 기존처럼 이번달
        var rule = s.getPeriodRule();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        if (rule == null) return ym(today);

        return switch (rule) {
            case CURRENT_MONTH -> ym(today);
            case PREV_MONTH -> ym(today.minusMonths(1));
            case YESTERDAY -> today.minusDays(1).toString(); // "2025-12-24"
            case LAST_7_DAYS -> "LAST_7_DAYS";               // 너 period 파서 규칙에 맞춰 조정
        };
    }

    private String ym(LocalDate d) {
        int y = d.getYear();
        int m = d.getMonthValue();
        return String.format("%04d-%02d", y, m);
    }


    private LocalDateTime calcNextRunAt(String cronExpr, LocalDateTime base) {
        try {
            CronExpression cron = CronExpression.parse(cronExpr);
            ZonedDateTime z = base.atZone(ZONE);
            ZonedDateTime next = cron.next(z);
            if (next == null) return base.plusHours(1);
            return next.toLocalDateTime();
        } catch (Exception e) {
            return base.plusHours(1);
        }

    }
//
//    private String currentYm() {
//        LocalDateTime now = LocalDateTime.now();
//        int y = now.getYear();
//        int m = now.getMonthValue();
//        return String.format("%04d-%02d", y, m);
//    }

    private String shortMsg(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
