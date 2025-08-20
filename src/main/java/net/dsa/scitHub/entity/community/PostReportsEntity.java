package net.dsa.scitHub.entity.community;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import net.dsa.scitHub.entity.user.UsersEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "post_reports",
    uniqueConstraints = @UniqueConstraint(name = "uq_report_once", columnNames = {"post_id", "reporter_id"}),
    indexes = @Index(name = "idx_reports_post_created", columnList = "post_id, created_at")
)
public class PostReportsEntity {

    public enum Status { PENDING, CONFIRMED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", columnDefinition = "int unsigned")
    private Integer reportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_rep_post"))
    private PostsEntity post;

    // 신고자 NULL 허용, 삭제 시 CASCADE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id",
        foreignKey = @ForeignKey(name = "fk_rep_user"))
    private UsersEntity reporter;

    @Size(max = 255)
    @Column(name = "reason", length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status; // default PENDING

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (status == null) status = Status.PENDING;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
