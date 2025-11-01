package project.pp_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class FriendShip extends BaseEntity {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_member_id")
    private Member owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_member_id")
    private Member friend;


    public FriendShip(Member owner, Member friend) {
        this.owner = owner;
        this.friend = friend;
    }
}
