package net.dsa.scitHub.repository.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.user.Notification;
import net.dsa.scitHub.entity.user.User;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    /**
     * 특정 사용자의 읽지 않은 알림 개수를 조회
     * @param user 알림의 주인인 사용자
     * @return 읽지 않은 알림의 수
     */
    long countByUserAndIsReadFalse(User user);

    /**
     * 특정 사용자의 알림 목록을 최신순으로 조회 (페이징 처리)
     * @param user 알림의 주인인 사용자
     * @param pageable 페이징 정보
     * @return 알림 목록
     */
    List<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findAllByUserAndIsReadFalse(User user);
}
