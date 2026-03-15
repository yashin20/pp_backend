package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.pp_backend.dto.MessageDto;
import project.pp_backend.entity.*;
import project.pp_backend.exception.DataNotFoundException;
import project.pp_backend.repository.MemberRepository;
import project.pp_backend.repository.MessageRepository;
import project.pp_backend.repository.RoomRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final FileService fileService;

    //message 개수 임계값 (100개)
    @Value("${chat.limits.max-messages-per-room}")
    private int MESSAGE_COUNT_THRESHOLD;
    @Value("${chat.limits.slice-size}")
    private int MESSAGE_SLICE_SIZE;

    @Value("${file.url-prefix.profile}")
    private String profileUrlPrefix;
    @Value("${file.url-prefix.chat}")
    private String chatUrlPrefix;

    //1. 메시지 생성
    @Transactional
    public MessageDto.Response createMessage(String username, Long roomId, MessageDto.CreateRequest request) {
        //1. 회원 및 채팅방 존재 유무 확인
        Member sender = findMemberByUsername(username);
        Room room = findRoomById(roomId);

        Member recipient = null;

        //2. 귓속말 수신자 처리 로직
        if (request.getRecipientUsername() != null && !request.getRecipientUsername().isEmpty()) {
            recipient = findMemberByUsername(request.getRecipientUsername());

            // 자기 자신에게 귓속말을 보낼 수 없도록 방지
            if (sender.getId().equals(recipient.getId())) {
                throw new IllegalArgumentException("자기 자신에게 귓속말을 보낼 수 없습니다.");
            }
        }

        //2. 메시지 엔티티 생성
//        MessageType determinedType = (recipient != null) && (request.getType() != null) ? MessageType.WHISPER : MessageType.CHAT;
        MessageType determinedType = request.getType(); // 기본적으로 클라이언트가 보낸 타입(CHAT, IMAGE 등)을 따름

        if (recipient != null) {
            determinedType = MessageType.WHISPER; // 수신자가 있으면 강제로 귓속말 타입
        } else if (determinedType == null) {
            determinedType = MessageType.CHAT;    // 아무것도 없으면 일반 채팅
        }
        Message message = Message.builder()
                .content(request.getContent())
                .type(determinedType)
                .member(sender) // DB에서 조회한 엔티티
                .room(room)     // DB에서 조회한 엔티티
                .recipient(recipient) // DB에서 조회한 엔티티 또는 null
                .build();

        //3. DB 저장
        messageRepository.save(message);

        // 4. 메시지 개수 관리 (100개 넘으면 오래된 것 삭제)
//        cleanupOldMessages(roomId);

        return new MessageDto.Response(message, profileUrlPrefix, chatUrlPrefix);
    }



    /**
     * 2. 메시지 조회 (특정 채팅방의 메시지 목록)
     * - 채팅방 진입 시 대화 기록을 불러오는데 사용
     * @param roomId : 메시지들은 조회할 채팅방 ID
     * @return : 메시지 응답 DTO 리스트 (최신순)
     */
    public List<MessageDto.Response> getVisibleMessagesByRoomAndMember(String username, Long roomId) {
        //1. 회원 및 채팅방 존재 유무 확인
        Member currentUser = findMemberByUsername(username);

        if (!roomRepository.existsById(roomId)) {
            throw new DataNotFoundException("채팅방을 찾을 수 없습니다.");
        }

        Pageable pageable = PageRequest.of(0, MESSAGE_SLICE_SIZE);

        //Slice 로 메시지 조회 및 List 추출
        Slice<Message> messageSlice = messageRepository.findVisibleMessagesByRoomAndMember(
                roomId,
                currentUser.getId(),
                pageable
        );

        List<Message> messages = messageSlice.getContent();

        //3. DTO 변환
        return messages.stream()
                .map(message -> new MessageDto.Response(message, profileUrlPrefix, chatUrlPrefix))
                .collect(Collectors.toList());
    }


    /** 채팅방 메시지 조회 (무한 스크롤)
     * Cursor(oldestMessageId) 를 이용해 메시지 목록을 불러옴
     * @param roomId : 메시지들을 조회할 채팅방 ID
     * @param oldestMessageId : 현재 화면에 보이는 가장 오래된 메시지 ID (커서 역할). 최초 요청 시 null.
     * @return : 메시지 응답 DTO Slice (최신순)
     */
    public Slice<MessageDto.Response> getMessagesByRoomWithCursor(String username, Long roomId, Long oldestMessageId) {
        // 1. 회원 및 채팅방 존재 유무 확인 (생략)
        Member currentUser = findMemberByUsername(username);

        if (!roomRepository.existsById(roomId)) {
            throw new DataNotFoundException("채팅방을 찾을 수 없습니다.");
        }

        Pageable pageable = PageRequest.of(0, MESSAGE_SLICE_SIZE);

        // 2. Repository 호출: 하나의 Repository 메서드가 두 시나리오를 모두 처리
        Slice<Message> messageSlice = messageRepository.findMessagesByRoomAndMemberWithBlockingFilter(
                currentUser.getId(),
                roomId,
                oldestMessageId,
                pageable
        );

        // 3. DTO 변환 및 Slice 반환
        return messageSlice.map(message -> new MessageDto.Response(message, profileUrlPrefix, chatUrlPrefix));
    }


    /**
     * 3. 오래된 CHAT 메시지 자동 삭제 로직
     * - CHAT 메시지 수가 임계값(MESSAGE_COUNT_THRESHOLD)을 초과하면 가장 오래된 메시지를 삭제합니다.
     * - 귓속말(WHISPER) 메시지는 삭제 대상에서 제외합니다.
     * @param roomId 채팅방 ID
     */
    @Transactional
    public void cleanupOldMessages(Long roomId) {
        // 1. 현재 CHAT 메시지 개수 조회
        long totalChatMessages = messageRepository.countByRoomIdAndType(roomId, MessageType.CHAT);

        // 2. 임계값(Threshold) 초과 여부 확인
        if (totalChatMessages > MESSAGE_COUNT_THRESHOLD) {
            // 삭제할 메시지 개수 계산
            int countToDelete = (int) (totalChatMessages - MESSAGE_COUNT_THRESHOLD);

            // 3. 가장 오래된 N개의 CHAT 메시지 ID 조회 (Native Query 사용)
            List<Long> oldestIds = messageRepository.findOldestChatMessageIdsByRoomIdLimit(
                    roomId,
                    countToDelete
            );

            // 4. 조회된 ID를 기반으로 일괄 삭제
            if (!oldestIds.isEmpty()) {
                messageRepository.deleteByIdIn(oldestIds);
            }
        }
    }


    //3-1. 메시지 삭제(단일)
    @Transactional
    public void deleteMessage(String username, Long messageId) {
        //1. 메시지 조회
        Message message = findMessageById(messageId);

        //2. 권한 확인 - 요청한 회원과 메시지 작성자가 일치하는지 확인
        if (!message.getMember().getUsername().equals(username)) {
            throw new SecurityException("메시지를 삭제할 권한이 없습니다. (작성자 불일치)");
        }

        //3. 메시지 삭제
        messageRepository.delete(message);
    }


    //3-2. 메시지 삭제(특정 채팅방)
//    @Transactional
//    public void deleteAllMessagesInRoom(String username, Long roomId) {
//        // 1. 방 조회 및 권한 검증
//        Room room = findRoomById(roomId);
//
//        // 2. 해당 방의 모든 메시지 삭제
//        messageRepository.deleteByRoomId(roomId);
//    }
//
//    //3-3. 메시지 삭제(특정 회원 작성)
//    @Transactional
//    public void deleteAllMessagesByMember(String username) {
//        // 1. 회원 조회 (존재하지 않아도 메시지 삭제는 진행 가능하지만, 검증 차원에서 조회)
//        Member member = findMemberByUsername(username);
//
//        // 2. 해당 회원이 작성한 모든 메시지 삭제
//        messageRepository.deleteByMemberId(member.getId());
//    }

    /**
     * 4. 이미지 메시지 업로드
     */
    public String uploadChatImage(MultipartFile file) {
        //1. FileService 를 호출하여 물리적 파일 저장 및 UUID 파일명 수령
        String savedFileName = fileService.storeChatImage(file);

        //2. 외부에서 접근 가능한 '상대 경로' 문자열 생성
        return savedFileName;
    }





    /**
     * Private Helper Method
     * 1. 'username' 으로 Member 찾기
     */
    private Member findMemberByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 회원: " + username));
    }

    /**
     * 2. 'roomId' 으로 Room 찾기
     */
    private Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 채팅방 : " + roomId));
    }

    /**
     * 3. 'roomId' 으로 Room 찾기
     */
    private Message findMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 메시지 : " + messageId));
    }

}
