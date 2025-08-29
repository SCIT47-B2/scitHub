package net.dsa.scitHub.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.board.PostLike;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {
    
    /** 게시글별 좋아요 목록 조회 */
    List<PostLike> findByPost_PostId(Integer postId);
    
    /** 사용자별 좋아요 목록 조회 */
    List<PostLike> findByUser_UserId(Integer userId);
    
    /** 특정 사용자의 특정 게시글 좋아요 조회 */
    Optional<PostLike> findByPost_PostIdAndUser_UserId(Integer postId, Integer userId);
    
    /** 좋아요 존재 여부 확인 */
    boolean existsByPost_PostIdAndUser_UserId(Integer postId, Integer userId);
    
    /** 게시글별 좋아요 수 조회 */
    Long countByPost_PostId(Integer postId);
    
    /** 가장 많은 좋아요를 받은 게시글들 조회 */
    @Query("SELECT pl.post, COUNT(pl) FROM PostLike pl GROUP BY pl.post ORDER BY COUNT(pl) DESC")
    List<Object[]> findMostLikedPosts();
    
    /** 특정 사용자가 누른 좋아요 개수 */
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.user.userId = :userId")
    Long countLikesByUser(@Param("userId") Integer userId);
    
    /** 게시글별 좋아요 수 조회 */
    @Query("SELECT pl.post.postId, COUNT(pl) FROM PostLike pl GROUP BY pl.post.postId")
    List<Object[]> countLikesByPost();
    
    /** 특정 게시판의 게시글 좋아요들 조회 */
    @Query("SELECT pl FROM PostLike pl WHERE pl.post.board.boardId = :boardId")
    List<PostLike> findLikesByBoard(@Param("boardId") Integer boardId);
    
    /** 사용자별 좋아요 수 조회 */
    @Query("SELECT pl.user.userId, COUNT(pl) FROM PostLike pl WHERE pl.user IS NOT NULL GROUP BY pl.user.userId ORDER BY COUNT(pl) DESC")
    List<Object[]> countLikesByUser();
}
