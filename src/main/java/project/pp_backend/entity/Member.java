package project.pp_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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


    public Member(String username, String password, String nickname, String email, MemberRole memberRole) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.memberRole = memberRole;
    }
}
