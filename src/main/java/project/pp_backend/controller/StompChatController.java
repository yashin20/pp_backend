package project.pp_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import project.pp_backend.config.MemberDetails;
import project.pp_backend.dto.MessageDto;
import project.pp_backend.entity.MessageType;
import project.pp_backend.service.MessageService;
import project.pp_backend.service.RoomService;

import java.security.Principal;


/**
 * STOMP 프로토콜을 사용하여 실시간 채팅 메시지 처리 컨트롤러
 * /////////////////////////////////////////////////
 * 클라이언트 메시지 전송: /pub/chat/message
 * 클라이언트 입장 알림: /pub/chat/enter
 * 클라이언트 퇴장 알림: /pub/chat/leave
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class StompChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final RoomService roomService;


    /**
     * 클라이언트가 일반 채팅 메시지를 보낼 때 사용
     * URL : "/pub/chat/message"
     *
     * @param request : 전송할 메시지 정보 (roomId, content, type 등 포함)
     * @param principal : 인증된 사용자 정보 (사용자 ID (USERNAME)만 가져온다.)
     */
    @MessageMapping("/chat/message")
    public void sendMessage(
            Principal principal,
            @Payload MessageDto.CreateRequest request) {

        String username = principal.getName();
        log.info("sendMessage: principal.getName(): {}", principal.getName());

        Long roomId = request.getRoomId();

        //1. DB 저장
        MessageDto.Response savedMessage = messageService.createMessage(username, roomId, request);

        // 2. STOMP 구독자들에게 메시지 전달 (브로드캐스팅)
        // 메시지 구독 주소: /sub/chat/room/{roomId}
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, savedMessage);
    }


    /**
     * 클라이언트가 채팅방에 입장했을 때 사용
     * URL : "/pub/chat/enter"
     *
     * @param request : 전송할 메시지 정보 (roomId, content, type 등 포함)
     * @param principal : 인증된 사용자 정보 (사용자 ID (USERNAME)만 가져온다.)
     */
    @MessageMapping("/chat/enter")
    public void enterRoom(
            Principal principal,
            MessageDto.CreateRequest request) {

        String username = principal.getName();
        Long roomId = request.getRoomId();

        //1. 알림 메시지 구성
        String content = username + "님이 입장하셨습니다.";
        request.setContent(content);
        request.setType(MessageType.ENTER);

        //2. DB 저장
        MessageDto.Response savedMessage = messageService.createMessage(username, roomId, request);

        //3. STOMP 구독자들에게 메시지 전달 (브로드캐스팅)
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, savedMessage);
    }

    /**
     * 클라이언트가 채팅방에서 퇴장했을 때 사용
     * URL : "/pub/chat/leave"
     *
     * @param request : 전송할 메시지 정보 (roomId, content, type 등 포함)
     * @param principal : 인증된 사용자 정보 (사용자 ID (USERNAME)만 가져온다.)
     */
    @MessageMapping("/chat/leave")
    public void leaveRoom(
            Principal principal,
            MessageDto.CreateRequest request) {

        String username = principal.getName();
        Long roomId = request.getRoomId();

        //1. 알림 메시지
        String content = username + "님이 퇴장하셨습니다.";
        request.setContent(content);
        request.setType(MessageType.LEAVE);

        //2. (회원의 채팅방 소속 삭제) RoomMember 삭제
        roomService.leaveRoom(username, roomId);

        //3. 알림 메시지 DB 저장
        MessageDto.Response savedMessage = messageService.createMessage(username, roomId, request);

        //4. STOMP 구독자들에게 메시지 전달 (브로드캐스팅)
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, savedMessage);
    }

}
