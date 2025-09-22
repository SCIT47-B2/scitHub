package net.dsa.scitHub.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.reservation.Reservation;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    /** 사용자별 예약 조회 */
    List<Reservation> findByUser_UserId(Integer userId);

    /** 강의실별 예약 조회 */
    List<Reservation> findByClassroom_ClassroomId(Integer classroomId);

    /** 시간 충돌하는 예약 조회 */
    @Query("SELECT r FROM Reservation r WHERE r.classroom.classroomId = :classroomId AND r.startAt < :endAt AND r.endAt > :startAt")
    List<Reservation> findConflictingReservations(@Param("classroomId") Integer classroomId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    /** 특정 날짜 범위의 예약 조회 */
    @Query("SELECT r FROM Reservation r WHERE r.startAt >= :start AND r.endAt <= :end")
    List<Reservation> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 현재 진행 중인 예약 조회 */
    @Query("SELECT r FROM Reservation r WHERE r.startAt <= :now AND r.endAt > :now")
    List<Reservation> findOngoingReservations(@Param("now") LocalDateTime now);

    /** 다가올 예약 조회 */
    @Query("SELECT r FROM Reservation r WHERE r.startAt > :now ORDER BY r.startAt")
    List<Reservation> findUpcomingReservations(@Param("now") LocalDateTime now);

    /** 지난 예약 조회 */
    @Query("SELECT r FROM Reservation r WHERE r.endAt < :now ORDER BY r.endAt DESC")
    List<Reservation> findPastReservations(@Param("now") LocalDateTime now);

    /** 사용자별 다가올 예약 조회 */
    @Query("SELECT r FROM Reservation r WHERE r.user.userId = :userId AND r.startAt > :now ORDER BY r.startAt")
    List<Reservation> findUpcomingReservationsByUser(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    /** 강의실별 예약 수 조회 */
    @Query("SELECT r.classroom.classroomId, COUNT(r) FROM Reservation r GROUP BY r.classroom.classroomId ORDER BY COUNT(r) DESC")
    List<Object[]> countReservationsByClassroom();

    /** 사용자별 예약 수 조회 */
    @Query("SELECT r.user.userId, COUNT(r) FROM Reservation r GROUP BY r.user.userId ORDER BY COUNT(r) DESC")
    List<Object[]> countReservationsByUser();

    /** 특정 시간대의 강의실 가용성 체크 */
    @Query("SELECT c.classroomId FROM Classroom c WHERE c.isActive = true AND c.classroomId NOT IN " +
            "(SELECT r.classroom.classroomId FROM Reservation r WHERE r.startAt < :endTime AND r.endAt > :startTime)")
    List<Integer> findAvailableClassroomIds(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /** 특정 기간의 예약 통계 */
    @Query("SELECT DATE(r.startAt), COUNT(r) FROM Reservation r WHERE r.startAt BETWEEN :startDate AND :endDate GROUP BY DATE(r.startAt) ORDER BY DATE(r.startAt)")
    List<Object[]> findReservationStatsByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 강의실ID와 기간(시작일, 종료일)을 기준으로 모든 예약을 조회하는 메소드
     * findAllBy 모든 데이터를 찾아라, 조건은
     * Classroom_ClassroomID : Reservation 엔티티 안의 Classroom 객체의 classroomId 필드가일치하는것
     * And
     * StartAtBetween : Reservation 엔티티 안의 startAt 필드가 주어진 시작일과 종료일 사이에 있는 것.
     */
    List<Reservation> findAllByClassroom_ClassroomIdAndStartAtBetween(Integer classroomId, LocalDateTime start,
            LocalDateTime end);

    /**
     * 특정 강의실에 현재 시간 이후로 시작하는 예약이 존재하는지 확인하는 메서드
     * 
     * @param classroomId 확인할 강의실의 ID
     * @param currentTime 현재 시간
     * @return 예약이 하나라도 존재하면 true, 없으면 false
     */
    boolean existsByClassroom_ClassroomIdAndStartAtAfter(Integer classroomId, LocalDateTime currentTime);

    /**
     * 특정 사용자가 특정 시작 시간을 가진 예약이 있는지 확인하는 메서드
     * 
     * @param userId  확인할 사용자의 ID
     * @param startAt 확인할 예약 시작 시간
     * @return 예약이 존재하면 true, 없으면 false
     */
    boolean existsByUser_UserIdAndStartAt(Integer userId, LocalDateTime startAt);

    /**
     * 특정 시간 이전의 모든 예약을 삭제합니다.
     * 
     * @param dateTime 기준 시간
     */
    @Modifying
    @Query("DELETE FROM Reservation r WHERE r.endAt < :dateTime")
    void deleteReservationsBefore(@Param("dateTime") LocalDateTime dateTime);
}
