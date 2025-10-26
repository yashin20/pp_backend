package project.pp_backend.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import project.pp_backend.entity.Member;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

@Getter
@NoArgsConstructor
public class MemberDetails implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;

    private Collection<? extends GrantedAuthority> authorities;

    public MemberDetails(Member member) {
        this.id = member.getId();
        this.username = member.getUsername();
        this.password = member.getPassword();
        this.nickname = member.getNickname();
        this.email = member.getEmail();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getMemberRole().name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}
