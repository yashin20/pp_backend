package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.pp_backend.dto.RoomDto;
import project.pp_backend.entity.Room;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {



    //Member.username AND Room.name
    @Query("""
        SELECT DISTINCT r
        FROM Room r
        JOIN r.members rm
        JOIN rm.member m
        WHERE m.username = :username
            AND UPPER(r.name) LIKE UPPER(CONCAT('%', :roomKeyword, '%'))
    """)
    List<Room> findParticipatingRoomsByMemberUsernameAndRoomNameContaining(
            @Param("username") String username,
            @Param("roomKeyword") String roomKeyword
    );

    //나의 채팅방 목록 + 그 방의 가장 최근 메시지 하나
//    @Query("SELECT new project.pp_backend.dto.RoomDto$Response(" +
//            "r.id, r.name, m.content, m.createdAt, r.createdAt) " +
//            "FROM RoomMember rm " +
//            "JOIN rm.room r " +
//            "LEFT JOIN Message m ON m.room = r " +
//            "WHERE rm.member.id = :memberId " +
//            "AND (m.id IS NULL OR m.id = (SELECT MAX(m2.id) FROM Message m2 WHERE m2.room = r)) " +
//            "ORDER BY m.createdAt DESC")
//    List<RoomDto.Response> findMyRoomsWithLastMessage(@Param("memberId") Long memberId);

    @Query("SELECT new project.pp_backend.dto.RoomDto$Response(" +
            "r.id, " +
            "r.name, " +
            "(SELECT COUNT(rm2) FROM RoomMember rm2 WHERE rm2.room = r), " + // 인원수 추가
            "m.content, " +
            "m.createdAt, " +
            "r.createdAt) " +
            "FROM RoomMember rm " +
            "JOIN rm.room r " +
            "LEFT JOIN Message m ON m.room = r " +
            "WHERE rm.member.id = :memberId " +
            "AND (m.id IS NULL OR m.id = (SELECT MAX(m2.id) FROM Message m2 WHERE m2.room = r)) " +
            "ORDER BY m.createdAt DESC")
    List<RoomDto.Response> findMyRoomsWithLastMessage(@Param("memberId") Long memberId);

}
