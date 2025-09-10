package net.dsa.scitHub.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.board.Post;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailDTO {
    private Integer postId;
    private String title;
    private String content;
    private Integer userId;
    private String userNameKor;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentDTO> comments;

    public static PostDetailDTO convertToPostDetailDTO(Post post) {
        if (post == null) {
            return null;
        }

        List<CommentDTO> commentDTOs = post.getComments().stream()
                .map(comment -> CommentDTO.builder()
                        .commentId(comment.getCommentId())
                        .userId(comment.getUser().getUserId())
                        .userNameKor(comment.getUser().getNameKor())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(comment.getUpdatedAt())
                        .build())
                .toList();

        return PostDetailDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .userId(post.getUser().getUserId())
                .userNameKor(post.getUser().getNameKor())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .comments(commentDTOs)
                .build();
    }
}
