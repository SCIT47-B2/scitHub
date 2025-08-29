package net.dsa.scitHub.entity.board;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "attachment_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"post"})
public class AttachmentFile {
    
    /** 첨부파일 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_file_id")
    private Integer attachmentFileId;
    
    /** 첨부파일이 속한 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    /** 파일 URL */
    @Column(name = "file_url", nullable = false, length = 1024)
    private String fileUrl;
    
    /** 파일 이름 */
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    /** 파일 크기 (바이트) */
    @Column(name = "file_size")
    private Integer fileSize;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttachmentFile)) return false;
        AttachmentFile that = (AttachmentFile) o;
        return Objects.equals(attachmentFileId, that.attachmentFileId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(attachmentFileId);
    }
}
