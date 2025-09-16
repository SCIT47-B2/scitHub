package net.dsa.scitHub.controller;

import java.nio.file.Paths;
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
import net.dsa.scitHub.utils.FileManager;



@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("ckEditor5")
public class CkEditor5Controller {

    // 업로드된 이미지의 실제 저장 경로
    // C:/uploads/...
    @Value("${file.uploadPath}")
    private String imagePath;

    private final FileManager fileManager;

    // 게시글 작성/수정 폼에서 이미지 저장
    @ResponseBody
    @PostMapping("uploadPostImage")
    public ResponseEntity<?> uploadPostImage(
        @RequestParam("upload") MultipartFile upload) {
        log.debug("이미지 업로드 요청 : ");
        try {
            // 이미지 업로드 경로 지정
            // C:/uploads/images/postImages
            String uploadPath = Paths.get(imagePath, "images", "postImages").toString();
            // 이미지 파일 업로드
            String savedFileName = fileManager.saveFile(uploadPath, upload);
            // ckEditor5에 돌려줄 업로드 성공 응답 작성
            // 외부 소스에 저장된 이미지를 불러올 수 있게 매핑된 경로를 반환해야 함
            String imageUrl = "/scitHub/images/postImages/" + savedFileName;
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
