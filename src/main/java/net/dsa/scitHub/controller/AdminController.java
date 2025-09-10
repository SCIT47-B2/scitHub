package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.dto.PostDTO;
import net.dsa.scitHub.dto.PostDetailDTO;
import net.dsa.scitHub.service.BoardService;
import net.dsa.scitHub.service.PostService;
import net.dsa.scitHub.service.UserService;

@Controller
@Slf4j
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {

    private final PostService ps;
    private final BoardService bs;
    private final UserService us;
    static private boolean readFlag = false; // 글 읽기 플래그

	@Value("${board.uploadPath}")
	String uploadPath;			// 첨부파일 저장 경로

	@Value("${board.pageSize}")
	int pageSize;				// 페이지당 글 수

	@Value("${board.linkSize}")
	int linkSize;				// 페이지 이동 링크 수

    int boardId;				// 게시판 ID
    int upperBoardId;          // 위쪽 공지 게시판 ID
    int lowerBoardId;          // 아래쪽 공지 게시판 ID

    /**
     * 운영실 메뉴 아이템
     * @return 메뉴 아이템 리스트
     */
    @ModelAttribute("menuItems")
    public List<MenuItem> menuItems() {
        return List.of(
            new MenuItem("お知らせ", "/admin/announcement"),
            new MenuItem("お問い合わせ", "/admin/inquiry"),
            // new MenuItem("신고", "/admin/report"),
            new MenuItem("会員管理", "/admin/manageUser")
        );
    }

    /**
     * 운영실 공지사항 페이지
     * @param model 모델
     * @param page 페이지 번호
     * @param searchType 검색 유형
     * @param searchWord 검색어
     * @param boardName 게시판 이름
     * @return 뷰 이름
     */
    @GetMapping({"", "/", "announcement"})
    public String adminPage(
        Model model,
        @RequestParam(name = "page", defaultValue = "1") int page,
		@RequestParam(name = "searchType", defaultValue = "all") String searchType,
		@RequestParam(name = "searchWord", defaultValue = "") String searchWord,
        @RequestParam(name = "boardName", defaultValue = "announcement") String boardName
    ) {
        log.debug("운영실 공지사항 페이지 요청: page={}, searchType={}, searchWord={}, boardName={}", page, searchType, searchWord, boardName);
        boardId = bs.getBoardIdFromName(boardName);
        upperBoardId = boardName.equals("announcement") ? bs.getBoardIdFromName("announcementIt") : bs.getBoardIdFromName("announcement");
        lowerBoardId = boardName.equals("announcementJp") ? bs.getBoardIdFromName("announcementIt") : bs.getBoardIdFromName("announcementJp");

        log.debug("게시판 ID: {}", boardId);
        if (boardId == -1) {
            log.warn("존재하지 않는 게시판 이름: {}", boardName);
        }

        Page<PostDTO> postPage = ps.getPostsByBoardAndSearchWithPaging(page, pageSize, boardId, searchType, searchWord);
        List<PostDTO> upperPosts = ps.getLatestPosts(upperBoardId, 3);
        List<PostDTO> lowerPosts = ps.getLatestPosts(lowerBoardId, 3);

		log.debug("목록 정보: {}", postPage.getContent());
		log.debug("현재 페이지: {}", postPage.getNumber());
		log.debug("전체 개수: {}", postPage.getTotalElements());
		log.debug("전체 페이지 수: {}", postPage.getTotalPages());
		log.debug("한 페이지 당 글 수: {}", postPage.getSize());
		log.debug("이전 페이지 존재 여부: {}", postPage.hasPrevious());
		log.debug("다음 페이지 존재 여부: {}", postPage.hasNext());
        log.debug("최신 위쪽 공지 3개: {}", upperPosts);
        log.debug("최신 아래쪽 공지 3개: {}", lowerPosts);

        model.addAttribute("postPage", postPage);       // 출력할 글 정보
		model.addAttribute("page", page);               // 현재 페이지
		model.addAttribute("linkSize", linkSize);       // 페이지 이동 링크 수
		model.addAttribute("searchType", searchType);   // 검색기준
		model.addAttribute("searchWord", searchWord);   // 검색어
        model.addAttribute("boardName", boardName);     // 게시판 이름
        model.addAttribute("upperPosts", upperPosts);   // 위쪽 공지 게시글
        model.addAttribute("lowerPosts", lowerPosts);   // 아래쪽 공지 게시글

        return "admin/announcement";
    }

    /**
     * 운영실 문의 페이지
     * @param model 모델
     * @param page 페이지 번호
     * @param searchType 검색 유형
     * @param searchWord 검색어
     * @param boardName 게시판 이름
     * @return 뷰 이름
     */
    @GetMapping("inquiry")
    public String inquiryPage(
        Model model,
        @RequestParam(name = "page", defaultValue = "1") int page,
		@RequestParam(name = "searchType", defaultValue = "all") String searchType,
		@RequestParam(name = "searchWord", defaultValue = "") String searchWord,
        @RequestParam(name = "boardName", defaultValue = "inquiry") String boardName
    ) {
        log.debug("운영실 문의 페이지 요청: page={}, searchType={}, searchWord={}, boardName={}", page, searchType, searchWord, boardName);
        boardId = bs.getBoardIdFromName(boardName);

        log.debug("게시판 ID: {}", boardId);
        if (boardId == -1) {
            log.warn("존재하지 않는 게시판 이름: {}", boardName);
        }

        Page<PostDTO> postPage = ps.getPostsByBoardAndSearchWithPaging(page, pageSize, boardId, searchType, searchWord);

		log.debug("목록 정보: {}", postPage.getContent());
		log.debug("현재 페이지: {}", postPage.getNumber());
		log.debug("전체 개수: {}", postPage.getTotalElements());
		log.debug("전체 페이지 수: {}", postPage.getTotalPages());
		log.debug("한 페이지 당 글 수: {}", postPage.getSize());
		log.debug("이전 페이지 존재 여부: {}", postPage.hasPrevious());
		log.debug("다음 페이지 존재 여부: {}", postPage.hasNext());

        model.addAttribute("postPage", postPage);       // 출력할 글 정보
		model.addAttribute("page", page);               // 현재 페이지
		model.addAttribute("linkSize", linkSize);       // 페이지 이동 링크 수
		model.addAttribute("searchType", searchType);   // 검색기준
		model.addAttribute("searchWord", searchWord);   // 검색어
        model.addAttribute("boardName", boardName);     // 게시판 이름

        return "admin/inquiry";
    }

    /**
     * 운영실 문의 글 제출 처리
     * @param postDTO 글 정보
     * @param userId 사용자 ID
     * @return 리다이렉트 URL
     */
    @PostMapping("inquiry/submit")
    public String submitInquiry(
        PostDTO postDTO,
        @AuthenticationPrincipal(expression = "userId") Integer userId
    ) {
        log.debug("문의 글 제출: title={}, content={}, userId={}", postDTO.getTitle(), postDTO.getContent(), userId);

		postDTO.setBoardId(bs.getBoardIdFromName("inquiry"));
        postDTO.setUserId(userId);
        postDTO.setUserNameKor(us.findNameKorById(userId));
        postDTO.setViewCount(0);
		log.debug("저장할 글 정보: {}", postDTO);

		try {
			ps.savePost(postDTO);
			log.debug("글 저장 성공!");
			return "redirect:/admin/inquiry";
		} catch (Exception e) {
			log.debug("[예외 발생] 글 저장 실패: {}", e.getMessage());
			return "redirect:/admin/inquiry?error=true";
		}
    }

    /**
     * 운영실 문의글 상세보기 페이지
     * @param postId 조회할 문의글 ID
     * @param model 뷰에 데이터를 전달할 객체
     * @return "admin/inquiryRead" 뷰 템플릿
     */
    @GetMapping("inquiryRead")
    public String inquiryRead(
        @RequestParam("postId") int postId,
        Model model
    ) {
        log.debug("운영실 문의글 상세보기 요청: postId={}", postId);

        try {
            PostDetailDTO postDetail = ps.findPostDetailById(postId);
            log.debug("조회된 게시글 상세 정보: {}", postDetail);

            model.addAttribute("post", postDetail);

            return "admin/inquiryRead";
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 게시글을 찾을 수 없습니다: {}", e.getMessage());

            return "redirect:/admin/inquiry?error=true";
        }
    }

    @PostMapping("inquiry/addComment")
    public String addComment(
        @RequestParam("postId") int postId,
        @RequestParam("content") String content,
        @AuthenticationPrincipal(expression = "userId") Integer userId
    ) {
        log.debug("댓글 등록 요청: postId={}, content={}, userId={}", postId, content, userId);
        try {
            ps.saveComment(postId, content, userId);
            log.debug("댓글 저장 성공!");

            return "redirect:/admin/inquiryRead?postId=" + postId;
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 댓글 저장 실패: {}", e.getMessage());

            return "redirect:/admin/inquiry?error=true";
        }
    }

    @GetMapping("report")
    public String reportPage(Model model) {
        return "admin/report";
    }

    @GetMapping("manageUser")
    public String manageUserPage(Model model) {
        return "admin/manageUser";
    }

}
