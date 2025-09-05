package net.dsa.scitHub.controller;

import lombok.RequiredArgsConstructor;
import net.dsa.scitHub.dto.MessageCreateRequestDto;
import net.dsa.scitHub.dto.MessageResponseDto;
import net.dsa.scitHub.service.MessageService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/mypage/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 메시지 전송
     */
    @PostMapping
    public ResponseEntity<MessageResponseDto> sendMessage(
            @RequestBody MessageCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String senderUsername = userDetails.getUsername();

        MessageResponseDto responseDto = messageService.sendMessage(requestDto, senderUsername);
        return ResponseEntity.created(URI.create("/mypage/api/messages/" + responseDto.getMessageId())).body(responseDto);
    }

    /**
     * 받은 메시지 목록 조회
     */
    @GetMapping("/received")
    public ResponseEntity<Page<MessageResponseDto>> getReceivedMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String receiverUsername = userDetails.getUsername();
        Page<MessageResponseDto> messages = messageService.getReceivedMessages(receiverUsername, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 보낸 메시지 목록 조회
     */
    @GetMapping("/sent")
    public ResponseEntity<Page<MessageResponseDto>> getSentMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String senderUsername = userDetails.getUsername();
        Page<MessageResponseDto> messages = messageService.getSentMessages(senderUsername, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 메시지 상세 조회
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponseDto> getMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails.getUsername();
        MessageResponseDto message = messageService.getMessage(messageId, username);
        return ResponseEntity.ok(message);
    }

    /**
     * 메시지 삭제
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails.getUsername();
        messageService.deleteMessage(messageId, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * 읽지 않은 메시지 수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> countUnreadMessages(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String receiverUsername = userDetails.getUsername();
        Long count = messageService.countUnreadMessages(receiverUsername);
        return ResponseEntity.ok(count);
    }
}
