package net.dsa.scitHub.entity.company;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.enums.CompanyType;
import net.dsa.scitHub.enums.Industry;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"reviews"})
public class Company {
    
    /** 회사 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Integer companyId;
    
    /** 회사 이름 */
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    
    /** 회사 로고 이미지 URL */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;
    
    /** 회사 위치 */
    @Column(name = "location", nullable = false, length = 100)
    private String location;
    
    /** 업계 분류 */
    @Enumerated(EnumType.STRING)
    @Column(name = "industry", nullable = false)
    private Industry industry;
    
    /** 회사 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CompanyType type;
    
    /** 회사 인원 수 */
    @Column(name = "headcount")
    private Integer headcount;
    
    /** 회사에 대한 리뷰 목록 */
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    private List<CompanyReview> reviews;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Company)) return false;
        Company company = (Company) o;
        return Objects.equals(companyId, company.companyId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(companyId);
    }
}
