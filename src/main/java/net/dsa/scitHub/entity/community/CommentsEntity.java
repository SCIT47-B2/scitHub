package net.dsa.scitHub.entity.community;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

import net.dsa.scitHub.entity.user.UsersEntity;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_comments_post_created", columnList = "post_id, created_at")
    }
)
public class CommentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id", columnDefinition = "int unsigned")
    private Integer commentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_comments_post"))
    private PostsEntity post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_comments_author"))
    private UsersEntity author;

    // 부모 댓글 (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id",
        foreignKey = @ForeignKey(name = "fk_comments_parent"))
    private CommentsEntity parent;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "is_answer", nullable = false, columnDefinition = "tinyint default 0")
    private Boolean isAnswer;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (isAnswer == null) isAnswer = false;
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // 자식 댓글들
    @Builder.Default
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = {}, orphanRemoval = false)
    private List<CommentsEntity> children = new ArrayList<>();
}

