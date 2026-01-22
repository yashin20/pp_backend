package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.pp_backend.entity.FriendShip;
import project.pp_backend.entity.FriendShipStatus;
import project.pp_backend.entity.Member;

import java.util.List;
import java.util.Optional;

public interface FriendShipRepository extends JpaRepository<FriendShip, Long> {

    //owner, friend username , status 이용 friendship 를 1개의 쿼리로 검색
    @Query("SELECT fs FROM FriendShip fs JOIN fs.owner o JOIN fs.friend f " +
            "WHERE o.username = :ownerUsername AND f.username = :friendUsername AND fs.status = :status")
    Optional<FriendShip> findByOwnerUsernameAndFriendUsernameAndStatusWithJoin(
            @Param("ownerUsername") String ownerUsername,
            @Param("friendUsername") String friendUsername,
            @Param("status") FriendShipStatus status
    );

    Optional<FriendShip> findByOwnerAndFriendAndStatus(
            Member owner,
            Member friend,
            FriendShipStatus status
    );


    List<FriendShip> findByFriendAndStatus(Member friend, FriendShipStatus status);
    List<FriendShip> findByOwnerAndStatus(Member owner, FriendShipStatus status);

    //A 와 B 사이에 모든 관계를 찾기 위함
    @Query("SELECT fs FROM FriendShip fs WHERE " +
            "(fs.owner = :member1 AND fs.friend = :member2) OR " +
            "(fs.owner = :member2 AND fs.friend = :member1)")
    List<FriendShip> findAllRelationBetween(
            @Param("member1") Member member1,
            @Param("member2") Member member2
    );

    //A 가 속한 모든 관계를 찾기 위함
    @Query("SELECT fs FROM FriendShip fs " +
            "WHERE fs.owner.id = :memberId OR fs.friend.id = :memberId")
    List<FriendShip> findAllByMemberId(@Param("id") Long memberId);

    Optional<FriendShip> findByOwnerAndFriend(Member owner, Member friend);

    //Owner 모든 친구를 friendNicknameKeyword(친구 닉네임) 으로 검색 가능
    List<FriendShip> findByOwnerUsernameAndFriendNicknameContaining(
            String ownerUsername,
            String friendNicknameKeyword
    );
}
