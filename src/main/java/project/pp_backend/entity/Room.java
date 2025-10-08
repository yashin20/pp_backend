package project.pp_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

    @Id @GeneratedValue
    private Long id;
    private String name;

    @OneToMany(mappedBy = "room")
    private List<RoomMember> members = new ArrayList<>();

    //Room 삭제 시 Message 자동 삭제 설정
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    public Room(String name) {
        this.name = name;
    }

    //채팅방 이름 수정 메서드
    public void updateName(String newName) {this.name = newName;}
}
