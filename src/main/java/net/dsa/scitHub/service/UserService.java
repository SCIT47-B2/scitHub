package net.dsa.scitHub.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.UserDTO;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.enums.Gender;
import net.dsa.scitHub.repository.user.UserRepository;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository ur;

    private final BCryptPasswordEncoder passwordEncoder;

	/**
	 * 가입시 아이디 중복 확인
	 * @param searchId	조회할 아이디
	 * @return 해당 아이디로 가입 가능 여부 true/false
	 */
	public boolean idCheck(String searchId) {
		
		return !ur.existsByUsername(searchId); //일치하는 true 회원이 있으면 해당 아이디가 이미 사용중이라는 것. 따라서 !를 붙여서 반대 경우로 유도한다.
	}

    /**
	 * 회원가입 처리
	 * @param dto	회원가입 정보
	 */
	public void join(UserDTO dto) {
		log.info("회원가입 처리 : {}", dto);
		User entity = User.builder()
                .cohortNo(dto.getCohortNo())
				.username(dto.getUserName())
				.passwordHash(
						passwordEncoder.encode(dto.getSignupPassword())
				)
				.nameKor(dto.getName())
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

}
