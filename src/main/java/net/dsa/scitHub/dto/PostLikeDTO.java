package net.dsa.scitHub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeDTO {
    private Integer postLikeId;
    private Integer postId;
    private Integer userId;
}
