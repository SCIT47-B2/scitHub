package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.dto.PostDTO;
import net.dsa.scitHub.service.BoardService;
import net.dsa.scitHub.service.PostService;

@Controller
@Slf4j
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {

    private final PostService ps;
    private final BoardService bs;
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

    // 메뉴 항목
    @ModelAttribute("menuItems")
    public List<MenuItem> menuItems() {
        return List.of(
            new MenuItem("공지사항", "/admin/announcement"),
            new MenuItem("문의", "/admin/support"),
            new MenuItem("신고", "/admin/report"),
            new MenuItem("회원관리", "/admin/manageUser")
        );
    }

    // 운영실 페이지 요청
    @GetMapping({"", "/", "announcement"})
    public String adminPage(
        Model model,
        @RequestParam(name = "page", defaultValue = "1") int page,
		@RequestParam(name = "searchType", defaultValue = "all") String searchType,
		@RequestParam(name = "searchWord", defaultValue = "") String searchWord,
        @RequestParam(name = "boardName", defaultValue = "announcement") String boardName
    ) {
        log.debug("운영실 페이지 요청: page={}, searchType={}, searchWord={}, boardName={}", page, searchType, searchWord, boardName);
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

    @GetMapping("support")
    public String supportPage(Model model) {
        return "admin/support";
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
