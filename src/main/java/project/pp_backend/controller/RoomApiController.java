package project.pp_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.pp_backend.config.MemberDetails;
import project.pp_backend.dto.RoomDto;
import project.pp_backend.exception.BasicErrorMessage;
import project.pp_backend.service.RoomService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomApiController {

    private final RoomService roomService;

    /**
     * 1. 채팅방 생성
     * POST - /api/rooms/
     */
    @PostMapping
    public ResponseEntity<RoomDto.Response> createRoom(
            @Valid @RequestBody RoomDto.CreateRequest request,
            @AuthenticationPrincipal MemberDetails memberDetails) {

        String username = memberDetails.getUsername();
        RoomDto.Response response = roomService.createRoom(username, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2-1. 채팅방 단일 조회
     * GET - /api/rooms/{roomId}
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDto.Response> getRoom(@PathVariable Long roomId) {
        RoomDto.Response response = roomService.getRoom(roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * 2-2. 로그인된 회원이 참가중인 채팅방 조회
     * GET - /api/rooms/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<RoomDto.Response>> getRoomsByUsername(
            @AuthenticationPrincipal MemberDetails memberDetails) {

        String username = memberDetails.getUsername();
        List<RoomDto.Response> rooms = roomService.getRoomsByUsername(username);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 3. 채팅방 수정
     * PUT - /api/rooms/{roomId}
     */
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomDto.Response> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomDto.UpdateRequest request,
            @AuthenticationPrincipal MemberDetails memberDetails) {

        String username = memberDetails.getUsername();
        RoomDto.Response response = roomService.updateRoom(username, roomId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * 4. 채팅방 삭제
     * DELETE - /api/rooms/{roomId}
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Long> deleteRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberDetails memberDetails) {

        String username = memberDetails.getUsername();
        Long deletedRoomId = roomService.deleteRoom(username, roomId);

        return ResponseEntity.ok(deletedRoomId);
    }

    /**
     * 5. 채팅방 초대 (Invite)
     * POST - /api/rooms/{roomId}/invite
     */
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<RoomDto.Response> inviteRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomDto.InviteRequest request
    ) {
        //유효성 검증: @PathVariable-roomId 와 request.roomId 동일
        if (!roomId.equals(request.getRoomId())) {
            return ResponseEntity.badRequest().build();
        }

        //초대 대상이 비어 있는지 확인
        if (request.getUsernames() == null || request.getUsernames().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        //한번에 다수의 화원 초대 로직
        RoomDto.Response response = roomService.batchJoinRoom(roomId, request.getUsernames());

        return ResponseEntity.ok(response);
    }


    /**
     * 6. 채팅방 참가 (Join)
     * POST - /api/rooms/{roomId}/join
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<RoomDto.Response> joinRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberDetails memberDetails) {

        String username = memberDetails.getUsername();
        RoomDto.Response response = roomService.joinRoom(username, roomId);

        return ResponseEntity.ok(response);
    }

    /**
     * 6. 채팅방 퇴장 (Leave)
     * POST - /api/rooms/{roomId}/leave
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Long> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberDetails memberDetails) {

        String username = memberDetails.getUsername();
        Long leftRoomId = roomService.leaveRoom(username, roomId);

        return ResponseEntity.ok(leftRoomId);
    }

}
