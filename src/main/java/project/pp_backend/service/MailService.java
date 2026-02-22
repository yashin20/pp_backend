package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    //인증 코드 유효 시간 (3분)
    private final long VERIFICATION_CODE_TTL = 3 * 60L;

    public void sendEmailCode(String email, String username) {
        //1. 6자리 난수 생성
        String code = String.format("%06d", new Random().nextInt(1000000));

        //2. Redis 저장 (Key: "AUTH:" + username, Value: code)
        redisTemplate.opsForValue().set(
                "AUTH:" + username,
                code,
                Duration.ofSeconds(VERIFICATION_CODE_TTL)
        );

        //3. 이메일 발송 내용 구성
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[PING] 인증 번호 안내");
        message.setText("안녕하세요 " + username + "님,\n\n인증 번호는 [" + code + "] 입니다.\n5분 이내에 입력해주세요.");

        mailSender.send(message);
    }

    public boolean verifyCode(String username, String inputCode) {
        String savedCode = redisTemplate.opsForValue().get("AUTH:" + username);

        if (savedCode != null && savedCode.equals(inputCode)) {
            //인증 성공시 즉시 삭제
            redisTemplate.delete("AUTH:" + username);

            //인증 완료 플래그 저장 (5분간 저장)
            redisTemplate.opsForValue().set("AUTH_COMPLETE:" + username, "true", Duration.ofMinutes(5));
            return true;
        }
        return false;
    }


    //메일로 임시 비밀번호를 보내는 로직
    public void sendTempPassword(String email, String username, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[PING] 임시 비밀번호 발급 안내");
        message.setText("안녕하세요 " + username + "님,\n\n비밀번호가 초기화되었습니다.\n" +
                "임시 비밀번호: [" + tempPassword + "]\n\n로그인 후 반드시 비밀번호를 변경해주세요.");

        mailSender.send(message);
    }
}
