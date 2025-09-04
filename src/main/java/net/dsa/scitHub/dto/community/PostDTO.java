package net.dsa.scitHub.dto.community;

import java.util.ArrayList;

import groovy.transform.builder.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO {
    public String board;
    public String username;
    public String title;
    public String content;
    public int viewCount;
    public ArrayList<String> tagList;
}
