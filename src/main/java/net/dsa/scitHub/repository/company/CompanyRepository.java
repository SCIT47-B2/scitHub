package net.dsa.scitHub.repository.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.company.Company;
import net.dsa.scitHub.enums.CompanyType;
import net.dsa.scitHub.enums.Industry;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {

    /** 업계별 회사 조회 */
    List<Company> findByIndustry(Industry industry);

    /** 회사 유형별 조회 */
    List<Company> findByType(CompanyType type);

    /** 회사 이름으로 검색 (페이징) */
    Page<Company> findByNameContaining(String name, Pageable pageable);

    /** 회사 이름으로 검색 */
    List<Company> findByNameContaining(String name);

    /** 지역으로 검색 */
    @Query("SELECT c FROM Company c WHERE TRIM(c.location) IN :locations")
    List<Company> findByLocationIn(@Param("locations") List<String> locations);

    @Query("SELECT c FROM Company c " +
           "LEFT JOIN c.reviews cr " +
           "WHERE (:name IS NULL OR c.name LIKE %:name%) " +
           "AND (:industry IS NULL OR c.industry = :industry) " +
           "AND (:type IS NULL OR c.type = :type) " +
           "AND (:locations IS NULL OR c.location IN :locations) " +
           "GROUP BY c.companyId")
    Page<Company> findWithFilters(@Param("name") String name,
                                  @Param("industry") Industry industry,
                                  @Param("type") CompanyType type,
                                  @Param("locations") List<String> locations,
                                  Pageable pageable);
    /** 직원 수 범위로 조회 */
    @Query("SELECT c FROM Company c WHERE c.headcount BETWEEN :min AND :max")
    List<Company> findByHeadcountBetween(@Param("min") Integer min, @Param("max") Integer max);

    /** 업계와 유형으로 조회 */
    List<Company> findByIndustryAndType(Industry industry, CompanyType type);

    /** 직원 수가 많은 순으로 조회 */
    List<Company> findAllByOrderByHeadcountDesc();

    /** 리뷰 평점과 함께 회사 조회 */
    @Query("SELECT c, AVG(cr.rating) FROM Company c LEFT JOIN c.reviews cr GROUP BY c ORDER BY AVG(cr.rating) DESC")
    List<Object[]> findCompaniesWithAverageRating();

    /** 특정 평점 이상의 회사들 조회 */
    @Query("SELECT c FROM Company c JOIN c.reviews cr GROUP BY c HAVING AVG(cr.rating) >= :minRating")
    List<Company> findCompaniesWithMinRating(@Param("minRating") Double minRating);
}
