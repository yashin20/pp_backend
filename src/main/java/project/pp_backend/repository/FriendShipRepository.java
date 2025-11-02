package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.pp_backend.entity.FriendShip;

import java.util.Optional;

public interface FriendShipRepository extends JpaRepository<FriendShip, Long> {
    Optional<FriendShip> findByOwnerUsernameAndFriendUsername(
            String ownerUsername,
            String friendUsername
    );
}
