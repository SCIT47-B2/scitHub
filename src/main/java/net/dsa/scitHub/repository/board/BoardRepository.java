package net.dsa.scitHub.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.board.Board;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Integer> {
    
    /** 게시판 이름으로 조회 */
    Optional<Board> findByName(String name);
    
    /** 게시판 이름 존재 여부 확인 */
    boolean existsByName(String name);
    
    /** 게시판 이름으로 검색 */
    List<Board> findByNameContaining(String name);
    
    /** 설명으로 검색 */
    List<Board> findByDescriptionContaining(String description);
    
    /** 게시글이 있는 게시판만 조회 */
    @Query("SELECT DISTINCT b FROM Board b JOIN b.posts p")
    List<Board> findBoardsWithPosts();
    
    /** 게시글 수와 함께 게시판 조회 */
    @Query("SELECT b, COUNT(p) FROM Board b LEFT JOIN b.posts p GROUP BY b ORDER BY COUNT(p) DESC")
    List<Object[]> findBoardsWithPostCount();
    
    /** 특정 사용자가 즐겨찾기한 게시판들 */
    @Query("SELECT b FROM Board b JOIN b.bookmarks bb WHERE bb.account.accountId = :accountId")
    List<Board> findBookmarkedBoardsByAccount(@Param("accountId") Integer accountId);
}
