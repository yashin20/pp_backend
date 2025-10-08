package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.pp_backend.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    void deleteByRoomId(Long roomId); //특정 Room 소속 모든 메시지 삭제
    void deleteByMemberId(Long memberId); //특정 Member 작성 모든 메시지 삭제

    /**
     * 특정 채팅방(Room)의 메시지들을 최신순(내림차순)으로 조회
     * @param roomId : 조회할 방 ID
     * @return : 해당 방의 메시지 리스트
     */
    List<Message> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}
