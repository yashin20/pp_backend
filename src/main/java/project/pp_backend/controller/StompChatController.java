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
import project.pp_backend.service.MessageService;
import project.pp_backend.service.RoomService;

import java.security.Principal;
import java.util.List;


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

    /**
     * 핵심 로직: 방 참여자들의 개인 큐(Queue)로 메시지를 각각 배달합니다.
     */
    private void sendToParticipants(Long roomId, MessageDto.Response message) {
        // 1. 방 참여자들의 'username'(로그인 아이디) 리스트를 가져오도록 수정
        List<String> usernames = roomService.findParticipantUsernamesByRoomId(roomId);

        for (String username : usernames) {
            // 2. 숫자가 아닌 'username'을 주소로 사용!
            messagingTemplate.convertAndSendToUser(
                    username, // 이제 "user123"이라는 문패와 일치하게 됩니다.
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

//
//    /**
//     * 클라이언트가 일반 채팅 메시지를 보낼 때 사용
//     * URL : "/pub/chat/message"
//     *
//     * @param request : 전송할 메시지 정보 (roomId, content, type 등 포함)
//     * @param principal : 인증된 사용자 정보 (사용자 ID (USERNAME)만 가져온다.)
//     */
//    @MessageMapping("/chat/message/{roomId}")
//    public void sendMessage(
//            @DestinationVariable("roomId") Long roomId,
//            Principal principal,
//            @Payload MessageDto.CreateRequest request) {
//
//        String username = principal.getName();
//        log.info("메시지 전송 - 사용자: {}, 방 번호: {}", username, roomId);
//
//        //1. DB 저장
//        MessageDto.Response savedMessage = messageService.createMessage(username, roomId, request);
//
//        // 2. STOMP 구독자들에게 메시지 전달 (브로드캐스팅)
//        // 메시지 구독 주소: /sub/chat/room/{roomId}
//        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, savedMessage);
//    }
//
//
//    /** (채팅방에 입장하였습니다.)
//     * 클라이언트가 채팅방에 최초 입장했을 때 사용
//     * URL : "/pub/chat/enter"
//     *
//     * @param request : 전송할 메시지 정보 (roomId, content, type 등 포함)
//     * @param principal : 인증된 사용자 정보 (사용자 ID (USERNAME)만 가져온다.)
//     */
//    @MessageMapping("/chat/enter/{roomId}")
//    public void enterRoom(
//            @DestinationVariable("roomId") Long roomId,
//            Principal principal,
//            MessageDto.CreateRequest request) {
//
//        String username = principal.getName();
//
//        //1. 알림 메시지 구성
//        String content = username + "님이 입장하셨습니다.";
//        request.setContent(content);
//        request.setType(MessageType.ENTER);
//
//        //2. DB 저장
//        MessageDto.Response savedMessage = messageService.createMessage(username, roomId, request);
//
//        //3. STOMP 구독자들에게 메시지 전달 (브로드캐스팅)
//        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, savedMessage);
//    }
//
//    /** (채팅방에 퇴장하였습니다.)
//     * 클라이언트가 채팅방에서 퇴장했을 때 사용
//     * URL : "/pub/chat/leave"
//     *
//     * @param request : 전송할 메시지 정보 (roomId, content, type 등 포함)
//     * @param principal : 인증된 사용자 정보 (사용자 ID (USERNAME)만 가져온다.)
//     */
//    @MessageMapping("/chat/leave/{roomId}")
//    public void leaveRoom(
//            @DestinationVariable("roomId") Long roomId,
//            Principal principal,
//            MessageDto.CreateRequest request) {
//
//        String username = principal.getName();
//
//        //1. 알림 메시지
//        String content = username + "님이 퇴장하셨습니다.";
//        request.setContent(content);
//        request.setType(MessageType.LEAVE);
//
//        //2. (회원의 채팅방 소속 삭제) RoomMember 삭제
//        roomService.leaveRoom(username, roomId);
//
//        //3. 알림 메시지 DB 저장
//        MessageDto.Response savedMessage = messageService.createMessage(username, roomId, request);
//
//        //4. STOMP 구독자들에게 메시지 전달 (브로드캐스팅)
//        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, savedMessage);
//    }

}
