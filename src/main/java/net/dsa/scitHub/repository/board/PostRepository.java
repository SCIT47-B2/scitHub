package net.dsa.scitHub.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.board.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    /** 특정 게시 게시글 조회 (페이징) */
    Page<Post> findByBoard_BoardId(Integer boardId, Pageable pageable);

    /** 사용자별 게시글 조회 (페이징) */
    Page<Post> findByUser_UserId(Integer userId, Pageable pageable);

    /** 제목과 내용으로 검색 */
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<Post> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /** 특정 게시판에서 제목과 내용으로 검색 */
    @Query("SELECT p FROM Post p WHERE p.board.boardId = :boardId AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> findByBoardAndKeyword(@Param("boardId") Integer boardId, @Param("keyword") String keyword, Pageable pageable);

    /** 조회수 증가 */
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    void incrementViewCount(@Param("postId") Integer postId);

    /** 조회수가 높은 게시글들 조회 */
    @Query("SELECT p FROM Post p ORDER BY p.viewCount DESC")
    List<Post> findTopByViewCount(Pageable pageable);

    /** 특정 게시판에서 제목으로 검색 (페이징) */
    Page<Post> findByBoard_BoardIdAndTitleContaining(Integer boardId, String title, Pageable pageable);

    /** 특정 게시판에서 태그로 검색 (페이징) */
    Page<Post> findByBoard_BoardIdAndTags_NameContaining(Integer boardId, String tagName, Pageable pageable);

    /** 특정 게시판에서 내용으로 검색 (페이징) */
    Page<Post> findByBoard_BoardIdAndContentContaining(Integer boardId, String content, Pageable pageable);

    /** 특정 게시판에서 사용자 한국어 이름으로 검색 (페이징) */
    Page<Post> findByBoard_BoardIdAndUser_NameKorContaining(Integer boardId, String nameKor, Pageable pageable);

    /** 특정 게시판에서 전체 검색: 제목, 내용, 작성자, 태그를 모두 포함하여 검색 (페이징) */
    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN p.user u " +
            "LEFT JOIN p.tags t " +
            "WHERE p.board.boardId = :boardId AND ( " +
            "p.title LIKE %:searchWord% " +
            "OR p.content LIKE %:searchWord% " +
            "OR u.nameKor LIKE %:searchWord% " +
            "OR t.name LIKE %:searchWord%)")
    Page<Post> findByBoard_BoardIdAndSearchWord(@Param("boardId") Integer boardId, @Param("searchWord") String searchWord, Pageable pageable);

    /** 최신 게시글들 조회 */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 댓글이 많은 게시글들 조회 */
    @Query("SELECT p, COUNT(c) FROM Post p LEFT JOIN p.comments c GROUP BY p ORDER BY COUNT(c) DESC")
    List<Object[]> findPostsWithCommentCount(Pageable pageable);

    /** 좋아요가 많은 게시글들 조회 */
    @Query("SELECT p, COUNT(pl) FROM Post p LEFT JOIN p.likes pl GROUP BY p ORDER BY COUNT(pl) DESC")
    List<Object[]> findPostsWithLikeCount(Pageable pageable);

    /** 특정 기간의 게시글들 조회 */
    @Query("SELECT p FROM Post p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Post> findPostsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);

    /** 첨부파일이 있는 게시글들 조회 */
    @Query("SELECT DISTINCT p FROM Post p JOIN p.attachmentFiles af")
    List<Post> findPostsWithAttachments();

    /** 태그가 있는 게시글들 조회 */
    @Query("SELECT DISTINCT p FROM Post p JOIN p.tags t")
    List<Post> findPostsWithTags();

    /** 특정 태그를 가진 게시글들 조회 */
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.name = :tagName")
    List<Post> findPostsByTag(@Param("tagName") String tagName);

    /** 게시판별 게시글 수 조회 */
    @Query("SELECT p.board.boardId, COUNT(p) FROM Post p GROUP BY p.board.boardId")
    List<Object[]> countPostsByBoard();

    /**
     * 게시글 ID로 게시글, 작성자, 댓글, 댓글 작성자 정보를 한 번에 조회
     * @param postId 게시글 ID
     * @return Post 엔티티 Optional
     */
    @Query(
        "SELECT p FROM Post p " +
        "JOIN FETCH p.user " +
        "LEFT JOIN FETCH p.comments c " +
        "LEFT JOIN FETCH c.user " +
        "WHERE p.postId = :postId " +
        "ORDER BY c.createdAt ASC"
    )
    Optional<Post> findPostWithDetailsById(@Param("postId") Integer postId);

}
