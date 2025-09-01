package net.dsa.scitHub.entity.company;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.user.Account;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "company_review")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"company", "account", "comments"})
public class CompanyReview {
    
    /** 회사 리뷰 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_review_id")
    private Integer companyReviewId;
    
    /** 리뷰 대상 회사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    /** 리뷰 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    
    /** 회사 평점 (1-5점) */
    @Column(name = "rating", nullable = false)
    private Byte rating;
    
    /** 리뷰 내용 */
    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;
    
    /** 리뷰 작성 시간 */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /** 리뷰에 달린 댓글 목록 */
    @OneToMany(mappedBy = "companyReview", cascade = CascadeType.ALL)
    private List<Comment> comments;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompanyReview)) return false;
        CompanyReview that = (CompanyReview) o;
        return Objects.equals(companyReviewId, that.companyReviewId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(companyReviewId);
    }
}
