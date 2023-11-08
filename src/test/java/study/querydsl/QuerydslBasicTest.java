package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
@Rollback(value = false)
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach//각 테스트 실행전에 데이터 미리 세팅 위해서
    public void before(){
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void versionJPQL(){
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void versionQuerydsl(){
        //QMember m = new QMember("m"); //어떤 QMember인지 구분하는 variable

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))        //파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test //검색 조건 쿼리
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test //검색 조건 쿼리
    public void searchAndParam(){   //위 serch()와 같은 결과가 나옴. where 문에 and 없이 쓰는 법.
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test // 결과 조회
    public void resultFetch(){
        //리스트 조회
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

       /* //단건 조회
        Member findMember = queryFactory
                .selectFrom(member)
                .fetchOne();
*/
        //처음 한건 조회
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

      /*
       //페이징에서 사용
       QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();            // 복잡하고 성능이 중요한 페이징 쿼리에서는 사용하면 안됨. 그냥 쿼리 2번 따로 날려야함.

        results.getTotal();                     //페이징 하기 위한 totalCount 가져오고
        List<Member> content = results.getResults();    //contents를 가져옴.*/

        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())  //회원 이름이 null인 경우 sort에 의해 맨 마지막.
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    /**
     * 집합 연산
     * */
    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory //Tuple: querydsl이 제공하는 타입으로 data타입이 여러개로 들어올 때 사용. 실무에서는 별로 안쓰이고 대신 DTO로 사용.
            .select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min()
            )
            .from(member)
            .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception{
        List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)  //member에 있는 team과 team을 join.
            .groupBy(team.name)       //team의 이름으로 grouping
            .fetch();

        //groupBy를 팀의 이름으로 했으므로 결과도 teamA, teamB 두개가 나올것이다.
        Tuple resultA = result.get(0);
        Tuple resultB = result.get(1);

        assertThat(resultA.get(team.name)).isEqualTo("teamA");
        assertThat(resultA.get(member.age.avg())).isEqualTo(15);
        assertThat(resultB.get(team.name)).isEqualTo("teamB");
        assertThat(resultB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원 (join 사용)
     * */
    @Test
    public void join(){
        List<Member> result = queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

        assertThat(result)
            .extracting("username")
            .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
            .select(member)
            .from(member, team)
            .where(member.username.eq(team.name))
            .fetch();

        assertThat(result)
            .extracting("username")
            .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회.
     * JPQL: select m,t from Member m left join m.team t on t.name = 'teamA'
     * 멤버는 다 가져오는데, 팀은 teamA인팀 하나만 선택
     * */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team).on(team.name.eq("teamA"))
            .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = "+tuple);
        }
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * 세타 조인인 경우 left join이 불가능함. 그래서 그것을 가능하게 하는 방법 설명.
     * */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team).on(member.username.eq(team.name))   //원래는 leftJoin(member.team, team).on(member.username.eq(team.name))임.
            .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = "+tuple);
        }
    }


    @PersistenceUnit    //EntityManager를 만드는 factory
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNO(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"))
            .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//괄호 안에 엔티티가 초기화된 엔티티인지 아닌지 알려줌.

        assertThat(loaded).as("페치 조인 미적용").isFalse();
        //Member 엔티티에 Team은 Lazy로 세팅이 되어있다.
        // 현재 findMember에서 Team이 쓰이지 않아 아직 호출되기 전 상태이다.
        // 따라서 findMember를 호출했을때 getTeam을 햇을 경우 아직 불리지 않았으므로 로딩(초기화)이 false가 나와야 한다.
    }

    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
            .selectFrom(member)
            .join(member.team, team).fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     *  */
    @Test
    public void subQuery(){
        QMember memberSub = new QMember("memberSub");
        //allias가 중복되면 안되는 경우 새로운 allias로 선언. 밖에 있는 allias랑 서브쿼리에서 쓰는 allias는 같으면 안됨.eq안에 서브쿼리가 들어감.

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(
                JPAExpressions
                    .select(memberSub.age.max())
                    .from(memberSub)
            ))
            .fetch();

        for (Member findMember : result) {
            System.out.println("findMember = "+findMember);
        }

        assertThat(result).extracting("age").containsExactly(40);
    }


    /**
     * 나이가 평균 이상인 회원 조회
     *  */
    @Test
    public void subQueryGoe(){
        QMember memberSub = new QMember("memberSub");
        //allias가 중복되면 안되는 경우 새로운 allias로 선언. 밖에 있는 allias랑 서브쿼리에서 쓰는 allias는 같으면 안됨.eq안에 서브쿼리가 들어감.

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.goe(
                JPAExpressions
                    .select(memberSub.age.avg())
                    .from(memberSub)
            ))
            .fetch();

        for (Member findMember : result) {
            System.out.println("findMember = "+findMember);
        }

        assertThat(result).extracting("age").containsExactly(30, 40);
    }




}
