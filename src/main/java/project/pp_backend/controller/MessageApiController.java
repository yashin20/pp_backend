package project.pp_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.pp_backend.config.MemberDetails;
import project.pp_backend.dto.MessageDto;
import project.pp_backend.service.MessageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** 1-2. 이미지 메시지 생성
     * POST - /api/messages/rooms/{roomId}/image
     */
    @PostMapping("/rooms/{roomId}/image")
    public ResponseEntity<Map<String, String>> uploadChatImage(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal MemberDetails memberDetails) {

        String username = memberDetails.getUsername();

        //1. 이미지 저장 후 URL 반환받기
        String imageUrl = messageService.uploadChatImage(file);

        //2. Map 으로 반환
        HashMap<String, String> response = new HashMap<>();
        response.put("content", imageUrl);

        return ResponseEntity.ok(response);
    }

//    /** 2. 특정 채팅방 + 회원이 볼수 있는 메시지 조회
//     * GET - /api/messages/rooms/{roomId}
//     * - 채팅방 진입 시 이전 대화 내용을 불러오는 용도
//     */
//    @GetMapping("/rooms/{roomId}")
//    public ResponseEntity<List<MessageDto.Response>> getVisibleMessages(
//            @PathVariable Long roomId,
//            @AuthenticationPrincipal MemberDetails memberDetails) {
//        String username = memberDetails.getUsername();
//        List<MessageDto.Response> messages = messageService.getVisibleMessagesByRoomAndMember(username, roomId);
//        return ResponseEntity.ok(messages);
//    }

    /**
     * 특정 채팅방 메시지 조회 (커서 기반 무한 스크롤)
     * GET /api/messages/rooms/{roomId}?cursor={oldestMessageId}
     * - 채팅방 진입 시 이전 대화 내용을 불러오는 용도
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Slice<MessageDto.Response>> getVisibleMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal MemberDetails memberDetails) {

        String username = memberDetails.getUsername();
        Slice<MessageDto.Response> messages = messageService.getMessagesByRoomWithCursor(username, roomId, cursor);
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
