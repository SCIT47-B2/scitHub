package net.dsa.scitHub.entity.inquiry;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import net.dsa.scitHub.entity.user.UsersEntity;
import net.dsa.scitHub.entity.classroom.SeatsEntity;
import java.util.List;
import java.util.ArrayList;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "inquiries",
    indexes = {
        @Index(name = "idx_inquiries_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_inquiries_status", columnList = "status, updated_at")
    }
)
public class InquiriesEntity {

    public enum Category { GENERAL, SEAT, RESERVATION, OTHER }
    public enum Status { OPEN, ANSWERED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id", columnDefinition = "int unsigned")
    private Integer inquiryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_inq_user")
    )
    private UsersEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 12)
    private Category category;

    @Size(max = 150)
    @Column(name = "subject", nullable = false, length = 150)
    private String subject;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "seat_id",
        foreignKey = @ForeignKey(name = "fk_inq_seat")
    )
    private SeatsEntity seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (category == null) category = Category.GENERAL;
        if (status == null) status = Status.OPEN;
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // 문의의 답변들
    @Builder.Default
    @OneToMany(mappedBy = "inquiry", fetch = FetchType.LAZY)
    private List<InquiryRepliesEntity> replies = new ArrayList<>();
}
