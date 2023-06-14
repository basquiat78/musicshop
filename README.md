# MusicShop Using Functional Endpoints With Coroutine Extension

`domain`패키지는 이전 브랜치에서 변경했기 때문에 이 부분은 따로 언급하지 않는다.

사실 이 작업은 크게 설명할 것이 없고 코틀린에서 제공하는 `coRouter`로 바꾸면 되기 때문에 내용 자체는 길지 않다.    

# ReadMusicianHandler 수정

이제는 핸들러를 수정해야 한다.

```kotlin
@Service
class ReadMusicianHandler(
    private val read: ReadMusicianService,
) {

    fun musicianById(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id").toLong()
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(read.musicianByIdOrThrow(id, "id [$id]로 조회되는 뮤지션이 없습니다."), Musician::class.java)
    }

    fun musiciansByQuery(request: ServerRequest): Mono<ServerResponse> {
        val queryPage = QueryPage.fromServerResponse(request)
        val matrixVariables = searchMatrixVariable(request)
        val match = createQuery(matrixVariables)
        val page = read.musiciansByQuery(queryPage.pagination(match))
                                               .collectList()
                                               .zipWith(read.totalCountByQuery(queryPage.pagination(match)))
                                               .map { tuple -> PageImpl(tuple.t1, queryPage.fromPageable(), tuple.t2) }
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(page, Page::class.java)
    }

}
```
기존의 핸들러의 경우에는 `Mono`로 감싸진 `ServerResponse`를 반환하게 되어 있다.

코루틴를 활용하면 이 부분 역시 `ServerResonse`로 반환하게 변경하면 된다.

```kotlin
@Service
class ReadMusicianHandler(
    private val read: ReadMusicianService,
) {

    suspend fun musicianById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        val musician = read.musicianByIdOrThrow(id, "id [$id]로 조회되는 뮤지션이 없습니다.")
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .bodyValueAndAwait(musician)
    }

    suspend fun musiciansByQuery(request: ServerRequest): ServerResponse {
        val queryPage = QueryPage.fromServerResponse(request)
        val matrixVariables = searchMatrixVariable(request)
        val match = createQuery(matrixVariables)
        val page = read.musiciansByQuery(queryPage.pagination(match))
                       .toList()
                       .countZipWith(read.totalCountByQuery(queryPage.pagination(match)))
                       .map { (musicians, count) -> PageImpl(musicians.toList(), queryPage.fromPageable(), count) }
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .bodyValueAndAwait(page)
    }
}
```
여기서 변경된 부분은 바로 `body`나 `bodyValue`함수는 사용하지 않고 `bodyValueAndAwait`라는 함수를 사용하게 된다.

# RouterConfiguration 수정

```kotlin
@Configuration
class RouterConfiguration {
    @Bean
    fun readMusicianRouter(handler: ReadMusicianHandler): RouterFunction<ServerResponse> {
         return coRouter {
            //path("/api/v1/musicians").nest {
            "/api/v1/musicians".nest {
                accept(APPLICATION_JSON).nest {
                    GET("/{id}", handler::musicianById)
                    GET("/query/{queryCondition}", handler::musiciansByQuery)
                }
            }
        }
    }
}
```
이전 코드에서는 `router`를 사용했지만 여기서는 `coRouter`로 변경만 하면 끝이다.

정말 간단하게 변경 작업이 완료가 된다.

# WriteMusicianHandler 수정

이제는 `Write`쪽을 작업하자.

```kotlin
@Service
class WriteMusicianHandler(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    suspend fun insert(request: ServerRequest): ServerResponse {
        val requestBody = request.awaitBody<CreateMusician>()
        return requestBody.let {
            validate(it)
            val created = write.create(it.toEntity())
            ServerResponse.created(URI.create("/api/v1/musicians/${created.id}"))
                          .contentType(MediaType.APPLICATION_JSON)
                          .bodyValueAndAwait(it)
        }
    }

    suspend fun update(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        val requestBody = request.awaitBody<UpdateMusician>()
        return requestBody.let {
            validate(it)
            val target = read.musicianByIdOrThrow(id)
            val (musician, assignments) = it.createAssignments(target)
            write.update(musician, assignments)
            ServerResponse.ok()
                          .contentType(MediaType.APPLICATION_JSON)
                          .bodyValueAndAwait(read.musicianById(target.id!!)!!)
        }
    }

}
```
크게 변경될 부분은 없다. 

다만 `requestBody`의 정보를 가져올 때는 `bodyToMono`가 아닌 `awaitBody`를 통해서 가져오도록 변경하면 된다.

결국 발행자인 `Mono`, `Flux`를 직접적으로 다루지 않고 우리가 정의한 객체로 다루기 때문에 기존 방식을 고스란히 유지할 수 있다.

# At a Glance

`functional endpoints`의 경우에도 쉽게 사용할 수 있도록 `coRouter`를 제공하기 때문에 작업하기가 수월하다.

게다가 `CoroutineCrudRepository`을 제공하면서 실제로 코루틴을 알지 못해도 적용하기 쉽다.

물론 코틀린의 코루틴을 공부하는 것은 중요하지만 빠르게 적용하기 쉽다.

여기서 주요 내용은 마무리하고자 한다.

다음은 `infobip`에서 제공하는 `queryDSL`적용과 `jooQ`에서 제공하는 `R2dbc`라이브러리를 사용해 보고자 한다.