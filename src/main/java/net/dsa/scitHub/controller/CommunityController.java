package net.dsa.scitHub.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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

import net.dsa.scitHub.dto.BoardDTO;
import net.dsa.scitHub.dto.CommentDTO;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.dto.PostDTO;
import net.dsa.scitHub.service.CommunityService;
import net.dsa.scitHub.service.PostService;

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
    private final PostService ps;

    @Value("${board.pageSize}")
	int pageSize;				// 페이지당 글 수

	@Value("${board.linkSize}")
	int linkSize;				// 페이지 이동 링크 수

    @ModelAttribute("menuItems")
    public List<MenuItem> menuItems() {
        return List.of(
            new MenuItem("게시판 홈", "/community/home"),
            new MenuItem("Q&A", "/community/qna"),
            new MenuItem("강의평", "/community/courseReview")
        );
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
        @AuthenticationPrincipal UserDetails user) {
        return "community/home"; // templates/community/home.html
    }

    // 게시글 작성 관련 ----------------------------------------------------------------------------------------

    /**
     * 글쓰기 페이지로 이동
     * @return community/writeForm.html
     */
    @GetMapping("writePost")
    public String writePostPage() {
        return "community/writeForm";
    }

    /**
     * 작성한 글을 DB에 저장(비동기)
     * @param postDTO
     */
    @PostMapping("write")
    public ResponseEntity<?> writePost(
            PostDTO postDTO,
            @AuthenticationPrincipal UserDetails user) {
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
            return ResponseEntity.badRequest().body("게시글 등록 실패");
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
		    Model model) {
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
			return "redirect:list";
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
            Model model) {
		try {
			PostDTO postDTO = cs.getPost(postId, false, user.getUsername());
            // 수정 권한이 있는 유저인지 체크
            log.debug("게시글 작성자 : {}", postDTO.getUsername());
            log.debug("현재 로그인 유저 : {}", user.getUsername());
			if (!user.getUsername().equals(postDTO.getUsername())) {
				throw new RuntimeException("수정 권한이 없습니다.");
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
			return "redirect:home";
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
            @RequestParam("postId") Integer postId,
			PostDTO postDTO,
			@AuthenticationPrincipal UserDetails user) {
		try {
            cs.updatePost(postDTO, user.getUsername());
            log.debug("불러온 정보 : {}", postDTO);
			log.debug("수정 성공!");
            return ResponseEntity.ok(postDTO.getPostId());
		} catch (Exception e) {
			log.debug("[예외 발생] {}", e.getMessage());
			return ResponseEntity.badRequest().body("게시글 수정 실패");
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
        @AuthenticationPrincipal UserDetails user) {
        try {
            cs.deletePost(postId, user.getUsername());
            return "redirect:home";
        } catch (Exception e) {
            return "redirect:home";
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
            @AuthenticationPrincipal UserDetails user) {
        try {
            cs.toggleLikePost(postId, user.getUsername());
            log.debug("좋아요 성공");
            int likeCount = cs.getLikeCount(postId);
            return ResponseEntity.ok(likeCount);
        } catch (Exception e) {
            log.debug("좋아요 처리 실패");
            return ResponseEntity.badRequest().body("좋아요 처리에 실패했습니다.");
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
            @AuthenticationPrincipal UserDetails user) {
        log.debug("요청된 게시글 번호", postId);
        try {
            List<CommentDTO> commentList = cs.getCommentList(postId, user.getUsername());
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
            @AuthenticationPrincipal UserDetails user) {
        log.debug("댓글 작성 데이터 : {}", commentDTO);
        try {
            cs.makeNewComment(commentDTO, user.getUsername());
            return ResponseEntity.ok("댓글 작성 성공!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("댓글 작성 실패");
        }
    }

    /**
     * 댓글 삭제(비동기)
     * @param commentId
     */
    @DeleteMapping("deleteComment/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable("commentId") Integer commentId,
            @AuthenticationPrincipal UserDetails user) {
        log.debug("삭제할 댓글", commentId);
        try {
            cs.deleteComment(commentId, user.getUsername());
            //cs.deleteComment(commentId, null);
            return ResponseEntity.ok("댓글 삭제 성공!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("댓글 삭제 실패");
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
            @AuthenticationPrincipal UserDetails user) {
        log.debug("수정할 댓글과 내용 : {}", commentDTO);
        try {
            cs.updateComment(commentDTO, user.getUsername());
            //cs.updateComment(commentDTO, null);
            return ResponseEntity.ok("댓글 수정 성공!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("댓글 삭제 실패");
        }
    }
}