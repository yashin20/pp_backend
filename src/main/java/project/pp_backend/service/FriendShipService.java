package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.pp_backend.dto.FriendShipDto;
import project.pp_backend.entity.FriendShip;
import project.pp_backend.entity.FriendShipStatus;
import project.pp_backend.entity.Member;
import project.pp_backend.exception.BasicErrorMessage;
import project.pp_backend.exception.DataNotFoundException;
import project.pp_backend.repository.FriendShipRepository;
import project.pp_backend.repository.MemberRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendShipService {
    final private FriendShipRepository friendShipRepository;
    final private MemberRepository memberRepository;

    @Value("${file.url-prefix.profile}")
    private String profileUrlPrefix;


    /** ============== 1. 조회 ============== **/

    /**
     * A 와 B 간의 모든 관계 조회
     */
    public List<FriendShipDto.Response> getMemberFriendShip(String username) {
        //1. username 으로 Member 를 찾아 ID를 획득
        Long memberId = memberRepository.findByUsername(username)
                .map(Member::getId)
                .orElseThrow(() -> new DataNotFoundException("사용자를 찾을 수 없습니다."));

        //2. 획득한 ID로 관계 조회
        return friendShipRepository.findAllByMemberIdWithMembers(memberId)
                .stream()
                .map(fs -> new FriendShipDto.Response(fs, profileUrlPrefix))
                .collect(Collectors.toList());
    }

    /**
     * 1-1. friendShip 단일 정보 조회 (FriendShip id 기반)
     */
    public FriendShipDto.Response getFriendShipById(Long id) {
        FriendShip friendShip = friendShipRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 관계 입니다. : " + id));
        return new FriendShipDto.Response(friendShip, profileUrlPrefix);
    }

    /**
     * 1-2. owner 의 친구 리스트 조회
     */
    public List<FriendShipDto.Response> getFriendShipsByOwner(String username) {
        Member member = findMemberByUsername(username);

        return friendShipRepository.findByOwnerAndStatus(member, FriendShipStatus.ACCEPTED)
                .stream()
                .map(fs -> new FriendShipDto.Response(fs, profileUrlPrefix))
                .collect(Collectors.toList());
    }

    /**
     * 1-3. owner 가 받은 친구 요청 리스트 조회
     */
    public List<FriendShipDto.Response> getReceivedFriendRequests(String username) {
        Member member = findMemberByUsername(username);

        return friendShipRepository.findByFriendAndStatus(member, FriendShipStatus.PENDING)
                .stream()
                .map(fs -> new FriendShipDto.Response(fs, profileUrlPrefix))
                .collect(Collectors.toList());
    }

    /**
     * 1-4. owner 가 보낸 진구 정보 리스트
     */
    public List<FriendShipDto.Response> getSendFriendRequests(String username) {
        Member member = findMemberByUsername(username);

        return friendShipRepository.findByOwnerAndStatus(member, FriendShipStatus.PENDING)
                .stream()
                .map(fs -> new FriendShipDto.Response(fs, profileUrlPrefix))
                .collect(Collectors.toList());
    }

    /**
     * 1-5. owner 가 차단한 회원 리스트
     */
    public List<FriendShipDto.Response> getBlockedMembers(String username) {
        Member member = findMemberByUsername(username);

        return friendShipRepository.findByOwnerAndStatus(member, FriendShipStatus.BLOCKED)
                .stream()
                .map(fs -> new FriendShipDto.Response(fs, profileUrlPrefix))
                .collect(Collectors.toList());
    }


    /** ============== 2. 관계 생성 로직 ============== **/

    /****
     * 닉네임 기반 친구 신청 로직
     */
    @Transactional
    public FriendShipDto.Response sendFriendShipRequestByNickname(FriendShipDto.SendNicknameRequest sendNicknameRequest) {
        //1. 닉네임으로 상대방 Member 찾기 (없으면 404 에러)
        Member target = memberRepository.findByNickname(sendNicknameRequest.getTargetNickname())
                .orElseThrow(() -> new DataNotFoundException("해당 닉네임을 가진 사용자가 존재하지 않습니다."));

        //2. DTO 생성
        FriendShipDto.Request request = FriendShipDto.Request
                .builder()
                .ownerId(sendNicknameRequest.getOwnerId())
                .friendId(target.getId())
                .build();

        return sendFriendShipRequest(request);
    }

    /**
     * 2-1. 친구 신청 로직 (A -> B)
     * - FriendShip(owner: A, friend: B, status: PENDING) 생성 목표
     * - 이미 관계가 존재하는지 (PENDING, ACCEPTED, BLOCKED 등) 확인
     * **** REQUEST DTO 를 이용하는 것으로 수정!!!!!!!!!!
     */
    @Transactional
    public FriendShipDto.Response sendFriendShipRequest(FriendShipDto.Request request) {
        Long requesterId = request.getOwnerId();
        Long targetId = request.getFriendId();

        //※ 본인에게 신청 방지
        if (requesterId.equals(targetId)) {
            throw new IllegalArgumentException("자신에게 관계를 형성할 수 없습니다.");
        }

        Member requester = findMemberById(requesterId);
        Member target = findMemberById(targetId);

        //1. 관계 확인 A -> B (정방향)
        Optional<FriendShip> forwardOpt = friendShipRepository.findByOwnerAndFriend(requester, target);
        //2. 관계 확인 B -> A (역방향)
        Optional<FriendShip> reverseOpt = friendShipRepository.findByOwnerAndFriend(target, requester);

        // ====================== 정방향 관계 처리 ======================
        if (forwardOpt.isPresent()) {
            FriendShip forward = forwardOpt.get();

            //ACCEPTED: (A, B, ACCEPTED)가 존재하던 이미 친구 상태
            if (forward.getStatus() == FriendShipStatus.ACCEPTED) {
                throw new IllegalStateException("이미 친구 관계(ACCEPTED)가 존재합니다.");
            }

            //PENDING: (A, B, PENDING)가 존재하던 중복 요청
            if (forward.getStatus() == FriendShipStatus.PENDING) {
                throw new IllegalStateException("이미 대상에게 친구 요청을 보낸 상태입니다.");
            }

            //ACCEPTED: (A, B, BLOCKED)가 존재하던 이미 친구 상태
            if (forward.getStatus() == FriendShipStatus.BLOCKED) {
                throw new IllegalStateException("당신이 상대를 차단했습니다. 먼저 차단 해제를 해야 합니다.");
            }
        }

        // ====================== 역방향 관계 처리 ======================
        if (reverseOpt.isPresent()) {
            FriendShip reverse = reverseOpt.get();

            // ACCEPTED: (B, A, ACCEPTED)가 존재하면 이미 친구 상태
            if (reverse.getStatus() == FriendShipStatus.ACCEPTED) {
                throw new IllegalStateException("이미 친구 관계(ACCEPTED)가 존재합니다.");
            }

            // PENDING: (B, A, PENDING)이 존재하면, B가 A에게 요청을 보낸 상태
            if (reverse.getStatus() == FriendShipStatus.PENDING) {
                throw new IllegalStateException("대상으로부터 이미 친구 요청을 받았습니다. 수락 또는 거절을 선택해야 합니다.");
            }

            // BLOCKED: (B, A, BLOCKED)이 존재하면, A가 B에게 차단당한 상태
            if (reverse.getStatus() == FriendShipStatus.BLOCKED) {
                throw new IllegalStateException("당신이 상대에게 차단당했습니다. 요청할 수 없습니다.");
            }
        }

        //3. (A -> B, PENDING) 생성
        FriendShip newRequest = new FriendShip(requester, target, FriendShipStatus.PENDING);
        FriendShip saved = friendShipRepository.save(newRequest);
        return new FriendShipDto.Response(saved, profileUrlPrefix);
    }

    /**
     * 2-2. 친구 요청 수락 로직 (B가 A의 요청을 수락)
     * FriendShip(owner: A, friend: B, status: PENDING) -> FriendShip(owner: A, friend: B, status: ACCEPTED)
     * FriendShip(owner: B, friend: A, status: ACCEPTED) -> 생성
     *
     * FriendShip(owner: A, friend: B, status: ACCEPTED)
     * FriendShip(owner: B, friend: A, status: ACCEPTED)
     * 위 2개가 존재하게 됨
     * ※조회 편의성을 위해 양방향으로 관계 생성
     */
    @Transactional
    public FriendShipDto.Response acceptFriendShipRequest(FriendShipDto.Request request) {
        Long accepterId = request.getOwnerId();
        Long requesterId = request.getFriendId();

        //※ 본인에게 신청 방지
        if (accepterId.equals(requesterId)) {
            throw new IllegalArgumentException("자신에게 관계를 형성할 수 없습니다.");
        }

        Member accepter = findMemberById(accepterId);
        Member requester = findMemberById(requesterId);

        //1. 기존 관계 (requester -> accepter, PENDING) 조회
        FriendShip existing = findFriendShipByOwnerFriend(requester, accepter);

        //※ 상태 유효성 검증
        if (existing.getStatus() != FriendShipStatus.PENDING) {
            throw new BasicErrorMessage("수락할 수 없는 관계 입니다.");
        }

        //2. (requester -> accepter, PENDING) -> (requester -> accepter, ACCEPTED)
        existing.updateStatus(FriendShipStatus.ACCEPTED);

        //※ 기존에 존재하는지 확인(accepter -> requester)
        if (friendShipRepository.findByOwnerAndFriend(accepter, requester).isPresent()) {
            throw new BasicErrorMessage("이미 존재하는 관계 입니다.");
        }

        //3.양방향 관계 구성 (accepter -> requester, ACCEPTED) 생성!
        FriendShip newFriendShip = new FriendShip(accepter, requester, FriendShipStatus.ACCEPTED);
        friendShipRepository.save(newFriendShip); //INSERT

        return new FriendShipDto.Response(newFriendShip, profileUrlPrefix);
    }

    /**
     * 2-3. 친구 차단 로직 (A 가 B를 차단)
     * FriendShip(owner: A, friend: B, status: ACCEPTED) -> FriendShip(owner: A, friend: B, status: BLOCKED)
     * FriendShip(owner: B, friend: A, status: ACCEPTED) -> 삭제!
     *
     * FriendShip(owner: A, friend: B, status: BLOCKED) 만 존재하게 됨.
     */
    @Transactional
    public FriendShipDto.Response blockMember(FriendShipDto.Request request) {
        Long blockerId = request.getOwnerId();
        Long blockedId = request.getFriendId();

        //※ 본인에게 신청 방지
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("자신을 차단할 수 없습니다.");
        }

        Member blocker = findMemberById(blockerId);
        Member blocked = findMemberById(blockedId);

        // ====================== 2. 역방향 관계 (B -> A) 제거 ======================
        Optional<FriendShip> reverseOpt = friendShipRepository.findByOwnerAndFriend(blocked, blocker);

        if (reverseOpt.isPresent()) {
            friendShipRepository.delete(reverseOpt.get());
        }

        // ====================== 3. 정방향 관계 (A -> B) BLOCKED 처리 ======================
        Optional<FriendShip> forwardOpt = friendShipRepository.findByOwnerAndFriend(blocker, blocked);
        FriendShip blockedFriendShip;

        if (forwardOpt.isPresent()) {
            // 이미 A -> B 관계가 존재한다면 (PENDING, ACCEPTED, BLOCKED 등) 상태만 업데이트
            blockedFriendShip = forwardOpt.get();
            if (blockedFriendShip.getStatus() != FriendShipStatus.BLOCKED) {
                // 이미 BLOCKED 상태가 아니라면 업데이트 (Dirty Checking)
                blockedFriendShip.updateStatus(FriendShipStatus.BLOCKED);
            }
        } else {
            // A -> B 관계가 없다면 새로 BLOCKED 관계 생성
            blockedFriendShip = new FriendShip(blocker, blocked, FriendShipStatus.BLOCKED);
            friendShipRepository.save(blockedFriendShip); // INSERT
        }

        // 4. 결과 반환 (A -> B, BLOCKED)
        return new FriendShipDto.Response(blockedFriendShip, profileUrlPrefix);
    }


    /**
     * 2-4. 친구 삭제 로직 (A가 B를 친구 목록에서 삭제)
     * A와 B간에 존재하는 모든 관계 삭제!
     * FriendShip(owner: A, friend: B, status: ACCEPTED) -> 삭제!
     * FriendShip(owner: B, friend: A, status: ACCEPTED) -> 삭제!
     */
    @Transactional
    public void deleteFriendShip(FriendShipDto.Request request) {
        Long ownerId = request.getOwnerId();
        Long targetId = request.getFriendId();

        //※ 본인에게 신청 방지
        if (ownerId.equals(targetId)) {
            throw new IllegalArgumentException("자신에게 관계를 형성할 수 없습니다.");
        }

        Member owner = findMemberById(ownerId);
        Member target = findMemberById(targetId);

        // ====================== 2. 정방향 관계 (A -> B) 제거 ======================
        Optional<FriendShip> forwardOpt = friendShipRepository.findByOwnerAndFriend(owner, target);
        forwardOpt.ifPresent(friendShipRepository::delete);

        // ====================== 3. 역방향 관계 (B -> A) 제거 ======================
        Optional<FriendShip> reverseOpt = friendShipRepository.findByOwnerAndFriend(target, owner);
        reverseOpt.ifPresent(friendShipRepository::delete);
    }

    //친구 이름으로 조회
    public List<FriendShipDto.Response> searchFriendShipForOwner(String ownerUsername, String friendNicknameKeyword) {
        return friendShipRepository.findByOwnerUsernameAndFriendNicknameContaining(ownerUsername, friendNicknameKeyword)
                .stream()
                .map(fs -> new FriendShipDto.Response(fs, profileUrlPrefix))
                .toList();
    }



    /**
     * Private Helper Method
     * 1. 'username'으로 Member를 찾고, 없으면 예외 발생
     */
    private Member findMemberByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 회원: " + username));
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 회원: " + id));
    }

    /**
     * 2. 'owner', 'friend' 간의 FriendShip 찾기
     */
    private FriendShip findFriendShipByOwnerFriend(Member owner, Member friend) {
        return friendShipRepository.findByOwnerAndFriend(owner, friend)
                .orElseThrow(() -> new DataNotFoundException("존재하지 않는 관계 : " + owner.getUsername() + " -> " + friend.getUsername()));
    }

}
