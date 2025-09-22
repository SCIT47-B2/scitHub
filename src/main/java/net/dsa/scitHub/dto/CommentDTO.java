package net.dsa.scitHub.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.user.User;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Integer commentId;
    private Integer userId;
    private String username;       // 로그인 시에 입력하는 ID
    private String userNameKor;
    private String avatarUrl;       // 댓글 작성자의 프로필 이미지
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer postId;
    // 수정 가능 여부 - DB엔 없음
    private boolean canEdit;

    public static CommentDTO convertToCommentDTO(Comment comment) {
        if (comment == null) {
            return null;
        }

        User user = comment.getUser();

        return CommentDTO.builder()
                .commentId(comment.getCommentId())
                .userId(user != null ? user.getUserId() : null)
                .username(user != null ? user.getUsername() : null)
                .userNameKor(user != null ? user.getNameKor() : null)
                .content(comment.getContent())
                .postId(comment.getPost().getPostId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
