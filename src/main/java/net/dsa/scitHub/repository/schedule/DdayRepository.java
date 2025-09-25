package net.dsa.scitHub.repository.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.schedule.Dday;
import net.dsa.scitHub.entity.user.User;

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

    /**
     * User 객체를 받아, 해당 사용자의 모든 Dday를 dday 필드(날짜) 기준으로 오름차순 정렬하여 조회합니다.
     * ((네이밍 의미))
     * findByUser : Dday Entity 안에 있는 user 필드를 조건으로 사용하겠다는 의미
     * OrderByDday : dday 필드 기준으로 정렬하겠다.
     * Asc : 오름차순 정렬하겠다.
    */
    List<Dday> findByUserOrderByDdayAsc(User user);

    /**
     * 특정 사용자의 모든 D-Day를 '고정 해제'(isPinned = false) 상태로 일괄 업데이트합니다.
     * @Modifying: 이 쿼리가 DB 상태를 변경하는 INSERT, UPDATE, DELETE 쿼리임을 나타냅니다.
     */
    @Modifying
    @Query("UPDATE Dday d SET d.isPinned = false WHERE d.user.id = :userId")
    void unpinAllByUserId(@Param("userId") Integer userId); // User ID의 타입(Long/Integer)을 확인하세요

    /**
     * 주어진 ID 목록에 해당하는 특정 사용자의 D-Day들을 '고정'(isPinned = true) 상태로 일괄 업데이트합니다.
     */
    @Modifying
    @Query("UPDATE Dday d SET d.isPinned = true WHERE d.id IN :ids AND d.user.id = :userId")
    void pinByIdsAndUserId(@Param("ids") List<Integer> ids, @Param("userId") Integer userId);
    
}
