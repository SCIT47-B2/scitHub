package net.dsa.scitHub.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.CommentDTO;
import net.dsa.scitHub.dto.PostDTO;
import net.dsa.scitHub.entity.board.Board;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.entity.board.PostBookmark;
import net.dsa.scitHub.entity.board.PostLike;
import net.dsa.scitHub.entity.board.Tag;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.repository.board.BoardRepository;
import net.dsa.scitHub.repository.board.CommentRepository;
import net.dsa.scitHub.repository.board.PostBookmarkRepository;
import net.dsa.scitHub.repository.board.PostLikeRepository;
import net.dsa.scitHub.repository.board.PostRepository;
import net.dsa.scitHub.repository.board.TagRepository;
import net.dsa.scitHub.repository.user.UserRepository;

import net.dsa.scitHub.dto.BoardDTO;

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
    private final PostLikeRepository plr;
    private final PostBookmarkRepository pbr;

    // 게시판 관련 ----------------------------------------------------------------------------------
    /**
     * 유저가 즐겨찾기한 게시판 가져오기
     */
    /* 
    public List<BoardDTO> getFavoriteBoards(String username) {

        // 현재 로그인 계정의 User 엔티티 탐색
        User userEntity = ur.findByUsername(username).orElseThrow(
            () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
        );
        // 로그인한 유저가 즐겨찾기한 게시판 목록 가져오기
        List<Board> bookmarkedBoardList = br.findBookmarkedBoardsByUser(userEntity.getUserId());

        // List<Entity> -> List<DTO>
        List<BoardDTO> bookmarkedBoardDTOList = new ArrayList<>();
        for (Board board : bookmarkedBoardList) {
            BoardDTO boardDTO = BoardDTO.builder()
                                        .boardId(board.getBoardId())
                                        .name(board.getName())
                                        .description(board.getDescription())
                                        .build();
            
            for (Post post : board.getPosts()) {
                
            }
        }

        // List<DTO> 반환
    }
    */

    /**
     * 게시판 이름 가져오기
     * @param boardId
     * @param pageable
     * @return
     */
    public BoardDTO getBoard(String name) {
        Board board = br.findByName(name).orElseThrow(
            () -> new EntityNotFoundException("해당 게시판을 찾을 수 없습니다.")
        );

        BoardDTO boardDTO = BoardDTO.builder()
                                    .boardId(board.getBoardId())
                                    .name(board.getName())
                                    .description(board.getDescription())
                                    .build();

        return boardDTO;
    }

    /**
     * 게시판 이름 일본어로 바꾸기
     * @param String
     * @return String
     */
    public String translateToJp(String name) {
        String result = "";
        switch (name) {
            case "free":
                result = "自由掲示板";
                break;
            case "it":
                result = "IT";
                break;
            case "japanese":
                result = "日本語";
                break;
            case "jpCulture":
                result = "日本文化&生活情報";
                break;
            case "job":
                result = "就活情報&コツ";
                break;
            case "hobby":
                result = "趣味&旅行&グルメ情報";
                break;
            case "certificate":
                result = "資格情報";
                break;
            case "graduated":
                result = "卒業生掲示板";
                break;
            default:
                result = "掲示板の名前が間違っています";
                break;
        }

        return result;
    }


    /**
     * 게시판 맵 가져오기
     * @return Map<Integer, String>
     */
    // DB 연동 이슈로 인해 미사용
    /*
    public Map<Integer, String> getBoardMap() {
        // 목록에 노출할 게시판 가져오기
        List<Board> boardList = br.findAllVisibleBoards();
        // 리스트 -> 맵 구조로 변경
        Map<Integer, String> boardMap = new HashMap<>();
        for (Board board : boardList) {
            // Key : 게시판 식별자, value : 게시판 이름
            boardMap.put(board.getBoardId(), board.getName());
        }
        return boardMap;
    }
    */

    /**
     * 게시판 내의 게시글 가져오기(페이징)
     * @param boardId
     * @param pageable
     * @return
     */
    public Page<PostDTO> findPostsByBoard(Integer boardId, Pageable pageable) {
        Board board = br.findById(boardId).orElseThrow(
            () -> new EntityNotFoundException("해당 게시판을 찾을 수 없습니다.")
        );

        Page<Post> postPage = pr.findByBoard_BoardId(board.getBoardId(), pageable);

        // 페이지의 Entity -> DTO
        List<PostDTO> postDTOList = new ArrayList<>();
        for (Post post : postPage) {
            PostDTO postDTO = PostDTO.builder()
                                     .postId(post.getPostId())
                                     .title(post.getTitle())
                                     .username(post.getUser().getUsername())
                                     .createdAt(post.getCreatedAt())
                                     .viewCount(post.getViewCount())
                                     .likeCount(post.getLikes().size())
                                     .commentCount(post.getComments().size())
                                     .build();
            postDTOList.add(postDTO);
        }

        // List<DTO> -> Page<DTO>
        Page<PostDTO> postDTOPage = new PageImpl<>(
            postDTOList,                    // 실제 데이터
            postPage.getPageable(),         // 페이지 정보
            postPage.getTotalElements()     // 전체 항목 수
        );

        return postDTOPage;
    }

    /**
     * 게시판 내의 게시글 검색 결과 가져오기(페이징)
     * @param boardId
     * @param searchType
     * @param keyword
     * @param pageable
     * @return
     */
    public Page<PostDTO> searchPosts(Integer boardId, String searchType, String keyword, Pageable pageable) {
        Board board = br.findById(boardId).orElseThrow(
            () -> new EntityNotFoundException("해당 게시판을 찾을 수 없습니다.")
        );
        boardId = board.getBoardId();

        // 게시글 페이지 생성
        Page<Post> postPage;

        // 리포지토리에서 검색 조건에 맞게 탐색
        switch (searchType) {
            case "titleAndContent":
                postPage = pr.findByBoardAndKeyword(boardId, keyword, pageable);
                break;
            case "title":
                postPage = pr.findByBoard_BoardIdAndTitleContaining(boardId, keyword, pageable);
                break;
            case "content":
                postPage = pr.findByBoard_BoardIdAndContentContaining(boardId, keyword, pageable);
                break;
            case "author":
                postPage = pr.findByBoard_BoardIdAndUser_UsernameContaining(boardId, keyword, pageable);
                break;
            case "tag":
                // 특정 게시판에서 특정 태그를 포함한 게시글의 페이지
                postPage = pr.findByBoard_BoardIdAndTags_NameContaining(boardId, keyword, pageable);
                break;
            default:
                postPage = pr.findByBoard_BoardId(boardId, pageable);
                break;
        }
        
        // 페이지의 Entity -> DTO
        List<PostDTO> postDTOList = new ArrayList<>();
        for (Post post : postPage) {
            PostDTO postDTO = PostDTO.builder()
                                     .postId(post.getPostId())
                                     .title(post.getTitle())
                                     .username(post.getUser().getUsername())
                                     .createdAt(post.getCreatedAt())
                                     .viewCount(post.getViewCount())
                                     .likeCount(post.getLikes().size())
                                     .commentCount(post.getComments().size())
                                     .build();
            postDTOList.add(postDTO);
        }

        // List<DTO> -> Page<DTO>
        Page<PostDTO> postDTOPage = new PageImpl<>(
            postDTOList,                    // 실제 데이터
            postPage.getPageable(),         // 페이지 정보
            postPage.getTotalElements()     // 전체 항목 수
        );

        return postDTOPage;
    }


    // 게시글 관련 ----------------------------------------------------------------------------------
    /** 새 게시글 등록
     *  @param PostDTO
     */
    public int makeNewPost(PostDTO postDTO) {

        // DTO log
        log.debug("postDTO : {}", postDTO);

        // DTO -> Entity
        Post post = Post.builder()
                    .board(br.findByName(postDTO.getBoard()).orElseThrow(
                        () -> new EntityNotFoundException("해당 게시판을 찾을 수 없습니다.")
                    ))
                    .user(ur.findByUsername(postDTO.getUsername()).orElseThrow(
                        () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
                    ))
                    .title(postDTO.getTitle())
                    .content(postDTO.getContent())
                    .build();
        // 게시글 저장
        Post persistedPost = pr.save(post);

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

        // 생성된 게시글의 식별자 반환
        return persistedPost.getPostId();
    }

    /**
     * 게시글 조회
     * @param postId
     * @param viewCheck
     * @return PostDTO
     */
    public PostDTO getPost(int postId, boolean viewCheck, String username) {

        // 해당 게시물 가져오기
        Post post = pr.findById(postId).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글을 찾을 수 없습니다.")
        );
        // 현재 로그인 계정의 User 엔티티 탐색
        User userEntity = ur.findByUsername(username).orElseThrow(
            () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
        );

        // viewCheck = true일 시 조회수 1 증가
        if (viewCheck) {
            post.setViewCount(post.getViewCount() + 1);
        }

        // Entity -> DTO 변환
        PostDTO postDTO = PostDTO.builder()
                          .postId(post.getPostId())
                          .boardId(post.getBoard().getBoardId())
                          .board(post.getBoard().getName())
                          .username(post.getUser().getUsername())
                          .avatarUrl(post.getUser().getAvatarUrl())
                          .title(post.getTitle())
                          .content(post.getContent())
                          .viewCount(post.getViewCount())
                          .likeCount(post.getLikes().size())
                          .createdAt(post.getCreatedAt())
                          .updatedAt(post.getUpdatedAt())
                          .commentList(post.getComments())
                          .build();
        // 현재 유저가 좋아요를 눌렀었는지 체크
        boolean isLiked = plr.existsByPost_PostIdAndUser_UserId(post.getPostId(), userEntity.getUserId());
        postDTO.setIsLiked(isLiked);
        // 현재 유저가 이 게시글을 북마크했었는지 체크
        boolean isBookmarked = pbr.existsByPost_PostIdAndUser_UserId(post.getPostId(), userEntity.getUserId());
        postDTO.setIsBookmarked(isBookmarked);

        // 태그 리스트를 List<Tag> -> List<String> 변환
        List<Tag> tags = post.getTags();
        List<String> tagList = new ArrayList<>();
        if (tags != null && !tags.isEmpty()) {
            for (Tag tag : tags) {
                String tagName = tag.getName();
                tagList.add(tagName);
            }
        postDTO.setTagList(tagList);
        }

        // postDTO 반환
        return postDTO;
    }

    /**
     * 게시글 수정 처리
     * @param postDTO
     * @param User
     */
    public void updatePost(PostDTO postDTO, String username)
        throws Exception {
        // DB에서 해당 엔티티 탐색
        Post post = pr.findById(postDTO.getPostId()).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글을 찾을 수 없습니다.")
        );
        // 현재 로그인 계정의 User 엔티티 탐색
        User userEntity = ur.findByUsername(username).orElseThrow(
            () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
        );

        // 수정 권한 체크
        log.debug("게시글 작성자 : {}", post.getUser().getUserId());
        log.debug("현재 로그인 유저 : {}", userEntity.getUserId());
        if (post.getUser().getUserId() != userEntity.getUserId()) {
            throw new Exception("수정 권한이 없습니다.");
        }

        // 수정 폼에서 받아온 데이터 반영
        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());
        // 태그 데이터 반영
        updateTagList(postDTO.getPostId(), postDTO.getTagList());
    }

    /**
     * 게시글 삭제 처리
     * @param postId
     * @param userId
     */
    public void deletePost(Integer postId, String username)
        throws Exception {
        // DB에서 해당 엔티티 탐색
        Post post = pr.findById(postId).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글을 찾을 수 없습니다.")
        );
        // 현재 로그인 계정의 User 엔티티 탐색
        User userEntity = ur.findByUsername(username).orElseThrow(
            () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
        );
        // 삭제 권한 체크
        log.debug("게시글 작성자 : {}", post.getUser().getUserId());
        log.debug("현재 로그인 유저 : {}", userEntity.getUserId());
        if (post.getUser().getUserId() != userEntity.getUserId()) {
            throw new Exception("수정 권한이 없습니다.");
        }

        // 게시글 삭제 처리
        pr.delete(post);
    }

    // 게시글 부가 기능 관련 --------------------------------------------------------------------------------

    /**
     * 좋아요 토글 처리
     * @param postId
     * @param username
     */
    public void toggleLikePost(Integer postId, String username) {
        // DB에서 해당 엔티티 탐색
        Post post = pr.findById(postId).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글을 찾을 수 없습니다.")
        );
        // 현재 로그인 계정의 User 엔티티 탐색
        User userEntity = ur.findByUsername(username).orElseThrow(
            () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
        );

        // 좋아요 엔티티 작성
        PostLike postLike = PostLike.builder().user(userEntity).post(post).build();
        // 해당 좋아요 존재 여부 확인
        Optional<PostLike> postLikeExisting = plr.findByPost_PostIdAndUser_UserId(post.getPostId(), userEntity.getUserId());
        
        if (postLikeExisting.isPresent()) {
            // 이미 존재하는 좋아요면 삭제
            plr.delete(postLikeExisting.get());
        } else {
            // 없으면 추가
            plr.save(postLike);
        }
    }
    /** 게시글의 좋아요 개수 받아오기
     * @param postId
     * @return likeCount
     */
    public int getLikeCount(int postId) {
        return plr.countByPost_PostId(postId);
    }

    /**
     * 북마크 토글 처리
     * @param postId
     * @param username
     */
    public boolean toggleBookmarkPost(Integer postId, String username) {
        // DB에서 해당 게시글 엔티티 탐색
        Post post = pr.findById(postId).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글을 찾을 수 없습니다.")
        );
        // 현재 로그인 계정의 User 엔티티 탐색
        User userEntity = ur.findByUsername(username).orElseThrow(
            () -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다.")
        );

        // 북마크 엔티티 작성
        PostBookmark postBookmark = PostBookmark.builder().user(userEntity).post(post).build();
        // 해당 좋아요 존재 여부 확인
        Optional<PostBookmark> postBookmarkExisting = pbr.findByPost_PostIdAndUser_UserId(post.getPostId(), userEntity.getUserId());

        if (postBookmarkExisting.isPresent()) {
            // 이미 존재하는 좋아요면 삭제
            pbr.delete(postBookmarkExisting.get());
            // 북마크가 존재하지 않으므로 북마크 여부로 false 리턴
            return false;
        } else {
            // 없으면 추가
            pbr.save(postBookmark);
            // 북마크가 존재하므로 북마크 여부로 true 리턴
            return true;
        }
    }

    // 게시글 태그 관련 -------------------------------------------------------------------------

    /**
     * 게시글 하나의 태그 모두 가져오기
     */
    public List<String> getTagList(Integer postId) {
        // 게시글 실존 여부 체크
        Post post = pr.findById(postId).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다.")
        );
        List<Tag> tagList = post.getTags();
        List<String> tagNames = new ArrayList<>();
        // List<Tag> -> List<String> 변환
        for (Tag tag : tagList) {
            String tagName = tag.getName();
            tagNames.add(tagName);
        }
        return tagNames;
    }

    /**
     * 게시글의 태그 데이터 갱신
     * @param postId
     * @param List<Tag> newTags
     */
    
    public void updateTagList(Integer postId, List<String> newTagList) {
        // 게시글 존재 여부 확인
        Post post = pr.findById(postId).orElseThrow(
            () -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다.")
        );
        // 기존 태그 리스트
        List<Tag> existingTags = tr.findByPost_PostId(postId);
        // 새 태그 매핑
        List<Tag> newTags = new ArrayList<>();
        for (String newTagStr : newTagList) {
            Tag newTag = Tag.builder()
                            .name(newTagStr)
                            .post(post)
                            .build();
            newTags.add(newTag);
        }

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


    // 댓글 관련 기능 관련 ----------------------------------------------------------------------------------

    /**
     * 댓글 목록 출력
     * @param postId
     * @return List<CommentDTO>
     */
    public List<CommentDTO> getCommentList(int postId, String username) throws Exception {
        // 게시글 실존 여부 확인
        if(pr.existsById(postId)) {
            // 해당 게시글에 연결된 댓글을 등록 날짜순으로 검색
            List<Comment> comments = cr.findByPost_PostIdOrderByCreatedAtDesc(postId);
            List<CommentDTO> commentList = new ArrayList<>();
            for (Comment comment : comments) {
                CommentDTO commentDTO = CommentDTO.builder()
                                                  .commentId(comment.getCommentId())
                                                  .userId(comment.getUser().getUserId())
                                                  .avatarUrl(comment.getUser().getAvatarUrl())
                                                  .username(comment.getUser().getUsername())
                                                  .content(comment.getContent())
                                                  .createdAt(comment.getCreatedAt())
                                                  .updatedAt(comment.getUpdatedAt())
                                                  .build();
                // 수정 가능 여부 추가
                commentDTO.setCanEdit(  username != null
                                        && comment.getUser().getUsername().equals(username)
                                        ? true : false);
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
    public void makeNewComment(CommentDTO commentDTO, String username) throws Exception {
        // 댓글이 달릴 원 게시글이 존재 시
        if (pr.existsById(commentDTO.getPostId())) {
            // 회원, 게시글 엔티티 조회
            User user = ur.findByUsername(username).orElseThrow(
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
    public void deleteComment(Integer commentId, String username) throws Exception {
        // 삭제할 댓글을 DB에서 찾기
        Comment comment = cr.findById(commentId).orElseThrow(
            () -> new EntityNotFoundException("해당 댓글이 존재하지 않습니다.")
        );
        // 삭제 권한 체크
         User loginUser = ur.findByUsername(username).orElseThrow(
                () -> new EntityNotFoundException("해당 회원이 존재하지 않습니다.")
        );
        if ( (loginUser.getUserId() != comment.getUser().getUserId())) {
            throw new Exception("삭제 권한이 없습니다.");
        }
        cr.delete(comment);

    }
    /**
     * 댓글 수정
     */
    public void updateComment(CommentDTO commentDTO, String username) throws Exception {
        // 수정할 댓글을 DB에서 찾기
        Comment comment = cr.findById(commentDTO.getCommentId()).orElseThrow(
            () -> new EntityNotFoundException("해당 댓글이 존재하지 않습니다.")
        );
        // 수정 권한 체크
        User loginUser = ur.findByUsername(username).orElseThrow(
                () -> new EntityNotFoundException("해당 회원이 존재하지 않습니다.")
        );
        if (loginUser.getUserId() != comment.getUser().getUserId()) {
            throw new Exception("수정 권한이 없습니다.");
        }
        comment.setContent(commentDTO.getContent());
    }
}