package net.dsa.scitHub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 인스타그램 API 응답의 전체 구조를 감싸는 외부 DTO
 * {"data": [...]} 형태를 매핑하기 위해 사용
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstagramResponseDTO {
    private List<InstagramPostDTO> data;
}