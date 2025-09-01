package net.dsa.scitHub.entity.board;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.user.Account;

import java.util.Objects;

@Entity
@Table(name = "board_bookmark")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"board", "account"})
public class BoardBookmark {
    
    /** 게시판 즐겨찾기 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_bookmark_id")
    private Integer boardBookmarkId;
    
    /** 즐겨찾기 대상 게시판 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    
    /** 즐겨찾기 등록 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardBookmark)) return false;
        BoardBookmark that = (BoardBookmark) o;
        return Objects.equals(boardBookmarkId, that.boardBookmarkId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(boardBookmarkId);
    }
}
