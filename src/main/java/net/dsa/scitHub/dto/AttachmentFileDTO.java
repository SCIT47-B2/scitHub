package net.dsa.scitHub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentFileDTO {
    private Integer attachmentFileId;
    private String fileUrl;
    private String fileName;
    private Integer fileSize;
}
