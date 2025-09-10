package net.dsa.scitHub.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.dsa.scitHub.dto.CompanyReviewDTO;
import net.dsa.scitHub.entity.company.Company;
import net.dsa.scitHub.entity.company.CompanyReview;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.repository.company.CompanyRepository;
import net.dsa.scitHub.repository.company.CompanyReviewRepository;
import net.dsa.scitHub.repository.user.UserRepository;

@Service
@RequiredArgsConstructor
public class CompanyReviewService {

    @Autowired
    private final CompanyRepository cr;
    private final CompanyReviewRepository crr;
    private final UserRepository ur;

    /**
     * 회사 ID에 해당하는 모든 리뷰 목록을 조회
     * @param companyId 조회할 회사 D
     * @return 해당 회사의 리뷰 목록 (CompanyReviewDTO)
     */
    @Transactional(readOnly = true)
    public List<CompanyReviewDTO> selectByCompanyId(Integer companyId) {

        List<CompanyReview> reviewEntities = crr.findByCompany_CompanyId(companyId);

        return reviewEntities.stream()
            .map(CompanyReviewDTO::convertToCompanyReviewDTO)
            .collect(Collectors.toList());

    }

    /**
     * 새로운 회사 리뷰 생성 후 저장
     * @param companyId
     * @param userId
     * @param rating
     * @param content
     */
    public void createReview(Integer companyId, Integer userId, Byte rating, String content) {

        //엔티티 조회하여 DTO로 변환
        Company company = cr.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid company ID: " + companyId));

        User user = ur.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));

        //CompanyReview 엔티티 생성
        CompanyReview newReview = CompanyReview.builder()
            .company(company)
            .user(user)
            .rating(rating)
            .content(content)
            .build();
        //엔티티 저장
        crr.save(newReview);
    }

    /**
     * 리뷰 등록 처리
     * @param companyId
     * @param username
     * @param reviewDTO
     */
    @Transactional
    public void createReview(Integer companyId, String username, CompanyReviewDTO reviewDTO) {
        // 회사 정보 조회
        Company company = cr.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("該当会社が見つかりません. id=" + companyId));

        // 사용자 정보 조회 (username은 보통 email이나 id를 의미)
        User user = ur.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません. username=" + username));

        // DTO를 Entity로 변환
        CompanyReview review = CompanyReview.builder()
                .company(company)
                .user(user)
                .rating(reviewDTO.getRating())
                .content(reviewDTO.getContent())
                .build();

        // 리뷰 저장
        crr.save(review);
    }


}
