package net.dsa.scitHub.entity.board;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.user.Account;

import java.util.Objects;

@Entity
@Table(name = "post_like")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"post", "account"})
public class PostLike {
    
    /** 게시글 좋아요 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id")
    private Integer postLikeId;
    
    /** 좋아요 대상 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    /** 좋아요 누른 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostLike)) return false;
        PostLike postLike = (PostLike) o;
        return Objects.equals(postLikeId, postLike.postLikeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(postLikeId);
    }
}
