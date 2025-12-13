package project.pp_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "friend_ship", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"owner_member_id", "friend_member_id"})
})
@Getter
@NoArgsConstructor
public class FriendShip extends BaseEntity {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_member_id", nullable = false)
    private Member owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_member_id", nullable = false)
    private Member friend;

    @Enumerated(EnumType.STRING)
    private FriendShipStatus status;


    public FriendShip(Member owner, Member friend, FriendShipStatus status) {
        this.owner = owner;
        this.friend = friend;
        this.status = status;
    }

    //관계 수정 메서드
    public void updateStatus(FriendShipStatus status) {
        this.status = status;
    }
}
