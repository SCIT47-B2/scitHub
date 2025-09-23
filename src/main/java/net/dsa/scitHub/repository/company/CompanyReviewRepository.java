package net.dsa.scitHub.repository.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.company.CompanyReview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyReviewRepository extends JpaRepository<CompanyReview, Integer> {
    
    // Fetch Join을 사용하여 Review와 User를 함께 조회
    @Query("SELECT cr FROM CompanyReview cr JOIN FETCH cr.user u WHERE cr.company.companyId = :companyId")
    List<CompanyReview> findByCompanyWithUser(@Param("companyId") Integer companyId);
    
    /** 회사별 리뷰 조회 */
    List<CompanyReview> findByCompany_CompanyId(Integer companyId);
    
    /** 회사별 리뷰 조회 (페이징) */
    Page<CompanyReview> findByCompany_CompanyId(Integer companyId, Pageable pageable);
    
    /** 사용자별 리뷰 조회 */
    List<CompanyReview> findByUser_UserId(Integer userId);
    
    /** 특정 회사에 대한 특정 사용자의 리뷰 조회 */
    Optional<CompanyReview> findByCompany_CompanyIdAndUser_UserId(Integer companyId, Integer userId);
    
    /** 회사별 평균 평점 조회 */
    @Query("SELECT AVG(cr.rating) FROM CompanyReview cr WHERE cr.company.companyId = :companyId")
    Double findAverageRatingByCompanyId(@Param("companyId") Integer companyId);
    
    /** 회사별 리뷰 수 조회 */
    @Query("SELECT COUNT(cr) FROM CompanyReview cr WHERE cr.company.companyId = :companyId")
    Long countByCompanyId(@Param("companyId") Integer companyId);
    
    /** 특정 평점 이상의 리뷰들 조회 */
    List<CompanyReview> findByRatingGreaterThanEqual(Byte rating);
    
    /** 최신 리뷰들 조회 (페이징) */
    Page<CompanyReview> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /** 특정 기간의 리뷰들 조회 */
    @Query("SELECT cr FROM CompanyReview cr WHERE cr.createdAt BETWEEN :startDate AND :endDate")
    List<CompanyReview> findReviewsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    /** 내용이 있는 리뷰들만 조회 */
    @Query("SELECT cr FROM CompanyReview cr WHERE cr.content IS NOT NULL AND LENGTH(cr.content) > 0")
    List<CompanyReview> findReviewsWithContent();
    
    /** 회사별 평점 분포 조회 */
    @Query("SELECT cr.rating, COUNT(cr) FROM CompanyReview cr WHERE cr.company.companyId = :companyId GROUP BY cr.rating ORDER BY cr.rating")
    List<Object[]> findRatingDistributionByCompanyId(@Param("companyId") Integer companyId);
}
