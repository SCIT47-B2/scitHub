package net.dsa.scitHub.entity.community;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "qna_posts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_qna_posts_post", columnNames = "post_id")
    }
)
public class QnaPostsEntity {

    public enum AnswerStatus { PENDING, ANSWERED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qna_posts_id", columnDefinition = "int unsigned")
    private Integer qnaPostsId;

    // 1:1 (post_id UNIQUE + FK)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "post_id",
        nullable = false,
        unique = true,
        foreignKey = @ForeignKey(name = "fk_qna_posts_post")
    )
    private PostsEntity post;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_status", nullable = false, length = 10)
    private AnswerStatus answerStatus;

    @PrePersist
    void onCreate() {
        if (answerStatus == null) answerStatus = AnswerStatus.PENDING;
    }
}
