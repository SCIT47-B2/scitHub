package net.dsa.scitHub.entity.course;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.user.User;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "course_review")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"course", "user"})
public class CourseReview {
    
    /** 강의 리뷰 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_review_id")
    private Integer courseReviewId;
    
    /** 리뷰 대상 강의 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    /** 리뷰 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /** 강사 준비성 점수 */
    @Column(name = "score_preparedness", nullable = false)
    private Integer scorePreparedness;
    
    /** 강사 전문성 점수 */
    @Column(name = "score_profesion", nullable = false)
    private Integer scoreProfesion;
    
    /** 강사 소통 능력 점수 */
    @Column(name = "score_communication", nullable = false)
    private Integer scoreCommunication;
    
    /** 강사 참여 유도 점수 */
    @Column(name = "score_engagement", nullable = false)
    private Integer scoreEngagement;
    
    /** 강사 공정성 점수 */
    @Column(name = "score_fairness", nullable = false)
    private Integer scoreFairness;
    
    /** 강의 난이도 */
    @Column(name = "course_difficulty", nullable = false)
    private Integer courseDifficulty;
    
    /** 강의 과제량 */
    @Column(name = "course_assignment", nullable = false)
    private Integer courseAssignment;
    
    /** 강의 연결성 */
    @Column(name = "course_connectivity", nullable = false)
    private Integer courseConnectivity;
    
    /** 리뷰 텍스트 내용 */
    @Column(name = "comment_text", columnDefinition = "MEDIUMTEXT")
    private String commentText;
    
    /** 리뷰 작성 시간 */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseReview)) return false;
        CourseReview that = (CourseReview) o;
        return Objects.equals(courseReviewId, that.courseReviewId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(courseReviewId);
    }
}
