package net.dsa.scitHub.controller;

import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.CommentDTO;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.dto.MypageDTO;
import net.dsa.scitHub.dto.PostDTO;
import net.dsa.scitHub.dto.PostDetailDTO;
import net.dsa.scitHub.dto.UserManageDTO;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.service.BoardService;
import net.dsa.scitHub.service.CommunityService;
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
    private final CommunityService cs;

	@Value("${file.uploadPath}")
	String uploadPath;			// 첨부파일 저장 경로

	@Value("${board.pageSize}")
	int pageSize;				// 페이지당 글 수

	@Value("${board.linkSize}")
	int linkSize;				// 페이지 이동 링크 수

    /**
     * 운영실 메뉴 아이템
     * @return 메뉴 아이템 리스트
     */
    @ModelAttribute("menuItems")
    public List<MenuItem> menuItems(@AuthenticationPrincipal(expression = "roleName") String role) {
        List<MenuItem> menuItems = new ArrayList<>();

        menuItems.add(new MenuItem("お知らせ", "/admin/announcement"));
        menuItems.add(new MenuItem("お問い合わせ", "/admin/inquiry"));

        if ("ROLE_ADMIN".equals(role)) {
            menuItems.add(new MenuItem("会員管理", "/admin/manageUser"));
        }

        return menuItems;
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
        int boardId = bs.getBoardIdFromName(boardName);
        int upperBoardId = boardName.equals("announcement") ? bs.getBoardIdFromName("announcementIt") : bs.getBoardIdFromName("announcement");
        int lowerBoardId = boardName.equals("announcementJp") ? bs.getBoardIdFromName("announcementIt") : bs.getBoardIdFromName("announcementJp");

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
     * 공지사항 글 작성 페이지
     * @return 뷰 이름
     */
    @GetMapping("announcement/write")
    public String announcementWriteForm(
        @RequestParam(name = "boardName", defaultValue = "announcement") String boardName,
        Model model
    ) {
        int boardId = bs.getBoardIdFromName(boardName);

        PostDTO postDTO = new PostDTO();
        postDTO.setBoardId(boardId);

        log.debug("공지사항 글 작성 페이지 요청: boardName={}, boardId={}", boardName, boardId);

        model.addAttribute("postDTO", postDTO);
        model.addAttribute("boardName", boardName);

        return "admin/announcementWriteForm";
    }

    /**
     * 공지사항 글 작성 처리 (AJAX)
     * @param postDTO 글 정보
     * @param userId 사용자 ID
     * @return ResponseEntity with saved post ID or error message
     */
    @PostMapping("announcement/write")
    @ResponseBody
    public ResponseEntity<?> announcementWriteSubmit(
        PostDTO postDTO,
        @AuthenticationPrincipal(expression = "userId") Integer userId
    ) {
        try {
            // DTO에 사용자 ID 설정
            postDTO.setUserId(userId);
            postDTO.setViewCount(0);

            Post savedPost = ps.savePostAndReturnEntity(postDTO);
            log.debug("글 저장 성공: {}", savedPost);

            return ResponseEntity.ok(savedPost.getPostId());

        } catch (Exception e) {
            log.error("[예외 발생] 글 저장 실패: {}", e.getMessage());

            return ResponseEntity.badRequest().body(String.format("글 저장 중 오류가 발생했습니다: %s", e.getMessage()));
        }
    }

    /**
     * 공지사항 글 상세보기 페이지
     * @param postId 조회할 글 ID
     * @param user 현재 로그인한 사용자 정보
     * @param model 뷰에 데이터를 전달할 객체
     * @return "admin/announcementRead" 뷰 템플릿
     */
	@GetMapping("announcement/read")
	public String announcementRead(
        @RequestParam(name = "postId", defaultValue = "0") int postId,
        @ModelAttribute("currentUser") MypageDTO currentUser,
        Model model
    ) {
        log.debug("공지사항 글 상세보기 요청: postId={}", postId);

		try {
			PostDTO postDTO = cs.getPost(postId, true, currentUser.getUserName());
			model.addAttribute("post", postDTO);

			log.debug("조회한 글 정보: {}", postDTO);
			return "admin/announcementRead";
		} catch (Exception e) {
			log.debug("[예외 발생] 글 정보 조회 실패..");
			return "redirect:/admin/announcement";
		}
	}

    /**
     * 공지사항 글 수정 페이지
     * @param postId 수정할 글 ID
     * @param currentUser 현재 로그인한 사용자 정보
     * @param model 뷰에 데이터를 전달할 객체
     * @return "admin/announcementUpdateForm" 뷰 템플릿
     */
	@GetMapping("announcement/update")
	public String announcementUpdate(
        @RequestParam("postId") int postId,
        @ModelAttribute("currentUser") MypageDTO currentUser,
        Model model
    ) {
		try {
			PostDTO postDTO = cs.getPost(postId, false, currentUser.getUserName());
            log.debug("수정할 글 정보: {}", postDTO);
            // 수정 권한이 있는 유저인지 체크
            log.debug("게시글 작성자 : {}", postDTO.getUsername());
            log.debug("현재 로그인 유저 : {}", currentUser.getUserName());
			if (!currentUser.getUserName().equals(postDTO.getUsername()) && !currentUser.getRole().equals("ROLE_ADMIN")) {
				throw new RuntimeException("修正の権限がありません。");
			}
			model.addAttribute("postDTO", postDTO);

			return "admin/announcementUpdateForm";
		} catch (Exception e) {
			log.debug("[예외 발생] {}", e.getMessage());
			return "redirect:";
		}
	}

    /**
     * 공지사항 글 수정 처리 (AJAX)
     * @param postDTO 수정할 게시글 정보
     * @param currentUser 현재 로그인한 사용자 정보
     * @return ResponseEntity with updated post ID or error message
     */
    @ResponseBody
	@PostMapping("announcement/update")
	public ResponseEntity<?> announcementUpdate(
        PostDTO postDTO,
        @ModelAttribute("currentUser") MypageDTO currentUser
    ) {
		try {
            PostDTO updatedPost = ps.updatePost(postDTO, currentUser.getUserId(), currentUser.getRole());
            log.debug("게시글 수정 성공: {}", updatedPost);

            return ResponseEntity.ok(updatedPost.getPostId());
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 게시글 수정 실패: {}", e.getMessage());

            return ResponseEntity.status(404).body("게시글을 찾을 수 없습니다.");
        } catch (SecurityException e) {
            log.error("[예외 발생] 게시글 수정 권한 없음: {}", e.getMessage());

            return ResponseEntity.status(403).body("게시글 수정 권한이 없습니다.");
        } catch (Exception e) {
            log.error("[예외 발생] 게시글 수정 중 오류: {}", e.getMessage());

            return ResponseEntity.status(500).body("게시글 수정 중 오류가 발생했습니다.");
        }
	}

    /**
     * 공지사항 글 삭제
     * @param postId 삭제할 글 ID
     * @param currentUser 현재 로그인한 사용자 정보
     * @return 리다이렉트 URL
     */
    @GetMapping("announcement/delete")
    public String deleteAnnouncement(
        @RequestParam("postId") Integer postId,
        @ModelAttribute("currentUser") MypageDTO currentUser
    ) {
        try {
            ps.deletePost(postId, currentUser.getUserId(), currentUser.getRole());
            log.debug("게시글 삭제 성공!");

            return "redirect:/admin/announcement";
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 게시글 삭제 실패: {}", e.getMessage());

            return "redirect:/admin/announcement?error=true";
        } catch (SecurityException e) {
            log.error("[예외 발생] 게시글 삭제 권한 없음: {}", e.getMessage());

            return "redirect:/admin/announcement?accessDenied=true";
        }
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
        int boardId = bs.getBoardIdFromName(boardName);

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

    /**
     * 운영실 문의글 수정 (AJAX)
     * @param postDTO 수정할 게시글 정보
     * @param currentUser 현재 로그인한 사용자 정보
     * @return ResponseEntity with updated post or error message
     */
    @PostMapping("inquiry/updatePost")
    @ResponseBody
    public ResponseEntity<?> updatePost(
        PostDTO postDTO,
        @ModelAttribute("currentUser") MypageDTO currentUser
    ) {
        log.debug("AJAX 문의글 수정 요청: postDTO={}, userId={}", postDTO, currentUser.getUserId());
        try {
            PostDTO updatedPost = ps.updatePost(postDTO, currentUser.getUserId(), currentUser.getRole());
            log.debug("게시글 수정 성공: {}", updatedPost);

            return ResponseEntity.ok(updatedPost);
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 게시글 수정 실패: {}", e.getMessage());

            return ResponseEntity.status(404).body("게시글을 찾을 수 없습니다.");
        } catch (SecurityException e) {
            log.error("[예외 발생] 게시글 수정 권한 없음: {}", e.getMessage());

            return ResponseEntity.status(403).body("게시글 수정 권한이 없습니다.");
        } catch (Exception e) {
            log.error("[예외 발생] 게시글 수정 중 오류: {}", e.getMessage());

            return ResponseEntity.status(500).body("게시글 수정 중 오류가 발생했습니다.");
        }
    }

    /**
     * 운영실 문의글 삭제
     * @param postId 삭제할 문의글 ID
     * @param currentUser 현재 로그인한 사용자 정보
     * @return 리다이렉트 URL
     */
    @PostMapping("inquiry/deletePost")
    public String deleteInquiryPost(
        @RequestParam("postId") int postId,
        @ModelAttribute("currentUser") MypageDTO currentUser
    ) {
        log.debug("운영실 문의글 삭제 요청: postId={}", postId);

        try {
            ps.deletePost(postId, currentUser.getUserId(), currentUser.getRole());
            log.debug("게시글 삭제 성공!");

            return "redirect:/admin/inquiry";
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 게시글 삭제 실패: {}", e.getMessage());

            return "redirect:/admin/inquiry?error=true";
        } catch (SecurityException e) {
            log.error("[예외 발생] 게시글 삭제 권한 없음: {}", e.getMessage());

            return "redirect:/admin/inquiry?accessDenied=true";
        }
    }

    /**
     * 운영실 문의글에 답변 추가
     * @param postId 답변을 추가할 문의글 ID
     * @param content 답변 내용
     * @param userId 답변 작성자 ID
     * @return 리다이렉트 URL
     */
    @PostMapping("inquiry/addComment")
    public String addComment(
        @RequestParam("postId") int postId,
        @RequestParam("content") String content,
        @AuthenticationPrincipal(expression = "userId") Integer userId
    ) {
        log.debug("답변 등록 요청: postId={}, content={}, userId={}", postId, content, userId);
        try {
            ps.saveComment(postId, content, userId);
            log.debug("답변 저장 성공!");

            return "redirect:/admin/inquiryRead?postId=" + postId;
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 답변 저장 실패: {}", e.getMessage());

            return "redirect:/admin/inquiry?error=true";
        }
    }

    /**
     * 운영실 문의글 답변 수정 (AJAX)
     * @param commentId 수정할 답변 ID
     * @param content 수정된 답변 내용
     * @param currentUser 현재 로그인한 사용자 정보
     * @return ResponseEntity with updated comment or error message
     */
    @PostMapping("inquiry/updateComment")
    @ResponseBody
    public ResponseEntity<?> updateComment(
        @RequestParam("commentId") Integer commentId,
        @RequestParam("content") String content,
        @ModelAttribute("currentUser") MypageDTO currentUser
    ) {
        log.debug("AJAX 답변 수정 요청: commentId={}, content={}, userId={}", commentId, content, currentUser.getUserId());
        try {
            CommentDTO updatedComment = ps.updateComment(commentId, content, currentUser.getUserId(), currentUser.getRole());
            log.debug("답변 수정 성공: {}", updatedComment);

            return ResponseEntity.ok(updatedComment);
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 답변 수정 실패: {}", e.getMessage());

            return ResponseEntity.status(404).body("답변을 찾을 수 없습니다.");
        } catch (SecurityException e) {
            log.error("[예외 발생] 답변 수정 권한 없음: {}", e.getMessage());

            return ResponseEntity.status(403).body("답변 수정 권한이 없습니다.");
        } catch (Exception e) {
            log.error("[예외 발생] 답변 수정 중 오류: {}", e.getMessage());

            return ResponseEntity.status(500).body("답변 수정 중 오류가 발생했습니다.");
        }
    }

    /**
     * 운영실 문의글 답변 삭제
     * @param commentId 삭제할 답변 ID
     * @param postId 답변이 달린 문의글 ID (리다이렉트용)
     * @param currentUser 현재 로그인한 사용자 정보
     * @return 리다이렉트 URL
     */
    @PostMapping("inquiry/deleteComment")
    public String deleteComment(
        @RequestParam("commentId") int commentId,
        @RequestParam("postId") int postId,
        @ModelAttribute("currentUser") MypageDTO currentUser
    ) {
        log.debug("운영실 문의글 답변 삭제 요청: commentId={}", commentId);

        try {
            ps.deleteComment(commentId, currentUser.getUserId(), currentUser.getRole());
            log.debug("답변 삭제 성공!");

            return "redirect:/admin/inquiryRead?postId=" + postId;
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 답변 삭제 실패: {}", e.getMessage());

            return "redirect:/admin/inquiry?error=true";
        } catch (SecurityException e) {
            log.error("[예외 발생] 답변 삭제 권한 없음: {}", e.getMessage());

            return "redirect:/admin/inquiry?accessDenied=true";
        }
    }

    @GetMapping("report")
    public String reportPage(Model model) {
        return "admin/report";
    }

    /**
     * 회원 관리 페이지
     * @param model 모델
     * @param page 페이지 번호
     * @param searchType 검색 유형
     * @param searchWord 검색어
     * @param cohortNo 기수 필터
     * @param role 역할 필터
     * @return 뷰 이름
     */
    @GetMapping("manageUser")
    public String manageUserPage(
        Model model,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "searchType", defaultValue = "all") String searchType,
        @RequestParam(name = "searchWord", defaultValue = "") String searchWord,
        @RequestParam(name = "cohortNo", defaultValue = "0") int cohortNo,
        @RequestParam(name = "role", defaultValue = "ALL") String role
    ) {
        log.debug("회원 관리 페이지 요청: page={}, searchType={}, searchWord={}, cohortNo={}, role={}", page, searchType, searchWord, cohortNo, role);
        Page<UserManageDTO> userPage = us.findUsersByCriteria(page, pageSize, searchType, searchWord, cohortNo, role);
        List<Integer> cohortList = us.findAllCohorts();

        log.debug("페이징된 사용자 목록: {}", userPage.getContent());
        log.debug("현재 페이지: {}", userPage.getNumber());
        log.debug("전체 개수: {}", userPage.getTotalElements());
        log.debug("전체 페이지 수: {}", userPage.getTotalPages());
        log.debug("기수 목록: {}", cohortList);

        model.addAttribute("userPage", userPage);   // 페이징된 사용자 목록
        model.addAttribute("cohortList", cohortList); // 기수 목록

        // 검색 및 필터링 파라미터 유지
        model.addAttribute("page", page);
        model.addAttribute("searchType", searchType);
        model.addAttribute("searchWord", searchWord);
        model.addAttribute("cohortNo", cohortNo);
        model.addAttribute("role", role);
        model.addAttribute("linkSize", linkSize);

        return "admin/manageUser";
    }

    /**
     * 사용자 활성/비활성 상태 토글 (AJAX)
     * @param userId 대상 사용자의 ID
     * @return ResponseEntity with updated user info or error message
     */
    @PostMapping("manageUser/toggleStatus")
    @ResponseBody
    public ResponseEntity<?> toggleUserStatus(@RequestParam("userId") Integer userId) {
        log.debug("AJAX 사용자 상태 토글 요청: userId={}", userId);
        try {
            UserManageDTO updatedUser = us.toggleUserStatus(userId);
            log.debug("사용자 상태 토글 성공: {}", updatedUser);

            return ResponseEntity.ok(updatedUser);
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 사용자 상태 토글 실패: {}", e.getMessage());

            return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
        } catch (Exception e) {
            log.error("[예외 발생] 사용자 상태 토글 중 오류: {}", e.getMessage());

            return ResponseEntity.status(500).body("사용자 상태 토글 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자 권한 변경 (AJAX)
     * @param userId 대상 사용자의 ID
     * @return ResponseEntity with updated user info or error message
     */
    @PostMapping("manageUser/changeRole")
    @ResponseBody
    public ResponseEntity<?> changeUserRole(@RequestParam("userId") Integer userId) {
        log.debug("AJAX 사용자 권한 변경 요청: userId={}", userId);
        try {
            UserManageDTO updatedUser = us.changeUserRole(userId);
            log.debug("사용자 권한 변경 성공: {}", updatedUser);

            return ResponseEntity.ok(updatedUser);
        } catch (EntityNotFoundException e) {
            log.error("[예외 발생] 사용자 권한 변경 실패: {}", e.getMessage());

            return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
        } catch (Exception e) {
            log.error("[예외 발생] 사용자 권한 변경 중 오류: {}", e.getMessage());

            return ResponseEntity.status(500).body("사용자 권한 변경 중 오류가 발생했습니다.");
        }
    }
}
