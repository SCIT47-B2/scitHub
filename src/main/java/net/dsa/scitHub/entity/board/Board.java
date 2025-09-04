package net.dsa.scitHub.entity.board;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "board")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"posts"})
public class Board {

    /** 게시판 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Integer boardId;

    /** 게시판 이름 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 게시판 설명 */
    @Column(name = "description")
    private String description;

    /** 게시판에 작성된 게시글들 */
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    private List<Post> posts;

    /** 게시판 즐겨찾기 목록 */
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    private List<BoardBookmark> bookmarks;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Board)) return false;
        Board board = (Board) o;
        return Objects.equals(boardId, board.boardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardId);
    }
}
