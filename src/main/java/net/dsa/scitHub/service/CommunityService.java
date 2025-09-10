package net.dsa.scitHub.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.CommentDTO;
import net.dsa.scitHub.dto.community.PostDTO;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.entity.board.Tag;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.repository.board.BoardRepository;
import net.dsa.scitHub.repository.board.CommentRepository;
import net.dsa.scitHub.repository.board.PostRepository;
import net.dsa.scitHub.repository.board.TagRepository;
import net.dsa.scitHub.repository.user.UserRepository;
import net.dsa.scitHub.security.AuthenticatedUser;
import net.dsa.scitHub.util.FileManager;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CommunityService {

    private final UserRepository ur;
    private final PostRepository pr;
    private final BoardRepository br;
    private final TagRepository tr;
    private final CommentRepository cr;
    private final FileManager fileManager;

    /** 새 게시글 등록
     *  @param PostDTO
     */
    public void makeNewPost(PostDTO postDTO) {

        // DTO log
        log.debug("postDTO : {}", postDTO);

        // DTO -> Entity
        Post post = Post.builder()
                    .board(br.findByName(postDTO.getBoard()).orElseThrow(
                        () -> new EntityNotFoundException("해당 게시판을 찾을 수 없습니다.")
                    ))
                    .user(ur.findByUsername(null).orElseThrow(
                        () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
                    ))
                    .title(postDTO.getTitle())
                    .content(postDTO.getContent())
                    .build();
        // 게시글 저장
        pr.save(post);

        // 태그 추가
        List<Tag> tagArray = new ArrayList<>();
        for (String tag : postDTO.getTagList()) {
            Tag tagEntity = Tag.builder()
                            .post(post)
                            .name(tag).build();
            tagArray.add(tagEntity);
        }
        // 태그 저장
        tr.saveAll(tagArray);
    }

    /**
     * 게시글 등록 데모 메서드
     * 추후 주석 처리나 삭제 필요
     * @param postDTO
     */
    public void makeNewPostDemo(PostDTO postDTO) {

        // DTO log
        log.debug("postDTO : {}", postDTO);

        // DTO -> Entity
        Post post = Post.builder()
                    .board(br.findByName("자유게시판").orElseThrow(
                        () -> new EntityNotFoundException("해당 게시판을 찾을 수 없습니다.")
                    ))
                    .user(ur.findByUsername("DemoUser").orElseThrow(
                        () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
                    ))
                    .title(postDTO.getTitle())
                    .content(postDTO.getContent())
                    .build();
        // 게시글을 리포지토리에 저장
        pr.save(post);

        // 태그 추가
        List<Tag> tagArray = new ArrayList<>();
        for (String tag : postDTO.getTagList()) {
            Tag tagEntity = Tag.builder()
                            .post(post)
                            .name(tag).build();
            tagArray.add(tagEntity);
        }
        // 태그를 리포지토리에 저장
        tr.saveAll(tagArray);
    }

    /**
     * 게시글 조회
     * @param postId
     * @param viewCheck
     * @return PostDTO
     */
    public PostDTO getPost(int postId, boolean viewCheck) {
        // 받아온 값 로그
        log.debug("postId : {}, viewCheck : {}", postId, viewCheck);

        // 해당 게시물 가져오기
        Post post = pr.findById(postId).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글을 찾을 수 없습니다.")
        );

        // viewCheck = true일 시 조회수 1 증가
        if (viewCheck) {
            post.setViewCount(post.getViewCount() + 1);
        }

        // Entity -> DTO 변환
        PostDTO postDTO = PostDTO.builder()
                          .postId(post.getPostId())
                          .board(post.getBoard().getName())
                          .username(post.getUser().getUsername())
                          .title(post.getTitle())
                          .content(post.getContent())
                          .viewCount(post.getViewCount())
                          .likeCount(post.getLikes().size())
                          .createdAt(post.getCreatedAt())
                          .updatedAt(post.getUpdatedAt())
                          .commentList(post.getComments())
                          .build();

        // 태그 리스트를 List<Tag> -> List<String> 변환
        List<Tag> tags = post.getTags();
        List<String> tagList = new ArrayList<>();
        for (Tag tag : tags) {
            String tagName = tag.getName();
            tagList.add(tagName);
        }
        postDTO.setTagList(tagList);

        // postDTO 반환
        return postDTO;
    }

    /**
     * 게시글 수정 처리 데모
     * @param postDTO
     */
    public void updatePostDemo(PostDTO postDTO) {
        // DB에서 해당 엔티티 탐색
        Post post = pr.findById(postDTO.getPostId()).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글을 찾을 수 없습니다.")
        );

        // 수정 폼에서 받아온 데이터 반영
        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());
        // 태그 데이터 반영
        // updateTagList(postDTO.getPostId(), postDTO.getTagList());
    }

    /**
     * 게시글 수정 처리
     * @param postDTO
     * @param User
     */
    public void updatePost(PostDTO postDTO, AuthenticatedUser user)
        throws Exception {
        // DB에서 해당 엔티티 탐색
        Post post = pr.findById(postDTO.getPostId()).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글을 찾을 수 없습니다.")
        );
        // 현재 로그인 계정의 User 엔티티 탐색
        User userEntity = ur.findByUsername(user.getUsername()).orElseThrow(
            () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
        );

        if (post.getUser().getUserId() != userEntity.getUserId()) {
            throw new Exception("수정 권한이 없습니다.");
        }

        // 수정 폼에서 받아온 데이터 반영
        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());
        // 태그 데이터 반영
        // updateTagList(postDTO.getPostId(), postDTO.getTagList());
    }

    /**
     * 댓글 목록 출력
     * @param postId
     * @return List<CommentDTO>
     */
    public List<CommentDTO> getCommentList(int postId) throws Exception {
        // 게시글 실존 여부 확인
        if(pr.existsById(postId)) {
            // 해당 게시글에 연결된 댓글을 등록 날짜순으로 검색
            List<Comment> comments = cr.findByPost_PostIdOrderByCreatedAtDesc(postId);
            List<CommentDTO> commentList = new ArrayList<>();
            for (Comment comment : comments) {
                CommentDTO commentDTO = CommentDTO.builder()
                                                  .commentId(comment.getCommentId())
                                                  .userId(comment.getUser().getUserId())
                                                  .userName(comment.getUser().getUsername())
                                                  .content(comment.getContent())
                                                  .createdAt(comment.getCreatedAt())
                                                  .updatedAt(comment.getUpdatedAt())
                                                  .build();
                commentList.add(commentDTO);
            }
            return commentList;
        } else {
            throw new EntityNotFoundException("해당 게시글이 존재하지 않습니다.");
        }
    }

    /**
     * 새 댓글 작성
     * @param CommentDTO
     */
    public void makeNewComment(CommentDTO commentDTO) throws Exception {
        // 댓글이 달릴 원 게시글이 존재 시
        if (pr.existsById(commentDTO.getPostId())) {
            // 회원, 게시글 엔티티 조회
            User user = ur.findByUsername(commentDTO.getUserName()).orElseThrow(
                () -> new EntityNotFoundException("해당 회원이 존재하지 않습니다.")
            );
            Post post = pr.findById(commentDTO.getPostId()).orElseThrow(
                () -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다.")
            );
            // 댓글 엔티티 작성
            Comment comment = Comment.builder()
                                     .user(user)
                                     .content(commentDTO.getContent())
                                     .post(post)
                                     .build();
            // DB에 댓글 저장
            cr.save(comment);
        } else { // 없으면
            // 에러 발생
            throw new Exception("해당 게시글이 존재하지 않습니다.");
        }
    }

    /**
     * 댓글 삭제
     * @param CommentId
     */
    public void deleteComment(Integer commentId, UserDetails user) throws Exception {
        // 삭제할 댓글을 DB에서 찾기
        Comment comment = cr.findById(commentId).orElseThrow(
            () -> new EntityNotFoundException("해당 댓글이 존재하지 않습니다.")
        );
        // 삭제 권한 체크
        /*
        User loginUser = ur.findByUsername(user.getUsername());
        if (loginUser.getUserId() != comment.getUser().getUserId()) {
            throw new Exception("삭제 권한이 없습니다.");
        }
        */
        cr.delete(comment);

    }
    /**
     * 댓글 수정
     */
    public void updateComment(CommentDTO commentDTO, UserDetails user) throws Exception {
        // 수정할 댓글을 DB에서 찾기
        Comment comment = cr.findById(commentDTO.getCommentId()).orElseThrow(
            () -> new EntityNotFoundException("해당 댓글이 존재하지 않습니다.")
        );
        // 수정 권한 체크
        /*
        User loginUser = ur.findByUsername(user.getUsername());
        if (loginUser.getUserId() != comment.getUser().getUserId()) {
            throw new Exception("삭제 권한이 없습니다.");
        }
        */
        comment.setContent(commentDTO.getContent());
    }


    /**
     * 게시글의 태그 데이터 갱신
     * @param postId
     * @param List<Tag> newTags
     */
    /*
    public void updateTagList(Integer postId, List<String> newTags) {
        // 기존 태그 리스트
        List<Tag> existingTags = tr.findByPost_PostId(postId);
        // 새 태그 매핑

        // 기존 태그를 name으로 매핑
        Map<String, Tag> existingTagMap = existingTags.stream()
            .collect(Collectors.toMap(Tag::getName, tag -> tag));
        
        // 새 태그를 name으로 매핑
        Map<String, Tag> newTagMap = newTags.stream()
            .collect(Collectors.toMap(Tag::getName, tag -> tag));
        
        // 삭제할 태그들 (새 리스트에 없는 것)
        List<Tag> toDelete = existingTags.stream()
            .filter(tag -> !newTagMap.containsKey(tag.getName()))
            .collect(Collectors.toList());
        log.debug("삭제할 태그 : {}", toDelete);
        
        // 추가할 태그들 (기존에 없는 새 태그)
        List<Tag> toAdd = newTags.stream()
            .filter(tag -> !existingTagMap.containsKey(tag.getName()))
            .collect(Collectors.toList());
        log.debug("추가할 태그 : {}", toAdd);
        
        // DB에 반영
        if (!toDelete.isEmpty()) {
            tr.deleteAll(toDelete);
        }
        if (!toAdd.isEmpty()) {
            tr.saveAll(toAdd);
        }
    }
    */
}