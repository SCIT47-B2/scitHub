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
    Byte rating;
    String content;

    User user;
    private Integer userCohortNo; // 작성자 기수
    Boolean isAuthor; // 현재 로그인한 사용자가 작성자인지 여부

    public static CompanyReviewDTO convertToCompanyReviewDTO(CompanyReview entity, Integer currentUserId) {
        return CompanyReviewDTO.builder()
            .companyReviewId(entity.getCompanyReviewId())
            .company(entity.getCompany())
            .user(entity.getUser())
            .rating(entity.getRating())
            .content(entity.getContent())
            .userCohortNo(entity.getUser().getCohortNo())
            .isAuthor(currentUserId != null && entity.getUser().getUserId().equals(currentUserId))
            .build();
    }
}
