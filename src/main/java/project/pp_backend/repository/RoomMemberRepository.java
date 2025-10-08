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

    void deleteByRoomIdAndMemberId(Long roomId, Long memberId);

    /**
     * 특정 방에 특정 회원이 이미 참가 중인지 확인
     */
    Optional<RoomMember> findByRoomIdAndMemberId(Long roomId, Long memberId);
}
