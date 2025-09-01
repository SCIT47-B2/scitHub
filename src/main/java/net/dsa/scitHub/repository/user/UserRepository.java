package net.dsa.scitHub.repository.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.enums.Gender;
import net.dsa.scitHub.enums.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    
    /** 사용자명으로 조회 */
    Optional<User> findByUsername(String username);
    
    /** 이메일로 조회 */
    Optional<User> findByEmail(String email);
    
    /** 기수별 사용자 조회 */
    List<User> findByCohortNo(Integer cohortNo);
    
    /** 학생 그룹별 사용자 조회 */
    List<User> findByStudentGroup_StudentGroupId(Integer studentGroupId);
    
    /** 역할별 사용자 조회 */
    List<User> findByRole(Role role);
    
    /** 성별 사용자 조회 */
    List<User> findByGender(Gender gender);
    
    /** 활성화된 사용자만 조회 */
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findActiveUsers();
    
    /** 기수별 활성화된 사용자 조회 */
    @Query("SELECT u FROM User u WHERE u.cohortNo = :cohortNo AND u.isActive = true")
    List<User> findActiveByCohortNo(@Param("cohortNo") Integer cohortNo);
    
    /** 사용자명 존재 여부 확인 */
    boolean existsByUsername(String username);
    
    /** 이메일 존재 여부 확인 */
    boolean existsByEmail(String email);
    
    /** 한국어 이름으로 검색 */
    List<User> findByNameKorContaining(String nameKor);
    
    /** 전화번호로 조회 */
    Optional<User> findByPhone(String phone);
    
    /** 예약 금지된 사용자 조회 */
    @Query("SELECT u FROM User u WHERE u.resvBannedUntil > :now")
    List<User> findBannedUsers(@Param("now") LocalDateTime now);
    
    /** 예약 위반 횟수가 특정 수 이상인 사용자 */
    List<User> findByResvStrikeCountGreaterThanEqual(Integer strikeCount);
    
    /** 최근 로그인한 사용자들 (페이징) */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NOT NULL ORDER BY u.lastLoginAt DESC")
    Page<User> findRecentlyLoggedInUsers(Pageable pageable);
    
    /** 관리자 사용자들 조회 */
    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN' AND u.isActive = true")
    List<User> findActiveAdmins();
    
    /** 기수별 사용자 수 조회 */
    @Query("SELECT u.cohortNo, COUNT(u) FROM User u WHERE u.isActive = true GROUP BY u.cohortNo ORDER BY u.cohortNo DESC")
    List<Object[]> countUsersByCohort();
    
    /** 생일이 특정 월인 사용자들 조회 */
    @Query("SELECT u FROM User u WHERE MONTH(u.birthDate) = :month AND u.isActive = true")
    List<User> findUsersByBirthMonth(@Param("month") Integer month);
}

