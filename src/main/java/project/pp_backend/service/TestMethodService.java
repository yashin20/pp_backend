package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.pp_backend.dto.MessageDto;
import project.pp_backend.entity.*;
import project.pp_backend.exception.DataNotFoundException;
import project.pp_backend.repository.FriendShipRepository;
import project.pp_backend.repository.MemberRepository;
import project.pp_backend.repository.MessageRepository;
import project.pp_backend.repository.RoomRepository;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestMethodService {

    final private FriendShipRepository friendShipRepository;
    final private MemberRepository memberRepository;
    final private MessageRepository messageRepository;
    final private RoomRepository roomRepository;

    /**
     * 친구 관계 생성 메서드
     */
    @Transactional
    public void testCreateFriendShip(String ownerUsername, String friendUsername, FriendShipStatus status) {
        Member owner = findMemberByUsername(ownerUsername);
        Member friend = findMemberByUsername(friendUsername);

        //1. 정방향 관계 생성
        FriendShip forward = new FriendShip(owner, friend, status);
        friendShipRepository.save(forward);
        //2. 역방향 관계 생성
        FriendShip reverse = new FriendShip(friend, owner, status);
        friendShipRepository.save(reverse);
    }

    //친구 요청 보내기 (A -> B)
    @Transactional
    public void testCreateAtoB(String A, String B, FriendShipStatus status) {
        Member owner = findMemberByUsername(A);
        Member friend = findMemberByUsername(B);

        //1. 정방향 관계 생성
        FriendShip forward = new FriendShip(owner, friend, status);
        friendShipRepository.save(forward);
    }

    //1. 메시지 생성
    @Transactional
    public void testCreateMessage(String username, Long roomId, String content, String recipientUsername) {
        //1. 회원 및 채팅방 존재 유무 확인
        Member sender = findMemberByUsername(username);
        Room room = findRoomById(roomId);

        Member recipient = null;

        //2. 귓속말 수신자 처리 로직
        if (recipientUsername != null && !recipientUsername.isEmpty()) {
            recipient = findMemberByUsername(recipientUsername);

            // 자기 자신에게 귓속말을 보낼 수 없도록 방지
            if (sender.getId().equals(recipient.getId())) {
                throw new IllegalArgumentException("자기 자신에게 귓속말을 보낼 수 없습니다.");
            }
        }

        //2. 메시지 엔티티 생성
        MessageType determinedType = (recipient != null) ? MessageType.WHISPER : MessageType.CHAT;

        Message message = Message.builder()
                .content(content)
                .type(determinedType)
                .member(sender) // DB에서 조회한 엔티티
                .room(room)     // DB에서 조회한 엔티티
                .recipient(recipient) // DB에서 조회한 엔티티 또는 null
                .build();

        //3. DB 저장
        messageRepository.save(message);
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
     * 2. 'owner', 'friend' 간의 FriendShip 찾기
     */
    private FriendShip findFriendShipByOwnerFriend(Member owner, Member friend) {
        return friendShipRepository.findByOwnerAndFriend(owner, friend)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 관계 : " + owner.getUsername() + " -> " + friend.getUsername()));
    }

    /**
     * 3. 'roomId' 으로 Room 찾기
     */
    private Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 채팅방 : " + roomId));
    }
}
