package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.pp_backend.dto.MessageDto;
import project.pp_backend.entity.Member;
import project.pp_backend.entity.Message;
import project.pp_backend.entity.Room;
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

    //message 개수 임계값 (100개)
    private static final long MESSAGE_COUNT_THRESHOLD = 100;

    //1. 메시지 생성
    @Transactional
    public MessageDto.Response createMessage(String username, Long roomId, MessageDto.CreateRequest request) {
        //1. 회원 및 채팅방 존재 유무 확인
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("Username not found"));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DataNotFoundException("Room not found"));

        //2. 메시지 엔티티 생성
        Message message = request.toEntity(member, room);

        //3. DB 저장
        messageRepository.save(message);

        return new MessageDto.Response(message);
    }


    /**
     * 2. 메시지 조회 (특정 채팅방의 메시지 목록)
     * - 채팅방 진입 시 대화 기록을 불러오는데 사용
     * @param roomId : 메시지들은 조회할 채팅방 ID
     * @return : 메시지 응답 DTO 리스트 (최신순)
     */
    public List<MessageDto.Response> getMessagesByRoom(Long roomId) {
        //1. 채팅방 존재 유무 확인
        if (!roomRepository.existsById(roomId)) {
            throw new DataNotFoundException("채팅방을 찾을 수 없습니다.");
        }

        //2. 메시지 조회 (최신순 내림차순 정렬)
        List<Message> messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId);

        //3. DTO 변환
        return messages.stream()
                .map(MessageDto.Response::new)
                .collect(Collectors.toList());
    }


    //3-1. 메시지 삭제(단일)
    @Transactional
    public void deleteMessage(String username, Long messageId) {
        //1. 메시지 조회
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new DataNotFoundException("Message not found"));

        //2. 권한 확인 - 요청한 회원과 메시지 작성자가 일치하는지 확인
        if (!message.getMember().getUsername().equals(username)) {
            throw new SecurityException("메시지를 삭제할 권한이 없습니다. (작성자 불일치)");
        }

        //3. 메시지 삭제
        messageRepository.delete(message);
    }


    //3-2. 메시지 삭제(특정 채팅방)
    @Transactional
    public void deleteAllMessagesInRoom(String username, Long roomId) {
        // 1. 방 조회 및 권한 검증
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DataNotFoundException("채팅방(Room)을 찾을 수 없음."));

        // 2. 해당 방의 모든 메시지 삭제
        messageRepository.deleteByRoomId(roomId);
    }

    //3-3. 메시지 삭제(특정 회원 작성)
    @Transactional
    public void deleteAllMessagesByMember(String username) {
        // 1. 회원 조회 (존재하지 않아도 메시지 삭제는 진행 가능하지만, 검증 차원에서 조회)
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원(Member)을 찾을 수 없음."));

        // 2. 해당 회원이 작성한 모든 메시지 삭제
        messageRepository.deleteByMemberId(member.getId());
    }

}
