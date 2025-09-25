package net.dsa.scitHub.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.EventDTO;
import net.dsa.scitHub.entity.schedule.Event;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.enums.NotificationType;
import net.dsa.scitHub.enums.Visibility;
import net.dsa.scitHub.repository.schedule.EventRepository;
import net.dsa.scitHub.repository.user.UserRepository;
import net.dsa.scitHub.security.AuthenticatedUser;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EventService {

    // DB 저장을 위한 Repository 주입
    private final EventRepository er;
    private final UserRepository ur;
    private final NotificationService ns;

    /**
     * 일정 등록 처리 함수
     */
    public EventDTO createEvent(EventDTO eventDTO, AuthenticatedUser user) {
        log.debug("createEvent 서비스 메서드 호출됨. 전달받은 DTO 내용: {}", eventDTO.toString());

        // 관리자 플래그
        boolean isAdmin = user.getRoleName().equals("ROLE_ADMIN");

        // ADMIN -> PUBLIC 일정, USER -> PRIVATE 일정
        if(isAdmin){
            eventDTO.setVisibility(Visibility.valueOf("PUBLIC"));
        } else {
            eventDTO.setVisibility(Visibility.valueOf("PRIVATE"));
        }
        eventDTO.setUserId(user.getId());

        log.debug("@@eventDTO 확인 {}", eventDTO);

        User eventUser = ur.findByUsername(user.getId()).orElseThrow(
            () -> new EntityNotFoundException("該当するIDが見つかりませんでした。")
        );

        Event event = Event.builder()
                        .visibility(eventDTO.getVisibility())
                        .user(eventUser)
                        .title(eventDTO.getTitle())
                        .content(eventDTO.getContent())
                        .startAt(eventDTO.getStart())
                        .endAt(eventDTO.getEnd())
                        .isAllDay(eventDTO.getAllDay())
                        .build();
        Event savedEvent = er.save(event);
        log.debug("ENTITY최종 저장될 eventENTITY: {}", savedEvent.toString());

        // 알림 전송
        // 생성자가 관리자이고, 생성된 일정이 전체 공개(PUBLIC) 일정인 경우에만 알림 전송
        if (isAdmin && savedEvent.getVisibility() == Visibility.PUBLIC) {
            List<User> allUsersExceptCreator = ur.findByUserIdNot(eventUser.getUserId());
            for (User recipient : allUsersExceptCreator) {
                ns.send(recipient, NotificationType.NEW_EVENT, savedEvent);
            }
            log.info("관리자가 전체 일정을 등록하여 {}명에게 알림을 전송했습니다.", allUsersExceptCreator.size());
        }

        return EventDTO.convertToEventDTO(savedEvent);
    }

    /**
     * 일정을 전부 불러오는 함수
     * @return List<EventDTO>
     */
    public List<EventDTO> getAllEvent() {

        List<Event> entityList = er.findAll();
        List<EventDTO> dtoList = new ArrayList<>();

        for(Event entity : entityList){
            EventDTO dto = EventDTO.builder()
                                .eventId(entity.getEventId())
                                .visibility(entity.getVisibility())
                                .userId(entity.getUser() != null ? entity.getUser().getUsername() : null)
                                .title(entity.getTitle())
                                .content(entity.getContent())
                                .start(entity.getStartAt())
                                .end(entity.getEndAt())
                                .allDay(entity.getIsAllDay())
                                .build();
            dtoList.add(dto);
        }

        return dtoList;
    }
    /**
     * 현재 로그인한 사용자에게 보여져야 할 이벤트들을 조회하는 메서드
     * (모든 전체일정 + 본인의 개인일정)
     * @param userDetails 현재 로그인한 사용자의 정보
     * @return 필터링된 이벤트 DTO 목록
     */
    public List<EventDTO> getVisibleEventsForUser(
        AuthenticatedUser userDetails, boolean showPublic, boolean showPrivate) {

        // AuthenticatedUser가 null이 아니라면 username을 보관
        String currentUserId = (userDetails != null) ? userDetails.getUsername() : null;
        // 여기부터 해서repository query문 수정해야 함.

        List<Event> entityList = er.findFilteredEvents(currentUserId, showPublic, showPrivate, Visibility.PUBLIC, Visibility.PRIVATE);

        // 조회된 엔티티 리스트를 DTO 리스트로 변환하여 반환.
        return entityList.stream()
                        .map(EventDTO::convertToEventDTO)
                        .toList();
    }

    /**
     * 단일 event 조회 함수
     * @return eventDTO
     */
    public EventDTO getEvent(Integer eventId) {
        Event event = er.findById(eventId)
                            .orElseThrow(() -> new EntityNotFoundException("아이디가 없습니다."));

        EventDTO eventDTO = EventDTO.convertToEventDTO(event);

        return eventDTO;
    }

    /**
     * event 수정 함수
     * @param eventId  호출 URL 경로에서 뽑아낸 eventId
     * @param eventDTO Form에서 작성한 값을 담은 eventDTO
     * @request updateEventDTO 업데이터 된 eventDTO를 반환
     */
    public EventDTO updateEvent(Integer eventId, EventDTO eventDTO) {

        // 업데이트 될 event의 Entity 호출
        Event eventEntity = er.findById(eventId).orElseThrow(() -> new EntityNotFoundException("해당 아이디가 없습니다."));

        // 호출된 Entity에 직접 값을 바꿔주면 Spring을 통해서 자동으로 변경 save까지 해줌.
        if (eventDTO.getTitle() != null) {
            eventEntity.setTitle(eventDTO.getTitle());
        }
        if (eventDTO.getContent() != null) {
            eventEntity.setContent(eventDTO.getContent());
        }

        eventEntity.setStartAt(eventDTO.getStart());
        eventEntity.setEndAt(eventDTO.getEnd());
        eventEntity.setIsAllDay(eventDTO.getAllDay());

        // controller에 넘겨주기 위해 DTO로 변환
        EventDTO updateEventDTO = EventDTO.convertToEventDTO(eventEntity);

        return updateEventDTO;
    }

    /**
     * 이벤트 삭제 함수
     * @param eventId
     */
    public void deleteEvent(Integer eventId) {

        // 삭제하기 전 eventId로 정말로 데이터가 존재하는 지 확인
        if (!er.existsById(eventId)) {
            throw new EntityNotFoundException("삭제할 이벤트를 찾을 수 없습니다. ID : " + eventId);
        }

        // event 삭제
        er.deleteById(eventId);
    }

    /**
     * 이벤트에 대한 현재 사용자의 권한을 확인하는 메서드
     * 전체 일정은 -> 관리자만 가능
     * 개인 일정은 -> 본인만 가능
     * @param eventId 검사할 이벤트 Id
     */
    public void verifyEventPermission(Integer eventId, AuthenticatedUser user) {
        // 이벤트 정보를 조회
        Event event = er.findById(eventId).orElseThrow(()->new EntityNotFoundException("해당 이벤트를 찾을 수 없습니다."));

        // 로그인한 사용자의 정보를 조회합니다.
        String loginUserId = user.getUsername();
        boolean isAdmin = false;


        // 관리자가 맞는지 확인. (전제. 관리자는 전체일정만 만들 수 있고, 일반사용자는 개인일정만 만들 수 있다.)
            // GrantedAuthority는 인터페이스
            // SimpleGrantedAuthority 권한의 이름을 담는 단순한 상자
            // getAuthorities() 함수가 userDetails 안에 roleName에 있는 것을 Spring 규칙에 맞게 변환하여
            //    SimpleGrntedAuthority가 들어있는 List 형태로 반환
        // userDetails의 권한 정보를 하나씩 추출
        for (GrantedAuthority authority : user.getAuthorities()) {
            if( authority.getAuthority().equals("ROLE_ADMIN")) {

                // 만약 ROLE_ADMIN과 같다면 true를 주고 반복문 탈출.
                isAdmin = true;
                break;
            }
        }

        // 이벤트의 유저와 현재 로그인된 유저가 같은지 검증.
        boolean isOwner = event.getUser().getUsername().equals(loginUserId);
        Visibility eventVisibility = event.getVisibility(); // "PUBLIC" or "PRIVATE"

        // -- 최종 권한 검증

        // 조건 1 : 관리자는 전체일정만 수정 삭제할 수 있다.
        if (isAdmin && Visibility.PUBLIC == eventVisibility) {
            return; // 허용
        }

        // 조건 2 : 일반 사용자는 로그인 된 본인의 개인일정만 수정 삭제할 수 있다.
        if (!isAdmin && Visibility.PRIVATE == eventVisibility && isOwner) {
            return; // 허용
        }

        // 둘 다 아니라면 예외를 발생.
        throw new AccessDeniedException("해당 이벤트에 대한 수정/삭제 권한이 없습니다.");
    }

    // username을 userId로 변환해주는 함수
    public Integer convertUsernameToUserId(String username) {
        User user = ur.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
        Integer userId = user.getUserId();
        return userId;
    }
}
