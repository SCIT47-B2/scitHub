package net.dsa.scitHub.entity.album;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "album")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"photos"})
/** 사진을 묶어둔 앨범 */
public class Album {
    
    /** 앨범 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "album_id")
    private Integer albumId;
    
    /** 앨범 이름 */
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    
    /** 앨범 설명 */
    @Column(name = "description", columnDefinition = "MEDIUMTEXT")
    private String description;
    
    /** 앨범에 포함된 사진들 */
    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL)
    private List<Photo> photos;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Album)) return false;
        Album album = (Album) o;
        return Objects.equals(albumId, album.albumId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(albumId);
    }
}
