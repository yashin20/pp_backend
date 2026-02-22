package project.pp_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import project.pp_backend.dto.FriendShipDto;
import project.pp_backend.service.FriendShipService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends/")
public class FriendShipApiController {

    private final FriendShipService friendShipService;

    /** [ADMIN] Member 의 FriendShip 조회 (Owner 기준)
     * GET - /api/friends/{owner.username}
     */
    @GetMapping("/{username}")
    public ResponseEntity<List<FriendShipDto.Response>> getFriends(@PathVariable String username) {
        List<FriendShipDto.Response> responses = friendShipService.getFriendShipsByOwner(username);
        return ResponseEntity.ok(responses);
    }

    /**
     * [통합] 로그인된 사용자의 모든 관계(친구, 보낸/받은 신청, 차단) 조회
     * GET - /api/friends/me
     */
    @GetMapping("/me")
    public ResponseEntity<List<FriendShipDto.Response>> getMyAllRelationships() {
        // 1. 토큰에서 사용자 이름 추출
        String username = getAuthenticatedUsername();

        // 2. 서비스에서 OR 쿼리를 사용하여 A와 관련된 모든 리스트 조회
        List<FriendShipDto.Response> responses = friendShipService.getMemberFriendShip(username);

        return ResponseEntity.ok(responses);
    }

    /** 1-1. '친구(FriendShipStatus.ACCEPTED)' 목록 조회
     * GET - /api/friends/me/list
     */
    @GetMapping("/me/list")
    public ResponseEntity<List<FriendShipDto.Response>> getMyFriends() {
        //1. 토큰에서 사용자 이름(username) 추출
        String username = getAuthenticatedUsername();

        List<FriendShipDto.Response> responses = friendShipService.getFriendShipsByOwner(username);
        return ResponseEntity.ok(responses);
    }

    /** 1-2. '보낸 친구 신청(FriendShipStatus.PENDING)' 목록 조회
     * GET - /api/friends/me/sent
     */
    @GetMapping("/me/sent")
    public ResponseEntity<List<FriendShipDto.Response>> getSentFriendRequests() {
        String username = getAuthenticatedUsername();
        return ResponseEntity.ok(friendShipService.getSendFriendRequests(username));
    }

    /** 1-3. '받은 친구 신청(FriendShipStatus.PENDING)' 목록 조회
     * GET - /api/friends/me/received
     */
    @GetMapping("/me/received")
    public ResponseEntity<List<FriendShipDto.Response>> getReceivedFriendRequests() {
        String username = getAuthenticatedUsername();
        return ResponseEntity.ok(friendShipService.getReceivedFriendRequests(username));
    }

    /** 1-4. '차단한 친구(FriendShipStatus.BLOCKED)' 목록 조회
     * GET - /api/friends/me/received
     */
    @GetMapping("/me/blocked")
    public ResponseEntity<List<FriendShipDto.Response>> getBlockedFriendRequests() {
        String username = getAuthenticatedUsername();
        return ResponseEntity.ok(friendShipService.getBlockedMembers(username));
    }



    /**
     * 2-*. 닉네임을 통한 친구 요청 보내기
     * POST - /api/friends/send-by-nickname
     * Request Parameter: myId(나의 ID), nickname(상대방 닉네임)
     */
    @PostMapping("/send-by-nickname")
    public ResponseEntity<FriendShipDto.Response> sendFriendRequestByNickname(
            @Valid @RequestBody FriendShipDto.SendNicknameRequest request) {

        // Service에서 이전에 만든 래퍼 메서드를 호출합니다.
        FriendShipDto.Response response = friendShipService.sendFriendShipRequestByNickname(request);
        return ResponseEntity.ok(response);
    }


    /** 2-1. 친구 요청 보내기
     * POST - /api/friends/send
     */
    @PostMapping("/send")
    public ResponseEntity<FriendShipDto.Response> sendFriendRequest(@Valid @RequestBody FriendShipDto.Request request) {
        FriendShipDto.Response response = friendShipService.sendFriendShipRequest(request);
        return ResponseEntity.ok(response);
    }
    /** 2-2. 친구 요청 수락 (친구 관계 성립)
     * POST - /api/friends/accept
     */
    @PostMapping("/accept")
    public ResponseEntity<FriendShipDto.Response> acceptFriendRequest(@Valid @RequestBody FriendShipDto.Request request) {
        FriendShipDto.Response response = friendShipService.acceptFriendShipRequest(request);
        return ResponseEntity.ok(response);
    }
    /** 2-3. 친구 차단하기
     * DELETE - /api/friends/block
     */
    @PostMapping("/block")
    public ResponseEntity<FriendShipDto.Response> blockFriendShip(@Valid @RequestBody FriendShipDto.Request request) {
        FriendShipDto.Response response = friendShipService.blockMember(request);
        return ResponseEntity.ok(response);
    }
    /** 2-4. 친구 삭제하기
     * DELETE - /api/friends/delete
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFriendShip(@Valid @RequestBody FriendShipDto.Request request) {
        friendShipService.deleteFriendShip(request);
        return ResponseEntity.noContent().build();
    }

    /** 4. 친구 키워드 검색 (친구 닉네임 검색)
     * GET - /api/friends/search/{keyword}
     */
    @GetMapping("/search/{keyword}")
    public ResponseEntity<List<FriendShipDto.Response>> searchFriends(@PathVariable String keyword) {
        //1. 토큰에서 사용자 이름(username) 추출
        String username = getAuthenticatedUsername();
        //2. 친구 키워드 검색
        List<FriendShipDto.Response> responses = friendShipService.searchFriendShipForOwner(username, keyword);
        return ResponseEntity.ok(responses);
    }

    //*********** Helper 메서드 **************
    //현재 인증된(로그인된) 사용자 이름(username) 추출
    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //인증에 실패 했거나 토큰이 없는 경우
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            throw new SecurityException("인증 정보가 유효하지 않습니다.");
        }

        return authentication.getName();
    }
}
