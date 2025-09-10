package net.dsa.scitHub.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import net.dsa.scitHub.dto.CommentDTO;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.dto.community.PostDTO;
import net.dsa.scitHub.security.AuthenticatedUser;
import net.dsa.scitHub.service.CommunityService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;


@Slf4j
@Controller
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService cs;

    @ModelAttribute("menuItems")
    public List<MenuItem> menuItems() {
        return List.of(
            new MenuItem("게시판 홈", "/community/home"),
            new MenuItem("Q&A", "/community/qna"),
            new MenuItem("강의평", "/community/courseReview")
        );
    }

    /**
     * 커뮤니티 페이지 요청
     * @param model
     * @return community/home.html
     */
    @GetMapping({"/community", "/community/home"})
    public String communityPage(Model model) {
        return "community/home"; // templates/community/home.html
    }

    /**
     * 글쓰기 페이지로 이동
     * @return community/writeForm.html
     */
    @GetMapping("/community/writePost")
    public String writePostPage() {
        return "community/writeForm";
    }

    /**
     * 작성한 글을 DB에 저장
     * @param postDTO
     */
    @PostMapping("/community/write")
    public ResponseEntity<?> writePost(PostDTO postDTO) {//, AuthenticatedUser user) {
        try {
            log.debug("postDTO : {}", postDTO);
            if(!postDTO.getTagList().isEmpty()) {
                for (String tag : postDTO.getTagList()) {
                    log.debug("tag : {}", tag);
                }
            }
            //postDTO.setUsername(user.getUsername());
            cs.makeNewPostDemo(postDTO);
            //cs.makeNewPost(postDTO);
            return ResponseEntity.ok("게시글 등록 완료");
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
	@GetMapping("community/readPost")
	public String read(
		@RequestParam(name = "postId", defaultValue = "0") int postId,
		Model model) {
		try {
			PostDTO postDTO = cs.getPost(postId, true);
			model.addAttribute("post", postDTO);
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
	@GetMapping("update")
	public String update(
			@RequestParam("postId") int postId,
            //@AuthenticationPrincipal UserDetails user,
            Model model) {
		try {
			PostDTO postDTO = cs.getPost(postId, false);
            // 수정 권한이 있는 유저인지 체크
            /*
			if (!user.getUsername().equals(boardDTO.getMemberId())) {
				throw new RuntimeException("수정 권한이 없습니다.");
			}
            */
			model.addAttribute("post", postDTO);
			return "community/updateForm";
		} catch (Exception e) {
			log.debug("[예외 발생] {}", e.getMessage());
			return "redirect:home";
		}
	}
	
	/**
	 * 게시글 수정 처리
	 * @param boardDTO 	수정할 글 정보
	 * @param user 		로그인한 사용자 정보
	 * @param upload 	업로드된 파일
	 * @return 글읽기페이지
	 */
	@PostMapping("updatePost")
	public ResponseEntity<?> updatePost(
			PostDTO postDTO
			//@AuthenticationPrincipal UserDetails user
            ) {
        
        // 유저 정보 추가
		// postDTO.setMemberId(user.getUsername());
		
		try {
			cs.updatePostDemo(postDTO);
            // cs.updatePost(postDTO, user);
			log.debug("수정 성공!");
            return ResponseEntity.ok("게시글 수정 성공");
		} catch (Exception e) {
			log.debug("[예외 발생] {}", e.getMessage());
			return ResponseEntity.badRequest().body("게시글 수정 실패");
		}
	}
    
    // 게시글 부가 기능 관련--------------------------------------------------------------------------------
    /**
     * 게시글 좋아요 처리(비동기)
     * @param postId
     */

     // 댓글 관련-------------------------------------------------------------------------------------------
    /**
     * 댓글 목록 불러오기(비동기)
     * @return List<CommentDTO>
     */
    @GetMapping("community/commentList")
    public ResponseEntity<List<CommentDTO>> commentList(@RequestParam("postId") int postId) {
        log.debug("요청된 게시글 번호", postId);
        try {
            List<CommentDTO> commentList = cs.getCommentList(postId);
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
    @PostMapping("community/writeComment")
    public ResponseEntity<?> writeComment(CommentDTO commentDTO) {
        log.debug("댓글 작성 데이터 : {}", commentDTO);
        try {
            cs.makeNewComment(commentDTO);
            return ResponseEntity.ok("댓글 작성 성공!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("댓글 작성 실패");
        }
    }

    /**
     * 댓글 삭제(비동기)
     * @param commentId
     */
    @DeleteMapping("community/deleteComment/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable("commentId") Integer commentId
                                            //, UserDetails user
    ) {
        log.debug("삭제할 댓글", commentId);
        try {
            //cs.deleteComment(commentId, user);
            cs.deleteComment(commentId, null);
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
    @PatchMapping("community/updateComment")
    public ResponseEntity<?> updateComment(@RequestBody CommentDTO commentDTO
                                            //, UserDetails. user
    ) {
        log.debug("수정할 댓글과 내용 : {}", commentDTO);
        try {
            // cs.updateComment(commentDTO.getCommentId, user);
            cs.updateComment(commentDTO, null);
            return ResponseEntity.ok("댓글 수정 성공!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("댓글 삭제 실패");
        }
    }

}