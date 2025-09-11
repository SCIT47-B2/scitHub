package net.dsa.scitHub.service;

import java.nio.file.Paths;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.MypageDTO;
import net.dsa.scitHub.dto.UserDTO;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.enums.Gender;
import net.dsa.scitHub.repository.user.UserRepository;
import net.dsa.scitHub.utils.AvatarFileManager;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository ur;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AvatarFileManager fileManager;

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
                .orElseThrow(() -> new EntityNotFoundException(username + ": 아이디가 없습니다."));

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
                .orElseThrow(() -> new EntityNotFoundException(dto.getUserName() + ": 아이디가 없습니다."));

        // 1) 텍스트 필드 반영
        entity.setNameKor(dto.getName());
        entity.setGender(Gender.valueOf(dto.getGender()));
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());

        // 2) 이미지가 올라온 경우에만 저장 처리
        if (upload != null && !upload.isEmpty()) {

            // 2-1) 실제 저장 디렉터리: {board.uploadPath}/uploads/avatar
            String avatarDir = Paths.get(uploadPath, "uploads", "avatar").toString();

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
}
