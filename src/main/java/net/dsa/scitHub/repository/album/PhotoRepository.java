package net.dsa.scitHub.repository.album;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.album.Photo;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Integer> {
    
    /** 앨범별 사진 조회 */
    List<Photo> findByAlbum_AlbumId(Integer albumId);
    
    /** 앨범별 사진 조회 (페이징) */
    Page<Photo> findByAlbum_AlbumId(Integer albumId, Pageable pageable);
    
    /** 사용자별 사진 조회 */
    List<Photo> findByAccount_AccountId(Integer accountId);
    
    /** 사용자별 사진 조회 (페이징) */
    Page<Photo> findByAccount_AccountId(Integer accountId, Pageable pageable);
    
    /** 설명이 있는 사진들 조회 */
    @Query("SELECT p FROM Photo p WHERE p.caption IS NOT NULL AND LENGTH(p.caption) > 0")
    List<Photo> findPhotosWithCaption();
    
    /** 설명으로 검색 */
    List<Photo> findByCaptionContaining(String caption);
    
    /** 최신 사진들 조회 (페이징) */
    Page<Photo> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /** 특정 기간의 사진들 조회 */
    @Query("SELECT p FROM Photo p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Photo> findPhotosBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /** 앨범별 사진 수 조회 */
    @Query("SELECT p.album.albumId, COUNT(p) FROM Photo p GROUP BY p.album.albumId")
    List<Object[]> countPhotosByAlbum();
    
    /** 사용자별 사진 수 조회 */
    @Query("SELECT p.account.accountId, COUNT(p) FROM Photo p GROUP BY p.account.accountId ORDER BY COUNT(p) DESC")
    List<Object[]> countPhotosByAccount();
    
    /** 댓글이 있는 사진들 조회 */
    @Query("SELECT DISTINCT p FROM Photo p JOIN p.comments c")
    List<Photo> findPhotosWithComments();
    
    /** 특정 월의 사진들 조회 */
    @Query("SELECT p FROM Photo p WHERE YEAR(p.createdAt) = :year AND MONTH(p.createdAt) = :month")
    List<Photo> findByMonth(@Param("year") Integer year, @Param("month") Integer month);
}
