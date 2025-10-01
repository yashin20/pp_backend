package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import project.pp_backend.dto.MemberDto;
import project.pp_backend.entity.Member;
import project.pp_backend.exception.DataAlreadyExistsException;
import project.pp_backend.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    //1. 회원가입 로직
    @Transactional
    public MemberDto.Response createMember(MemberDto.CreateRequest request) {
        //1-1. 중복 검사 (username, nickname, email)
        validateDuplicateMember(
                request.getUsername(),
                request.getNickname(),
                request.getEmail()
        );

        //1-2. password encoding
        request.setPassword(passwordEncoder.encode(request.getPassword()));

        //1-3. 회원 생성
        Member newMember = request.toEntity();

        //1-4. 엔티티 저장
        Member savedMember = memberRepository.save(newMember);

        //1-5. responseDto 타입 반환
        return new MemberDto.Response(savedMember);
    }

    private void validateDuplicateMember(String username, String nickname, String email) {
        // 1. Username 중복 검사
        if (StringUtils.hasText(username)) {
            if (memberRepository.findByUsername(username).isPresent()) {
                throw new DataAlreadyExistsException("이미 존재하는 username 입니다.");
            }
        }

        // 2. Nickname 중복 검사
        if (StringUtils.hasText(nickname)) {
            if (memberRepository.findByNickname(nickname).isPresent()) {
                throw new DataAlreadyExistsException("이미 존재하는 nickname 입니다.");
            }
        }

        // 3. Email 중복 검사
        if (StringUtils.hasText(email)) {
            if (memberRepository.findByEmail(email).isPresent()) {
                throw new DataAlreadyExistsException("이미 존재하는 email 입니다.");
            }
        }
    }

}
