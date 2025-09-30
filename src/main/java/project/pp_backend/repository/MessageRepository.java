package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.pp_backend.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
