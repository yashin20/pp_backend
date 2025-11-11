package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.pp_backend.dto.FriendShipDto;
import project.pp_backend.entity.FriendShip;
import project.pp_backend.entity.Member;
import project.pp_backend.exception.DataAlreadyExistsException;
import project.pp_backend.repository.FriendShipRepository;
import project.pp_backend.repository.MemberRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendShipService {
    final private FriendShipRepository friendShipRepository;
    final private MemberRepository memberRepository;

    /**
     * friendShip Owner 기준 정보 조회
     */
    public List<FriendShipDto.Response> getFriendShipsByOwner(String ownerUsername) {
        return friendShipRepository.findByOwnerUsername(ownerUsername)
                .stream()
                .map(FriendShipDto.Response::new)
                .collect(Collectors.toList());
    }

    /**
     * friendShip 단일 정보 조회
     * @return : ResponseDto
     */
    public FriendShipDto.Response getFriendShip(String ownerUsername, String friendUsername) {
        return new FriendShipDto.Response(
                friendShipRepository.findByOwnerUsernameAndFriendUsername(ownerUsername, friendUsername)
                        .orElseThrow(() -> new IllegalArgumentException("FriendShip not found"))
        );
    }


    /**
     * A -> B 친구 추가
     */
    @Transactional
    public FriendShipDto.Response createFriendShip(FriendShipDto.CreateRequest request) {
        //1. owner 객체 찾기
        Member owner = memberRepository.findByUsername(request.getOwnerUsername())
                .orElseThrow(() -> new DataAlreadyExistsException("Owner not found"));
        //2. friend 객체 찾기
        Member friend = memberRepository.findByUsername(request.getFriendUsername())
                .orElseThrow(() -> new DataAlreadyExistsException("Friend not found"));
        //※ 중복 체크
        if (friendShipRepository.findByOwnerUsernameAndFriendUsername(
                owner.getUsername(), friend.getUsername()
        ).isPresent()){
            throw new DataAlreadyExistsException(
                    String.format("%s - %s: Friendship already exists", owner.getUsername(), friend.getUsername())
            );
        }
        //3. friendShip 객체 생성 및 저장
        FriendShip friendShip = request.toEntity(owner, friend);
        FriendShip savedEntity = friendShipRepository.save(friendShip);
        //4. return
        return new FriendShipDto.Response(savedEntity);
    }


    /**
     * A -> B 친구 삭제
     */
    @Transactional
    public void deleteFriendShip(String ownerUsername, String friendUsername) {
        FriendShip friendShip = friendShipRepository.findByOwnerUsernameAndFriendUsername(ownerUsername, friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("FriendShip not found"));
        friendShipRepository.delete(friendShip);
    }

    //친구 이름으로 조회
    public List<FriendShipDto.Response> searchFriendShipForOwner(String ownerUsername, String friendNicknameKeyword) {
        return friendShipRepository.findByOwnerUsernameAndFriendNicknameContaining(ownerUsername, friendNicknameKeyword)
                .stream()
                .map(FriendShipDto.Response::new)
                .toList();
    }
}
