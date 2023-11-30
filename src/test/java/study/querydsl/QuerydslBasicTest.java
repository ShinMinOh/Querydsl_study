package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
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
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
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
                select(memberSub.age.max())
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
                select(memberSub.age.avg())
                    .from(memberSub)
            ))
            .fetch();

        for (Member findMember : result) {
            System.out.println("findMember = "+findMember);
        }

        assertThat(result).extracting("age").containsExactly(30, 40);
    }


    /**
     * 나이가 10살 이상인 회원 조회
     *  */
    @Test
    public void subQueryIn(){
        QMember memberSub = new QMember("memberSub");
        //allias가 중복되면 안되는 경우 새로운 allias로 선언. 밖에 있는 allias랑 서브쿼리에서 쓰는 allias는 같으면 안됨.eq안에 서브쿼리가 들어감.

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.age.gt(10))
            ))
            .fetch();

        for (Member findMember : result) {
            System.out.println("findMember = "+findMember);
        }

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }


    /**
     * select  절 안에서 SubQuery 사용.
     * */
    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryFactory
            .select(member.username,
                select(memberSub.age.avg())     //JPAExpressions static import 시킨것.
                    .from(memberSub))
            .from(member)
            .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = "+tuple);
        }
    }


    @Test
    public void basicCase(){
        List<String> result = queryFactory
            .select(member.age
                .when(10).then("열살")
                .when(20).then("스무살")
                .otherwise("기타"))
            .from(member)
            .fetch();

        for (String s : result) {
            System.out.println("s = "+s);
        }
    }

    /**
     * 복잡한 case문 작성의 경우 Casebuilder 사용
     */
    @Test
    public void complexCase(){
        List<String> result = queryFactory
            .select(new CaseBuilder()
                .when(member.age.between(0, 20)).then("0~20살")
                .when(member.age.between(21, 30)).then("21~30살")
                .otherwise("기타"))
            .from(member)
            .fetch();

        for (String s : result) {
            System.out.println("s = "+s);
        }
    }
        //전환하고 바꾸고 하는 case문 예제의 경우 DB보다는 애플리케이션이나 프레젠테이션 레이어에서 해결하는 것이 좋은 방법이다.


    /**
    * 상수 더하기
    * */
    @Test
    public void constant(){
        List<Tuple> result = queryFactory
            .select(member.username, Expressions.constant("A"))
            .from(member)
            .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = "+tuple);
        }
    }

    /**
     * 문자 더하기
     * */
    @Test
    public void concat(){
        List<String> result = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();

        for (String s : result) {
            System.out.println("s = "+s);
        }
    }

    /**
     * 프로젝션 대상이 1개
     * */
    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .fetch();

        for (String s : result) {
            System.out.println("S = "+s);
        }
    }

    /**
     * 프로젝션(select) 대상이 여러개 (tuple)
     * Tuple은 레포지토리 안에서만 쓰고, 바깥 레이어에 나갈대는 DTO로 변환시켜서 보내는 것이 좋은 설계
     * */
    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
            .select(member.username, member.age)
            .from(member)
            .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = "+username);
            System.out.println("age = "+age);

        }
    }

    /**
     * 프로젝션과 결과 반환 - DTO 조회 (JPQL ver.)
     * */
    @Test
    public void findDtoByJPQL(){
        List<MemberDto> result = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                MemberDto.class)
            .getResultList();
            // 단순히 ("select m.username, m.age from Member m", MemberDto.class)를 할경우 Member와 MemberDto간 타입 불일치로 쓸 수 없다.
            // 따라서 new 오퍼레이션을 활용해 마치 MemberDto안에 생성자를 넣어주는 것처럼 써줘야 한다. new + 패키지명 + MemberDto(  )
            // setter나 필드 직접 주입이 안됨. 오직 생성자 방식만 가능.
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }

    /**
     * 프로젝션과 결과 반환 - DTO 조회 (Querydsl ver.)
     * */
    @Test   // 프로퍼티 접근 - setter , bean : getter, setter말함.
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
            .select(Projections.bean(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }

    @Test   // 필드 직접 접근 - getter,setter 없이 필드에 직접 꽂힘.
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
            .select(Projections.fields(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }

    @Test   // 생성자 사용
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory
            .select(Projections.constructor(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }


    @Test   // 필드 직접 접근 사용한 UserDto 조회
    public void findUserDto(){
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                member.username.as("name"),

                ExpressionUtils.as(
                    JPAExpressions
                    .select(memberSub.age.max())
                    .from(memberSub),"age")     // 두번째 서브쿼리가 이름이 없으므로 ExpressionsUtils의 두번째 파라미터로 alias인 age를 설정하면 UserDto의 age와 매칭된다.
            ))
            .from(member)
            .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = "+userDto);
        }
    }

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
            .select(new QMemberDto(member.username, member.age))
            .from(member)
            .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = "+memberDto);
        }
    }

    /**
     * 동적 쿼리-BooleanBuilder
     * */
    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    /**
     * 동적 쿼리-BooleanBuilder
     * */
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
            .selectFrom(member)
            .where(builder)
            .fetch();
    }


    /**
     * 동적 쿼리-WhereParam
     * 장점
     * 1. BooleanBuilder보다 코드 가독성이 좋다.
     * 2. 함수끼리 조립을 할 수 있다.
     * 3. 메서드 재활용이 가능하다.
     * */
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
            .selectFrom(member)
//            .where(usernameEq(usernameCond), ageEq(ageCond))   // 만약 usernameEq(usernameCond) 의 리턴값이 null이면 username에 대한 where조건은 무시되고 age조건만 동적쿼리로 만들어진다.
            .where(allEq(usernameCond, ageCond))                //  usernameEq와 ageEq 함수를 조립.
            .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) { //Predicate타입보다 BooleanExpression타입으로 지정하는게 나중에 함수를 이용하기가 좋다.
        if(usernameCond != null){
            return member.username.eq(usernameCond);
        } else {
            return null;
        }
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if(ageCond != null){
            return member.age.eq(ageCond);
        } else {
            return null;
        }
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate(){
        //DB와 영속성 컨텍스트안의 데이터가 일치하지 않을 경우 영속성 컨텍스트의 데이터가 우선권을 가짐.

        //전
        //영속성 컨텍스트 member1 = 10 -> DB member1
        //영속성 컨텍스트 member2 = 20 -> DB member2
        //영속성 컨텍스트 member3 = 30 -> DB member3
        //영속성 컨텍스트 member4 = 40 -> DB member4

        long count = queryFactory       //영속성 컨텍스트를 거치지 않고 DB에 바로 update 시킴.
            .update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(28))
            .execute();

        //위와 같은 벌크 연산이 나가면 이미 영속성 컨텍스트의 값과 DB의 값이 맞지 않기 때문에
        //아래와 같이 em.flush(), em.clear()로 영속성 컨텍스트를 초기화 하는 것이 좋은 방법이다.
        em.flush();
        em.clear();

        //후
        //영속성 컨텍스트 member1 = 10 -> DB 비회원
        //영속성 컨텍스트 member2 = 20 -> DB 비회원
        //영속성 컨텍스트 member3 = 30 -> DB member3
        //영속성 컨텍스트 member4 = 40 -> DB member4

        List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();

        for (Member member1 : result) {
            //비록 DB안의값은 비회원이지만 영속성 컨텍스트 안의 값이 우선권을 가지므로 DB에서 가져온 값을 버림.
            System.out.println("member1 = "+member1);
        }
    }

    @Test
    public void bulkAdd(){
        queryFactory
            .update(member)
            .set(member.age, member.age.add(2))
            .execute();
    }

    @Test
    public void bulkDelete(){
        queryFactory
            .delete(member)
            .where(member.age.gt(18))
            .execute();
    }

}
