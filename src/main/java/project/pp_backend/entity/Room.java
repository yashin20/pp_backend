package project.pp_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class Room extends BaseEntity {

    @Id @GeneratedValue
    private Long id;

    private String name;
}
