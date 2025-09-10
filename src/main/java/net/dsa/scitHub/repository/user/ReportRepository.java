package net.dsa.scitHub.repository.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.user.Report;
import net.dsa.scitHub.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {
    
    /** 신고자별 신고 조회 */
    List<Report> findByUser_UserId(Integer userId);
    
    /** 상태별 신고 조회 */
    List<Report> findByStatus(ReportStatus status);
    
    /** 상태별 신고 조회 (페이징) */
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
    
    /** 게시글 관련 신고 조회 */
    List<Report> findByPost_PostId(Integer postId);
    
    /** 댓글 관련 신고 조회 */
    List<Report> findByComment_CommentId(Integer commentId);
    
    /** 회사 리뷰 관련 신고 조회 */
    List<Report> findByCompanyReview_CompanyReviewId(Integer companyReviewId);
    
    /** 예약 관련 신고 조회 */
    List<Report> findByReservation_ReservationId(Integer reservationId);
    
    /** 대기 중인 신고들 조회 */
    @Query("SELECT r FROM Report r WHERE r.status = 'PENDING' ORDER BY r.createdAt")
    List<Report> findPendingReports();
    
    /** 처리 완료된 신고들 조회 */
    @Query("SELECT r FROM Report r WHERE r.status = 'RESOLVED' ORDER BY r.createdAt DESC")
    List<Report> findResolvedReports();
    
    /** 반려된 신고들 조회 */
    @Query("SELECT r FROM Report r WHERE r.status = 'REJECTED' ORDER BY r.createdAt DESC")
    List<Report> findRejectedReports();
    
    /** 특정 기간의 신고들 조회 */
    @Query("SELECT r FROM Report r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<Report> findReportsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
    
    /** 신고 내용으로 검색 */
    List<Report> findByContentContaining(String content);
    
    /** 상태별 신고 수 조회 */
    @Query("SELECT r.status, COUNT(r) FROM Report r GROUP BY r.status")
    List<Object[]> countReportsByStatus();
    
    /** 신고 유형별 개수 조회 */
    @Query("SELECT CASE " +
           "WHEN r.post IS NOT NULL THEN 'POST' " +
           "WHEN r.comment IS NOT NULL THEN 'COMMENT' " +
           "WHEN r.companyReview IS NOT NULL THEN 'COMPANY_REVIEW' " +
           "WHEN r.reservation IS NOT NULL THEN 'RESERVATION' " +
           "ELSE 'OTHER' END, COUNT(r) " +
           "FROM Report r GROUP BY CASE " +
           "WHEN r.post IS NOT NULL THEN 'POST' " +
           "WHEN r.comment IS NOT NULL THEN 'COMMENT' " +
           "WHEN r.companyReview IS NOT NULL THEN 'COMPANY_REVIEW' " +
           "WHEN r.reservation IS NOT NULL THEN 'RESERVATION' " +
           "ELSE 'OTHER' END")
    List<Object[]> countReportsByType();
}
