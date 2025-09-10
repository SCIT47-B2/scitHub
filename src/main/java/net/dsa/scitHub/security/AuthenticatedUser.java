package net.dsa.scitHub.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 회원 인증 정보 객체
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AuthenticatedUser implements UserDetails {

	private static final long serialVersionUID = -2757521351355190L;

	private Integer userId;

	//인증 관련 정보
	String id;
	String password;
	String name;
	String roleName;
	boolean enabled;

	//권한명 리턴
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singletonList(new SimpleGrantedAuthority(roleName));
	}

	//비밀번호 리턴
	@Override
	public String getPassword() {
		return this.password;
	}

	//사용자 아이디 리턴
	@Override
	public String getUsername() {
		return this.id;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	//정상 아이디인지 여부
	@Override
	public boolean isEnabled() {
		return this.enabled;
	}


}
