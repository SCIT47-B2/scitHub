package net.dsa.scitHub.service;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.MypageDTO;
import net.dsa.scitHub.dto.UserDTO;
import net.dsa.scitHub.dto.UserManageDTO;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.enums.Gender;
import net.dsa.scitHub.enums.Role;
import net.dsa.scitHub.repository.user.UserRepository;
import net.dsa.scitHub.utils.FileManager;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository ur;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FileManager fileManager;

    /**
     * 아이디 중복 확인
     * @param searchId String - 조회할 아이디
     * @return boolean - 사용 가능(true)/불가(false)
     */
    public boolean idCheck(String searchId) {
        return !ur.existsByUsername(searchId);
    }

    /**
     * 회원가입 처리
     * @param dto UserDTO - 회원가입 정보
     * @return void
     */
    public void join(UserDTO dto) {
        log.info("회원가입 처리 : {}", dto);

        User entity = User.builder()
                .cohortNo(dto.getCohortNo())
                .username(dto.getUserName())
                .passwordHash(passwordEncoder.encode(dto.getSignupPassword()))
                .nameKor(dto.getName())
                /**
                 * A안 원칙: DB에는 파일명만 저장.
                 * - 가입 시 기본 이미지는 템플릿에서 처리(값이 비었으면 기본 이미지 노출)
                 * - NOT NULL 제약이면 ""(빈문자열) 사용을 고려
                 */
                .avatarUrl(null)
                .birthDate(dto.getBirth())
                .gender(Gender.valueOf(dto.getGender()))
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .build();

        log.info("회원가입 엔티티 : {}", entity);
        ur.save(entity);
    }
    /**
     * userId로 username 조회
     * @param userId
     * @return username or null
     */
    public String findNameKorById(Integer userId) {
        return ur.findById(userId)
            .map(User::getNameKor)
            .orElse(null);
    }

    /**
     * 회원정보 조회
     * @param username String - 회원 아이디
     * @return MypageDTO - 화면 표시용 정보
     */
    public MypageDTO getMemberInfo(String username) {
        User entity = ur.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("該当するユーザーが見つかりません。IDをご確認のうえ、再度お試しください。"));

        return MypageDTO.builder()
                .userId(entity.getUserId())
                .cohortNo(entity.getCohortNo())
                .avatarUrl(entity.getAvatarUrl())
                .userName(entity.getUsername())
                .name(entity.getNameKor())
                .birth(entity.getBirthDate())
                .gender(entity.getGender().name())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .role(entity.getRole().getDisplayName())
                .build();
    }

    /**
     * 개인정보 수정 처리
     * @param dto MypageDTO - 수정할 정보(이름/성별/이메일/전화 등)
     * @param uploadPath String - board.uploadPath(예: C:/spring/data/scitHub/uploads)
     * @param upload MultipartFile - name="avatar" 로 넘어온 프로필 이미지(없을 수 있음)
     * @return void
     * @throws Exception 저장 중 오류
     */
    public void edit(MypageDTO dto, String uploadPath, MultipartFile upload) throws Exception {
        User entity = ur.findByUsername(dto.getUserName())
                .orElseThrow(() -> new EntityNotFoundException("該当するユーザーが見つかりません。IDをご確認のうえ、再度お試しください。"));

        // 1) 텍스트 필드 반영
        entity.setNameKor(dto.getName());
        entity.setGender(Gender.valueOf(dto.getGender()));
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());

        // 2) 이미지가 올라온 경우에만 저장 처리
        if (upload != null && !upload.isEmpty()) {

            // 2-1) 실제 저장 디렉터리: {board.uploadPath}/images/avatar
            String avatarDir = Paths.get(uploadPath, "images", "avatar").toString();

            // 2-2) 파일 저장(유틸은 디렉터리 생성/파일명 생성/저장 담당, 리턴: "yyyyMMdd_uuid.ext")
            String savedFileName = fileManager.saveFile(avatarDir, upload);

            // 2-3) 기존 파일 삭제 (DB에 파일명이 있을 때만, 기본이미지 경로가 아닌 경우만)
            String oldFileName = entity.getAvatarUrl();
            if (oldFileName != null && !oldFileName.isBlank() && !oldFileName.contains("/")) {
                try {
                    fileManager.deleteFile(avatarDir, oldFileName);
                } catch (Exception ignore) {
                    log.warn("기존 아바타 삭제 실패(무시): {}", oldFileName);
                }
            }

            // 2-4) DB에는 '파일명만' 저장
            entity.setAvatarUrl(savedFileName);

            log.debug("아바타 저장 완료: dir={}, file={}", avatarDir, savedFileName);
        }

        log.debug("저장되는 Entity: {}", entity);
        ur.save(entity); // @Transactional 이므로 커밋 시점에 flush
        log.debug("저장 후 조회: {}", ur.findByUsername(dto.getUserName()));
    }

    /**
     * 관리자 페이지 - 사용자 목록 조회
     * @param page int - 페이지 번호
     * @param pageSize int - 페이지당 사용자 수
     * @param searchType String - 검색 유형 (ID, 이름 등)
     * @param searchWord String - 검색어
     * @param cohortNo Integer - 코호트 번호
     * @param roleStr String - 역할 (ALL, ADMIN, USER 등)
     * @return Page<UserManageDTO> - 페이징된 사용자 목록
     */
    public Page<UserManageDTO> findUsersByCriteria(int page, int pageSize, String searchType, String searchWord, Integer cohortNo, String roleStr) {
        // 정렬 기준: 회원번호(userId) 오름차순
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("userId").ascending());

        // 권한 문자열을 Role Enum으로 변환 (ALL은 null)
        final Role role;
        if (roleStr != null && !roleStr.equalsIgnoreCase("ALL")) {
            // "ROLE_ADMIN" -> Role.ROLE_ADMIN 으로 변환
            role = Arrays.stream(Role.values())
                .filter(r -> r.getDisplayName().equalsIgnoreCase(roleStr))
                .findFirst()
                .orElse(null);
            log.info("검색할 role: {}", role);
            if (role == null) {
                log.warn("알 수 없는 role: {}, 전체 검색으로 처리", roleStr);
            }
        } else {
            role = null; // ALL인 경우
        }

        // Repository 호출
        Page<User> userPage = ur.findByCriteria(searchType, searchWord, cohortNo, role, pageable);

        // Page<User>를 Page<UserManageDTO>로 변환
        return userPage.map(UserManageDTO::convertToUserDTO);
    }

    /**
     * 기수 목록 조회
     * @return List<Integer> - 기수 번호 목록
     */
    public List<Integer> findAllCohorts() {
        return ur.findDistinctCohortNos();
    }

    /**
     * 사용자 활성/비활성 상태 토글
     * @param userId 대상 사용자의 ID
     * @return UserManageDTO - 변경된 사용자 정보
     * @throws EntityNotFoundException - 사용자가 존재하지 않을 경우
     */
    @Transactional
    public UserManageDTO toggleUserStatus(Integer userId) {
        User user = ur.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("該当するユーザーが見つかりません。IDをご確認のうえ、再度お試しください。"));

        // 현재 상태를 반전시켜서 설정
        user.setIsActive(!user.getIsActive());
        User updatedUser = ur.save(user); // 변경된 엔티티를 받아옴

        return UserManageDTO.convertToUserDTO(updatedUser); // DTO로 변환하여 반환
    }

    /**
     * 사용자의 권한을 변경 (ADMIN <-> USER)
     * @param userId 대상 사용자의 ID
     * @return UserManageDTO - 변경된 사용자 정보
     * @throws EntityNotFoundException - 사용자가 존재하지 않을 경우
     */
    @Transactional
    public UserManageDTO changeUserRole(Integer userId) {
        User user = ur.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("該当するユーザーが見つかりません。IDをご確認のうえ、再度お試しください。"));

        // 현재 역할을 ADMIN과 USER 사이에서 토글
        Role newRole = (user.getRole() == Role.ADMIN) ? Role.USER : Role.ADMIN;
        user.setRole(newRole);
        User updatedUser = ur.save(user); // 변경된 엔티티를 받아옴

        return UserManageDTO.convertToUserDTO(updatedUser); // DTO로 변환하여 반환
    }
}
