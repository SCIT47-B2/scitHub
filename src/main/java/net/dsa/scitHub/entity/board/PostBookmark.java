package net.dsa.scitHub.entity.board;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.user.Account;

import java.util.Objects;

@Entity
@Table(name = "post_bookmark")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"post", "account"})
public class PostBookmark {
    
    /** 게시글 즐겨찾기 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_bookmark_id")
    private Integer postBookmarkId;
    
    /** 즐겨찾기 대상 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    /** 즐겨찾기 등록 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostBookmark)) return false;
        PostBookmark that = (PostBookmark) o;
        return Objects.equals(postBookmarkId, that.postBookmarkId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(postBookmarkId);
    }
}
