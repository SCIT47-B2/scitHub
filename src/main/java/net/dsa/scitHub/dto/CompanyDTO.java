package net.dsa.scitHub.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.company.Company;
import net.dsa.scitHub.entity.company.CompanyReview;
import net.dsa.scitHub.enums.CompanyType;
import net.dsa.scitHub.enums.Industry;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDTO {
    
    private Integer companyId;
    private String name;
    private String logoUrl;
    private String location;
    private Industry industry;
    private CompanyType type;
    private Integer headcount;
    private List<CompanyReview> reviews;

    public static CompanyDTO convertToCompanyDTO(Company entity) {
        return CompanyDTO.builder()
            .companyId(entity.getCompanyId())
            .name(entity.getName())
            .logoUrl(entity.getLogoUrl())
            .industry(entity.getIndustry())
            .type(entity.getType())
            .location(entity.getLocation())
            .headcount(entity.getHeadcount())
            .build();
    }

}
