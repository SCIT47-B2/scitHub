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

    /** 회사 이름으로 검색 */
    List<Company> findByNameContaining(String name);

    /** 지역으로 검색 */
    @Query("SELECT c FROM Company c WHERE TRIM(c.location) IN :locations")
    List<Company> findByLocationIn(@Param("locations") List<String> locations);

    /**
     * 다양한 필터 조건(이름, 업종, 유형, 지역)을 적용하여 회사를 검색하고 페이징
     * LEFT JOIN과 GROUP BY를 사용하여 각 회사 정보를 가져옴
     * @param name       검색할 회사 이름 (LIKE 검색)
     * @param industry   필터링할 업종
     * @param type       필터링할 회사 유형
     * @param locations  필터링할 세부 지역 목록
     * @param pageable   페이징 및 정렬 정보 (Sort 객체 포함)
     * @return           필터링 및 페이징된 회사 목록
     */
    @Query(value = "SELECT c, COALESCE(AVG(cr.rating), 0.0) as avgRating FROM Company c " +
           "LEFT JOIN c.reviews cr " +
           "WHERE (:name IS NULL OR c.name LIKE %:name%) " +
           "AND (:industry IS NULL OR c.industry = :industry) " +
           "AND (:type IS NULL OR c.type = :type) " +
           "AND (:locations IS NULL OR c.location IN :locations) " +
           "GROUP BY c.companyId, c.name, c.logoUrl, c.location, c.industry, c.type, c.headcount",
           countQuery = "SELECT COUNT(DISTINCT c) FROM Company c WHERE (:name IS NULL OR c.name LIKE %:name%) AND (:industry IS NULL OR c.industry = :industry) AND (:type IS NULL OR c.type = :type) AND (:locations IS NULL OR c.location IN :locations)")
    Page<Object[]> findWithFilters(@Param("name") String name,
                                  @Param("industry") Industry industry,
                                  @Param("type") CompanyType type,
                                  @Param("locations") List<String> locations,
                                  Pageable pageable);
}
