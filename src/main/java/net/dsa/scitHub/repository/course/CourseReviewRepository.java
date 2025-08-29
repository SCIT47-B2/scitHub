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
    
    /** 강의별 준비성 평균 점수 */
    @Query("SELECT AVG(cr.scorePreparedness) FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findAveragePreparednessScore(@Param("courseId") Integer courseId);
    
    /** 강의별 전문성 평균 점수 */
    @Query("SELECT AVG(cr.scoreProfesion) FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findAverageProfesionScore(@Param("courseId") Integer courseId);
    
    /** 강의별 소통 능력 평균 점수 */
    @Query("SELECT AVG(cr.scoreCommunication) FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findAverageCommunicationScore(@Param("courseId") Integer courseId);
    
    /** 강의별 참여 유도 평균 점수 */
    @Query("SELECT AVG(cr.scoreEngagement) FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findAverageEngagementScore(@Param("courseId") Integer courseId);
    
    /** 강의별 공정성 평균 점수 */
    @Query("SELECT AVG(cr.scoreFairness) FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findAverageFairnessScore(@Param("courseId") Integer courseId);
    
    /** 강의별 전체 평균 점수 */
    @Query("SELECT AVG(cr.scorePreparedness + cr.scoreProfesion + cr.scoreCommunication + cr.scoreEngagement + cr.scoreFairness) / 5 " +
           "FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findOverallAverageScore(@Param("courseId") Integer courseId);
    
    /** 강의별 난이도 평균 */
    @Query("SELECT AVG(cr.courseDifficulty) FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findAverageDifficulty(@Param("courseId") Integer courseId);
    
    /** 강의별 과제량 평균 */
    @Query("SELECT AVG(cr.courseAssignment) FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findAverageAssignment(@Param("courseId") Integer courseId);
    
    /** 강의별 연결성 평균 */
    @Query("SELECT AVG(cr.courseConnectivity) FROM CourseReview cr WHERE cr.course.courseId = :courseId")
    Double findAverageConnectivity(@Param("courseId") Integer courseId);
    
    /** 코멘트가 있는 리뷰들 조회 */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.commentText IS NOT NULL AND LENGTH(cr.commentText) > 0")
    List<CourseReview> findReviewsWithComments();
    
    /** 특정 기간의 리뷰들 조회 */
    @Query("SELECT cr FROM CourseReview cr WHERE cr.createdAt BETWEEN :startDate AND :endDate")
    List<CourseReview> findReviewsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
}
