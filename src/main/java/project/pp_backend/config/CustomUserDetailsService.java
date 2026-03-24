package project.pp_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import project.pp_backend.entity.Member;
import project.pp_backend.repository.MemberRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Qualifier("userRedisTemplate")
    private final RedisTemplate<String, Object> userRedisTemplate;

//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        Member member = memberRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("[CustomUserDetailsService] User not found with name: " + username));
//
//        log.info("📢 [USER DETAIL SERVICE] Member found: {}", member.getUsername());
//
//        return new MemberDetails(member);
//    }
@Override
public UserDetails loadUserByUsername(String username) {
    String cacheKey = "user:profile:" + username;

    // 1. Redis 확인
    Object cached = userRedisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        return (MemberDetails) cached; // 캐시 적중!
    }

    // 2. 캐시 없으면 DB 조회 (로그 1~2번 발생 시점)
    Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("유저 없음"));

    MemberDetails details = new MemberDetails(member);

    // 3. Redis에 30분 동안 보관
    userRedisTemplate.opsForValue().set(cacheKey, details, Duration.ofMinutes(30));

    return details;
}

    private Collection<? extends GrantedAuthority> getAuthorities(Member member) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + member.getMemberRole().name()));
        return authorities;
    }
}