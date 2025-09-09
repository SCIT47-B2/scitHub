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
     * 새 메시지를 전송합니다.
     *
     * @param requestDto  MessageCreateRequestDto - 메시지 생성 요청 정보 (수신자, 제목, 내용)
     * @param userDetails UserDetails - 현재 로그인한 사용자 정보
     * @return ResponseEntity<MessageResponseDto> - 생성된 메시지 정보와 201 Created 상태
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
     * 현재 로그인한 사용자가 받은 메시지 목록을 조회합니다.
     *
     * @param userDetails UserDetails - 현재 로그인한 사용자 정보
     * @param pageable    Pageable - 페이징 정보 (size, sort 등)
     * @return ResponseEntity<Page<MessageResponseDto>> - 받은 메시지 목록 페이지
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
     * 현재 로그인한 사용자가 보낸 메시지 목록을 조회합니다.
     *
     * @param userDetails UserDetails - 현재 로그인한 사용자 정보
     * @param pageable    Pageable - 페이징 정보 (size, sort 등)
     * @return ResponseEntity<Page<MessageResponseDto>> - 보낸 메시지 목록 페이지
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
     * 메시지 한 건을 상세 조회합니다.
     *
     * @param messageId   Integer - 조회할 메시지 ID
     * @param userDetails UserDetails - 현재 로그인한 사용자 정보
     * @return ResponseEntity<MessageResponseDto> - 메시지 상세 정보
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponseDto> getMessage(
            @PathVariable("messageId") Integer messageId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails.getUsername();
        MessageResponseDto message = messageService.getMessage(messageId, username);
        return ResponseEntity.ok(message);
    }

    /**
     * 메시지를 삭제합니다.
     *
     * @param messageId   Integer - 삭제할 메시지 ID
     * @param userDetails UserDetails - 현재 로그인한 사용자 정보
     * @return ResponseEntity<Void> - 204 No Content 상태
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable("messageId") Integer messageId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails.getUsername();
        messageService.deleteMessage(messageId, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * 현재 로그인한 사용자의 읽지 않은 메시지 수를 조회합니다.
     *
     * @param userDetails UserDetails - 현재 로그인한 사용자 정보
     * @return ResponseEntity<Long> - 읽지 않은 메시지 개수
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
