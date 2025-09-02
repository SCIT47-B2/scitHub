package net.dsa.scitHub.entity.board;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.album.Photo;
import net.dsa.scitHub.entity.company.CompanyReview;
import net.dsa.scitHub.entity.user.User;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "comment")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "post", "companyReview", "photo"})
public class Comment {
    
    /** 댓글 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Integer commentId;
    
    /** 댓글 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /** 댓글 내용 */
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
    
    /** 댓글 작성 시간 */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /** 댓글 수정 시간 */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /** 댓글이 달린 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    
    /** 댓글이 달린 회사 리뷰 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_review_id")
    private CompanyReview companyReview;
    
    /** 댓글이 달린 사진 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id")
    private Photo photo;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        Comment comment1 = (Comment) o;
        return Objects.equals(commentId, comment1.commentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(commentId);
    }
}
