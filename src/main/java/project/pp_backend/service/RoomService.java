package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.pp_backend.dto.RoomDto;
import project.pp_backend.entity.Member;
import project.pp_backend.entity.Room;
import project.pp_backend.entity.RoomMember;
import project.pp_backend.exception.DataAlreadyExistsException;
import project.pp_backend.exception.DataNotFoundException;
import project.pp_backend.repository.MemberRepository;
import project.pp_backend.repository.MessageRepository;
import project.pp_backend.repository.RoomMemberRepository;
import project.pp_backend.repository.RoomRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    //한 회원이 생성/참가할 수 있는 최대 채팅방 개수 (임의로 50개 설정)
    private static final int MAX_ROOM_CREATION_LIMIT = 50;

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final RoomMemberRepository roomMemberRepository;

    /**
     * 1. 채팅방 생성
     * @param username : 방을 생성하는 회원의 username
     * @param request : 채팅방 생성 요청 DTO
     * @return : 생성된 채팅방 DTO
     */
    @Transactional
    public RoomDto.Response createRoom(String username, RoomDto.CreateRequest request) {
        // 1. 회원 존재 확인
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원을 찾을 수 없습니다. 채팅방을 생성할 수 없습니다."));

        //2. 회원당 채팅방 생성 개수 제한
        long existingRoomCount = roomMemberRepository.countByMemberId(member.getId());
        if (existingRoomCount >= MAX_ROOM_CREATION_LIMIT) {
            throw new SecurityException("최대 채팅방 생성/참가 개수(" + MAX_ROOM_CREATION_LIMIT + "개)");
        }

        //3. 엔티티 변환 및 저장
        Room room = request.toEntity();
        roomRepository.save(room);

        //4. 방 생성자(Owner)를 RoomMember 에 자동 추가 (채팅방 참가 처리)
        RoomMember roomMember = new RoomMember(room, member);
        roomMemberRepository.save(roomMember);

        return new RoomDto.Response(room);
    }

    //2-1. 채팅방 조회 (단일 조회)
    public RoomDto.Response getRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DataNotFoundException("채팅방을 찾을 수 없습니다."));
        return new RoomDto.Response(room);
    }

    //2-2. 채팅방 목록 조회 (회원이 참가중인 채팅방 목록)
    public List<RoomDto.Response> getRoomsByUsername(String username) {
        return roomMemberRepository.findByMemberUsername(username).stream()
                .map(roomMember -> new RoomDto.Response(roomMember.getRoom()))
                .collect(Collectors.toList());
    }

    //2-3. 채팅방 목록 조회 (전체 조회 - 관리자용)
    public List<RoomDto.Response> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(RoomDto.Response::new)
                .collect(Collectors.toList());
    }


    /**
     * 현재, '채팅방 수정'은 불가
     * '채팅방 삭제' 는 채팅방에 참가중인 회원이 있는 경우 삭제 기능은 없음
     * 채팅방 퇴장을 통해 모든 회원이 채팅방에서 퇴장을 하면 그때 채팅방이 삭제됨.
     */


    //3. 채팅방 수정
    @Transactional
    public RoomDto.Response updateRoom(String username, Long roomId, RoomDto.UpdateRequest request) {
        //1. 채팅방 및 회원 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DataNotFoundException("채팅방을 찾을 수 없습니다."));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원을 찾을 수 없습니다."));


        //2. 권한 확인 (채팅방 수정 권한이 있는가?)
        //1) 채팅방 방장인가?
        //2) 운영자 권한인가?


        //3. 채팅방 이름 수정
        room.updateName(request.getName());
        //4. 수정된 데이터 반환
        return new RoomDto.Response(room);
    }

    //4. 채팅방 삭제
    @Transactional
    public Long deleteRoom(String username, Long roomId) {
        //1. 채팅방 및 회원 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DataNotFoundException("채팅방을 찾을 수 없습니다."));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원을 찾을 수 없습니다."));


        //2. 권한 확인 (채팅방 삭제 권한이 있는가?)
        //1) 채팅방 방장인가?
        //2) 운영자 권한인가?


        // 2. 해당 방의 모든 메시지 삭제
        messageRepository.deleteByRoomId(roomId);

        // 3. 해당 방의 모든 RoomMember 기록 삭제
        roomMemberRepository.deleteByRoomId(roomId);

        // 4. 채팅방 삭제
        roomRepository.delete(room);

        return roomId;
    }


    //5. 채팅방에 회원 참가 기능 (Join)
    @Transactional
    public RoomDto.Response joinRoom(String username, Long roomId) {
        //1. 채팅방 및 회원 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DataNotFoundException("채팅방을 찾을 수 없습니다."));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원을 찾을 수 없습니다."));

        //2. 이미 참가 중인지 확인
        Optional<RoomMember> existingMember = roomMemberRepository.findByRoomIdAndMemberId(roomId, member.getId());
        if (existingMember.isPresent()) {
            throw new DataAlreadyExistsException("이미 채팅방에 참가 중입니다.");
        }

        //3. RoomMember 엔티티 생성 및 저장 (참가 처리)
        RoomMember roomMember = new RoomMember(room, member);
        roomMemberRepository.save(roomMember);
        return new RoomDto.Response(room);
    }

    //6. 채팅방에서 회원 퇴장 기능 (Leave)
    @Transactional
    public Long leaveRoom(String username, Long roomId) {
        //1. 채팅방 및 회원 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DataNotFoundException("채팅방을 찾을 수 없습니다."));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("회원을 찾을 수 없습니다."));

        //2. RoomMember 기록 삭제 (퇴장 처리)
        roomMemberRepository.deleteByRoomIdAndMemberId(roomId, member.getId());

        //3. 방에 남아 있는 멤버가 0명인 경우, 방을 자동으로 삭제
        long remainingMembers = roomMemberRepository.countByRoomId(roomId);

        if (remainingMembers == 0) {
            // 3-1. 해당 방의 모든 메시지 삭제
            // (RoomMember 기록은 이미 삭제되었으므로, 메시지 삭제 후 방 자체를 삭제)
            messageRepository.deleteByRoomId(roomId);

            // 3-2. 채팅방 삭제
            roomRepository.delete(room);
        }


        return roomId;
    }
}
