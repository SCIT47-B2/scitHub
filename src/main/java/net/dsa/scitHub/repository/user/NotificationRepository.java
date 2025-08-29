package net.dsa.scitHub.repository.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.user.Notification;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    /** 사용자별 알림 조회 */
    List<Notification> findByUser_UserId(Integer userId);
    
    /** 사용자별 알림 조회 (페이징) */
    Page<Notification> findByUser_UserId(Integer userId, Pageable pageable);
    
    /** 읽지 않은 알림 수 조회 */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.userId = :userId AND n.isRead = false")
    Long countUnreadNotifications(@Param("userId") Integer userId);
    
    /** 알림을 읽음으로 표시 */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :notificationId")
    void markAsRead(@Param("notificationId") Integer notificationId);
    
    /** 사용자의 모든 알림을 읽음으로 표시 */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId")
    void markAllAsReadByUserId(@Param("userId") Integer userId);
    
    /** 읽지 않은 알림들 조회 */
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotifications(@Param("userId") Integer userId);
    
    /** 읽은 알림들 조회 */
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId AND n.isRead = true ORDER BY n.createdAt DESC")
    List<Notification> findReadNotifications(@Param("userId") Integer userId);
    
    /** 제목으로 검색 */
    List<Notification> findByTitleContaining(String title);
    
    /** 내용으로 검색 */
    List<Notification> findByContentContaining(String content);
    
    /** 특정 게시글 관련 알림 조회 */
    List<Notification> findByPost_PostId(Integer postId);
    
    /** 특정 댓글 관련 알림 조회 */
    List<Notification> findByComment_CommentId(Integer commentId);
    
    /** 특정 메시지 관련 알림 조회 */
    List<Notification> findByMessage_MessageId(Integer messageId);
    
    /** 특정 기간의 알림들 조회 */
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    List<Notification> findNotificationsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    /** 오래된 읽은 알림들 삭제를 위한 조회 */
    @Query("SELECT n FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    List<Notification> findOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}
