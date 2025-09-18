package net.dsa.scitHub.repository.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.course.Course;
import net.dsa.scitHub.enums.CourseType;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    
    /** 강의 유형별 조회 */
    List<Course> findByCourseType(CourseType courseType);
    
    /** 기수별 강의 조회 */
    List<Course> findByCohortNo(Integer cohortNo);
    
    /** 기수와 강의 유형으로 조회 */
    List<Course> findByCohortNoAndCourseType(Integer cohortNo, CourseType courseType);
    
    /** 강사명으로 조회 */
    List<Course> findByInstructorName(String instructorName);
    
    /** 강의명으로 검색 */
    List<Course> findByNameContaining(String name);
    
    /** 정확한 강의명으로 조회 */
    Optional<Course> findByName(String name);
    
    /** 강사명으로 검색 */
    List<Course> findByInstructorNameContaining(String instructorName);
    
    /** 기수별로 정렬된 강의 조회 */
    List<Course> findAllByOrderByCohortNoDesc();
    
    /** 리뷰가 있는 강의만 조회 */
    @Query("SELECT DISTINCT c FROM Course c JOIN c.reviews cr")
    List<Course> findCoursesWithReviews();
    
    /** 평균 평점과 함께 강의 조회 */
    @Query("SELECT c, AVG(cr.rating) FROM Course c LEFT JOIN c.reviews cr " +
           "GROUP BY c ORDER BY AVG(cr.rating) DESC")
    List<Object[]> findCoursesWithAverageScore();
    
    /** 특정 기수의 강의 유형별 개수 */
    @Query("SELECT c.courseType, COUNT(c) FROM Course c WHERE c.cohortNo = :cohortNo GROUP BY c.courseType")
    List<Object[]> countCoursesByTypeForCohort(@Param("cohortNo") Integer cohortNo);
}
