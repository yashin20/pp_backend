package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.pp_backend.entity.RoomMember;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    /**
     * 특정 회원이 참가 중인 모든 Room 조회를 위함
     */
    List<RoomMember> findByMemberUsername(String username);

    /**
     * Room 삭제 시, 해당 방의 모든 참여 기록 일괄 삭제
     */
    void deleteByRoomId(Long roomId);

    /**
     * 특정 Room 에서 특정 회원의 참여 기록을 삭제 (채팅방 나가기 기능)
     */
    void deleteByRoomIdAndMemberId(Long roomId, Long memberId);

    /**
     * 특정 방에 특정 회원이 이미 참가 중인지 확인
     */
    Optional<RoomMember> findByRoomIdAndMemberId(Long roomId, Long memberId);

    //특정 채팅방에 소속된 회원의 수
    long countByRoomId(Long roomId);

    //특정 회원에 참가 중인 채팅방의 수
    long countByMemberId(Long memberId);

    List<RoomMember> findByRoomIdAndMemberIdIn(Long roomId, List<Long> memberIds);
}
