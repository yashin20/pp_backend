package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
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
    private final FileService fileService;

    @Value("${file.url-prefix.profile}")
    private String profileUrlPrefix;

    @Qualifier("userRedisTemplate")
    private final RedisTemplate<String, Object> userRedisTemplate;

    private static final String CACHE_KEY_PREFIX = "user:profile:";


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
        Member newMember = Member.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .email(request.getEmail())
                .memberRole(request.getRole())
                .build();

        //1-4. 엔티티 저장
        Member savedMember = memberRepository.save(newMember);

        //1-5. responseDto 타입 반환
        return new MemberDto.Response(savedMember, profileUrlPrefix);
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

    //"nickname" 중복 검사 메서드(회원가입, 회원 정보 수정)
    public boolean isNicknameDuplicated(String nickname) {
        return memberRepository.findByNickname(nickname).isPresent();
    }
    //"username" 중복 검사 메서드(회원가입)
    public boolean isUsernameDuplicated(String username) {
        return memberRepository.findByUsername(username).isPresent();
    }
    //"email" 중복 검사 메서드(회원가입, 회원 정보 수정)
    public boolean isEmailDuplicated(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }


    //2-1. 회원 정보 조회 (id 기반)
    public MemberDto.Response getMemberById(Long id) {
        //1. ID 기반 Member 조회
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음"));

        //2. return DTO
        return new MemberDto.Response(member, profileUrlPrefix);
    }
    //2-1. 회원 정보 조회 (username 기반)
    public MemberDto.Response getMemberByUsername(String username) {
        //1. ID 기반 Member 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음"));

        //2. return DTO
        return new MemberDto.Response(member, profileUrlPrefix);
    }

    //3. 회원 정보 수정
    @Transactional
    public MemberDto.Response updateMember(String username, MemberDto.UpdateRequest request) {

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

        /** [Redis] Redis 캐시 삭제 **/
        userRedisTemplate.delete(CACHE_KEY_PREFIX + username);

        //4. 변경된 회원 정보 반환
        return new MemberDto.Response(member, profileUrlPrefix);
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

        /** [Redis] Redis 캐시 삭제 **/
        userRedisTemplate.delete(CACHE_KEY_PREFIX + username);

        // 6. 변경 회원 username 반환
        return username;
    }

    //4. 회원 삭제
    @Transactional
    public String deleteMember(String username) {
        //1. Username 기반 Member 조회
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음"));
        //2. 회원 삭제
        memberRepository.delete(member);

        /** [Redis] Redis 캐시 삭제 **/
        userRedisTemplate.delete(CACHE_KEY_PREFIX + username);

        return username;
    }


    /**
     * 프로필 이미지 업데이트 로직
     * @param username: 현재 로그인한 사용자의 ID
     * @param file: Flutter에서 넘어온 이미지 파일
     */
    @Transactional
    public String updateProfileImage(String username, MultipartFile file) {
        // 1. [사용자 조회] DB에서 해당 유저를 찾습니다.
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        // 2. [기존 파일 삭제]
        // 유저에게 이미 프로필 사진이 있다면, 서버 하드디스크 용량 확보를 위해 옛날 사진을 삭제합니다.
        if (member.getProfileImage() != null) {
            fileService.deleteProfile(member.getProfileImage());
        }

        // 3. [새 파일 저장]
        // FileService의 storeFile을 호출하여 물리적 저장 후 새 UUID 이름을 받아옵니다.
        String newFileName = fileService.storeProfile(file);

        // 4. [DB 업데이트]
        // Member 엔티티의 필드를 변경합니다. @Transactional 덕분에 메서드 종료 시 자동 반영(Dirty Check)됩니다.
        member.updateProfileImage(newFileName);

        /** [Redis] Redis 캐시 삭제 **/
        userRedisTemplate.delete(CACHE_KEY_PREFIX + username);

        // 5. 나중에 Flutter에서 확인하기 편하도록 바뀐 파일명을 반환합니다.
        return newFileName;
    }


}
