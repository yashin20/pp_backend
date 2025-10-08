package project.pp_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class RoomMember extends BaseEntity {
    @Id @GeneratedValue
    private Long id;

    //RoomMember - Member 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    //RoomMember - Room 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;
}
