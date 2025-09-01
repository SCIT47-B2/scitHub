package net.dsa.scitHub.repository.classroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.classroom.Seat;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    
    /** 강의실별 좌석 조회 */
    List<Seat> findByClassroom_ClassroomId(Integer classroomId);
    
    /** 사용자별 좌석 조회 */
    List<Seat> findByUser_UserId(Integer userId);
    
    /** 특정 좌석 위치로 조회 */
    Optional<Seat> findByClassroom_ClassroomIdAndRowNoAndColNo(Integer classroomId, Integer rowNo, Integer colNo);
    
    /** 빈 좌석들 조회 */
    @Query("SELECT s FROM Seat s WHERE s.classroom.classroomId = :classroomId AND s.user IS NULL")
    List<Seat> findAvailableSeats(@Param("classroomId") Integer classroomId);
    
    /** 사용 중인 좌석들 조회 */
    @Query("SELECT s FROM Seat s WHERE s.classroom.classroomId = :classroomId AND s.user IS NOT NULL")
    List<Seat> findOccupiedSeats(@Param("classroomId") Integer classroomId);
    
    /** 강의실별 좌석을 위치 순으로 조회 */
    @Query("SELECT s FROM Seat s WHERE s.classroom.classroomId = :classroomId ORDER BY s.rowNo, s.colNo")
    List<Seat> findByClassroomIdOrderByPosition(@Param("classroomId") Integer classroomId);
    
    /** 강의실별 좌석 수 조회 */
    @Query("SELECT s.classroom.classroomId, COUNT(s) FROM Seat s GROUP BY s.classroom.classroomId")
    List<Object[]> countSeatsByClassroom();
    
    /** 강의실별 사용 중인 좌석 수 조회 */
    @Query("SELECT s.classroom.classroomId, COUNT(s) FROM Seat s WHERE s.user IS NOT NULL GROUP BY s.classroom.classroomId")
    List<Object[]> countOccupiedSeatsByClassroom();
    
    /** 강의실별 빈 좌석 수 조회 */
    @Query("SELECT s.classroom.classroomId, COUNT(s) FROM Seat s WHERE s.user IS NULL GROUP BY s.classroom.classroomId")
    List<Object[]> countAvailableSeatsByClassroom();
    
    /** 특정 행의 좌석들 조회 */
    @Query("SELECT s FROM Seat s WHERE s.classroom.classroomId = :classroomId AND s.rowNo = :rowNo ORDER BY s.colNo")
    List<Seat> findSeatsByRow(@Param("classroomId") Integer classroomId, @Param("rowNo") Integer rowNo);
    
    /** 특정 열의 좌석들 조회 */
    @Query("SELECT s FROM Seat s WHERE s.classroom.classroomId = :classroomId AND s.colNo = :colNo ORDER BY s.rowNo")
    List<Seat> findSeatsByColumn(@Param("classroomId") Integer classroomId, @Param("colNo") Integer colNo);
    
    /** 강의실의 최대 행 번호 조회 */
    @Query("SELECT MAX(s.rowNo) FROM Seat s WHERE s.classroom.classroomId = :classroomId")
    Integer findMaxRowByClassroomId(@Param("classroomId") Integer classroomId);
    
    /** 강의실의 최대 열 번호 조회 */
    @Query("SELECT MAX(s.colNo) FROM Seat s WHERE s.classroom.classroomId = :classroomId")
    Integer findMaxColByClassroomId(@Param("classroomId") Integer classroomId);
}
