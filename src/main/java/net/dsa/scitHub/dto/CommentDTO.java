package net.dsa.scitHub.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.entity.user.User;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Integer commentId;
    private Integer userId;
    private String userNameKor;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
