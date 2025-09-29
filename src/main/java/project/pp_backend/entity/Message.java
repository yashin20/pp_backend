package project.pp_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Message extends BaseEntity {
    @Id @GeneratedValue
    private Long id;
    private String message;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room; //소속 채팅방
}
