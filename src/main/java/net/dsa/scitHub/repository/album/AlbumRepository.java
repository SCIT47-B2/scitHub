package net.dsa.scitHub.repository.album;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.album.Album;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Integer> {
    
    /** 앨범 이름으로 검색 */
    List<Album> findByNameContaining(String name);
    
    /** 정확한 앨범 이름으로 조회 */
    Optional<Album> findByName(String name);
    
    /** 앨범 이름 존재 여부 확인 */
    boolean existsByName(String name);
    
    /** 사진이 있는 앨범들만 조회 */
    @Query("SELECT DISTINCT a FROM Album a JOIN a.photos p")
    List<Album> findAlbumsWithPhotos();
    
    /** 사진 개수와 함께 앨범 조회 */
    @Query("SELECT a, COUNT(p) FROM Album a LEFT JOIN a.photos p GROUP BY a ORDER BY COUNT(p) DESC")
    List<Object[]> findAlbumsWithPhotoCount();
}
