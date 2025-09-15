package net.dsa.scitHub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 게시글 업로드 시 실제 파일이 저장된 경로
    @Value("${post.upload.image.path}")
    private String postImagePath;

    // 프로필 사진 업로드 시 실제 파일이 저장된 경로
    // 지연 상이 작성한 부분이지만 일단 풀리퀘 날린 재식이 기억을 바탕으로 임의로 설정함
    @Value("${app.upload.image.path}")
    private String ImagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 게시글 작성 시 업로드된 이미지 파일들 매핑
        // URL "/images/postImages/abc.jpg" 요청을 "C:/uploads/images/postImages/abc.jpg" 파일로 매핑
        registry.addResourceHandler("/images/postImages/**")
                .addResourceLocations("file:" + postImagePath + "/");

        // 프로필 사진 업로드 시 업로드된 이미지 파일들 매핑
        // URL "/images/abc.jpg" 요청을 "C:/uploads/images/abc.jpg" 파일로 매핑
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + ImagePath + "/");
    }
}