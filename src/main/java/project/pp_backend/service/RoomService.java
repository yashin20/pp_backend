package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.pp_backend.dto.RoomDto;
import project.pp_backend.entity.Room;
import project.pp_backend.exception.DataNotFoundException;
import project.pp_backend.repository.MemberRepository;
import project.pp_backend.repository.MessageRepository;
import project.pp_backend.repository.RoomMemberRepository;
import project.pp_backend.repository.RoomRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

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
        //1. 회원 존재 확인
        if (!memberRepository.findByUsername(username).isPresent()) {
            throw new DataNotFoundException("회원을 찾을 수 없습니다. 채팅방을 생성할 수 없습니다.");
        }

        //2. 중복 검사 (같은 회원 구성으로 이루어진 채팅방이 존재하는가?) + 회원당 채팅방 생성 개수 제한


        //3. 엔티티 변환 및 저장
        Room room = request.toEntity();
        roomRepository.save(room);

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


    //3. 채팅방 수정

    //4. 채팅방 삭제

    //??? 채팅방에 회원 참가 기능 / 회원 퇴장 기능
}
