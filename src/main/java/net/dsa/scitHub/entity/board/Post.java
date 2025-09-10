package net.dsa.scitHub.entity.board;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.user.User;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "post")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"board", "user", "comments", "attachmentFiles", "tags", "likes"})
public class Post {

    /** 게시글 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Integer postId;

    /** 게시글이 속한 게시판 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /** 게시글 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 게시글 제목 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 게시글 내용 */
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    /** 게시글 조회 수 */
    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    /** 게시글 작성 시간 */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 게시글 수정 시간 */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 게시글에 달린 댓글 목록 */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments;

    /** 게시글 첨부파일 목록 */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<AttachmentFile> attachmentFiles;

    /** 게시글 태그 목록 */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Tag> tags;

    /** 게시글 좋아요 목록 */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostLike> likes;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Post)) return false;
        Post post = (Post) o;
        return Objects.equals(postId, post.postId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId);
    }
}
