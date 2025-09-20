package net.dsa.scitHub.repository.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.course.CourseReview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Integer> {
    
    /** 강의별 리뷰 조회 */
    List<CourseReview> findByCourse_CourseId(Integer courseId);
    
    /** 사용자별 리뷰 조회 */
    List<CourseReview> findByUser_UserId(Integer userId);
    
    /** 특정 사용자의 특정 강의 리뷰 조회 */
    Optional<CourseReview> findByUser_UserIdAndCourse_CourseId(Integer userId, Integer courseId);
    
    /** 강의별 평균 평점 */
    @Query("SELECT AVG(cr.rating) FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findAverageRatingByCourseId(@Param("courseId") Integer courseId);
    
    /** 코멘트가 있는 리뷰들 조회 */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.commentText IS NOT NULL AND LENGTH(cr.commentText) > 0")
    List<CourseReview> findReviewsWithComments();
    
    /** 특정 기간의 리뷰들 조회 */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.createdAt BETWEEN :startDate AND :endDate")
    List<CourseReview> findReviewsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
}
