package net.dsa.scitHub.repository.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.board.AttachmentFile;

import java.util.List;

@Repository
public interface AttachmentFileRepository extends JpaRepository<AttachmentFile, Integer> {
    
    /** 게시글별 첨부파일 조회 */
    List<AttachmentFile> findByPost_PostId(Integer postId);
    
    /** 파일명으로 검색 */
    List<AttachmentFile> findByFileNameContaining(String fileName);
    
    /** 특정 파일 크기 이상의 첨부파일들 조회 */
    List<AttachmentFile> findByFileSizeGreaterThanEqual(Integer fileSize);
    
    /** 특정 파일 크기 이하의 첨부파일들 조회 */
    List<AttachmentFile> findByFileSizeLessThanEqual(Integer fileSize);
    
    /** 게시글별 첨부파일 수 조회 */
    @Query("SELECT af.post.postId, COUNT(af) FROM AttachmentFile af GROUP BY af.post.postId")
    List<Object[]> countAttachmentsByPost();
    
    /** 전체 첨부파일 크기 합계 조회 */
    @Query("SELECT SUM(af.fileSize) FROM AttachmentFile af WHERE af.fileSize IS NOT NULL")
    Long getTotalFileSize();
    
    /** 게시글별 첨부파일 크기 합계 조회 */
    @Query("SELECT af.post.postId, SUM(af.fileSize) FROM AttachmentFile af WHERE af.fileSize IS NOT NULL GROUP BY af.post.postId")
    List<Object[]> getTotalFileSizeByPost();
    
    /** 파일 확장자별 개수 조회 */
    @Query("SELECT SUBSTRING(af.fileName, LOCATE('.', af.fileName) + 1), COUNT(af) " +
           "FROM AttachmentFile af WHERE af.fileName LIKE '%.%' GROUP BY SUBSTRING(af.fileName, LOCATE('.', af.fileName) + 1)")
    List<Object[]> countFilesByExtension();
}

