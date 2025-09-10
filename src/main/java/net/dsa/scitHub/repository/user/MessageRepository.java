package net.dsa.scitHub.repository.user;

import net.dsa.scitHub.entity.user.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    /** 수신자별 메시지 조회 (페이징) */
    Page<Message> findByReceiver_UserId(Integer receiverId, Pageable pageable);

    /** 발신자별 메시지 조회 (페이징) */
    Page<Message> findBySender_UserId(Integer senderId, Pageable pageable);

    /** 읽지 않은 메시지 수 조회 */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.userId = :userId AND m.isRead = false")
    Long countUnreadMessages(@Param("userId") Integer userId);

    // --- 받은 쪽지 검색 ---
    Page<Message> findByReceiver_UserIdAndTitleContaining(Integer userId, String title, Pageable pageable);
    Page<Message> findByReceiver_UserIdAndContentContaining(Integer userId, String content, Pageable pageable);
    @Query("SELECT m FROM Message m JOIN m.sender s WHERE m.receiver.userId = :userId AND s.nameKor LIKE %:keyword%")
    Page<Message> findReceivedMessagesBySenderName(@Param("userId") Integer userId, @Param("keyword") String keyword, Pageable pageable);


    // --- 보낸 쪽지 검색 ---
    Page<Message> findBySender_UserIdAndTitleContaining(Integer userId, String title, Pageable pageable);
    Page<Message> findBySender_UserIdAndContentContaining(Integer userId, String content, Pageable pageable);
    @Query("SELECT m FROM Message m JOIN m.receiver r WHERE m.sender.userId = :userId AND r.nameKor LIKE %:keyword%")
    Page<Message> findSentMessagesByReceiverName(@Param("userId") Integer userId, @Param("keyword") String keyword, Pageable pageable);


    /**
     * 받은 쪽지 검색 (보낸사람 이름, 제목, 내용)
     * User 엔티티에 nameKor 필드가 있다고 가정합니다.
     */
    @Query("SELECT m FROM Message m JOIN m.sender s " +
           "WHERE m.receiver.userId = :userId " +
           // "AND m.deletedByReceiver = false " + // TODO: 논리적 삭제 구현 시 주석 해제
           "AND (m.title LIKE %:keyword% OR m.content LIKE %:keyword% OR s.nameKor LIKE %:keyword%)")
    Page<Message> findReceivedMessagesWithKeyword(@Param("userId") Integer userId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 보낸 쪽지 검색 (받는사람 이름, 제목, 내용)
     * User 엔티티에 nameKor 필드가 있다고 가정합니다.
     */
    @Query("SELECT m FROM Message m JOIN m.receiver r " +
           "WHERE m.sender.userId = :userId " +
           // "AND m.deletedBySender = false " + // TODO: 논리적 삭제 구현 시 주석 해제
           "AND (m.title LIKE %:keyword% OR m.content LIKE %:keyword% OR r.nameKor LIKE %:keyword%)")
    Page<Message> findSentMessagesWithKeyword(@Param("userId") Integer userId, @Param("keyword") String keyword, Pageable pageable);

    // --- 기타 메서드 (중복 제거 및 통합) ---

    /** 발신자별 메시지 조회 */
    List<Message> findBySender_UserId(Integer senderId);

    /** 수신자별 메시지 조회 */
    List<Message> findByReceiver_UserId(Integer receiverId);

    /** 메시지를 읽음으로 표시 */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.messageId = :messageId")
    void markAsRead(@Param("messageId") Integer messageId);

    /** 수신자의 모든 메시지를 읽음으로 표시 */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver.userId = :receiverId AND m.isRead = false")
    void markAllAsReadByReceiver(@Param("receiverId") Integer receiverId);

    /** 읽지 않은 메시지들 조회 */
    @Query("SELECT m FROM Message m WHERE m.receiver.userId = :receiverId AND m.isRead = false ORDER BY m.createdAt DESC")
    List<Message> findUnreadMessages(@Param("receiverId") Integer receiverId);

    /** 두 사용자 간의 메시지 대화 조회 */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.userId = :user1Id AND m.receiver.userId = :user2Id) OR " +
           "(m.sender.userId = :user2Id AND m.receiver.userId = :user1Id) " +
           "ORDER BY m.createdAt")
    List<Message> findConversationBetweenUsers(@Param("user1Id") Integer user1Id, @Param("user2Id") Integer user2Id);
}
