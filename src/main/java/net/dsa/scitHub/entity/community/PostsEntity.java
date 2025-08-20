package net.dsa.scitHub.entity.community;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;
import java.util.ArrayList;

import net.dsa.scitHub.entity.user.UsersEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "posts",
    indexes = {
        @Index(name = "idx_posts_board_created", columnList = "board_id, created_at"),
        @Index(name = "idx_posts_author_created", columnList = "author_id, created_at")
    }
)
public class PostsEntity {

    public enum Status { ACTIVE, DELETED, BLOCKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", columnDefinition = "int unsigned")
    private Integer postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_posts_board"))
    private BoardsEntity board;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_posts_author"))
    private UsersEntity author;

    @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status; // default ACTIVE

    @Column(name = "view_count", nullable = false, columnDefinition = "int unsigned default 0")
    private Integer viewCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (status == null) status = Status.ACTIVE;
        if (viewCount == null) viewCount = 0;
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseViewCount() {
        this.viewCount = (this.viewCount == null ? 1 : this.viewCount + 1);
    }

    // Q&A 1:1 역방향
    @OneToOne(mappedBy = "post", fetch = FetchType.LAZY)
    private QnaPostsEntity qna;

    // 댓글 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<CommentsEntity> comments = new ArrayList<>();

    // 좋아요 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<PostLikesEntity> likes = new ArrayList<>();

    // 북마크 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<PostBookmarksEntity> bookmarks = new ArrayList<>();

    // 첨부파일 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<PostAttachmentsEntity> attachments = new ArrayList<>();

    // 신고 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<PostReportsEntity> reports = new ArrayList<>();

    // 태그 매핑(조인 엔티티 방식 권장)
    @Builder.Default
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<PostTagsEntity> postTags = new ArrayList<>();

}
