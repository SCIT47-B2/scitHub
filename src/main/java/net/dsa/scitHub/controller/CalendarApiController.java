package net.dsa.scitHub.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.EventDTO;
import net.dsa.scitHub.enums.Visibility;
import net.dsa.scitHub.security.AuthenticatedUser;
import net.dsa.scitHub.service.EventService;

// Calendar 비동기 처리를 위한 컨트롤러
@RestController // @Controller + @ResponseBody
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalendarApiController {

    // EventService 주입
    private final EventService es;

    /**
     * 일정 등록하기 위한 Controller
     * 
     * @param AuthenticatedUser
     * @param eventDTO
     * @return
     */
    @PostMapping("/calendar/events")
    public ResponseEntity<?> createEvent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody EventDTO eventDTO) {

        log.debug("회원 정보:{}", user);

        log.debug("폼 데이터 받음 : {}", eventDTO);

        EventDTO createdEvent = es.createEvent(eventDTO, user);

        // 생성된 이벤트에 접근할 수 있는 고유 URL(URI)을 만든다.
        URI location = URI.create(String.format("/calendar/events/%d", createdEvent.getEventId()));

        // 상태코드와 함께 location 헤더, 그리고 생성된 이벤트 객체를 응답으로 반환
        return ResponseEntity.created(location).body(createdEvent);
    }

    /**
     * 현재 사용자에게 보여져야 할 모든 이벤트를 조회하는 API
     * @param user 현재 로그인한 사용자 정보
     * @return ResponseEntity (200 OK 상태와 함께 필터링된 이벤트 목록 반환)
     */
    @GetMapping("/calendar/events")
    public ResponseEntity<List<EventDTO>> getInitialEvents(
                @AuthenticationPrincipal AuthenticatedUser user,
                @RequestParam(name = "showPublic", defaultValue = "true") boolean showPublic,
                @RequestParam(name = "showPrivate",defaultValue = "true") boolean showPrivate) {
        List<EventDTO> events = es.getVisibleEventsForUser(user, showPublic, showPrivate);
        
        return ResponseEntity.ok(events);
    }


    /**
     * 일정을 수정하는 메서드
     * 
     * @param eventId  URL 경로에서 추출한 eventId
     * @param eventDTO 요청 본문의 JSON 데이터를 매핑한 DTO 객체
     * @return 성공 여부를 담은 HTTP 응답
     */
    @PutMapping("/calendar/events/{eventId}") // PUT 요청이라서 PutMapping
    public ResponseEntity<EventDTO> updateEvent(
                    @AuthenticationPrincipal AuthenticatedUser user,
                    @PathVariable("eventId") Integer eventId,
                    @RequestBody EventDTO eventDTO
    ) {

        // 수정 시 권한을 점검하는 함수 호출
        es.verifyEventPermission(eventId, user);
        // 수정하는 함수 호출
        EventDTO updateEventDTO = es.updateEvent(eventId, eventDTO);

        // HTTP 상태 코드를 200 OK, body에 수정된 eventDTO를 담아서 보냄.
        return ResponseEntity.ok(updateEventDTO);
    }

    /**
     * 일정을 삭제하는 메서드
     * @param eventId URL 경로에서 추출한 삭제할 이벤트의 ID
     * @return HTTP 응답
     */
    @DeleteMapping("/calendar/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("eventId") Integer eventId
    ) {

        // 삭제시 올바른 권한인지 검증하는 함수.
        es.verifyEventPermission(eventId, user);
        // eventService에 있는 삭제 함수 호출
        es.deleteEvent(eventId);

        // 성공적으로 삭제되었음을 알리는 응답을 반환
        return ResponseEntity.ok().build();
    }
}
