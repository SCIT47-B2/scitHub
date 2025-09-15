package net.dsa.scitHub.service;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.CommentDTO;
import net.dsa.scitHub.dto.PostDTO;
import net.dsa.scitHub.dto.PostDetailDTO;
import net.dsa.scitHub.entity.board.Board;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.entity.interfaces.Authorizable;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.repository.board.BoardRepository;
import net.dsa.scitHub.repository.board.CommentRepository;
import net.dsa.scitHub.repository.board.PostRepository;
import net.dsa.scitHub.repository.user.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository pr;
    private final UserRepository ur;
    private final BoardRepository br;
    private final CommentRepository cr;

    /**
     * 게시판 ID와 검색 조건에 따른 게시글 목록 조회 (페이징)
     * @param page          현재 페이지 번호 (1부터 시작)
     * @param pageSize      페이지당 게시글 수
     * @param boardId       게시판 ID
     * @param searchType    검색 유형 (제목, 내용, 작성자 등)
     * @param searchWord    검색어
     * @return              페이징 처리된 게시글 목록
     */
    public Page<PostDTO> getPostsByBoardAndSearchWithPaging(int page, int pageSize, int boardId,String searchType, String searchWord) {
        // 내부적으로 0부터 시작
        page--;

        // 정렬 기준 생성
		Sort sort = Sort.by(
			Sort.Order.desc("postId"),
			Sort.Order.desc("createdAt")
		);

        // 페이지 요청 객체 생성 (페이지 번호, 크기, 정렬 기준)
		Pageable pageable = PageRequest.of(page, pageSize, sort);

        // 검색 조건에 따른 데이터 조회
		Page<Post> entityPage;

        switch (searchType) {
            case "title" -> entityPage = pr.findByBoard_BoardIdAndTitleContaining(boardId, searchWord, pageable);
            case "tag" -> entityPage = pr.findByBoard_BoardIdAndTags_NameContaining(boardId, searchWord, pageable);
            case "content" -> entityPage = pr.findByBoard_BoardIdAndContentContaining(boardId, searchWord, pageable);
            case "user" -> entityPage = pr.findByBoard_BoardIdAndUser_NameKorContaining(boardId, searchWord, pageable);
            case "all" -> entityPage = pr.findByBoard_BoardIdAndSearchWord(boardId, searchWord, pageable);
            default -> entityPage = pr.findByBoard_BoardId(boardId, pageable);
        }

        List<PostDTO> postDTOList = new ArrayList<>();
        for (Post post : entityPage) {
            postDTOList.add(PostDTO.convertToPostDTO(post));
        }

        return new PageImpl<>(
            postDTOList,
            entityPage.getPageable(),
            entityPage.getTotalElements()
        );
    }

    /**
     * 특정 게시판에서 최신 게시글 3개 조회
     * @param boardId   게시판 ID
     * @return          최신 게시글 3개 목록
     */
    public List<PostDTO> getLatestPosts(int boardId, int count) {
        Pageable pageable = PageRequest.of(0, count, Sort.by(Sort.Order.desc("createdAt")));
        Page<Post> postPage = pr.findByBoard_BoardId(boardId, pageable);

        List<PostDTO> postDTOList = new ArrayList<>();
        for (Post post : postPage) {
            postDTOList.add(PostDTO.convertToPostDTO(post));
        }

        return postDTOList;
    }

    /**
     * 게시글 저장
     * @param postDTO   저장할 게시글 정보
     */
    public void savePost(PostDTO postDTO) {

        User user = ur.findById(postDTO.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. ID: " + postDTO.getUserId()));
        Board board = br.findById(postDTO.getBoardId())
            .orElseThrow(() -> new EntityNotFoundException("게시판을 찾을 수 없습니다. ID: " + postDTO.getBoardId()));

        Post post = PostDTO.convertToPostEntity(postDTO, user, board);
        pr.save(post);
    }

    /**
     * 게시글 상세 정보 조회 (작성자, 댓글, 댓글 작성자 포함)
     * @param postId 조회할 게시글 ID
     * @return PostDetailDTO
     */
    public PostDetailDTO findPostDetailById(Integer postId) {
        Post post = pr.findPostWithDetailsById(postId)
            .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));

        return PostDetailDTO.convertToPostDetailDTO(post);
    }

    /**
     * 게시글 수정 (관리자 또는 작성자만 가능)
     * @param postDTO 수정할 게시글 정보
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param currentUserRole 현재 로그인한 사용자 역할
     * @return 수정된 게시글 DTO
     */
    @Transactional
    public PostDTO updatePost(PostDTO postDTO, Integer currentUserId, String currentUserRole) {
        Post post = pr.findById(postDTO.getPostId())
            .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다. ID: " + postDTO.getPostId()));

        if (!isAuthority(currentUserRole, currentUserId, post)) {
            throw new SecurityException("게시글 수정 권한이 없습니다.");
        }

        // 게시글 정보 업데이트
        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());
        post.setUpdatedAt(LocalDateTime.now());

        // 변경 사항을 저장(flush)하고 갱신된 엔티티를 반환
        Post updatedPost = pr.save(post);

        // 갱신된 엔티티로 PostDTO 생성 및 반환
        return PostDTO.convertToPostDTO(updatedPost);
    }

    /**
     * 게시글 삭제 (관리자 또는 작성자만 가능)
     * @param postId 삭제할 게시글 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param currentUserRole 현재 로그인한 사용자 역할
     */
    public void deletePost(Integer postId, Integer currentUserId, String currentUserRole) {
        Post post = pr.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));

        if (!isAuthority(currentUserRole, currentUserId, post)) {
            throw new SecurityException("게시글 삭제 권한이 없습니다.");
        }

        pr.delete(post);
    }

    /**
     * 댓글 저장
     * @param postId 댓글을 달 게시글 ID
     * @param content 댓글 내용
     * @param userId 댓글 작성자 ID
     */
    public void saveComment(Integer postId, String content, Integer userId) {
        Post post = pr.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));
        User user = ur.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        Comment comment = Comment.builder()
            .user(user)
            .content(content)
            .post(post)
            .build();

        cr.save(comment);
    }

    /**
     * 댓글 수정 (관리자 또는 작성자만 가능)
     * @param commentId 수정할 댓글 ID
     * @param content 수정할 내용
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param currentUserRole 현재 로그인한 사용자 역할
     * @return 수정된 댓글 DTO
     */
    @Transactional
    public CommentDTO updateComment(Integer commentId, String content, Integer currentUserId, String currentUserRole) {
        Comment comment = cr.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("답변을 찾을 수 없습니다. ID: " + commentId));

        if (!isAuthority(currentUserRole, currentUserId, comment)) {
            throw new SecurityException("답변 수정 권한이 없습니다.");
        }

        // 댓글 내용 업데이트
        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());

        // 변경 사항을 저장(flush)하고 갱신된 엔티티를 반환
        Comment updatedComment = cr.save(comment);

        return CommentDTO.convertToCommentDTO(updatedComment);
    }

    /**
     * 댓글 삭제 (관리자 또는 작성자만 가능)
     * @param commentId 삭제할 댓글 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param currentUserRole 현재 로그인한 사용자 역할
     */
    public void deleteComment(Integer commentId, Integer currentUserId, String currentUserRole) {
        Comment comment = cr.findById(commentId)
            .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다. ID: " + commentId));

        if (!isAuthority(currentUserRole, currentUserId, comment)) {
            throw new SecurityException("댓글 삭제 권한이 없습니다.");
        }

        cr.delete(comment);
    }

    /**
     * 권한 확인 (관리자 또는 작성자인지)
     * @param role 현재 사용자 역할
     * @param userId 현재 사용자 ID
     * @param target 권한을 확인할 대상
     * @return 권한 여부
     */
    private boolean isAuthority(String role, Integer userId, Authorizable target) {
        return "ROLE_ADMIN".equals(role) || target.getUser().getUserId().equals(userId);
    }
}
