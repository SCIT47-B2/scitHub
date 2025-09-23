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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import net.dsa.scitHub.repository.user.UserRepository;
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
     * @param model         뷰에 데이터를 전달하기 위한 Model 객체
     * @param industry      필터링할 업종
     * @param type          필터링할 회사 유형
     * @param location      필터링할 지역
     * @param headcount     정렬할 직원 수 (asc/desc)
     * @param averageRating 정렬할 평균 평점 (asc/desc)
     * @param name          검색할 회사 이름
     * @param page          페이지 번호 (0부터 시작)
     * @return              회사 목록 뷰 (archive/companyList)
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
        // 사이드바 메뉴 아이템 설정
        List<MenuItem> menuItems = List.of(
            new MenuItem("会社レビュー", "/archive/companyList")
            // new MenuItem("사진 앨범", "/archive/photoAlbum")
        );
        model.addAttribute("menuItems", menuItems);

        // 정렬 로직: 기본 정렬은 회사명 오름차순
        Sort sort = Sort.by("name").ascending();
        // 직원 수(headcount) 파라미터가 있으면 해당 기준으로 정렬
        if (headcount != null) {
            sort = Sort.by(headcount.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, "headcount");
        // 평균 평점(averageRating) 파라미터가 있으면 해당 기준으로 정렬
        } else if (averageRating != null) {
            // 실제 정렬은 Repository의 JPQL에서 처리하지만, Controller에서는 정렬 기준 필드명을 지정
            sort = Sort.by(averageRating.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, "companyId");
        }

        // 페이징 객체 생성 (페이지당 15개, 정렬 조건 적용)
        Pageable pageable = PageRequest.of(page, 15, sort);
        // 서비스 레이어를 호출하여 필터링 및 페이징된 회사 목록 조회
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

    private final UserRepository ur;

    /**
     * 회사 리뷰 페이지 요청
     * @param companyId
     * @param model
     * @return
     */
    @GetMapping("/archive/companyReview")
    public String companyReview(
            @RequestParam(name="id", required=true) Integer companyId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
        ) {
        // 사이드바 메뉴 아이템 설정
        List<MenuItem> menuItems = List.of(
            new MenuItem("会社レビュー", "/archive/companyList")
            // new MenuItem("사진 앨범", "/archive/photoAlbum")
        );
        model.addAttribute("menuItems", menuItems);

        // 현재 로그인한 사용자의 ID를 가져옴.
        // 비로그인 상태(userDetails == null)를 고려하여 null을 허용
        Integer currentUserId = (userDetails != null) ? ur.findByUsername(userDetails.getUsername()).get().getUserId() : null;

        //회사 정보 가져오기
        CompanyDTO company = cs.selectById(companyId);

        // 서비스 메서드에 현재 사용자 ID를 전달
        List<CompanyReviewDTO> reviews = crs.selectByCompanyId(companyId, currentUserId);

        // 뷰에 데이터 전달
        model.addAttribute("company", company);
        model.addAttribute("reviews", reviews);

        return "archive/companyReview"; // templates/archive/companyReview.html
    }

    /**
     * 리뷰 작성 처리
     * @param companyId   리뷰를 작성할 회사의 ID
     * @param reviewDTO   폼에서 전송된 리뷰 데이터 (평점, 내용)
     * @param userDetails 현재 로그인한 사용자 정보
     * @return            처리 결과에 대한 ResponseEntity
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
        } catch (IllegalStateException e) {
            log.warn("강의 리뷰 중복 등록 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 예외 발생 시 서버 로그에 기록하고 클라이언트에게 에러 메시지 전송
            log.error("강의 리뷰 등록 실패", e); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("レビューの登録中にエラーが発生しました.");
        }
    }

    /**
     * 리뷰 삭제 처리
     * @param reviewId    삭제할 리뷰의 ID
     * @param userDetails 현재 로그인한 사용자 정보
     * @return            처리 결과에 대한 ResponseEntity
     */
    @DeleteMapping("/archive/companyReview/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable("reviewId") Integer reviewId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ログインが必要です.");
        }

        try {
            // 현재 로그인한 사용자의 ID 조회
            Integer currentUserId = ur.findByUsername(userDetails.getUsername()).get().getUserId();
            // 서비스 레이어를 호출하여 리뷰 삭제 (작성자 본인인지 권한 확인 포함)
            crs.deleteReview(reviewId, currentUserId);
            return ResponseEntity.ok("レビューが正常に削除されました.");
        } catch (IllegalStateException e) {
            log.warn("会社レビュー 삭제 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage()); // 권한 없는 경우 403 Forbidden
        } catch (Exception e) {
            log.error("会社レビュー 삭제 실패", e); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("レビューの削除中にエラーが発生しました.");
        }
    }

}
