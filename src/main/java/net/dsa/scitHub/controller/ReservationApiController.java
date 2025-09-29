package net.dsa.scitHub.controller;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.ReservationDTO;
import net.dsa.scitHub.dto.ReservationRequestDTO;
import net.dsa.scitHub.entity.classroom.Classroom;
import net.dsa.scitHub.security.AuthenticatedUser;
import net.dsa.scitHub.service.ReservationService;

@Controller
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationApiController {
    private final ReservationService rs;

    /**
     * 새로운 예약을 생성하는 메서드
     */
    @PostMapping("/reservations")
    @ResponseBody
    public ResponseEntity<?> createReservation(
            @AuthenticationPrincipal AuthenticatedUser userDetails,
            @RequestBody ReservationRequestDTO reservationRequestDTO) {

        // --- 디버깅 코드 추가 ---
        System.out.println("--- Controller에 들어온 데이터 확인 ---");
        System.out.println("userDetails: " + userDetails);
        System.out.println("Request DTO: " + reservationRequestDTO);
        // -------------------------

        // Service 호출
        ReservationDTO reservationDTO = rs.createReservation(userDetails, reservationRequestDTO);

        // HTTP 201 Created 응답 반환, reservationDTO 응답 본문에 포함
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationDTO);
    }

    /**
     * 특정 강의실의 예약 현황 모달에 들어갈 HTML 조각을 반환하는 API
     */
    @GetMapping("/reservations/modal/{roomId}")
    public String getReservationModal(@PathVariable("roomId") Integer roomId,
            @AuthenticationPrincipal AuthenticatedUser userDetails, Model model) {
        // 서비스 계층을 호출하여 해당 강의실의 예약 현황 데이터를 Map 형태로 가져옴
        Map<Integer, ReservationDTO> timeSlots = rs.getReservationSlotsForClassroom(roomId, userDetails);

        // 강의실 정보 조회
        Classroom classroom = rs.findById(roomId);

        // 관리자 권한 체크 추가
        boolean isAdmin = false;
        for (GrantedAuthority auth : userDetails.getAuthorities()) {
            if (auth.getAuthority().equals("ROLE_ADMIN")) {
                isAdmin = true;
                break;
            }
        }

        // Thymeleaf 탬플릿에서 사용할 데이터들을 Model 객체에 추가
        model.addAttribute("timeSlots", timeSlots); // 예약 현황 데이터 Map
        model.addAttribute("roomName", classroom.getName()); // 강의실 이름
        model.addAttribute("allSlotIds", List.of(1, 2, 3, 4)); // 전체 시간 슬롯 ID
        model.addAttribute("roomId", roomId); // 현재 조회 중인 강의실 Id
        model.addAttribute("currentUser", userDetails); // 현재 로그인한 사용자 정보
        model.addAttribute("isAdmin", isAdmin); // 관리자 여부
        model.addAttribute("isRoomActive", classroom.getIsActive()); // 강의실 활성화 상태
        model.addAttribute("currentHour", LocalTime.now().getHour()); // 현재 '시(hour)' 정보를 모델에 추가
        // 렌더링할 HTML 조각 파일의 경로를 반환합니다.
        return "fragments/reservationModal";
    }

    /**
     * 특정 예약을 취소(삭제)하는 API
     * 
     * @param reservationId 취소할 예약의 ID
     * @param userDetails   현재 로그인한 사용자 정보
     * @return HTTP 상태 정보
     */
    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable("reservationId") Integer reservationId,
            @AuthenticationPrincipal AuthenticatedUser userDetails) {

        rs.cancelReservation(reservationId, userDetails);

        // 성공 HTTP
        return ResponseEntity.ok().build();
    }

    // ------------- 강의실 관련
    /**
     * 강의실 활성화 비활성화 토글 함수
     */
    @PutMapping("/classrooms/{roomId}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleClassroomStatus(@PathVariable("roomId") Integer roomId) {
        try {
            boolean newStatus = rs.toggleClassroomStatus(roomId);
            Map<String, Object> response = new HashMap<>();
            response.put("classroomId", roomId);
            response.put("isActive", newStatus);
            response.put("message", newStatus ? "教室が有効化されました。" : "教室が無効化されました。");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("상태 변경 실패");
        }
    }

}
