package net.dsa.scitHub.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.board.Tag;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    
    /** 게시글별 태그 조회 */
    List<Tag> findByPost_PostId(Integer postId);
    
    /** 태그 이름으로 조회 */
    List<Tag> findByName(String name);
    
    /** 태그 이름으로 검색 */
    @Query("SELECT t FROM Tag t WHERE t.name LIKE %:keyword%")
    List<Tag> findByNameContaining(@Param("keyword") String keyword);
    
    /** 모든 고유한 태그 이름 조회 */
    @Query("SELECT DISTINCT t.name FROM Tag t ORDER BY t.name")
    List<String> findAllDistinctTagNames();
    
    /** 인기 태그 조회 (사용 빈도 순) */
    @Query("SELECT t.name, COUNT(t) FROM Tag t GROUP BY t.name ORDER BY COUNT(t) DESC")
    List<Object[]> findPopularTags();
    
    /** 태그별 게시글 수 조회 */
    @Query("SELECT t.name, COUNT(DISTINCT t.post) FROM Tag t GROUP BY t.name ORDER BY COUNT(DISTINCT t.post) DESC")
    List<Object[]> countPostsByTag();
    
    /** 최근에 사용된 태그들 조회 */
    @Query("SELECT DISTINCT t.name FROM Tag t JOIN t.post p ORDER BY p.createdAt DESC")
    List<String> findRecentlyUsedTags();
}
