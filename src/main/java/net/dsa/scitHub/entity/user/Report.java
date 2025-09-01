package net.dsa.scitHub.entity.user;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.entity.company.CompanyReview;
import net.dsa.scitHub.entity.reservation.Reservation;
import net.dsa.scitHub.enums.ReportStatus;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "report")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"account", "post", "comment", "reservation", "companyReview"})
public class Report {
    
    /** 신고 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Integer reportId;
    
    /** 신고 접수자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    /** 신고 내용 */
    @Column(name = "content")
    private String content;
    
    /** 신고 처리 상태 (PENDING: 대기중, RESOLVED: 처리완료, REJECTED: 반려) */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING;
    
    /** 신고 접수 시간 */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /** 신고 대상 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    
    /** 신고 대상 댓글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;
    
    /** 신고 대상 예약 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;
    
    /** 신고 대상 회사 리뷰 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_review_id")
    private CompanyReview companyReview;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Report)) return false;
        Report report = (Report) o;
        return Objects.equals(reportId, report.reportId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(reportId);
    }
}
