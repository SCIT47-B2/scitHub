package net.dsa.scitHub.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.repository.user.UserRepository;

/**
 * 사용자 인증 처리
 * Spring Security를 사용하는 애플리케이션에서 사용자가 로그인할 때 실행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticatedUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        log.info("로그인 시도 : {}", id);

        User userEntity = userRepository.findByUsername(id)
                .orElseThrow(() -> {
                        return new UsernameNotFoundException(id + " : 없는 ID입니다.");
                });

        log.debug("조회정보 : {}", userEntity);

        // 인증정보 생성
        AuthenticatedUser user = AuthenticatedUser.builder()
                .userId(userEntity.getUserId())
                .id(userEntity.getUsername())
                .password(userEntity.getPasswordHash())
                .name(userEntity.getNameKor())
                .roleName(userEntity.getRole().getDisplayName())
                .enabled(userEntity.getIsActive())
                .build();

		log.debug("인증정보 : {}", user);

		return user;
	}

}
