package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
}
