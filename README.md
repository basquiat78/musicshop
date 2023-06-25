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

## Reactive Programming

먼저 우리가 하고자 하는 `WebFlux`를 다루고자 하기 전에 먼저 리액티브 프로그래밍에 대한 이해가 있어야 한다.

이 저장소에서 이것을 전부 커버하기에는 일단 나 자신이 명확하게 설명하기엔 능력 부족임을 먼저 인정할 수 밖에 없다.

다만 개인적으로 몇 년전부터 `RxJava`나 `reactor project`에 대해 관심을 가지면서 자연스럽게 접하게 된 경우이다.

이것을 이해하기 위해서는 디자인 패턴 중 `The Observer Pattern`, `The Iterator Pattern`애 대해 어느 정도 알면 좋다.

특히 `The Observer Pattern`은 에릭 마이어(Erik Meijer)에 정의된 리액티브 프로그래맹에 많은 영향을 줬다.

이와 관련 여러분에게 도움이 될 만한 사이트를 소개하고자 한다.

[baeldung reactive programming](https://www.baeldung.com/cs/reactive-programming)

[projectreactor official docs](https://projectreactor.io/docs/core/release/reference/)

우리가 먼저 인지해야 하는 것은 `Publisher`이다.

발행자에 대한 개념은 `reactor project`에서 `Reactive Stream`의 `Publisher`인터페이스를 구현하는 `Mono`와 `Flux`를 마주하게 된다.

여러분이 `WebFlux`를 간지나게 사용하고 싶다면 최소한 위 링크에서 [projectreactor official docs](https://projectreactor.io/docs/core/release/reference/)는 필독하길 권장한다.

어쩌면 이것인 스프링 MVC 패턴에 비해서 러닝커브가 상당히 높아지는 이유가 되지 않을까?

# Agenda

[01-using-controller-musician](https://github.com/basquiat78/musicshop/tree/01-using-controller-musician)

[02-using-controller-record](https://github.com/basquiat78/musicshop/tree/02-using-controller-record)

[03-using-functional-endpoints](https://github.com/basquiat78/musicshop/tree/03-using-functional-endpoints)

[04-using-controller-with-coroutine](https://github.com/basquiat78/musicshop/tree/04-using-controller-with-coroutine)

[05-using-functional-endpoints-with-coroutine](https://github.com/basquiat78/musicshop/tree/05-using-functional-endpoints-with-coroutine)

[06-using-jooq-with-coroutine](https://github.com/basquiat78/musicshop/tree/06-using-jooq-with-coroutine)