# MusicShop
회사에서 주 언어가 타입스크립트와 고랭이다.

최근 프로젝트는 모두 타입스크립트와 `nest.js`를 이용해 구축해 왔는데 문득 코틀린이 하고 싶어졌다.

비동기 서버를 구축한다는 마인드로 다시 `WebFlux`를 꺼내들었다.

간만에 작업하다보니 시간가는줄 모르고 즐겁게 작업했는데 이 저장소는 다음과 같은 형식으로 진행될 것이다.

`use Controller > use Functional Endpoint`로 진행한다.

하지만 이 저장소는 이것이 최종 목표는 아니다.

코틀린으로 `WebFlux`를 사용할 때 스프링 공식 사이트에 보면 코루틴을 이용한 `suspend` 함수를 통해 구현하는게 최종 목표이다.

[Going Reactive with Spring, Coroutines and Kotlin Flow](https://spring.io/blog/2019/04/12/going-reactive-with-spring-coroutines-and-kotlin-flow)

하지만 어떤 상황에서도 다룰 수 있어야 하기 때문에 이런 방식으로 빌드업을 할 생각이다.

## Prerequisites

- macOS M1
- Kotlin 1.8.21 on Java 17
- IDE: IntelliJ
- Spring Boot 3.1.0 WebFlux
- RDBMS: mySql v8.0.33
- build: gradle v8.1.1

[Spring Initializr](https://start.spring.io/)를 통해서 필요한 라이브러리를 먼저 설정한다.

최종적으로 이 프로젝트에서 사용하는 것은 `build.gradle.kts`를 통해서 확인하면 된다.
