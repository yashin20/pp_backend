package project.pp_backend.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.pp_backend.entity.Member;
import project.pp_backend.repository.MemberRepository;
import project.pp_backend.service.MailService;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MailController {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;

    // [1단계] 인증 번호 발송 API
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        // 1. 유저가 존재하는지 먼저 확인
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 아이디입니다."));

        // 2. MailService를 통해 메일 발송 및 Redis 저장
        mailService.sendEmailCode(member.getEmail(), username);

        return ResponseEntity.ok("인증 코드가 발송되었습니다.");
    }

    // [2단계] 인증 번호 검증 API (Flutter Pinput 완료 시 호출)
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String code = request.get("code");

        boolean isSuccess = mailService.verifyCode(username, code);

        if (isSuccess) {
            return ResponseEntity.ok(Map.of("success", true, "message", "인증되었습니다."));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "인증 번호가 틀렸거나 만료되었습니다."));
        }
    }

    // [3단계] 비밀번호 재설정 API (최종 변경)
    // 무작위 난수로 비밀번호를 초기화 시키고 사용자에게 초기화된 비밀번호를 알려줌
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        System.out.println("비밀번호 재설정 요청 시작: " + username);

        // 1. Redis 에서 인증 완료 여부 확인
        String isVerified = redisTemplate.opsForValue().get("AUTH_COMPLETE:" + username);
        if (isVerified == null || !isVerified.equals("true")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("이메일 인증이 만료되었거나 완료되지 않았습니다.");
        }

        // 2. 유저 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 3. 무작위 임시 비밀번호 생성 (예: 10자리 영문+숫자)
        String tempPassword = generateTempPassword();

        // 4. 새 비밀번호 암호화 후 DB 저장
        member.updatePassword(passwordEncoder.encode(tempPassword));
        memberRepository.save(member);

        // 5. 사용자에게 초기화된 비밀번호 메일로 전송
        mailService.sendTempPassword(member.getEmail(), username, tempPassword);

        // 6. Redis 플래그 삭제
        redisTemplate.delete("AUTH_COMPLETE:" + username);

        return ResponseEntity.ok("임시 비밀번호가 이메일로 발송되었습니다.");
    }

    // 임시 비밀번호 생성 로직 (영문 대소문자 + 숫자 혼합)
    private String generateTempPassword() {
        String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            sb.append(charSet.charAt(random.nextInt(charSet.length())));
        }
        return sb.toString();
    }
}
