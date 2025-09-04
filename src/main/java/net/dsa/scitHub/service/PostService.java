package net.dsa.scitHub.service;

import java.util.List;
import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.PostDTO;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.repository.board.PostRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository pr;

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
}
