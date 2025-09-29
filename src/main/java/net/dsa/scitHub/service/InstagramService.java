package net.dsa.scitHub.service;

import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.InstagramPostDTO;
import net.dsa.scitHub.dto.InstagramResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InstagramService {

    // application.properties에서 토큰 값을 읽어옴
    @Value("${instagram.api.access-token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<InstagramPostDTO> getRecentPosts() {
        // API 요청을 위한 URL 생성
        final String url = "https://graph.instagram.com/me/media?fields=id,caption,media_url,permalink&access_token=" + accessToken;

        try {
            // RestTemplate을 사용하여 API 호출 및 응답을 DTO로 변환
            InstagramResponseDTO response = restTemplate.getForObject(url, InstagramResponseDTO.class);

            if (response != null && response.getData() != null) {
                log.info("인스타그램에서 {}개의 게시물을 성공적으로 가져왔습니다.", response.getData().size());

                return response.getData().stream()
                        .limit(6) // 최신 6개 게시물만 반환
                        .collect(Collectors.toList());  // 다시 리스트로 변환
            }
        } catch (Exception e) {
            log.error("인스타그램 API 호출 중 오류 발생", e);
        }

        // 오류 발생 시 빈 리스트 반환
        return Collections.emptyList();
    }
}