package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import net.dsa.scitHub.dto.CompanyDTO;
import net.dsa.scitHub.dto.CompanyReviewDTO;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.enums.CompanyType;
import net.dsa.scitHub.enums.Industry;
import net.dsa.scitHub.service.CompanyReviewService;
import net.dsa.scitHub.service.CompanyService;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ArchiveController {

    private final CompanyService cs;
    private final CompanyReviewService crs;

    /**
     * 아카이브 메인 페이지 (회사 목록) 요청
     * @param model
     * @param industry
     * @param type
     * @param location
     * @param headcount
     * @param averageRating
     * @param name
     * @return //templates/archive/companyList.html
     */
    @GetMapping({"/archive", "/archive/companyList"})
    public String archivePage(
            Model model,
            @RequestParam(name="industry", required=false) Industry industry,
            @RequestParam(name="type", required=false) CompanyType type,
            @RequestParam(name="location", required=false) String location,
            @RequestParam(name="headcount", required=false) String headcount,
            @RequestParam(name="rating", required=false) String averageRating,
            @RequestParam(name="name", required=false) String name,
            @RequestParam(name = "page", defaultValue = "0") int page
        ) {
        List<MenuItem> menuItems = List.of(
            new MenuItem("会社レビュー", "/archive/companyList")
            // new MenuItem("사진 앨범", "/archive/photoAlbum")
        );
        model.addAttribute("menuItems", menuItems);

        // 정렬 로직
        Sort sort = Sort.by("name").ascending(); // 기본 정렬
        if (headcount != null) {
            sort = Sort.by(headcount.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, "headcount");
        } else if (averageRating != null) {
            // 평균 평점 정렬은 복잡하므로 Repository에서 직접 처리하거나 별도 로직 필요
            // 여기서는 간단하게 ID 기준 정렬로 대체합니다. 필요 시 확장 가능.
            sort = Sort.by(averageRating.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, "companyId");
        }

        Pageable pageable = PageRequest.of(page, 15, sort);
        Page<CompanyDTO> companyPage = cs.findCompanies(name, industry, type, location, pageable);

        //필터링된 회사 목록 model에 담기
        model.addAttribute("companyPage", companyPage);
        model.addAttribute("companyList", companyPage.getContent());

        // 현재 필터 값을 모델에 추가하여 링크 생성 시 사용
        model.addAttribute("name", name);
        model.addAttribute("industry", industry);
        model.addAttribute("type", type);
        model.addAttribute("location", location);
        model.addAttribute("headcount", headcount);
        model.addAttribute("rating", averageRating);

        return "archive/companyList"; // templates/archive/companyList.html
    }

    /**
     * 회사 리뷰 페이지 요청
     * @param companyId
     * @param model
     * @return
     */
    @GetMapping("/archive/companyReview")
    public String companyReview(
            @RequestParam(name="id", required=true) Integer companyId,
            Model model
        ) {
        List<MenuItem> menuItems = List.of(
            new MenuItem("会社レビュー", "/archive/companyList")
            // new MenuItem("사진 앨범", "/archive/photoAlbum")
        );
        model.addAttribute("menuItems", menuItems);

        //회사 정보 가져오기
        CompanyDTO company = cs.selectById(companyId);

        //해당 회사의 리뷰 정보 가져오기
        List<CompanyReviewDTO> reviews = crs.selectByCompanyId(companyId);

        //model에 담기
        model.addAttribute("company", company);
        model.addAttribute("reviews", reviews);

        return "archive/companyReview"; // templates/archive/companyReview.html
    }

    /**
     * 리뷰 작성 처리
     * @param companyId
     * @param reviewDTO
     * @param userDetails
     * @return
     */
    @PostMapping("/archive/companyReview/{companyId}")
    public ResponseEntity<String> createReview(@PathVariable("companyId") Integer companyId,
                                               @ModelAttribute CompanyReviewDTO reviewDTO,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        // @AuthenticationPrincipal을 통해 현재 로그인한 사용자 정보를 가져옵니다.
        // Spring Security 설정이 필요합니다.
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ログインが必要です.");
        }

        try {
            // 서비스 로직 호출
            crs.createReview(companyId, userDetails.getUsername(), reviewDTO);
            return ResponseEntity.ok("レビューが正常に登録されました.");
        } catch (Exception e) {
            // 예외 발생 시 서버 로그에 기록하고 클라이언트에게 에러 메시지 전송
            // log.error("Review creation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("レビューの登録中にエラーが発生しました.");
        }
    }

}
