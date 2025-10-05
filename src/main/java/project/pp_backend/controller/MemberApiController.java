package project.pp_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import project.pp_backend.dto.MemberDto;
import project.pp_backend.service.MemberService;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /**
     * 1. 회원가입 엔드포인트
     * POST /api/members/register
     * @param request ID, PW, 닉네임, 이메일 등을 포함하는 회원가입 요청 DTO
     * @return 생성된 회원 정보 (MemberDto.Response)
     */
    @PostMapping("/register")
    public ResponseEntity<MemberDto.Response> register(@Valid @RequestBody MemberDto.CreateRequest request) {
        //회원가입 처리
        MemberDto.Response response = memberService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); //HTTP 201 Created 응답
    }

    /** *****TEST 용 엔드포인트야!
     * 2. 인증된 사용자 정보 조회 엔드포인트
     * GET /api/members/my
     * AccessToken을 포함한 요청만 접근 가능
     * @return 현재 인증된 사용자의 ID(username)와 닉네임
     */
    @GetMapping("/my")
    public ResponseEntity<String> getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //AccessToken이 없거나 유효하지 않아 인증에 실패한 경우
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized: 인증 정보가 유효하지 않습니다.");
        }

        String username = authentication.getName();
        return ResponseEntity.ok("인증 성공! 현재 로그인 ID: " + username);
    }

    /**
     * 2. 사용자 정보 (로그인 회원 정보 - GET)
     * GET - /api/members/me
     * AccessToken을 포함한 요청만 접근 가능
     * @return 회원 정보 - MemberDto.Response
     */

    /**
     * 3. 회원 정보 수정 (PATCH)
     * PATCH - /api/members/me
     * @param request 수정할 닉네임, 이메일 등의 정보를 포함하는 DTO
     * @return 업데이트된 회원 정보 - MemberDto.Response
     */

    /**
     * 4. 회원 탈퇴 (Delete)
     * DELETE - /api/members/me
     * @return 성공 / 실패 코드
     */

}
