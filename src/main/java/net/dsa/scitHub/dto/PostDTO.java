package net.dsa.scitHub.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.board.Board;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.entity.user.User;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Integer postId;
    private Integer boardId;
    private String board;
    private Integer userId;
    private String username;    // 로그인 시 입력하는 아이디
    private String userNameKor;
    private String title;
    private String content;
    private Integer viewCount;
    private int likeCount;
    private Boolean isLiked;          // 현재 로그인 유저가 좋아요를 눌렀는지 전달하는 용도
    private Boolean isComment; // 문의글 답변 완료 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> tagList;
    private List<Comment> commentList;

    public static PostDTO convertToPostDTO(Post post) {
        if (post == null) {
            return null;
        }

        return PostDTO.builder()
                .postId(post.getPostId())
                .boardId(post.getBoard().getBoardId())
                .userId(post.getUser().getUserId())
                .userNameKor(post.getUser().getNameKor())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .isComment(!post.getComments().isEmpty())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public static Post convertToPostEntity(PostDTO postDTO, User user, Board board) {
        if (postDTO == null) {
            return null;
        }

        return Post.builder()
                .postId(postDTO.getPostId())
                .board(board)
                .user(user)
                .title(postDTO.getTitle())
                .content(postDTO.getContent())
                .viewCount(postDTO.getViewCount())
                .createdAt(postDTO.getCreatedAt())
                .updatedAt(postDTO.getUpdatedAt())
                .build();
    }
}
