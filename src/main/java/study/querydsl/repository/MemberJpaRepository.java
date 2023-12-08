package study.querydsl.repository;

import static study.querydsl.entity.QMember.member;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

@Repository
public class MemberJpaRepository {
  private final EntityManager em; //순수 jpa에 접근할때는 entityManager 필요
  private final JPAQueryFactory queryFactory;


  public MemberJpaRepository(EntityManager em) { //이 방식 추천
    this.em = em;
    this.queryFactory = new JPAQueryFactory(em); //Querydsl쓰려면 파라미터로 EntityManager 필요
  }

  /*  // JPAQueryFactory를 @Bean으로 등록했을 경우 바로 인젝션받을수 있다.
    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
      this.em = em;
      this.queryFactory = queryFactory;
    }
  */
  public void save(Member member){
    em.persist(member);
  }

  public Optional<Member> findById(Long id){
    Member findMember = em.find(Member.class, id);
    return Optional.ofNullable(findMember); //null을 반환할수도 있기에 Optional로 반환
  }

  public List<Member> findAll(){
    return em.createQuery("select m from Member m", Member.class)
        .getResultList();
  }

  public List<Member> findAll_Querydsl(){
    return queryFactory
        .selectFrom(member)
        .fetch();
  }

  public List<Member> findByUserName(String username){
    return em.createQuery("select m from Member m where m.username = :username", Member.class)
        .setParameter("username", username)
        .getResultList();
  }

  public List<Member> findByUsername_Querydsl(String username){
    return queryFactory
        .selectFrom(member)
        .where(member.username.eq(username))
        .fetch();
  }
}
