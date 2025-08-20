package net.dsa.scitHub.entity.community;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

import net.dsa.scitHub.entity.user.UsersEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_bookmarks")
public class PostBookmarksEntity {

    @EmbeddedId
    private PostBookmarksId id;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_pb_post"))
    private PostsEntity post;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_pb_user"))
    private UsersEntity user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    @Embeddable
    public static class PostBookmarksId implements Serializable {
        @Column(name = "post_id", columnDefinition = "int unsigned")
        private Integer postId;

        @Column(name = "user_id", columnDefinition = "int unsigned")
        private Integer userId;
    }
}

