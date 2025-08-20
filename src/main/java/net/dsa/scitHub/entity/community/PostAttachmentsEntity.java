package net.dsa.scitHub.entity.community;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "post_attachments",
    indexes = @Index(name = "idx_att_post_created", columnList = "post_id, created_at")
)
public class PostAttachmentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id", columnDefinition = "int unsigned")
    private Integer attachmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_att_post"))
    private PostsEntity post;

    @Size(max = 1024)
    @Column(name = "file_url", nullable = false, length = 1024)
    private String fileUrl;

    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_bytes", columnDefinition = "int unsigned")
    private Integer fileSizeBytes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}

