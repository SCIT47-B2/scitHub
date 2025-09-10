package net.dsa.scitHub.repository.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.schedule.Event;

import java.beans.Visibility;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    
    /** 사용자별 이벤트 조회 */
    List<Event> findByUser_UserId(Integer userId);
    
    /** 공개 범위별 조회 */
    List<Event> findByVisibility(Visibility visibility);
    
    /** 특정 시간 범위의 이벤트 조회 */
    @Query("SELECT e FROM Event e WHERE e.startAt >= :start AND e.startAt <= :end")
    List<Event> findByStartAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /** 사용자에게 보여질 수 있는 이벤트들 조회 (공개 이벤트 + 본인 이벤트) */
    @Query("SELECT e FROM Event e WHERE e.visibility = 'PUBLIC' OR e.user.userId = :userId")
    List<Event> findVisibleEvents(@Param("userId") Integer userId);
    
    /** 종일 이벤트 조회 */
    List<Event> findByIsAllDay(Boolean isAllDay);
    
    /** 제목으로 검색 */
    List<Event> findByTitleContaining(String title);
    
    /** 내용으로 검색 */
    List<Event> findByContentContaining(String content);
    
    /** 현재 진행 중인 이벤트들 조회 */
    @Query("SELECT e FROM Event e WHERE e.startAt <= :now AND (e.endAt IS NULL OR e.endAt >= :now)")
    List<Event> findOngoingEvents(@Param("now") LocalDateTime now);
    
    /** 다가올 이벤트들 조회 */
    @Query("SELECT e FROM Event e WHERE e.startAt > :now ORDER BY e.startAt")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);
    
    /** 종료된 이벤트들 조회 */
    @Query("SELECT e FROM Event e WHERE e.endAt IS NOT NULL AND e.endAt < :now ORDER BY e.endAt DESC")
    List<Event> findPastEvents(@Param("now") LocalDateTime now);
    
    /** 특정 월의 이벤트들 조회 */
    @Query("SELECT e FROM Event e WHERE YEAR(e.startAt) = :year AND MONTH(e.startAt) = :month")
    List<Event> findByMonth(@Param("year") Integer year, @Param("month") Integer month);
    
    /** 공개 이벤트만 조회 */
    @Query("SELECT e FROM Event e WHERE e.visibility = 'PUBLIC' ORDER BY e.startAt")
    List<Event> findPublicEvents();
}
