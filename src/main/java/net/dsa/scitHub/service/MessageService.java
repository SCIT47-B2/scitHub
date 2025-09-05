package net.dsa.scitHub.service;

import lombok.RequiredArgsConstructor;
import net.dsa.scitHub.dto.MessageCreateRequestDto;
import net.dsa.scitHub.dto.MessageResponseDto;
import net.dsa.scitHub.entity.user.Message;
import net.dsa.scitHub.entity.user.User;
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

    @Transactional
    public MessageResponseDto sendMessage(MessageCreateRequestDto requestDto, String senderUsername) {
        User sender = findUserByUsername(senderUsername);
        User receiver = findUserByUsername(requestDto.getReceiverUsername());

        Message message = requestDto.toEntity(sender, receiver);
        Message savedMessage = messageRepository.save(message);

        // TODO: 수신자에게 알림을 보내는 로직 추가 (NotificationService 사용)

        return MessageResponseDto.from(savedMessage);
    }

    public Page<MessageResponseDto> getReceivedMessages(String receiverUsername, Pageable pageable) {
        User receiver = findUserByUsername(receiverUsername);
        Page<Message> messages = messageRepository.findByReceiver_UserId(receiver.getUserId(), pageable);
        return messages.map(MessageResponseDto::from);
    }

    public Page<MessageResponseDto> getSentMessages(String senderUsername, Pageable pageable) {
        User sender = findUserByUsername(senderUsername);
        Page<Message> messages = messageRepository.findBySender_UserId(sender.getUserId(), pageable);
        return messages.map(MessageResponseDto::from);
    }

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

    public Long countUnreadMessages(String receiverUsername) {
        User receiver = findUserByUsername(receiverUsername);
        return messageRepository.countUnreadMessages(receiver.getUserId());
    }

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

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + username));
    }

    private Message findMessageById(Integer messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다. id=" + messageId));
    }

    private void validateMessageAccess(Message message, Integer userId) {
        // 발신자 또는 수신자 정보가 null일 수 있는 경우(사용자 탈퇴 등)를 대비
        boolean isSender = message.getSender() != null && message.getSender().getUserId().equals(userId);
        boolean isReceiver = message.getReceiver() != null && message.getReceiver().getUserId().equals(userId);

        if (!isSender && !isReceiver) {
            throw new SecurityException("메시지를 보거나 삭제할 권한이 없습니다.");
        }
    }
}
