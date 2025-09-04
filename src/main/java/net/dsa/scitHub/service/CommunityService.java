package net.dsa.scitHub.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.community.PostDTO;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.entity.board.Tag;
import net.dsa.scitHub.repository.board.BoardRepository;
import net.dsa.scitHub.repository.board.PostRepository;
import net.dsa.scitHub.repository.board.TagRepository;
import net.dsa.scitHub.repository.user.UserRepository;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class CommunityService {

    private final UserRepository ur;
    private final PostRepository pr;
    private final BoardRepository br;
    private final TagRepository tr;

    /** 새 게시글 등록
     *  @param PostDTO
     */
    public void makeNewPost(PostDTO postDTO) {

        // DTO log
        log.debug("postDTO : {}", postDTO);

        // DTO -> Entity
        Post post = Post.builder()
                    .board(br.findByName(postDTO.board).orElseThrow(
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
        for (String tag : postDTO.tagList) {
            Tag tagEntity = Tag.builder()
                            .post(post)
                            .name(tag).build();
            tagArray.add(tagEntity);
        }
        // 태그 저장
        tr.saveAll(tagArray);
    }

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
        for (String tag : postDTO.tagList) {
            Tag tagEntity = Tag.builder()
                            .post(post)
                            .name(tag).build();
            tagArray.add(tagEntity);
        }
        // 태그를 리포지토리에 저장
        tr.saveAll(tagArray);
    }
}
