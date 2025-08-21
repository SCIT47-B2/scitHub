package net.dsa.scitHub.entity.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.dsa.scitHub.entity.community.*;
import net.dsa.scitHub.entity.notification.NotificationsEntity;
import net.dsa.scitHub.entity.schedule.EventsEntity;
import net.dsa.scitHub.entity.inquiry.*;
import net.dsa.scitHub.entity.classroom.*;
import net.dsa.scitHub.entity.career.*;
import net.dsa.scitHub.entity.reservation.*;
import net.dsa.scitHub.entity.course.*;
import net.dsa.scitHub.entity.assignment.*;
import net.dsa.scitHub.entity.album.*;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;

/**
 * 사용자 기본 정보 (TABLE: users)
 * - MySQL 8.0 / utf8mb4_0900_ai_ci
 * - TINYINT → Boolean
 * - DATE → LocalDate, DATETIME → LocalDateTime
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
    }
//  , indexes = { // 스키마 자동생성 시에만 사용; 실제 운영 DDL과 다르면 생략
//      @Index(name = "idx_users_last_login_at", columnList = "last_login_at")
//  }
)
public class UsersEntity {

    // ====== ENUM 정의 ======
    public enum Gender { M, F, O, N }        // 남/여/기타/무응답
    public enum ITClass { A, B }              // IT 반
    public enum ITSession { AM, PM }          // IT 오전/오후
    public enum JPClass { A, B, C, D, E, F }  // 일본어 반

    // ====== 컬럼 매핑 ======
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", columnDefinition = "int unsigned")
    private Integer userId;

    @Size(min = 4, max = 32)
    @Column(name = "username", nullable = false, length = 32)
    private String username;

    @Email
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    // BCrypt/Argon2 해시 저장
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name_kor", nullable = false, length = 50)
    private String nameKor;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 1)
    private Gender gender; // DB 기본값 'N'

    @Column(name = "cohort_no")
    private Integer cohortNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "it_class", length = 1)
    private ITClass itClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "it_session", length = 2)
    private ITSession itSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "jp_class", length = 1)
    private JPClass jpClass;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(
        name = "is_active",
        nullable = false,
        columnDefinition = "tinyint default 1"
    )
    private Boolean isActive;

    @Column(
        name = "is_admin",
        nullable = false,
        columnDefinition = "tinyint default 0"
    )
    private Boolean isAdmin;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ====== 라이프사이클 콜백 ======
    @PrePersist
    void onCreate() {
        // DB 기본값과 동기화
        if (this.isActive == null) this.isActive = true;
        if (this.isAdmin == null) this.isAdmin = false;
        if (this.gender == null) this.gender = Gender.N;
    }


    /*
     * 연관관계 매핑
     * 자주 호출할 것 같은 것만 리스트로 매핑하고, 나머지는 그때그때
     * 쿼리로 불러오는 것이 좋음
     */

    // 내가 작성한 게시글
    @Builder.Default
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<PostsEntity> posts = new ArrayList<>();

    // 내가 작성한 댓글
    @Builder.Default
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<CommentsEntity> comments = new ArrayList<>();

    // 내가 보낸 쪽지
    @Builder.Default
    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY)
    private List<DirectMessagesEntity> sentMessages = new ArrayList<>();

    // 내가 받은 쪽지
    @Builder.Default
    @OneToMany(mappedBy = "receiver", fetch = FetchType.LAZY)
    private List<DirectMessagesEntity> receivedMessages = new ArrayList<>();

    // 내가 누른 좋아요 (user_id NULL인 행은 여기 포함되지 않음)
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PostLikesEntity> likes = new ArrayList<>();

    // 내가 한 북마크
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PostBookmarksEntity> bookmarks = new ArrayList<>();

    // 내가 접수한 신고 (reporter_id NULL 허용)
    @Builder.Default
    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    private List<PostReportsEntity> reports = new ArrayList<>();

    // 내가 즐겨찾기한 게시판 레코드
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<BoardFavoritesEntity> boardFavorites = new ArrayList<>();

    // 내가 받은 알림
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<NotificationsEntity> notifications = new ArrayList<>();

    // 내 개인일정 소유 (owner_user_id)
    @Builder.Default
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private List<EventsEntity> ownedEvents = new ArrayList<>();

    // 내가 생성한 일정 (created_by)
    @Builder.Default
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    private List<EventsEntity> createdEvents = new ArrayList<>();

    // 내가 작성한 문의
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<InquiriesEntity> inquiries = new ArrayList<>();

    // 내가 단 문의 답변
    @Builder.Default
    @OneToMany(mappedBy = "responder", fetch = FetchType.LAZY)
    private List<InquiryRepliesEntity> inquiryReplies = new ArrayList<>();

    // 내가 속한 그룹
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<GroupAssignmentsEntity> groupAssignments = new ArrayList<>();

    // 내가 속한 좌석
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<SeatAssignmentsEntity> seatAssignments = new ArrayList<>();

    // 내가 만든 예약들
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ReservationsEntity> reservations = new ArrayList<>();

    // 내가 작성한 예약 신고
    @Builder.Default
    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    private List<ReservationReportsEntity> reservationReports = new ArrayList<>();

    // 내 패널티(1:1)
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private ReservationPenaltiesEntity reservationPenalty;

    // 내가 남긴 회사 리뷰
    @Builder.Default
    @OneToMany(mappedBy = "reviewer", fetch = FetchType.LAZY)
    private List<CompanyReviewsEntity> companyReviews = new ArrayList<>();

    // 내가 단 리뷰 댓글
    @Builder.Default
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    private List<CompanyReviewCommentsEntity> companyReviewComments = new ArrayList<>();

    // 내가 담당하는 강의들
    @Builder.Default
    @OneToMany(mappedBy = "instructor", fetch = FetchType.LAZY)
    private List<CoursesEntity> coursesTaught = new ArrayList<>();

    // 내가 수강 등록한 강의(조인 엔티티)
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<EnrollmentsEntity> enrollments = new ArrayList<>();

    // 내가 작성한 강의평가
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<CourseEvaluationsEntity> courseEvaluations = new ArrayList<>();

    // 내가 만든 과제
    @Builder.Default
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    private List<AssignmentsEntity> assignmentsCreated = new ArrayList<>();

    // 내가 제출한 과제
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<AssignmentSubmissionsEntity> assignmentSubmissions = new ArrayList<>();

    // 내가 만든 앨범
    @Builder.Default
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    private List<AlbumsEntity> albumsCreated = new ArrayList<>();

    // 내가 올린 사진
    @Builder.Default
    @OneToMany(mappedBy = "uploader", fetch = FetchType.LAZY)
    private List<PhotosEntity> photosUploaded = new ArrayList<>();

    // 내가 누른 사진 좋아요 (user_id NULL 행은 포함 안 됨)
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PhotoLikesEntity> photoLikes = new ArrayList<>();

    // 내가 단 사진 댓글
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PhotoCommentsEntity> photoComments = new ArrayList<>();
}
