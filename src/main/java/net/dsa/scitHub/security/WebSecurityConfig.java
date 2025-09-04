package net.dsa.scitHub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 환경설정 클래스
 */
@Configuration 		// 스프링 설정 클래스임을 명시
@EnableWebSecurity 	// Spring Security 활성화
/*
	Spring Security 6.x 이후 버전에서 메서드 보안 기능을 활성화하는 Annotation
	- @PreAuthorize, @PostAuthorize 사용 가능하게 함.
 */
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

	// 로그인 없이 접근 가능한 URL 목록
    private static final String[] PUBLIC_URLS = {
    		"/"                     // root
            , "/images/**"          // 이미지 경로
            , "/css/**"             // CSS 파일들
            , "/js/**"              // JavaScript 파일들
            , "/**"                 // 테스트를 위해 모든 경로 허용 (실제 서비스 시에는 제거 필요)
    };

    // @Component 와 같이 객체를 등록할때 사용, 보통 @Configuration 클래스 내에서 사용
    @Bean 	// (개발자가 직접 생성)
    protected SecurityFilterChain config(HttpSecurity http) throws Exception {
    	// HttpSecurity는 보안 설정을 정의하는 DSL 객체로, 요청 인증/인가 처리, 로그인/로그아웃 처리 등을 설정할 수 있음
        http
            // 요청에 대한 권한 설정
            .authorizeHttpRequests(author -> author
                .requestMatchers(PUBLIC_URLS).permitAll()   // 모두 접근 허용
                // /admin/ 경로로 들어오는 모든 요청은 ADMIN 권한을 가진 사용자만 접근 가능
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()               // 그 외의 모든 요청은 인증 필요
            )
            // 폼 로그인 설정
            .formLogin(formLogin -> formLogin
                    .loginPage("/user/landingPage")         // 로그인폼 페이지 경로
                    .usernameParameter("username")                // 폼의 ID 파라미터 이름
                    .passwordParameter("password")          // 폼의 비밀번호 파라미터 이름
                    .loginProcessingUrl("/user/login")    // 로그인폼 제출하여 처리할 경로
                    .defaultSuccessUrl("/", true)           // 로그인 성공 시 이동할 경로
                    .permitAll()                            // 로그인 페이지는 모두 접근 허용
            )
            // 로그아웃 설정
            .logout(logout -> logout
                .logoutUrl("/member/logout")            // 로그아웃 처리 경로
                .logoutSuccessUrl("/")                  // 로그아웃 성공 시 이동할 경로
            )
            // 권한 예외 설정, 인가(authorization) 실패에만 적용
            .exceptionHandling( ex -> ex
                // 접근 권한이 없을 때(403) 자동으로 보여줄 페이지 경로를 지정
                .accessDeniedHandler(
                    (request, response, accessDeniedException) -> {
                        response.sendRedirect("/scitHub/error/403"); // 403 에러 페이지로 리다이렉트
                    }
                )
            );

        // 개발용 설정: CORS, CSRF 비활성화 (실제 서비스 시에는 활성화 필요)
        http
            .cors(AbstractHttpConfigurer::disable)			// CORS (Cross-Origin Resource Sharing) 설정 비활성화
            .csrf(AbstractHttpConfigurer::disable);			// CSRF (Cross-Site Request Forgery) 보호 비활성화

        return http.build();
    }

    /*
		암호화 하는 객체를 정의
		비밀번호 + 임의의 문자열(솔트) > 해싱함수 > 암호화
		단방향 해싱함수(각 이름별 함수)를 이용하기 떄문에 복호화 불가능
		비교할 비밀번호와 일치하는지는 확인 가능
     */
    @Bean
    BCryptPasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
