package project.pp_backend.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.pp_backend.entity.FriendShipStatus;
import project.pp_backend.entity.Message;
import project.pp_backend.entity.FriendShip;
import project.pp_backend.entity.MessageType;

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

    //특정 채팅방 메시지 개수 확인 (CHAT 만)
    long countByRoomIdAndType(Long roomId, MessageType type);
    //message ID 기반으로 일괄 삭제
    void deleteByIdIn(List<Long> ids);


    /**
     * user 가 채팅방에서 볼 수 있는 메시지 조회
     */
    @Query("SELECT m FROM Message m " +
            "WHERE m.room.id = :roomId " + // 1. 특정 방의 메시지
            "AND (m.type = 'CHAT' " + // 2. 일반 메시지(CHAT)는 무조건 포함
            "OR (m.type = 'WHISPER' AND (m.member.id = :memberId OR m.recipient.id = :memberId))) " + // 3. 귓속말(WHISPER)은 발신자 또는 수신자일 때만 포함
            "ORDER BY m.createdAt DESC")
    Slice<Message> findVisibleMessagesByRoomAndMember(
            @Param("roomId") Long roomId,
            @Param("memberId") Long memberId,
            Pageable pageable);

    /**
     * 3. 가장 오래된 CHAT 메시지 ID 조회 (메시지 개수 제한 시 삭제용)
     * - Native Query를 사용하여 성능을 최적화합니다.
     * @param roomId 채팅방 ID
     * @param limit 조회할 메시지 ID 개수 (삭제할 개수)
     * @return 가장 오래된 CHAT 메시지 ID 목록
     */
    @Query(value = "SELECT m.id FROM message m " +
            "WHERE m.room_id = :roomId " +
            "AND m.type = 'CHAT' " +
            "ORDER BY m.created_at ASC LIMIT :limit", nativeQuery = true)
    List<Long> findOldestChatMessageIdsByRoomIdLimit(
            @Param("roomId") Long roomId,
            @Param("limit") int limit);


    /**
     * 특정 채팅방 + 특정 회원이 볼 수 있는 메시지 (SLICE)
     * 내가 차단한 회원의 메시지 제외
     */
//    @Query("SELECT m FROM Message m " +
//            "WHERE m.room.id = :roomId " +
//            "AND (:oldestMessageId IS NULL OR m.id < :oldestMessageId) " +
//            "AND m.member.id NOT IN (" +
//            "    SELECT fs.friend.id FROM FriendShip fs " +
//                "WHERE fs.owner.id = :userId " +
//                "AND fs.status = project.pp_backend.entity.FriendShipStatus.BLOCKED" +
//            ") " +
//            "ORDER BY m.id DESC")
//    Slice<Message> findNextVisibleMessagesByRoomAndMember(
//            @Param("roomId") Long roomId,
//            @Param("userId") Long userId,
//            @Param("oldestMessageId") Long oldestMessageId,
//            Pageable pageable
//    );


    /**
     * 특정 채팅방 + 특정 회원이 볼 수 있는 메시지 (SLICE)
     * (LEFT JOIN 을 이용하여 효율성 증대)
     * 내가 차단한 회원의 메시지 제외
     */
    @Query("SELECT m FROM Message m " +
            "LEFT JOIN FriendShip fs " +
            "ON fs.owner.id = :userId " +
            "AND fs.friend.id = m.member.id " +
            "AND fs.status = project.pp_backend.entity.FriendShipStatus.BLOCKED " +
            "WHERE m.room.id = :roomId " +
            "AND (:oldestMessageId IS NULL OR m.id < :oldestMessageId) " +
            "AND fs.id IS NULL " + // 차단 관계가 없는(JOIN되지 않은) 메시지만 선택
            "ORDER BY m.id DESC")
    Slice<Message> findMessagesByRoomAndMemberWithBlockingFilter(
            @Param("userId") Long userId,
            @Param("roomId") Long roomId,
            @Param("oldestMessageId") Long oldestMessageId,
            Pageable pageable
    );

}
