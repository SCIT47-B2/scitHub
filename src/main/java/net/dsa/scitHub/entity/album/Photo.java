package net.dsa.scitHub.entity.album;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.user.User;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "photo")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"album", "user", "comments"})
/** 앨범에 포함된 사진 */
public class Photo {
    
    /** 사진 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Integer photoId;
    
    /** 사진이 속한 앨범 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;
    
    /** 사진 업로드 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /** 사진 파일 URL */
    @Column(name = "file_url", nullable = false, length = 1024)
    private String fileUrl;
    
    /** 사진 설명 */
    @Column(name = "caption")
    private String caption;
    
    /** 사진 업로드 시간 */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /** 사진에 달린 댓글 목록 */
    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL)
    private List<Comment> comments;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Photo)) return false;
        Photo photo = (Photo) o;
        return Objects.equals(photoId, photo.photoId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(photoId);
    }
}
