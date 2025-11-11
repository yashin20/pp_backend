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

    /** 1. Member 의 FriendShip 조회 (Owner 기준)
     * GET - /api/friends/me
     */
    @GetMapping("/me")
    public ResponseEntity<List<FriendShipDto.Response>> getMyFriends() {
        //1. 토큰에서 사용자 이름(username) 추출
        String username = getAuthenticatedUsername();

        List<FriendShipDto.Response> responses = friendShipService.getFriendShipsByOwner(username);
        return ResponseEntity.ok(responses);
    }

    /** 2. FriendShip 생성
     * POST - /api/friends/create
     */
    @PostMapping("/create")
    public ResponseEntity<FriendShipDto.Response> createFriendShip(@Valid @RequestBody FriendShipDto.CreateRequest request) {
        FriendShipDto.Response response = friendShipService.createFriendShip(request);
        return ResponseEntity.ok(response);
    }

    /** 3. FriendShip 삭제
     * DELETE - /api/friends/delete
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFriendShip(@Valid @RequestBody FriendShipDto.DeleteRequest request) {
        friendShipService.deleteFriendShip(request.getOwnerUsername(), request.getFriendUsername());
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
