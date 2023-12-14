package study.querydsl.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

  //메서드 이름으로 자동으로 JPQL 만듬.
  //select m from Member m where m.username = ?
  List<Member> findByUsername(String username);
}
