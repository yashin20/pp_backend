package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.pp_backend.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
