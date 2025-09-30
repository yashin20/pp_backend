package project.pp_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.pp_backend.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    Optional<Member> findByNickname(String nickname);
    Optional<Member> findByEmail(String email);
}
