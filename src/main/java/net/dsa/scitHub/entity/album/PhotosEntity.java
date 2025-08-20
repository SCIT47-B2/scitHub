package net.dsa.scitHub.entity.album;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;
import java.util.ArrayList;

import net.dsa.scitHub.entity.user.UsersEntity;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "photos",
    indexes = {
        @Index(name = "idx_ph_album_created", columnList = "album_id, created_at"),
        @Index(name = "idx_ph_uploader", columnList = "uploader_id")
    }
)
public class PhotosEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id", columnDefinition = "int unsigned")
    private Integer photoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_ph_album"))
    private AlbumsEntity album;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id",
        foreignKey = @ForeignKey(name = "fk_ph_uploader"))
    private UsersEntity uploader;

    @Size(max = 1024)
    @Column(name = "file_url", nullable = false, length = 1024)
    private String fileUrl;

    @Size(max = 255)
    @Column(name = "caption", length = 255)
    private String caption;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // 좋아요 목록
    @Builder.Default
    @OneToMany(mappedBy = "photo", fetch = FetchType.LAZY)
    private List<PhotoLikesEntity> likes = new ArrayList<>();

    // 댓글 목록
    @Builder.Default
    @OneToMany(mappedBy = "photo", fetch = FetchType.LAZY)
    private List<PhotoCommentsEntity> comments = new ArrayList<>();
}
