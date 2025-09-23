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
    public List<CompanyReviewDTO> selectByCompanyId(Integer companyId, Integer currentUserId) {

        List<CompanyReview> reviewEntities = crr.findByCompanyWithUser(companyId);

        return reviewEntities.stream()
            .map(reviewEntity -> CompanyReviewDTO.convertToCompanyReviewDTO(reviewEntity, currentUserId))
            .collect(Collectors.toList());

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

        // 이미 해당 회사에 리뷰를 작성했는지 확인
        crr.findByCompany_CompanyIdAndUser_UserId(companyId, user.getUserId()).ifPresent(review -> {
            throw new IllegalStateException("1つの会社につき、レビューは1件のみ登録できます。");
        });

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

    /**
     * 리뷰 삭제
     * @param reviewId
     * @param currentUserId
     */
    public void deleteReview(Integer reviewId, Integer currentUserId) {
        CompanyReview review = crr.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("該当するレビューが見つかりません。"));

        if (!review.getUser().getUserId().equals(currentUserId)) {
            throw new IllegalStateException("レビューを削除する権限がありません。");
        }

        crr.delete(review);
    }


}
