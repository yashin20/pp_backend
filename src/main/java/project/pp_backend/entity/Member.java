package project.pp_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseEntity {
    @Id @GeneratedValue
    private Long id;

    private String username;
    private String password;

    private String nickname;
    private String email;

    @Enumerated(EnumType.STRING)
    private MemberRole memberRole;


    //회원 삭제시 메시지도 모두 삭제
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();


    public Member(String username, String password, String nickname, String email, MemberRole memberRole) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.memberRole = memberRole;
    }

    /** Encoding Password 가 입력됨 **/
    public void updatePassword(String encodedPassword) {this.password = encodedPassword;}
    public void updateNickname(String nickname) {this.nickname = nickname;}
    public void updateEmail(String email) {this.email = email;}
}
