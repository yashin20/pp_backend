package project.pp_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Member extends BaseEntity {
    @Id @GeneratedValue
    private Long id;

    private String username;
    private String password;

    private String nickname;
    private String email;

    @Enumerated(EnumType.STRING)
    private MemberRole memberRole;
}
