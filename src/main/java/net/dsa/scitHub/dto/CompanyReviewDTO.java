package net.dsa.scitHub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.company.Company;
import net.dsa.scitHub.entity.company.CompanyReview;
import net.dsa.scitHub.entity.user.User;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyReviewDTO {

    Integer companyReviewId;
    Company company;
    User user;
    Byte rating;
    String content;

    public static CompanyReviewDTO convertToCompanyReviewDTO(CompanyReview entity) {
        return CompanyReviewDTO.builder()
            .companyReviewId(entity.getCompanyReviewId())
            .company(entity.getCompany())
            .user(entity.getUser())
            .rating(entity.getRating())
            .content(entity.getContent())
            .build();
    }
}
