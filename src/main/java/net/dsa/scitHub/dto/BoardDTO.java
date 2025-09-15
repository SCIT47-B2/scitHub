package net.dsa.scitHub.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.*;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardDTO {
    
    private int boardId;
    private String name;
    private String description;

    // 커뮤니티 홈 부를 때 사용
    private List<PostDTO> postList;
    // 게시판 상세 부를 때 사용
    private Page<PostDTO> postPage;
}
