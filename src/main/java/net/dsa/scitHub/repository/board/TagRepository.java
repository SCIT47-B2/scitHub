package net.dsa.scitHub.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    
    /** 특정 게시판의 태그들 조회 */
    @Query("SELECT t FROM Tag t WHERE t.post.board.boardId = :boardId")
    List<Tag> findTagsByBoard(@Param("boardId") Integer boardId);
    
    /** 특정 사용자가 사용한 태그들 조회 */
    @Query("SELECT t FROM Tag t WHERE t.post.user.userId = :userId")
    List<Tag> findTagsByUser(@Param("userId") Integer userId);
    
    /** 태그별 게시글 수 조회 */
    @Query("SELECT t.name, COUNT(DISTINCT t.post) FROM Tag t GROUP BY t.name ORDER BY COUNT(DISTINCT t.post) DESC")
    List<Object[]> countPostsByTag();
    
    /** 최근에 사용된 태그들 조회 */
    @Query("SELECT DISTINCT t.name FROM Tag t JOIN t.post p ORDER BY p.createdAt DESC")
    List<String> findRecentlyUsedTags();
    
    /** 특정 태그를 사용한 게시글들의 작성자 조회 */
    @Query("SELECT DISTINCT t.post.user FROM Tag t WHERE t.name = :tagName")
    List<Object> findUsersByTagName(@Param("tagName") String tagName);

    /** 목록에 존재하는 태그 삭제 */
    @Modifying
    @Query("DELETE FROM Tag t WHERE t.id IN :deleteIds")
    void deleteByIdIn(@Param("deleteIds") List<Integer> deleteIds);
    
    /** 목록에 존재하지 않는 태그 삭제 */
    @Modifying
    @Query("DELETE FROM Tag t WHERE t.id NOT IN :keepIds")
    void deleteNotInIds(@Param("keepIds") List<Integer> keepIds);
    
    /** 목록에 존재하는 태그 탐색 */
    @Query("SELECT t FROM Tag t WHERE t.name IN :names")
    List<Tag> findByNameIn(@Param("names") List<String> names);
}
