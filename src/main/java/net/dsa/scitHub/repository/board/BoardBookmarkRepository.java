package net.dsa.scitHub.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.board.BoardBookmark;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardBookmarkRepository extends JpaRepository<BoardBookmark, Integer> {
    
    /** 사용자별 즐겨찾기한 게시판 조회 */
    List<BoardBookmark> findByUser_UserId(Integer userId);
    
    /** 게시판별 즐겨찾기 목록 조회 */
    List<BoardBookmark> findByBoard_BoardId(Integer boardId);
    
    /** 특정 사용자의 특정 게시판 즐겨찾기 조회 */
    Optional<BoardBookmark> findByBoard_BoardIdAndUser_UserId(Integer boardId, Integer userId);
    
    /** 즐겨찾기 존재 여부 확인 */
    boolean existsByBoard_BoardIdAndUser_UserId(Integer boardId, Integer userId);
    
    /** 게시판별 즐겨찾기 수 조회 */
    @Query("SELECT bb.board.boardId, COUNT(bb) FROM BoardBookmark bb GROUP BY bb.board.boardId")
    List<Object[]> countBookmarksByBoard();
    
    /** 가장 많이 즐겨찾기된 게시판들 조회 */
    @Query("SELECT bb.board, COUNT(bb) FROM BoardBookmark bb GROUP BY bb.board ORDER BY COUNT(bb) DESC")
    List<Object[]> findMostBookmarkedBoards();
    
    /** 특정 사용자의 즐겨찾기 개수 */
    @Query("SELECT COUNT(bb) FROM BoardBookmark bb WHERE bb.user.userId = :userId")
    Long countBookmarksByUser(@Param("userId") Integer userId);
}
