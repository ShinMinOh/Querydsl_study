package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Commit //DB 값 확인하기 위해서 작성.
class QuerydslApplicationTests {

	@Autowired//@PersistenceContext차이점?
	EntityManager em;


	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory query = new JPAQueryFactory(em);	//JPAQueryFactory : Querydsl 사용하기 위해서 지정.
		QHello qHello = new QHello("h");				// Querydsl과 관련된 쿼리를 쓸때는 반드시 Q타입을 넣어야함.

		Hello result = query
				.selectFrom(qHello)
				.fetchOne();

		assertThat(result).isEqualTo(hello);					//Querydsl Q타입이 정상 동작하는가?
		assertThat(result.getId()).isEqualTo(hello.getId());	// lombok이 정상 동작하는가?
	}

}
