package net.dsa.scitHub.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.util.FileManager;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("ckEditor5")
public class CkEditor5Controller {

    // 업로드된 이미지의 저장 경로
    @Value("${app.upload.image.path}")
    private String imagePath;

    // 업로드된 파일을 불러올 때 매핑되는 경로
    @Value("${app.upload.image.url-pattern}")
    private String urlPattern;

    private final FileManager fileManager;


    // 게시글 작성/수정 폼에서 이미지 저장
    @ResponseBody
    @PostMapping("uploadPostImage")
    public ResponseEntity<?> uploadPostImage(
        @RequestParam("upload") MultipartFile upload) {
        log.debug("이미지 업로드 요청 : ");
        try {
            // 이미지 파일 업로드
            String savedFileName = fileManager.saveFile(imagePath, upload);
            // ckEditor5에 돌려줄 업로드 성공 응답 작성
            String imageUrl = urlPattern + savedFileName;
            Map<String, String> response = new HashMap<>();
            response.put("url", imageUrl);
            log.debug("이미지 업로드 완료");

            // 업로드 성공 응답 전송
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.debug("이미지 업로드 실패 : {}", e);
            // 업로드 에러 응답 작성
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, String> error = new HashMap<>();
            error.put("message", "이미지 업로드에 실패했습니다.");
            errorResponse.put("error", error);

            // 업로드 에러 응답 전송
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
