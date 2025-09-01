package net.dsa.scitHub.repository.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.user.Message;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    
    /** 발신자별 메시지 조회 */
    List<Message> findBySender_AccountId(Integer senderId);
    
    /** 수신자별 메시지 조회 */
    List<Message> findByReceiver_AccountId(Integer receiverId);
    
    /** 수신자별 메시지 조회 (페이징) */
    Page<Message> findByReceiver_AccountId(Integer receiverId, Pageable pageable);
    
    /** 발신자별 메시지 조회 (페이징) */
    Page<Message> findBySender_AccountId(Integer senderId, Pageable pageable);
    
    /** 읽지 않은 메시지 수 조회 */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.accountId = :receiverId AND m.isRead = false")
    Long countUnreadMessages(@Param("receiverId") Integer receiverId);
    
    /** 메시지를 읽음으로 표시 */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.messageId = :messageId")
    void markAsRead(@Param("messageId") Integer messageId);
    
    /** 수신자의 모든 메시지를 읽음으로 표시 */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver.accountId = :receiverId AND m.isRead = false")
    void markAllAsReadByReceiver(@Param("receiverId") Integer receiverId);
    
    /** 읽지 않은 메시지들 조회 */
    @Query("SELECT m FROM Message m WHERE m.receiver.accountId = :receiverId AND m.isRead = false ORDER BY m.createdAt DESC")
    List<Message> findUnreadMessages(@Param("receiverId") Integer receiverId);
    
    /** 제목으로 검색 */
    List<Message> findByTitleContaining(String title);
    
    /** 내용으로 검색 */
    List<Message> findByContentContaining(String content);
    
    /** 특정 기간의 메시지들 조회 */
    @Query("SELECT m FROM Message m WHERE m.createdAt BETWEEN :startDate AND :endDate")
    List<Message> findMessagesBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    /** 두 사용자 간의 메시지 대화 조회 */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.accountId = :user1Id AND m.receiver.accountId = :user2Id) OR " +
           "(m.sender.accountId = :user2Id AND m.receiver.accountId = :user1Id) " +
           "ORDER BY m.createdAt")
    List<Message> findConversationBetweenAccounts(@Param("user1Id") Integer user1Id, @Param("user2Id") Integer user2Id);
}
