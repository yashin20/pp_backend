package project.pp_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {
    @Id @GeneratedValue
    private Long id;
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room; //소속 채팅방

    //귓속말 수신자 (Recipient)
    //일반 메시지일 경우 NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_member_id", nullable = true)
    private Member recipient;

    @Builder
    public Message(String content, MessageType type, Member member, Room room) {
        this.content = content;
        this.type = type;
        this.member = member;
        this.room = room;
    }

    /**
     * 이 메시지가 귓속말인지 확인
     * @return 귓속말 수신자(recipient) 필드가 null이 아니면 true
     */
    public boolean isWhisper() {
        return this.recipient != null;
    }
}
