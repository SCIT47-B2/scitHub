package net.dsa.scitHub.dto;

import java.util.List;
import java.util.OptionalDouble;

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
    private Double averageRating;

    public static CompanyDTO convertToCompanyDTO(Company entity) {
        
        //CompanyReview 목록에서 평균 평점 계산
        Double avgRating = 0.0;
        if (entity.getReviews() != null && !entity.getReviews().isEmpty()) {
            OptionalDouble optionalAvg = entity.getReviews().stream()
                .mapToDouble(CompanyReview::getRating)
                .average();
            if (optionalAvg.isPresent()) {
                avgRating = Math.round(optionalAvg.getAsDouble() * 10) / 10.0; //소수점 첫째자리까지 반올림
            }
        }

        return CompanyDTO.builder()
            .companyId(entity.getCompanyId())
            .name(entity.getName())
            .logoUrl(entity.getLogoUrl())
            .industry(entity.getIndustry())
            .type(entity.getType())
            .location(entity.getLocation())
            .headcount(entity.getHeadcount())
            .averageRating(avgRating) //계산된 평균 평점을 DTO에 넣기
            .build();
    }

}
