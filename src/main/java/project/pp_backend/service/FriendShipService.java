package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.pp_backend.dto.FriendShipDto;
import project.pp_backend.entity.FriendShip;
import project.pp_backend.entity.Member;
import project.pp_backend.repository.FriendShipRepository;
import project.pp_backend.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendShipService {
    final private FriendShipRepository friendShipRepository;
    final private MemberRepository memberRepository;

    /**
     * friendShip 정보 조회
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
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
        //2. friend 객체 찾기
        Member friend = memberRepository.findByUsername(request.getFriendUsername())
                .orElseThrow(() -> new IllegalArgumentException("Friend not found"));

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
}
