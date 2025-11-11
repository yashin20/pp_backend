package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.pp_backend.entity.FriendShip;

import java.util.List;
import java.util.Optional;

public interface FriendShipRepository extends JpaRepository<FriendShip, Long> {
    Optional<FriendShip> findByOwnerUsernameAndFriendUsername(
            String ownerUsername,
            String friendUsername
    );

    //Owner 의 모든 친구 리스트
    List<FriendShip> findByOwnerUsername(String ownerUsername);


    //Owner 모든 친구를 friendNicknameKeyword(친구 닉네임) 으로 검색 가능
    List<FriendShip> findByOwnerUsernameAndFriendNicknameContaining(
            String ownerUsername,
            String friendNicknameKeyword
    );
}
