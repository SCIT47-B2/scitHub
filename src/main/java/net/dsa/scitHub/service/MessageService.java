package net.dsa.scitHub.service;

import lombok.RequiredArgsConstructor;
import net.dsa.scitHub.dto.MessageCreateRequestDto;
import net.dsa.scitHub.dto.MessageResponseDto;
import net.dsa.scitHub.entity.user.Message;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.enums.NotificationType;
import net.dsa.scitHub.repository.user.MessageRepository;
import net.dsa.scitHub.repository.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 새 메시지를 생성하고 저장합니다.
     *
     * @param requestDto     MessageCreateRequestDto - 메시지 생성 요청 정보
     * @param senderUsername String - 발신자 아이디
     * @return MessageResponseDto - 저장된 메시지 정보
     */
    @Transactional
    public MessageResponseDto sendMessage(MessageCreateRequestDto requestDto, String senderUsername) {
        User sender = findUserByUsername(senderUsername);
        User receiver = findUserByUsername(requestDto.getReceiverUsername());

        // 나에게 보내는 메시지였을 시에는 예외 발생
        if (sender.getUserId() == receiver.getUserId()) {
            throw new IllegalArgumentException("自分宛のメッセージは送信できません。");
        }

        Message message = requestDto.toEntity(sender, receiver);
        Message savedMessage = messageRepository.save(message);

        notificationService.send(receiver, NotificationType.NEW_MESSAGE, savedMessage);

        return MessageResponseDto.from(savedMessage);
    }

    /**
     * 특정 사용자가 받은 메시지 목록을 페이징하여 조회합니다.
     *
     * @param receiverUsername String - 수신자 아이디
     * @param searchType       String - 검색 유형
     * @param searchKeyword    String - 검색어
     * @param pageable         Pageable - 페이징 정보
     * @return Page<MessageResponseDto> - 받은 메시지 목록 페이지
     */
    public Page<MessageResponseDto> getReceivedMessages(String receiverUsername, String searchType, String searchKeyword, Pageable pageable) {
        User receiver = findUserByUsername(receiverUsername);
        Page<Message> messages;
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            messages = messageRepository.findByReceiver_UserId(receiver.getUserId(), pageable);
        } else {
            switch (searchType) {
                case "author":
                    messages = messageRepository.findReceivedMessagesBySenderName(receiver.getUserId(), searchKeyword, pageable);
                    break;
                case "title":
                    messages = messageRepository.findByReceiver_UserIdAndTitleContaining(receiver.getUserId(), searchKeyword, pageable);
                    break;
                case "content":
                    messages = messageRepository.findByReceiver_UserIdAndContentContaining(receiver.getUserId(), searchKeyword, pageable);
                    break;
                default: // "all"
                    messages = messageRepository.findReceivedMessagesWithKeyword(receiver.getUserId(), searchKeyword, pageable);
                    break;
            }
        }
        return messages.map(MessageResponseDto::from);
    }

    /**
     * 특정 사용자가 보낸 메시지 목록을 페이징하여 조회합니다.
     *
     * @param senderUsername String - 발신자 아이디 
     * @param searchType     String - 검색 유형
     * @param searchKeyword  String - 검색어
     * @param pageable       Pageable - 페이징 정보
     * @return Page<MessageResponseDto> - 보낸 메시지 목록 페이지
     */
    public Page<MessageResponseDto> getSentMessages(String senderUsername, String searchType, String searchKeyword, Pageable pageable) {
        User sender = findUserByUsername(senderUsername);
        Page<Message> messages;
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            messages = messageRepository.findBySender_UserId(sender.getUserId(), pageable);
        } else {
            switch (searchType) {
                case "author":
                    messages = messageRepository.findSentMessagesByReceiverName(sender.getUserId(), searchKeyword, pageable);
                    break;
                case "title":
                    messages = messageRepository.findBySender_UserIdAndTitleContaining(sender.getUserId(), searchKeyword, pageable);
                    break;
                case "content":
                    messages = messageRepository.findBySender_UserIdAndContentContaining(sender.getUserId(), searchKeyword, pageable);
                    break;
                default: // "all"
                    messages = messageRepository.findSentMessagesWithKeyword(sender.getUserId(), searchKeyword, pageable);
                    break;
            }
        }
        return messages.map(MessageResponseDto::from);
    }

    /**
     * 메시지 단건을 조회하고, 수신자가 처음 읽는 경우 '읽음' 상태로 변경합니다.
     *
     * @param messageId Integer - 조회할 메시지 ID
     * @param username  String - 현재 로그인한 사용자 아이디
     * @return MessageResponseDto - 메시지 상세 정보
     */
    @Transactional
    public MessageResponseDto getMessage(Integer messageId, String username) {
        User user = findUserByUsername(username);
        Message message = findMessageById(messageId);

        // 메시지 접근 권한 확인
        validateMessageAccess(message, user.getUserId());

        // 수신자가 읽었고, 아직 안 읽은 메시지라면 읽음 처리 (더티 체킹)
        // 수신자 정보가 존재하고, 현재 사용자가 수신자이며, 아직 안 읽은 메시지라면 읽음 처리
        if (message.getReceiver() != null && message.getReceiver().getUserId().equals(user.getUserId()) && !message.getIsRead()) {
            message.setIsRead(true);
        }

        return MessageResponseDto.from(message);
    }

    /**
     * 특정 사용자의 읽지 않은 메시지 개수를 조회합니다.
     *
     * @param receiverUsername String - 수신자 아이디
     * @return Long - 읽지 않은 메시지 개수
     */
    public Long countUnreadMessages(String receiverUsername) {
        User receiver = findUserByUsername(receiverUsername);
        return messageRepository.countUnreadMessages(receiver.getUserId());
    }

    /**
     * 메시지를 삭제합니다. 발신자 또는 수신자만 삭제할 수 있습니다.
     *
     * @param messageId Integer - 삭제할 메시지 ID
     * @param username  String - 현재 로그인한 사용자 아이디
     */
    @Transactional
    public void deleteMessage(Integer messageId, String username) {
        User user = findUserByUsername(username);
        Message message = findMessageById(messageId);
        validateMessageAccess(message, user.getUserId());

        // 현재는 DB에서 바로 삭제합니다.
        // TODO: '보낸사람에게서 삭제', '받은사람에게서 삭제' 상태를 관리하는 방식으로 변경을 고려해볼 수 있습니다.
        messageRepository.delete(message);
    }

    // --- Helper Methods ---

    /**
     * 사용자 아이디로 User 엔티티를 조회합니다. 없으면 예외를 발생시킵니다.
     *
     * @param username String - 조회할 사용자 아이디
     * @return User - 조회된 사용자 엔티티
     */
    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + username));
    }

    /**
     * 메시지 ID로 Message 엔티티를 조회합니다. 없으면 예외를 발생시킵니다.
     *
     * @param messageId Integer - 조회할 메시지 ID
     * @return Message - 조회된 메시지 엔티티
     */
    private Message findMessageById(Integer messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다. id=" + messageId));
    }

    /**
     * 현재 사용자가 메시지에 접근할 권한(발신자 또는 수신자)이 있는지 확인합니다.
     *
     * @param message Message - 확인할 메시지 엔티티
     * @param userId  Integer - 현재 사용자의 ID
     */
    private void validateMessageAccess(Message message, Integer userId) {
        // 발신자 또는 수신자 정보가 null일 수 있는 경우(사용자 탈퇴 등)를 대비
        boolean isSender = message.getSender() != null && message.getSender().getUserId().equals(userId);
        boolean isReceiver = message.getReceiver() != null && message.getReceiver().getUserId().equals(userId);

        if (!isSender && !isReceiver) {
            throw new SecurityException("메시지를 보거나 삭제할 권한이 없습니다.");
        }
    }
}
