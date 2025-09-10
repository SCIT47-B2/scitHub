package net.dsa.scitHub.repository.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.schedule.Dday;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DdayRepository extends JpaRepository<Dday, Integer> {
    
    /** 사용자별 디데이 조회 */
    List<Dday> findByUser_UserId(Integer userId);
    
    /** 특정 날짜의 디데이 조회 */
    List<Dday> findByDday(LocalDate dday);
    
    /** 사용자별 디데이를 날짜 순으로 조회 */
    @Query("SELECT d FROM Dday d WHERE d.user.userId = :userId ORDER BY d.dday")
    List<Dday> findByUserIdOrderByDday(@Param("userId") Integer userId);
    
    /** 특정 기간의 디데이들 조회 */
    @Query("SELECT d FROM Dday d WHERE d.dday >= :fromDate AND d.dday <= :toDate")
    List<Dday> findByDdayBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
    
    /** 다가오는 디데이들 조회 (오늘 이후) */
    @Query("SELECT d FROM Dday d WHERE d.dday >= :today ORDER BY d.dday")
    List<Dday> findUpcomingDdays(@Param("today") LocalDate today);
    
    /** 지난 디데이들 조회 (오늘 이전) */
    @Query("SELECT d FROM Dday d WHERE d.dday < :today ORDER BY d.dday DESC")
    List<Dday> findPastDdays(@Param("today") LocalDate today);
    
    /** 사용자별 다가오는 디데이들 조회 */
    @Query("SELECT d FROM Dday d WHERE d.user.userId = :userId AND d.dday >= :today ORDER BY d.dday")
    List<Dday> findUpcomingDdaysByUser(@Param("userId") Integer userId, @Param("today") LocalDate today);
    
    /** 제목으로 검색 */
    List<Dday> findByTitleContaining(String title);
    
    /** 특정 사용자의 디데이 수 조회 */
    @Query("SELECT COUNT(d) FROM Dday d WHERE d.user.userId = :userId")
    Long countByUserId(@Param("userId") Integer userId);
    
    /** 이번 달의 디데이들 조회 */
    @Query("SELECT d FROM Dday d WHERE YEAR(d.dday) = :year AND MONTH(d.dday) = :month")
    List<Dday> findByMonth(@Param("year") Integer year, @Param("month") Integer month);
}
