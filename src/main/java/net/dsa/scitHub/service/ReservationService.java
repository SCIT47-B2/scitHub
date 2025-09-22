package net.dsa.scitHub.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.ClassroomDTO;
import net.dsa.scitHub.dto.ReservationDTO;
import net.dsa.scitHub.dto.ReservationRequestDTO;
import net.dsa.scitHub.entity.classroom.Classroom;
import net.dsa.scitHub.entity.reservation.Reservation;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.repository.classroom.ClassroomRepository;
import net.dsa.scitHub.repository.reservation.ReservationRepository;
import net.dsa.scitHub.repository.user.UserRepository;
import net.dsa.scitHub.security.AuthenticatedUser;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReservationService {
    private final ReservationRepository rr;
    private final UserRepository ur;
    private final ClassroomRepository cr;

    /**
     * 새로운 예약을 생성하는 메서드
     * 
     * @param userDetails           인증된 사용자 정보
     * @param reservationRequestDTO 예약 요청 데이터
     * @return 생성된 예약 정보를 담은 ReservationDTO
     */
    public ReservationDTO createReservation(AuthenticatedUser userDetails,
            ReservationRequestDTO reservationRequestDTO) {

        // user 찾기
        User user = ur.findByUsername(userDetails.getId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // classroom 찾기
        Classroom classroom = cr.findById(reservationRequestDTO.getClassroomId())
                .orElseThrow(() -> new EntityNotFoundException("강의실을 찾을 수 없습니다."));

        // timeSlotId를 실제 예약 시작 및 종료 시간(LocalDateTime)으로 변환
        Map<String, LocalDateTime> times = calculateStartAndEndTimes(reservationRequestDTO.getTimeSlotId());
        LocalDateTime startAt = times.get("startAt");
        LocalDateTime endAt = times.get("endAt");

        boolean alreadyReserved = rr.existsByUser_UserIdAndStartAt(user.getUserId(),
                startAt);

        if (alreadyReserved) {
            throw new IllegalStateException("既にその時間帯に他の教室を予約しています。");
        }

        // 새로운 Reservation 엔티티 생성
        Reservation newReservation = Reservation.builder()
                .user(user)
                .classroom(classroom)
                .startAt(startAt)
                .endAt(endAt)
                .build();
        // DB에 저장
        Reservation savedReservation = rr.save(newReservation);

        // 저장된 엔티티를 ReservationDTO로 변환하여 반환합니다.
        return ReservationDTO.convertToReservationDTO(savedReservation);
    }

    /**
     * timeSlotId(1~4)를 기반으로 실제 예약 시작 시간 및 종료 시간을 계산하는 메서드
     * 
     * @param timeSlotId 1~4 사이의 정수
     * @return Map<String, LocalDateTime> startAt, endAt의 키를 가지는 맵
     */
    private Map<String, LocalDateTime> calculateStartAndEndTimes(Integer timeSlotId) {
        if (timeSlotId < 1 || timeSlotId > 4) {
            throw new IllegalArgumentException("잘못된 시간 슬롯 ID입니다.");
        }

        // 현재 날짜만 가져옴 (시간 정보 없음)
        LocalDate today = LocalDate.now();

        // 슬롯 번호에 따른 시작 시간 계산
        int startHour = timeSlotId + 17; // 1번 슬롯 -> 18시, 2번 슬롯 -> 19시 ...

        // 시작시간과 종료시간 만들기
        LocalDateTime startAt = today.atTime(startHour, 0);
        LocalDateTime endAt = startAt.plusHours(1);

        return Map.of("startAt", startAt, "endAt", endAt);
    }

    /**
     * 특정 강의실의 오늘 하루 예약 현황을 조회하여 Map 형태로 반환하는 메서드
     * 
     * @param roomId      조회할 강의실 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return Key: timeSlotId, Value: ReservationDTO인 Map
     */
    public Map<Integer, ReservationDTO> getReservationSlotsForClassroom(Integer roomId, AuthenticatedUser userDetails) {

        // 단순히 오늘 시작(00:00:00)과 끝 시간(23:59:59)을 계산
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        // Repository를 호출하여 오늘 하루 동안 해당 강의실의 모든 예약을 DB에서 조회 (쿼리 메소드 사용)
        List<Reservation> reservationsToday = rr.findAllByClassroom_ClassroomIdAndStartAtBetween(roomId, startOfDay,
                endOfDay);

        // 반환할 Map을 생성
        Map<Integer, ReservationDTO> reservationMap = new HashMap<>();

        // 조회된 예약 목록을 반복하면서 DTO로 변환하고, Map에 추가
        for (Reservation reservation : reservationsToday) {
            ReservationDTO dto = ReservationDTO.convertToReservationDTO(reservation);
            reservationMap.put(dto.getTimeSlotId(), dto);
        }

        // 완성된 Map 반환
        return reservationMap;
    }

    /**
     * 특정 예약을 취소(삭제)하는 API
     * 
     * @param reservationId 취소할 예약의 ID
     * @param userDetails   현재 로그인한 사용자
     */
    public void cancelReservation(Integer reservationId, AuthenticatedUser userDetails) {
        // 예약을 찾습니다.
        Reservation reservation = rr.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("찾는 예약이 없습니다."));

        // 권한 검사, 본인의 예약이 맞는지, 혹은 관리자인지 확인
        if (!reservation.getUser().getUsername().equals(userDetails.getId()) &&
                !userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            throw new AccessDeniedException("예약을 취소할 권한이 없습니다.");
        }

        // 권한이 확인되면 예약을 삭제
        rr.delete(reservation);
    }

    // ------------- 강의실 관련
    /**
     * 토글 시켜주는 함수
     */
    public boolean toggleClassroomStatus(Integer roomId) {
        // 토글할 강의실 Entity 불러오기
        Classroom classroom = cr.findById(roomId)
                .orElseThrow(() -> new RuntimeException("강의실을 찾을 수 없습니다."));

        // 예약이 있으면 비활성화를 막는 로직
        if (classroom.getIsActive()) {
            // 해당 강의실에 현재 시간 이후의 예약이 있는지 확인
            boolean hasReservations = rr.existsByClassroom_ClassroomIdAndStartAtAfter(roomId, LocalDateTime.now());

            // 예약이 존재한다면, 예외 발생
            if (hasReservations) {
                throw new IllegalStateException("予約がある教室は無効化できません。");
            }
        }
        // 검증을 통과했거나, 비활성 상태를 활성 상태로 바꾸는 경우에는 상태 토글
        classroom.setIsActive(!classroom.getIsActive());
        cr.save(classroom);

        return classroom.getIsActive();
    }

    /**
     * 강의실 엔티티 반환
     */
    public Classroom findById(Integer roomId) {
        return cr.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("강의실을 찾을 수 없습니다: " + roomId));
    }

    /**
     * 모든 강의실 정보를 가져오는 메서드
     */
    public Map<Integer, ClassroomDTO> getAllClassrooms() {
        List<Classroom> listClassroom = cr.findAll();
        Map<Integer, ClassroomDTO> classroomMap = new HashMap<>();

        for (Classroom classroom : listClassroom) {
            ClassroomDTO classroomDTO = ClassroomDTO.convertToClassroomDTO(classroom);

            classroomMap.put(classroomDTO.getClassroomId(), classroomDTO);
        }
        return classroomMap;
    }
}
