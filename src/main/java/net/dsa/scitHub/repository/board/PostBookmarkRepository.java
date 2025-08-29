package net.dsa.scitHub.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.board.PostBookmark;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Integer> {
    
    /** 사용자별 즐겨찾기한 게시글 조회 */
    List<PostBookmark> findByUser_UserId(Integer userId);
    
    /** 게시글별 즐겨찾기 목록 조회 */
    List<PostBookmark> findByPost_PostId(Integer postId);
    
    /** 특정 사용자의 특정 게시글 즐겨찾기 조회 */
    Optional<PostBookmark> findByPost_PostIdAndUser_UserId(Integer postId, Integer userId);
    
    /** 즐겨찾기 존재 여부 확인 */
    boolean existsByPost_PostIdAndUser_UserId(Integer postId, Integer userId);
    
    /** 게시글별 즐겨찾기 수 조회 */
    @Query("SELECT pb.post.postId, COUNT(pb) FROM PostBookmark pb GROUP BY pb.post.postId")
    List<Object[]> countBookmarksByPost();
    
    /** 가장 많이 즐겨찾기된 게시글들 조회 */
    @Query("SELECT pb.post, COUNT(pb) FROM PostBookmark pb GROUP BY pb.post ORDER BY COUNT(pb) DESC")
    List<Object[]> findMostBookmarkedPosts();
    
    /** 특정 사용자의 즐겨찾기 개수 */
    @Query("SELECT COUNT(pb) FROM PostBookmark pb WHERE pb.user.userId = :userId")
    Long countBookmarksByUser(@Param("userId") Integer userId);
    
    /** 특정 게시판의 게시글 즐겨찾기들 조회 */
    @Query("SELECT pb FROM PostBookmark pb WHERE pb.post.board.boardId = :boardId")
    List<PostBookmark> findBookmarksByBoard(@Param("boardId") Integer boardId);
}
