package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import project.pp_backend.dto.MemberDto;
import project.pp_backend.entity.Member;
import project.pp_backend.exception.DataAlreadyExistsException;
import project.pp_backend.exception.DataNotFoundException;
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

    //중복검사 메서드
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


    //2-1. 회원 정보 조회 (id 기반)
    public MemberDto.Response getMemberById(Long id) {
        //1. ID 기반 Member 조회
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음"));

        //2. return DTO
        return new MemberDto.Response(member);
    }
    //2-1. 회원 정보 조회 (username 기반)
    public MemberDto.Response getMemberByUsername(String username) {
        //1. ID 기반 Member 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음"));

        //2. return DTO
        return new MemberDto.Response(member);
    }

    //3. 회원 정보 수정
    @Transactional
    public MemberDto.Response updateMember(String username, MemberDto.UpdateRequest request) {
        //1. ID 기반 Member 조회
//        Member member = memberRepository.findById(memberId)
//                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음"));

        //1. Username 기반 Member 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음"));

        //2. 회원 정보 수정 (nickname)
        if (StringUtils.hasText(request.getNickname()) && !member.getNickname().equals(request.getNickname())) {
            if (memberRepository.findByNickname(request.getNickname()).isPresent()) {
                throw new DataAlreadyExistsException("이미 존재하는 닉네임 입니다.");
            }
            member.updateNickname(request.getNickname());
        }

        //3. 회원 정보 수정 (email)
        if (StringUtils.hasText(request.getEmail()) && !member.getEmail().equals(request.getEmail())) {
            if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new DataAlreadyExistsException("이미 존재하는 이메일 입니다.");
            }
            member.updateEmail(request.getEmail());
        }

        //4. 변경된 회원 정보 반환
        return new MemberDto.Response(member);
    }

    //3-2. 비밀번호 수정 메서드
    @Transactional
    public String updatePassword(String username, MemberDto.PasswordRequest request) {
        //1. Username 기반 Member 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음"));

        //2. 현재 비밀번호가 올바른가?
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new SecurityException("현재 비밀번호가 일치하지 않습니다.");
        }

        //3. newPassword == repeatPassword
        if (!request.getNewPassword().equals(request.getRepeatPassword())) {
            throw new SecurityException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 4. 새로운 비밀번호가 현재 비밀번호와 동일한지 확인
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new SecurityException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        // 5. 성공시, 새 비밀번호 암호화 후 업데이트
        String newEncodedPassword = passwordEncoder.encode(request.getNewPassword());
        member.updatePassword(newEncodedPassword);

        // 6. 변경 회원 username 반환
        return username;
    }

    //4. 회원 삭제
    @Transactional
    public String deleteMember(String username) {
        //1. ID 기반 Member 조회
//        Member memberToDelete = memberRepository.findById(memberId)
//                .orElseThrow(() -> new DataNotFoundException("삭제할 회원(Member)을 찾을 수 없음"));

        //1. Username 기반 Member 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음"));
        //2. 회원 삭제
        memberRepository.delete(member);
        return username;
    }

}
