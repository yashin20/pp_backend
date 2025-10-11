package project.pp_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.pp_backend.config.MemberDetails;
import project.pp_backend.dto.MessageDto;
import project.pp_backend.service.MessageService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageApiController {

    private final MessageService messageService;

    /** 1. 메시지 생성
     * POST - /api/messages/rooms/{roomId}
     * - 인증된 사용자 (@AuthenticationPrincipal)를 통해 username 을 가져와 메시지 생성
     */
    @PostMapping("/rooms/{roomId}")
    public ResponseEntity<MessageDto.Response> createMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody MessageDto.CreateRequest request,
            @AuthenticationPrincipal MemberDetails memberDetails) {

        String username = memberDetails.getUsername();
        MessageDto.Response response = messageService.createMessage(username, roomId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** 2. 특정 채팅방의 메시지 목록 조회
     * GET - /api/messages/rooms/{roomId}
     * - 채팅방 진입 시 이전 대화 내용을 불러오는 용도
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<List<MessageDto.Response>> getMessagesByRoom(@PathVariable Long roomId) {
        List<MessageDto.Response> messages = messageService.getMessagesByRoom(roomId);
        return ResponseEntity.ok(messages);
    }

    /** 3. 단일 메시지 삭제
     * DELETE - /api/messages/{messageId}
     * - 메시지 삭제에 대한 권한을 고민해보자......
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId,
                                              @AuthenticationPrincipal MemberDetails memberDetails) {
        String username = memberDetails.getUsername();
        messageService.deleteMessage(username, messageId);

        return ResponseEntity.noContent().build();
    }
}
