package net.dsa.scitHub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 실제 파일이 저장된 경로
    @Value("${app.upload.image.path}")
    private String imagePath;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // URL "/images/abc.jpg" 요청을 "C:/uploads/images/abc.jpg" 파일로 매핑
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + imagePath + "/");
    }
}