package project.pp_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import project.pp_backend.dto.MessageDto;
import project.pp_backend.entity.MessageType;
import project.pp_backend.service.ChatCacheService;
import project.pp_backend.service.MessageService;
import project.pp_backend.service.RoomService;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * STOMP 프로토콜을 사용하여 실시간 채팅 메시지 처리 컨트롤러
 * /////////////////////////////////////////////////
 * 클라이언트 메시지 전송: /pub/chat/message/{roomId}
 * 클라이언트 입장 알림: /pub/chat/enter/{roomId}
 * 클라이언트 퇴장 알림: /pub/chat/leave/{roomId}
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StompChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final RoomService roomService;
    private final ChatCacheService chatCacheService;

    /**
     * 핵심 로직: 방 참여자들의 개인 큐(Queue)로 메시지를 각각 배달합니다.
     */
    private void sendToParticipants(Long roomId, MessageDto.Response message) {
//        List<String> usernames = roomService.findParticipantUsernamesByRoomId(roomId);
        Set<String> usernames = chatCacheService.getRoomParticipants(roomId);

        // Redis 캐시가 비어있는 경우 -> 서버 재시작 / DB에서 복구하는 로직
        if (usernames == null || usernames.isEmpty()) {
            List<String> dbUsernames = roomService.findParticipantUsernamesByRoomId(roomId);
            dbUsernames.forEach(name -> chatCacheService.addRoomMember(roomId, name));
            usernames = new HashSet<>(dbUsernames);
        }

        for (String username : usernames) {
            // 2. 숫자가 아닌 'username'을 주소로 사용!
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/messages",
                    message
            );
        }
    }

    @MessageMapping("/chat/message/{roomId}")
    public void sendMessage(
            @DestinationVariable("roomId") Long roomId,
            Principal principal,
            @Payload MessageDto.CreateRequest request) {

        String username = principal.getName();
        log.info("메시지 전송 - 사용자: {}, 방 번호: {}", username, roomId);

        // 1. DB 저장
        MessageDto.Response savedMessage = messageService.createMessage(username, roomId, request);

        // 2. [수정] 기존 브로드캐스팅 삭제 후 개인별 전송 호출
        sendToParticipants(roomId, savedMessage);
    }

    @MessageMapping("/chat/enter/{roomId}")
    public void enterRoom(
            @DestinationVariable("roomId") Long roomId,
            Principal principal,
            MessageDto.CreateRequest request) {

        String username = principal.getName();
        String content = username + "님이 입장하셨습니다.";
        request.setContent(content);
        request.setType(MessageType.ENTER);

        // 1. DB 저장
        MessageDto.Response savedMessage = messageService.createMessage(username, roomId, request);

        // 2. [수정] 개인별 전송 호출
        sendToParticipants(roomId, savedMessage);
    }

    @MessageMapping("/chat/leave/{roomId}")
    public void leaveRoom(
            @DestinationVariable("roomId") Long roomId,
            Principal principal,
            MessageDto.CreateRequest request) {

        String username = principal.getName();
        String content = username + "님이 퇴장하셨습니다.";
        request.setContent(content);
        request.setType(MessageType.LEAVE);

        // 1. 퇴장 알림 DB 저장
        MessageDto.Response savedMessage = messageService.createMessage(username, roomId, request);

        // 2. [수정] 퇴장 전송 (DB에서 삭제하기 전에 보내야 현재 인원들에게 전달됨)
        sendToParticipants(roomId, savedMessage);

        // 3. DB에서 RoomMember 삭제
        roomService.leaveRoom(username, roomId);
    }


}
