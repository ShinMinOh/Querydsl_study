package study.querydsl.repository;

import static io.micrometer.common.util.StringUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

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

  public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){
    BooleanBuilder builder = new BooleanBuilder();
    //if(null, "")형태로도 많이 넘어온다. 그래서 이것을 방지하기 위해 StringUtils.hasText 사용
    if(hasText(condition.getUsername())){
      builder.and(member.username.eq(condition.getUsername()));
    }
    if(hasText(condition.getTeamName())){
      builder.and(team.name.eq(condition.getTeamName()));
    }
    if(condition.getAgeGoe() != null){
      builder.and(member.age.goe(condition.getAgeGoe()));
    }
    if(condition.getAgeLoe() != null){
      builder.and(member.age.loe(condition.getAgeLoe()));
    }

    return queryFactory
        .select(new QMemberTeamDto(
            //member.id.as("memberId"),
            member.id,
            member.username,
            member.age,
            team.id,
            team.name
        ))
        .from(member)
        .leftJoin(member.team, team)
        .where(builder)
        .fetch();

  }

  public List<MemberTeamDto> search(MemberSearchCondition condition){
    return  queryFactory
        .select(new QMemberTeamDto(
            member.id,
            member.username,
            member.age,
            team.id,
            team.name))
        .from(member)
        .leftJoin(member.team, team)
        .where(
            usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        )
        .fetch();
  }

  private BooleanExpression usernameEq(String username) {
    return hasText(username) ? member.username.eq(username) : null;
    //hasText(username) 참이면 member.username.eq(username) 반환
    // 아니면 null 반환
  }

  private BooleanExpression teamNameEq(String teamName) {
    return hasText(teamName) ? team.name.eq(teamName) : null;
  }

  private BooleanExpression ageGoe(Integer ageGoe) {
    return ageGoe != null ? member.age.goe(ageGoe) : null;
  }

  private BooleanExpression ageLoe(Integer ageLoe) {
    return ageLoe != null ? member.age.loe(ageLoe) : null;
  }
}
