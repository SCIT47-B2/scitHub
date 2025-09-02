package net.dsa.scitHub.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.board.Comment;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    
    /** 게시글별 댓글 조회 */
    List<Comment> findByPost_PostId(Integer postId);
    
    /** 사용자별 댓글 조회 (페이징) */
    Page<Comment> findByUser_UserId(Integer userId, Pageable pageable);
    
    /** 회사 리뷰별 댓글 조회 */
    List<Comment> findByCompanyReview_CompanyReviewId(Integer companyReviewId);
    
    /** 사진별 댓글 조회 */
    List<Comment> findByPhoto_PhotoId(Integer photoId);
    
    /** 게시글별 댓글 수 조회 */
    Long countByPost_PostId(Integer postId);
    
    /** 최신 댓글들 조회 (페이징) */
    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /** 특정 기간의 댓글들 조회 */
    @Query("SELECT c FROM Comment c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<Comment> findCommentsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    /** 댓글 내용으로 검색 */
    List<Comment> findByContentContaining(String keyword);
    
    /** 수정된 댓글들 조회 */
    @Query("SELECT c FROM Comment c WHERE c.updatedAt IS NOT NULL AND c.updatedAt > c.createdAt")
    List<Comment> findModifiedComments();
    
    /** 사용자별 댓글 수 조회 */
    @Query("SELECT c.user.userId, COUNT(c) FROM Comment c GROUP BY c.user.userId ORDER BY COUNT(c) DESC")
    List<Object[]> countCommentsByUser();
    
    /** 게시글별 댓글 수 조회 */
    @Query("SELECT c.post.postId, COUNT(c) FROM Comment c WHERE c.post IS NOT NULL GROUP BY c.post.postId")
    List<Object[]> countCommentsByPost();
    
    /** 회사 리뷰별 댓글 수 조회 */
    @Query("SELECT c.companyReview.companyReviewId, COUNT(c) FROM Comment c WHERE c.companyReview IS NOT NULL GROUP BY c.companyReview.companyReviewId")
    List<Object[]> countCommentsByCompanyReview();
    
    /** 사진별 댓글 수 조회 */
    @Query("SELECT c.photo.photoId, COUNT(c) FROM Comment c WHERE c.photo IS NOT NULL GROUP BY c.photo.photoId")
    List<Object[]> countCommentsByPhoto();
}
