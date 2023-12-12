package study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
  //회원명, 팀명, 나이(ageGoe, ageLoe)가 넘어옴.
  private String username;
  private String teamName;
  private Integer ageGoe; //타입이 Integer인 이유 : Null이 나올수 있기 때문이다.
  private Integer ageLoe;


}
