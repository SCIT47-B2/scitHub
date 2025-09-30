package net.dsa.scitHub.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import net.dsa.scitHub.dto.CommentDTO;
import net.dsa.scitHub.dto.BoardDTO;
import net.dsa.scitHub.dto.CourseDTO;
import net.dsa.scitHub.dto.CourseReviewDTO;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.dto.PostDTO;
import net.dsa.scitHub.service.CommunityService;
import net.dsa.scitHub.service.CourseReviewService;
import net.dsa.scitHub.service.CourseService;
import net.dsa.scitHub.repository.user.UserRepository;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;


@Slf4j
@Controller
@RequestMapping("community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService cs;
    private final UserRepository ur;

    @Value("${board.pageSize}")
	int pageSize;				// 페이지당 글 수

	@Value("${board.linkSize}")
	int linkSize;				// 페이지 이동 링크 수

    @ModelAttribute("menuItems")
    public List<MenuItem> menuItems() {
        return List.of(
            new MenuItem("掲示板", "/community/home"),
            new MenuItem("講義評", "/community/courseList")
        );
    }

    @ModelAttribute("boardMap")
    public Map<String, String> boardMap() {
        Map<String, String> boardMap = new LinkedHashMap<String,String>();
        // key:value = 영어이름(DB에 있는):일본어이름
        boardMap.put("free", "自由掲示板");
        boardMap.put("it", "IT情報");
        boardMap.put("japanese", "日本語情報");
        boardMap.put("jpCulture", "日本文化&生活情報");
        boardMap.put("job", "就活情報&コツ");
        boardMap.put("hobby", "趣味&旅行&グルメ情報");
        boardMap.put("certificate", "資格情報");
        boardMap.put("graduated", "卒業生掲示板");
        boardMap.put("qna", "Q&A");

        log.debug("게시글 맵 정보 : {}", boardMap);
        return boardMap;
    }

    // 게시판 관련 ---------------------------------------------------------------------------------------------
    /**
     * 커뮤니티 홈 페이지 요청
     * @param model
     * @return community/home.html
     */
    @GetMapping({"", "/", "home"})
    public String communityPage(
        Model model,
        @AuthenticationPrincipal UserDetails user
    ) {
        // return "community/home"; // templates/community/home.html
        return "redirect:board?name=free";  // 일단 자유게시판 페이지로 이동시키기
    }

    /**
     * 각 게시판 페이지로 이동
     * @param model
     * @return community/board.html
     */
    @GetMapping("board")
    public String gotoBoard(
        @RequestParam("name") String name,
        Model model
    ) {
        try {
            BoardDTO boardDTO = cs.getBoard(name);
            model.addAttribute("boardName", boardDTO.getName());
            model.addAttribute("boardId", boardDTO.getBoardId());
            return "community/board";     // templates/community/board
            // 페이징된 게시글 불러오기와 렌더링은 비동기 처리로 진행함
        } catch (Exception e) {
            // 게시판 조회 실패 시 홈 화면으로
            log.debug("게시판 조회 실패 : {}", e.getMessage());
            return "redirect:home";
        }
    }

    /**
     * 페이징된 게시글 조회, 또는 페이징된 검색 목록 조회
     * @param searchFrom
     * @param searchType
     * @param keyWord
     * @param pageable
     * @return
     */
    @GetMapping("getBoard")
    public ResponseEntity<Page<PostDTO>> getPostsByBoard(
        @RequestParam("boardId") Integer boardId,
        @RequestParam(name = "searchType", required = false) String searchType,
        @RequestParam(name = "keyword", required = false) String keyword,
        @PageableDefault(size = 10, sort = {"createdAt", "postId"}, direction = Sort.Direction.DESC) Pageable pageable) {

        // 수신 데이터 로그
        log.debug("출력할 게시판의 id : {}", boardId);
        log.debug("검색 범위 : {}", searchType);
        log.debug("검색 키워드 : {}", keyword);
        log.debug("pageable 정보 : {}", pageable);

        // postDTO 페이지 생성
        Page<PostDTO> postPage;

        try {
            // keyword가 존재하면 검색 로직 수행, 없으면 일반 목록 조회
            if (keyword != null && !keyword.trim().isEmpty()) {
                postPage = cs.searchPosts(boardId, searchType, keyword, pageable);
            } else {
                postPage = cs.findPostsByBoard(boardId, pageable);
            }
            log.debug("페이지 정보 : {}", postPage);
            /*
            for (PostDTO postDTO : postPage) {
                log.debug("각 게시글 정보 : {}", postDTO);
            }
            */
            return ResponseEntity.ok(postPage);
        } catch (Exception e) {
            log.debug("게시글 검색 중 오류 발생 : {}", e);
            Page<PostDTO> errorPostPage = Page.empty(pageable);
            return ResponseEntity.badRequest().body(errorPostPage);
        }
    }



    // 게시글 작성 관련 ----------------------------------------------------------------------------------------

    /**
     * 글쓰기 페이지로 이동
     * @return community/writeForm.html
     */
    @GetMapping("writePost")
    public String writePostPage(
        @RequestParam("name") String name,
        Model model) {
        model.addAttribute("boardName", name);
        return "community/writeForm";
    }

    /**
     * 작성한 글을 DB에 저장(비동기)
     * @param postDTO
     */
    @PostMapping("write")
    public ResponseEntity<?> writePost(
        PostDTO postDTO,
        @AuthenticationPrincipal UserDetails user
    ) {
        try {
            log.debug("postDTO : {}", postDTO);
            if(!postDTO.getTagList().isEmpty()) {
                for (String tag : postDTO.getTagList()) {
                    log.debug("tag : {}", tag);
                }
            }
            postDTO.setUsername(user.getUsername());
            int postId = cs.makeNewPost(postDTO);
            return ResponseEntity.ok(postId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("ポストの作成に失敗しました。");
        }
    }

    /**
     * 게시글 읽기 폼으로 이동
     * @param postId
     * @param model
     * @return read.html
     */
	@GetMapping("readPost")
	public String read(
        @RequestParam(name = "postId", defaultValue = "0") int postId,
        @AuthenticationPrincipal UserDetails user,
        Model model
    ) {
		try {
			PostDTO postDTO = cs.getPost(postId, true, user.getUsername());
			model.addAttribute("post", postDTO);

            // 태그 리스트를 JSON 문자열 형식으로 변환
            ObjectMapper mapper = new ObjectMapper();
            String tagsJson = mapper.writeValueAsString(postDTO.getTagList());
            log.debug("JSON으로 변환된 태그 데이터 : {}", tagsJson);
            model.addAttribute("tagList", tagsJson);

			log.debug("조회한 글 정보: {}", postDTO);
			return "community/readPost";
		} catch (Exception e) {
			log.debug("[예외 발생] 글 정보 조회 실패..");
			return "redirect:home";
		}
	}

    /**
	 * 게시글 수정 폼으로 이동
	 * @param postId    수정할 글 번호
	 * @param user 		로그인한 사용자 정보
	 * @param Model
	 * @return updateForm.html
	 */
	@GetMapping("updatePost")
	public String update(
        @RequestParam("postId") int postId,
        @AuthenticationPrincipal UserDetails user,
        Model model
    ) {
		try {
			PostDTO postDTO = cs.getPost(postId, false, user.getUsername());
            // 수정 권한이 있는 유저인지 체크
            log.debug("게시글 작성자 : {}", postDTO.getUsername());
            log.debug("현재 로그인 유저 : {}", user.getUsername());
			if (!user.getUsername().equals(postDTO.getUsername())) {
				throw new RuntimeException("修正の権限がありません。");
			}
			model.addAttribute("post", postDTO);

            // 태그 리스트를 JSON 문자열 형식으로 변환
            ObjectMapper mapper = new ObjectMapper();
            String tagsJson = mapper.writeValueAsString(postDTO.getTagList());
            log.debug("JSON으로 변환된 태그 데이터 : {}", tagsJson);
            model.addAttribute("tagList", tagsJson);

			return "community/updateForm";
		} catch (Exception e) {
			log.debug("[예외 발생] {}", e.getMessage());
			return "redirect:";
		}
	}

	/**
	 * 게시글 수정 처리(비동기)
	 * @param postDTO 	수정할 글 정보
	 * @param user 		로그인한 사용자 정보
	 * @param upload 	업로드된 파일
	 * @return 글읽기페이지
	 */
	@PatchMapping("updatePost")
	public ResponseEntity<?> updatePost(
        PostDTO postDTO,
        @AuthenticationPrincipal UserDetails user
    ) {
		try {
            cs.updatePost(postDTO, user.getUsername());
            log.debug("불러온 정보 : {}", postDTO);
			log.debug("수정 성공!");
            return ResponseEntity.ok(postDTO.getPostId());
		} catch (Exception e) {
			log.debug("[예외 발생] {}", e.getMessage());
			return ResponseEntity.badRequest().body("ポストの修正に失敗しました。");
		}
	}

    /**
     * 게시글 삭제
     * @param postId
     * @param user
     */
    @GetMapping("deletePost")
    public String deletePost(
        @RequestParam("postId") Integer postId,
        @AuthenticationPrincipal UserDetails user
    ) {
        try {
            String boardName = cs.deletePost(postId, user.getUsername());
            return "redirect:board?name=" + boardName;
        } catch (Exception e) {
            return "redirect:readPost?postId=" + postId;
        }
    }

    // 게시글 부가 기능 관련--------------------------------------------------------------------------------
    /**
     * 게시글 좋아요 처리(비동기)
     * @param postId
     */
    @PostMapping("likePost")
    public ResponseEntity<?> likePost(
        @RequestParam("postId") int postId,
        @AuthenticationPrincipal UserDetails user
    ) {
        try {
            cs.toggleLikePost(postId, user.getUsername());
            log.debug("좋아요 토글 처리 성공");
            int likeCount = cs.getLikeCount(postId);
            return ResponseEntity.ok(likeCount);
        } catch (Exception e) {
            log.debug("좋아요 처리 실패");
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 게시글 북마크 처리(비동기)
     * @param postId
     */
    @PostMapping("bookmarkPost")
    public ResponseEntity<?> bookmarkPost(
        @RequestParam("postId") int postId,
        @AuthenticationPrincipal UserDetails user
    ) {
        try {
            // 북마크 토글 처리를 한 다음 결과값으로 북마크됐는지 여부를 가져옴
            boolean isBookmarked = cs.toggleBookmarkPost(postId, user.getUsername());
            log.debug("북마크 토글 처리 성공");
            return ResponseEntity.ok(isBookmarked);
        } catch (Exception e) {
            log.debug("북마크 토글 처리 실패");
            return ResponseEntity.badRequest().body("ブックマークの処理に失敗しました。");
        }
    }

    // 댓글 관련-------------------------------------------------------------------------------------------
    /**
     * 댓글 목록 불러오기(비동기)
     * @return List<CommentDTO>
     */
    @GetMapping("commentList")
    public ResponseEntity<List<CommentDTO>> commentList(
        @RequestParam("postId") int postId,
        @AuthenticationPrincipal UserDetails user
    ) {
        log.debug("요청된 게시글 번호 : {}", postId);
        try {
            List<CommentDTO> commentList = cs.getCommentList(postId, user.getUsername());
            for (CommentDTO commentDTO : commentList) {
                log.debug("댓글 정보 : {}", commentDTO);
            }
            return ResponseEntity.ok(commentList);
        } catch (Exception e) {
            log.debug("댓글 불러오기 중 에러 발생 : {}", e.getMessage());
            List<CommentDTO> voidCommentList = new ArrayList<CommentDTO>();
            return ResponseEntity.badRequest().body(voidCommentList);
        }
    }

    /**
     * 댓글 작성(비동기)
     * @param CommentDTO
     */
    @PostMapping("writeComment")
    public ResponseEntity<?> writeComment(
        CommentDTO commentDTO,
        @AuthenticationPrincipal UserDetails user
    ) {
        log.debug("댓글 작성 데이터 : {}", commentDTO);
        try {
            cs.makeNewComment(commentDTO, user.getUsername());
            return ResponseEntity.ok("댓글 작성 성공!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("コメントの作成に失敗しました。");
        }
    }

    /**
     * 댓글 삭제(비동기)
     * @param commentId
     */
    @DeleteMapping("deleteComment/{commentId}")
    public ResponseEntity<?> deleteComment(
        @PathVariable("commentId") Integer commentId,
        @AuthenticationPrincipal UserDetails user
    ) {
        log.debug("삭제할 댓글", commentId);
        try {
            cs.deleteComment(commentId, user.getUsername());
            //cs.deleteComment(commentId, null);
            return ResponseEntity.ok("댓글 삭제 성공!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("コメントの削除に失敗しました。");
        }
    }

    /**
     * 댓글 수정(비동기)
     * @param commentId
     * @param content
     */
    @PatchMapping("updateComment")
    public ResponseEntity<?> updateComment(
        @RequestBody CommentDTO commentDTO,
        @AuthenticationPrincipal UserDetails user
    ) {
        log.debug("수정할 댓글과 내용 : {}", commentDTO);
        try {
            cs.updateComment(commentDTO, user.getUsername());
            return ResponseEntity.ok("댓글 수정 성공!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("コメントの修正に失敗しました。");
        }
    }


    /** 강의평 */

    private final CourseService ccs;
    private final CourseReviewService crs;

    /**
     * 강의평 페이지
     * @param model
     * @param name 검색어 (강의명)
     * @return courseList.html
     */
    @GetMapping("courseList")
    public String courseList(
        Model model,
        @RequestParam(name="name", required=false) String name
    ) {

        // 이름으로 검색하거나 전체 강의 정보 가져오기
        List<CourseDTO> courseList = ccs.getCourseList(name);

        model.addAttribute("courseList", courseList);

        return "community/courseList";
    }

    /**
     * 강의 리뷰 페이지
     * @param courseId
     * @param model
     * @return courseReview.html
     */
    @GetMapping("courseList/readReview")
    public String courseReview(
        @RequestParam(name="id", required=true) Integer courseId,
        Model model,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 현재 로그인한 사용자의 ID를 가져옴. 비로그인 상태일 수 있으므로 null 체크가 필요
        Integer currentUserId = (userDetails != null) ? ur.findByUsername(userDetails.getUsername()).get().getUserId() : null;

        CourseDTO course = ccs.selectById(courseId);
        // 서비스 메서드에 현재 사용자 ID를 전달
        List<CourseReviewDTO> reviews = crs.selectByCourseId(courseId, currentUserId);

        model.addAttribute("course", course);
        model.addAttribute("reviews", reviews);

        return "community/courseReview";
    }

    // 리뷰 삭제를 위한 DELETE 메서드
    @DeleteMapping("courseReview/{reviewId}")
    public ResponseEntity<?> deleteReview(
        @PathVariable("reviewId") Integer reviewId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ログインが必要です。");
        }
        try {
            Integer currentUserId = ur.findByUsername(userDetails.getUsername()).get().getUserId();
            crs.deleteReview(reviewId, currentUserId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("리뷰 삭제 중 에러 발생", e);
            return ResponseEntity.badRequest().body("レビューの削除中にエラーが発生しました。");
        }
    }


    /**
     * 강의 리뷰 작성 처리(비동기)
     * @param courseId
     * @return
     */
    @PostMapping("/courseReview/{courseId}")
    public ResponseEntity<String> createReview(
        @PathVariable("courseId") Integer courseId,
        // @ModelAttribute를 통해 form에서 전송된 데이터(rating, commentText)가 CourseReviewDTO에 자동으로 바인딩
        @ModelAttribute CourseReviewDTO reviewDTO,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ログインが必要です.");
        }
        try {
            // 서비스 레이어로 DTO를 전달하여 리뷰 생성 로직을 위임
            crs.createReview(courseId, userDetails.getUsername(), reviewDTO);
            return ResponseEntity.ok("レビューが正常に登録されました.");
        } catch (IllegalStateException e) {
            log.warn("강의 리뷰 중복 등록 시도: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("강의 리뷰 등록 실패", e); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("レビューの登録中にエラーが発生しました.");
        }

    }

}
