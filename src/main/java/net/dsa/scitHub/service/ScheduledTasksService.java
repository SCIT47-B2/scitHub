package net.dsa.scitHub.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.repository.reservation.ReservationRepository;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledTasksService {

    private final ReservationRepository rr;

    /**
     * 매일 새벽 4시에 실행되어, '오늘' 이전의모든 예약을 삭제합니다.
     */
    @Scheduled(cron = "0 0 4 * * *") // 초 분 시 일 월 요일

    public void cleanupOldReservations() {
        log.info("오래된 예약 데이터 정리 작업을 시작합니다.");
        LocalDateTime today = LocalDate.now().atStartOfDay(); // 오늘 날짜의 00:00:00
        rr.deleteReservationsBefore(today);
        log.info("오래된 예약 데이터 정리 작업을 완료했습니다.");
    }
}
