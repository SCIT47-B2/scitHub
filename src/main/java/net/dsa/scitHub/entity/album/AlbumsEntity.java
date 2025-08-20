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
    name = "albums",
    indexes = {
        @Index(name = "idx_albums_creator", columnList = "created_by"),
        @Index(name = "idx_albums_cohort", columnList = "cohort_no, created_at")
    }
)
public class AlbumsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "album_id", columnDefinition = "int unsigned")
    private Integer albumId;

    @Column(name = "cohort_no")
    private Integer cohortNo;

    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false,
        foreignKey = @ForeignKey(name = "fk_album_user"))
    private UsersEntity createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // 이 앨범에 속한 사진 목록
    @Builder.Default
    @OneToMany(mappedBy = "album", fetch = FetchType.LAZY)
    private List<PhotosEntity> photos = new ArrayList<>();
}
