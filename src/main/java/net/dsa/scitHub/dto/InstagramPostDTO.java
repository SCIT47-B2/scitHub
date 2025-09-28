package net.dsa.scitHub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


// 개별 게시물 데이터를 담는 내부 DTO
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstagramPostDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("media_url")
    private String mediaUrl;

    @JsonProperty("permalink")
    private String permalink;

    @JsonProperty("caption")
    private String caption;
}