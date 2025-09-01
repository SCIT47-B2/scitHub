package net.dsa.scitHub.entity.user;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.studentGroup.StudentGroup;
import net.dsa.scitHub.enums.Gender;
import net.dsa.scitHub.enums.Role;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@Table(name = "account")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "studentGroup")
public class Account {
    
    /** 사용자 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer accountId;
    
    /** 기수 번호 */
    @Column(name = "cohort_no", nullable = false)
    private Integer cohortNo;
    
    /** 로그인 아이디 */
    @Column(name = "username", nullable = false, length = 32)
    private String username;
    
    /** 암호화된 비밀번호 */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    /** 한국어 이름 */
    @Column(name = "name_kor", nullable = false, length = 50)
    private String nameKor;
    
    /** 생년월일 */
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    
    /** 성별 (M: 남성, F: 여성, N: 미선택) */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender = Gender.N;
    
    /** 이메일 주소 */
    @Column(name = "email", nullable = false)
    private String email;
    
    /** 전화번호 */
    @Column(name = "phone", nullable = false, length = 30)
    private String phone;
    
    /** 프로필 이미지 URL */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    /** 소속 학생 그룹 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_group_id")
    private StudentGroup studentGroup;
    
    /** 예약 위반 횟수 */
    @Builder.Default
    @Column(name = "resv_strike_count", nullable = false)
    private Integer resvStrikeCount = 0;
    
    /** 예약 금지 종료 시간 */
    @Column(name = "resv_banned_until")
    private LocalDateTime resvBannedUntil;
    
    /** 계정 활성화 여부 */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /** 사용자 권한 (USER: 일반사용자, ADMIN: 관리자) */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.USER;
    
    /** 마지막 로그인 시간 */
    @LastModifiedDate
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    /** 계정 생성 시간 */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return Objects.equals(accountId, account.accountId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }
}
