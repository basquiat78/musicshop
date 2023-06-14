# MusicShop Using Functional Endpoints

`functional endpoints`는 아마도 `node.js`의 `Express`를 사용해보신 분들이라면 상당히 익숙한 형식이다.

라우터라는 것이 `IT`에서 말하면 일반적으로 네트워크 상에서 최상의 경로를 찾는 것을 의미한다.

보통 거치는 거점을 하나의 `hop`으로 계산해서 가장 작은 카운트의 경로라든가 작은 카운트라할지라도 `hop`사이의 거리가 길다면 `hop`보다는 거리를 측정해서 최상의 경로를 찾는다.

소스에서 특정 대상으로 이동하는 테이터의 경로를 결정한다가 사전적인 의미인데 클라이언트의 요청을 적절하게 `URL`매핑을 통해 전달하는 것이라고 보면 된다.

`nest.js`가 `Express`를 사용하고 있지만 스프링 부트와 비슷하게 컨트롤러처럼 사용하기 때문에 이것이 가려져 있긴 하지만 결국 이런 라우터의 개념이 적용된다.

우선 이 방법을 사용하기 위해서는 `node.js`처럼 `URL`과 그에 상응하는 핸들러를 매핑해주는 빈을 등록해야 한다.

또한 컨트롤러 방식과는 다르게 `ServerRequest`객체를 통해 요청 정보를 받아서 `ServerResponse`객체를 통해 클라이언트로 정보를 내려준다.

따라서 라우터를 담당할 빈을 등록하기 전에 우리는 지금까지 작성한 `useCase`를 이에 맞게 수정해야 한다.

그리고 컨트롤러 부분은 `RouterFunction`작업을 위해서 주석처리만 해놓고 마지막에는 삭제할 것이다.

## ReadMusician Handler 작업

```kotlin
@Service
class ReadMusicianUseCase(
    private val read: ReadMusicianService,
) {

    fun musicianById(id: Long): Mono<Musician> {
        return read.musicianByIdOrThrow(id)
    }

    fun musiciansByQuery(queryPage: QueryPage, matrixVariable: MultiValueMap<String, Any>): Mono<Page<Musician>> {
        val match = createQuery(matrixVariable)
        return read.musiciansByQuery(queryPage.pagination(match))
                   .collectList()
                   .zipWith(read.totalCountByQuery(match))
                   .map { tuple -> PageImpl(tuple.t1, queryPage.fromPageable(), tuple.t2) }
    }
}
```
기존의 `ReadMusicianUseCase`는 다음과 같다.

`musicianById`함수의 경우에는 `id`를 받는 함수이다.

컨트롤러에서 `@PathVariable`를 통해서 받는 방식인데 `functional endpoints`에서는 다음과 같이 처리를 해야 한다.

다만 컨트롤러 방식처럼 리졸버를 통해서 원하는 타입으로의 자동 매핑이 불가능하기 때문에 내부적으로는 스트링으로 반환한다.

그리고 이후에 원하는 타입으로 캐스팅을 해줘야 한다.

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
}

@GetMapping("/{id}")
@ResponseStatus(HttpStatus.OK)
fun fetchMusician(@PathVariable("id") id: Long): Mono<Musician> {
    return readMusicianUseCase.musicianById(id)
}
```
아래 부분은 컨트롤러 단의 코드인데 이때 `@ResponseStatus(HttpStatus.OK)`를 `ServerResponse`에 응답코드를 설정한다.

`json`형식이기 때문에 응답 형식을 설정하고 `body`를 통해서 정보를 담아 보내주게 된다.

## Registry RouterFunction

온갖 기술 블로그를 다 뒤지면서 된다는 테스트 코드를 전부 해봤지만 핸들러 자체를 테스트하기에는 무리가 있다.

결국 `WebClient`를 통해서 테스트를 해볼 수밖에 없다.

따라서 다음과 같이 `RouterFunction`을 설정해야 한다.

```kotlin
@Configuration
class RouterConfiguration {
    @Bean
    fun readMusicianRouter(readMusicianHandler: ReadMusicianHandler): RouterFunction<ServerResponse> {
        return router {
            //path("/api/v1/musicians").nest {
            "/api/v1/musicians".nest {
                accept(APPLICATION_JSON).nest {
                    GET("/{id}", readMusicianHandler::musicianById)
                }
                /*
                accept(APPLICATION_ATOM_XML).nest {
                    GET(...)
                }
                */
            }
        }
    }
}
```
`path`의 경우에는 공통 `URI`를 묶을 때 사용한다. 

명시적으로 하고 싶다면 주석된 부분을 사용하면 되고 간략하게 하고 싶다면 위처럼 한다.

`nest`는 중첩으로 공통사항을 묶고 싶은 경우에 사용하는데 `accept`의 경우에는 특별한 케이스가 아니면 대부분 `json`일테니 위와 같이 작성하면 반복되는 코드를 줄일 수 있다.

물론 다른 `accept`타입이 들어온다면 주석된 부분처럼 아래로 추가해서 작성도 가능하다.

```kotlin
@SpringBootTest
@AutoConfigureWebTestClient
class ReadMusicianRouterTest @Autowired constructor(
    private var webTestClient: WebTestClient,
) {

    @Test
    @DisplayName("musicianById router test")
    fun musicianByIdTEST() {
        // given
        val id = "1"
        
        // when
        webTestClient.get()
                     .uri("/api/v1/musicians/{id}", id)
                     .exchange()
                     .expectStatus().isOk
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(Musician::class.java)
                     // then
                     .value { musician ->
                        assertThat(musician.name).isEqualTo("Charlie Parker")
                     }
    }

}
```
이전 테스트 코드 방식과는 다르게 한번 작성 해 본 검증 코드이다.

`json`패스말고 넘어오는 객체의 정보를 지정해 `value`를 통해 해당 객체 정보를 가져와 검증 코드를 통해 테스트를 하는 방식을 취하게 된다.

물론 이전 방식처럼 테스트 코드로 검증해도 유효하다.

## 전역 에러 처리

`onErrorResume`, `onErrorReturn`같은 함수를 통해 핸들러에서 `functional level`에서 처리할 수도 있다.

하지만 기존처럼 전역 에러 처리를 하는 것을 우선으로 두고 시작해 보자.

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): Mono<ApiError> {
        return Mono.just(ApiError(code = HttpStatus.NOT_FOUND.value(), message = ex.message!!))
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(MissingInformationException::class)
    fun handleMissingInformationException(ex: MissingInformationException): Mono<ApiError> {
        return Mono.just(ApiError(code = HttpStatus.NOT_FOUND.value(), message = ex.message!!))
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(BadParameterException::class)
    fun handleBadParameterException(ex: BadParameterException): Mono<ApiError> {
        return Mono.just(ApiError(code = HttpStatus.NOT_FOUND.value(), message = ex.message!!))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleException(ex: WebExchangeBindException): Mono<ApiError> {
        val errors = ex.bindingResult.allErrors.first()
        return Mono.just(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = errors.defaultMessage!!))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DecodingException::class)
    fun handleJsonParserErrors(ex: DecodingException): Mono<ApiError> {
        val enumMessage = Pattern.compile("not one of the values accepted for Enum class: \\[([^\\]]+)]")
        if (ex.cause != null && ex.cause is InvalidFormatException) {
            val matcher: Matcher = enumMessage.matcher(ex.cause!!.message)
            if (matcher.find()) {
                return Mono.just(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = "enum value should be: " + matcher.group(1)))
            }
        }
        return Mono.just(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = ex.message!!))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TypeMismatchException::class)
    fun handleValidationExceptions(ex: TypeMismatchException): Mono<ApiError> {
        val enumMessage = Pattern.compile(".*Sort.*")
        if (ex.cause != null && ex.cause is ConversionFailedException) {
            val matcher: Matcher = enumMessage.matcher(ex.cause!!.message)
            if (matcher.matches()) {
                return Mono.just(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = "Sort Direction should be: [DESC, ASC]"))
            }
        }
        return Mono.just(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = ex.message!!))
    }

}
```
기존의 방식으로 하게 되면 생각과는 다르게 작동을 한다.

물론 우리가 던질떼 보내는 메세지가 드러나긴 하지만 전역 처리 핸들러를 타지 않아 전체 에러 정보가 전부 뜨게 된다.

따라서 전역 에러 처리도 이 방식에 맞게 해야 한다.

그렇기 때문에 `AbstractErrorWebExceptionHandler`를 구현하는 방식을 사용하게 된다.

그리고 최종적으로는 `ServerResponse`로 에러 정보를 내려주게 될 것이다.

참고로 우리는 이 방식에서는 기존 방식과 다르게 다양한 커스텀 익셉션을 만들고 그에 따른 에러 처리를 할 수 없다.

다만 기존의 만든 것을 유지하고 싶기 때문에 공통 에러 처리부분에서 결국에는 에러 정보를 비교하고 처리하는 방식으로 간다.

```kotlin
@Component
@Order(-2)
class GlobalExceptionHandler(
    errorAttributes: ErrorAttributes,
    serverCodecConfigurer: ServerCodecConfigurer,
    applicationContext: ApplicationContext,
) : AbstractErrorWebExceptionHandler(errorAttributes, WebProperties.Resources(), applicationContext) {

    init {
        super.setMessageWriters(serverCodecConfigurer.writers);
        super.setMessageReaders(serverCodecConfigurer.readers);
    }

    private val log = logger<GlobalExceptionHandler>()

    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return RouterFunctions.route(RequestPredicates.all(), rendering)
    }

    val rendering = HandlerFunction { request ->
        when (val ex = super.getError(request)) {
            is NotFoundException -> {
                log.error(ex.message)
                ServerResponse.status(HttpStatus.NOT_FOUND)
                              .bodyValue(ApiError(code = HttpStatus.NOT_FOUND.value(), message = ex.message!!))
            }
            else -> {
                log.error(ex.message)
                ServerResponse.status(HttpStatus.NOT_FOUND)
                    .bodyValue(ApiError(code = HttpStatus.NOT_FOUND.value(), message = ex.message!!))
            }
        }
    }

}
```
이와 관련 [spring-webflux-errors](https://www.baeldung.com/spring-webflux-errors)에 자세히 나와 있다.

다만 버전의 차이로 위와 같이 몇가지 손을 좀 봐야 한다.

차후 `whne`절에 스마트 연산자인 `is`를 통해 하나 하나 작업해야 하는 번거로움이 있지만 기존의 익셉션을 그대로 사용할 수 있는 베스트가 아닌가 싶다.

## @MatrixVariable, @RequestBody, @RequestParam and etc Annotations Not Use

컨트롤러의 경우에는 `@MatrixVariable`, `@ReqeustParam`, `@RequestBody`같은 다양한 어노테이션이 사용 가능하지만 `functional endpoint`에서는 이런 이점을 이용할 수 없다.

어노테이션 사용 불가로 `org.springframework.web.reactive.result.method.annotation`패키지내의 다양한 리졸버를 사용할 수 없기 때문이다.

그중에 몇 가지는 `API`로 제공하지만 `@MatrixVariable`의 경우에는 제공하지 않는다.

그래서 결국 최종적으로는 `ServerReqeust`객체로부터 필요한 정보를 가져오는 수밖에 없다.

### 어떻게?

`@RequestParam`은 `API`를 통해 사용할 수 있다.

```kotlin
data class QueryPage(
    @field:Min(1, message = "페이지 정보는 0보다 커야 합니다.")
    val page: Int? = 1,
    @field:Min(1, message = "사이즈 정보는 0보다 커야 합니다.")
    val size: Int? = 10,
    val column: String? = null,
    val sort: Sort.Direction? = null,
) {
    private val offset : Int
        get() = this.page!! - 1

    private val limit  : Int
        get() = this.size!!

    val currentPage: Int
        get() = this.page!!

    fun fromPageable(): PageRequest {
        val sort = if (column != null && sort != null) Sort.by(sort, column) else Sort.unsorted()
        return PageRequest.of(offset, limit, sort)
    }

    fun pagination(match: Query): Query {
        val sort = if (column != null && sort != null) Sort.by(sort, column) else Sort.unsorted()
        return match.offset(offset.toLong())
                    .limit(limit)
                    .sort(sort)
    }

    companion object {
        fun fromServerResponse(request: ServerRequest): QueryPage {
            val map = request.queryParams()
            return QueryPage(
                page = map["page"]?.first()?.toInt() ?: 1,
                size =  map["size"]?.first()?.toInt() ?: 10,
                column =  map["column"]?.firstOrNull(),
                sort = map["sort"]?.first()?.let { Sort.Direction.valueOf(it) },
            )
        }
    }

}
```
여기서 `fromServerResponse`함수는 `ServerRequest`로부터 `queryParams`를 통해 `MultiValueMap`으로 다룰 수 있다.

물론 `queryParam`이나 코틀린의 경우에는 `queryParamOrNull`를 활용할 수 있다.

`@MatrixVariable`의 경우에는 `ServerReqeust`객체에서 `ServerWebExchange`를 가져와 `request`정보를 분석하면 `path`쪽에 `pathWithinApplication`을 이용하는 방법이다.

`PathContainer`에는 `path`정보를 보면 `Element`객체를 리스트 형식으로 가지고 있다.

예를 들면 `/api/v1/musicians/query/search;name=like,윙스`를 구분자와 각 `uri`정보들을 리스트로 갖는데 그 형식이 다음과 같다.

```
["/", "api", "/", "v1", "/", "musicians", "/", "query", "search;name=like,%EC%9C%99%EC%8A%A4;"]
```
우리는 `{queryCondition}`를 `PathVariable`로 사용하고 있다.

들어온 `PathVariable`정보를 꺼내와 `Element`리스트에서 `pathVariable`과 일치하는 요소를 가져와 `MultiValueMap`으로 생성하도록 하자.

아래 코드는 `MatrixVariableUtils`에 작업한 코드로 `@PathVariable`에 해당하는 `API`인 `pathVariable`를 사용한 예제도 포함한다.

물론 이 `API`도 `Map`형식으로 다룰 수 있는 `pathVariables`도 존재한다.

```kotlin
fun searchMatrixVariable(serverRequest: ServerRequest): MultiValueMap<String, Any> {
    val resource = "queryCondition"
    val pathVariable = serverRequest.pathVariable(resource)
    val elements = serverRequest.exchange().request.path.pathWithinApplication().elements()
    val target = elements.firstOrNull { it.value().startsWith(pathVariable) }?.value()?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
                                ?: throw BadParameterException("매트릭스 변수 정보가 누락되었거나 요청이 잘못되었습니다.")
    val splitArray = target.split(";")
    val sliceArray = splitArray.slice(1 until splitArray.size)
                               .filter { it.isNotBlank() }

    val result = mutableMapOf<String, Any>()
    sliceArray.associateTo(result) {
        try {
            val split = it.split("=")
            val key = split[0]
            val list = if (key == "all") {
                listOf("all")
            } else {
                split[1].split(",")
            }
            key to list
        } catch (ex: Exception) {
            throw BadParameterException("매트릭스 변수 정보가 누락되었거나 요청이 잘못되었습니다.")
        }
    }
    return mapToMultiValueMap(result)
}

fun mapToMultiValueMap(map: MutableMap<String, Any>): MultiValueMap<String, Any> {
    val multiValueMap = LinkedMultiValueMap<String, Any>()
    for ((key, value) in map) {
        for (elem in value as List<Any>) {
            multiValueMap.add(key, elem)
        }
    }
    return multiValueMap
}
```
이제는 다음과 같이 핸들러에 함수를 기존에 것과 대조해가며 작업해 보자.

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
지금까지 작업한 `Read`쪽의 라우터 추가와 테스트 검증을 해보자.

```kotlin
@Configuration
class RouterConfiguration {
    @Bean
    fun readMusicianRouter(readMusicianHandler: ReadMusicianHandler): RouterFunction<ServerResponse> {
         return router {
            //path("/api/v1/musicians").nest {
            "/api/v1/musicians".nest {
                accept(APPLICATION_JSON).nest {
                    GET("/{id}", readMusicianHandler::musicianById)
                    GET("/query/{queryCondition}", readMusicianHandler::musiciansByQuery)
                }
            }
        }
    }
}
```
```kotlin
@SpringBootTest
@AutoConfigureWebTestClient
class ReadMusicianRouterTest @Autowired constructor(
    private var webTestClient: WebTestClient,
) {

    @Test
    @DisplayName("musicianById router test")
    fun musicianByIdTEST() {
        // given
        val id = "1"
        
        // when
        webTestClient.get()
                     .uri("/api/v1/musicians/{id}", id)
                     .exchange()
                     .expectStatus().isOk
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(Musician::class.java)
                     // then
                     .value { musician ->
                        assertThat(musician.name).isEqualTo("Charlie Parker")
                     }
    }
    
    @Test
    @DisplayName("musiciansByQuery adjust Matrix Variable test")
    fun musiciansByQueryTEST() {
        // given
        val page = 1
        val size = 10
        
        // when
        webTestClient.get()
                     .uri("/api/v1/musicians/query/search;name=like,윙스?page=$page&size=$size")
                     .exchange()
                     .expectStatus().isOk
                     .expectHeader().contentType(MediaType.APPLICATION_JSON)
                     .expectBody()
                     // then
                     .jsonPath("$.content[0].name").isEqualTo("스윙스")
    }

}
```
그렇다면 이제 `WriteMusicianHandler`작업을 진행해 보자.

## WriteMusician Handler 작업

다음은 기존의 `WriteMusicianUseCase`이다. 

이제 이것을 하나씩 작업해 나가자.

```kotlin
@Service
class WriteMusicianUseCase(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    fun insert(command: CreateMusician): Mono<Musician> {
        val created = Musician(name = command.name, genre = command.genre)
        return write.create(created)
    }

    fun update(id: Long, command: UpdateMusician): Mono<Musician> {
        return read.musicianByIdOrThrow(id).flatMap { musician ->
            val (musician, assignments) = command.createAssignments(musician)
            write.update(musician, assignments)
        }.onErrorResume {
            Mono.error(BadParameterException(it.message))
        }
        .then(read.musicianById(id))
    }

}
```

## Create Musician API

확실히 우리는 어노테이션을 활용해 쉽게 접근했던 방식을 버려야 한다.

결국 `ServerRequest`로부터 모든 정보를 얻어 와야 한다.

```kotlin
data class CreateMusician(
    @field:NotNull
    @field:Size(min = 2, message = "뮤지션의 이름이 누락되었습니다. 최소 한 글자 이상이어야 합니다.")
    val name: String,
    @field:NotNull(message = "장르 정보가 누락되었습니다.")
    val genre: Genre?,
) {
    fun toEntity(): Musician {
        return Musician(name = name, genre = genre)
    }
}

@Service
class WriteMusicianHandler(
    private val write: WriteMusicianService,
) {

    fun insert(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(CreateMusician::class.java)
                      .flatMap {
                            write.create(it.toEntity())
                      }
                      .flatMap {
                            ServerResponse.created(URI.create("/api/v1/musicians/${it.id}"))
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .body(it.toMono(), Musician::class.java)
                     }
    }
}
```
`@RequestBody`와 대응되는 `API`는 `bodyToMono`가 있다.

함수명에서 솔솔 풍기듯이 해당 결과는 `Mono`로 쌓여져 있기 때문에 이를 위해선 `flatMap`를 사용해서 비지니스 로직을 처리한다.

한줄이라도 줄이고픈 욕망이 생기니 `CreateMusician`객체에서 정보로부터 생성한 엔티티를 만들어 주도록 하자.      

기존의 컨트롤러의 응답 상태 코드가 `HttpStatus.CREATED`기 때문에 상태를 `created`로 처리한다.

응답 로케이션 정보를 생성해 주게 되면 포스트맨의 경우에는 `Headers`의 `location`항목에 포함되는 것을 확인할 수 있다.

테스트 코드는 업데이트 작업이 완료된 이후 일괄적으로 검증을 해볼 것이다.

## Update Musician API

일단 폼 데이터로 넘어온 정보를 객체에 매핑하는 방법을 알면 이제는 다음 단계도 쉽게 처리할 수 있다.

```kotlin
@Service
class WriteMusicianHandler(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    fun insert(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(CreateMusician::class.java)
                      .flatMap {
                            write.create(it.toEntity())
                      }
                      .flatMap {
                            ServerResponse.created(URI.create("/api/v1/musicians/${it.id}"))
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .body(it.toMono(), Musician::class.java)
                      }
    }

    fun update(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id").toLong()
        return request.bodyToMono(UpdateMusician::class.java)
                      .flatMap {
                            read.musicianByIdOrThrow(id)
                                .flatMap { musician ->
                                    val (musician, assignments) = it.createAssignments(musician)
                                    write.update(musician, assignments)
                                }.then(read.musicianById(id))
                      }
                      .flatMap {
                            ServerResponse.ok()
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .body(it.toMono(), Musician::class.java)
                     }
    }
}
```
프로듀서인 `Mono`로 감싸져 있기 때문에 어떤 정보를 다룰 때는 위와 같은 메소드 체이닝을 통해서 맞춰서 처리해야 한다.

핸들러 레벨에서 `onErrorResume`을 통해 처리하는 방식도 고려해 볼 수 있지만 전역 처리로 넘기도록 하자.

이제는 과연 원하는대로 잘 되는지 테스트 코드를 통해 검증을 해야 한다.

먼저 라우터 설정을 추가하자.

```kotlin
@Configuration
class RouterConfiguration {
    @Bean
    fun readMusicianRouter(readMusicianHandler: ReadMusicianHandler): RouterFunction<ServerResponse> {
         return router {
            //path("/api/v1/musicians").nest {
            "/api/v1/musicians".nest {
                accept(APPLICATION_JSON).nest {
                    GET("/{id}", readMusicianHandler::musicianById)
                    GET("/query/{queryCondition}", readMusicianHandler::musiciansByQuery)
                }
            }
        }
    }

    @Bean
    fun writeMusicianRouter(writeMusicianHandler: WriteMusicianHandler): RouterFunction<ServerResponse> {
        return router {
            "/api/v1/musicians".nest {
                accept(APPLICATION_JSON).nest {
                    POST("", writeMusicianHandler::insert)
                    PATCH("/{id}", writeMusicianHandler::update)
                }
            }
        }
    }

}
```

테스트 코드

```kotlin
@SpringBootTest
@AutoConfigureWebTestClient
class WriteMusicianRouterTest @Autowired constructor(
    private var webTestClient: WebTestClient,
) {
    
    @Test
    @DisplayName("musician insert router test")
    fun musicianInsertTEST() {
        // given
        val createMusician = CreateMusician(name = "beenzin000", Genre.HIPHOP)
        
        // when
        webTestClient.post()
                     .uri("/api/v1/musicians")
                     .contentType(MediaType.APPLICATION_JSON)
                     .body(createMusician.toMono(), CreateMusician::class.java)
                     .exchange()
                     .expectStatus().isCreated
                     .expectBody(Musician::class.java)
                     //then
                     .value {
                        assertThat(it.id).isGreaterThan(0)
                     }
    }
    
    @Test
    @DisplayName("musician update router test")
    fun musicianUpdateTEST() {
        // given
        val id = 15L
        val updateMusician = UpdateMusician(name = "beenzino")
        
        // when
        webTestClient.patch()
                     .uri("/api/v1/musicians/{id}", id)
                     .contentType(MediaType.APPLICATION_JSON)
                     .body(updateMusician.toMono(), UpdateMusician::class.java)
                     .exchange()
                     .expectStatus().isOk
                     .expectBody(Musician::class.java)
                     //then
                     .value {
                         assertThat(it.id).isEqualTo(15)
                         assertThat(it.name).isEqualTo(updateMusician.name)
                     }
    }

}
```

## Record 변경 작업

이미 여기까지 왔다면 기존의 방식과 크게 다르지 않을 것이라는 것을 알 수 있다.

완료된 코드는 확인하면 된다.

## QueryPage Validate

기존에 우리가 만든 `QueryPage`의 `sort`필드가 `enum`클래스로 정의되어 있었다.

애초에 필드 자체가 `enum`이기 때문에 유효성 이전에 에러를 뱉는다는 것은 이전 브랜치에서 언급한 적이 있었다.      

하지만 곰곰히 생각해보니 스트링으로 정의하고 `enum`에 정의된 값인지 확인하면 되기 때문에 이 방법을 다시 한번 끄집어 내본다.

먼저 예제로 만들었던 `@EnumCheck`와 `EnumValueValidator`를 활용해 보고자 한다.

컨트롤러와 달리 `functional endpoint`에서는 `queryParam`의 경우에는 `ServerReqeust`로부터 다음과 같이 작업을 했다.

```kotlin
companion object {
    fun fromServerResponse(request: ServerRequest): QueryPage {
        val map = request.queryParams()
        println(map["name"]?.first())
        return QueryPage(
            page = map["page"]?.first()?.toInt() ?: 1,
            size =  map["size"]?.first()?.toInt() ?: 10,
            column =  map["column"]?.firstOrNull(),
            sort = map["sort"]?.first()?.let { Sort.Direction.valueOf(it) },
        )
    }
}
```
이것을 보면 사실 넘어온 쿼리 파라미터에 대한 유효성을 체크하고 있지 않다.

따라서 기존의 붙여 둔 애노테이션과 커스텀 애노테이션을 활용해보자.

```kotlin
class EnumValueValidator: ConstraintValidator<EnumCheck, String?> {

    private lateinit var annotation: EnumCheck

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return value?.let {
            val enumValues = this.annotation.enumClazz.java.enumConstants
            val checkedEnum = enumValues.firstOrNull { it.name.equals(value, ignoreCase = true) }
            checkedEnum != null
        } ?: checked()
    }

    override fun initialize(constraintAnnotation: EnumCheck) {
        this.annotation = constraintAnnotation
    }

    private fun checked(): Boolean {
        if(annotation.permitNull) {
            return true
        }
        return false
    }

}

@Documented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_SETTER)
@ReportAsSingleViolation
@Constraint(validatedBy = [EnumValueValidator::class])
annotation class EnumCheck(
    val enumClazz: KClass<out Enum<*>>,
    val message: String = "",
    val permitNull: Boolean = false,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
```
이제는 `sort`필드를 `enum class`가 아닌 스트링으로 정의하고 `@EnumCheck`로 검증을 할 것이다.

`QueryPage`의 경우에는 이 필드가 `null`일 수 있기 때문에 이 부분도 체크하도록 한다.

또한 `page`, `size`에 대해서도 `validator`를 통해 검증하도록 하자.

이제는 유효성 체크하는 유틸을 하나 만들자.

```kotlin
fun <T> validate(any: T) {
    val violations: Set<ConstraintViolation<T>> = Validation.buildDefaultValidatorFactory().validator.validate(any)
    if (violations.isNotEmpty()) {
        throw BadParameterException(violations.joinToString(separator = ", ") { "[${it.message}]" })
    }
}
```
여러 개일 수 있기 때문에 유효성에 걸린 메세지 정보를 묶어서 보내주도록 하자.

기존의 로직을 건드리지 않는 방법은 `QueryPage`객체에 위임하는 것이다.

```kotlin
data class QueryPage(
    @field:Min(1, message = "페이지 정보는 0보다 커야 합니다.")
    val page: Int? = 1,
    @field:Min(1, message = "사이즈 정보는 0보다 커야 합니다.")
    val size: Int? = 10,
    val column: String? = null,
    @field:EnumCheck(enumClazz = Sort.Direction::class, permitNull = true, message = "sort 필드는 DESC, ASC 만 가능합니다.")
    val sort: String? = null,
) {
    private val offset : Int
        get() = this.page!! - 1

    private val limit  : Int
        get() = this.size!!

    val currentPage: Int
        get() = this.page!!

    fun fromPageable(): PageRequest {
        val sorted = if (column != null && sort != null) Sort.by(Sort.Direction.valueOf(sort!!.uppercase()), column) else Sort.unsorted()
        return PageRequest.of(offset, limit, sorted)
    }

    fun pagination(match: Query): Query {
        val sorted = if (column != null && sort != null) Sort.by(Sort.Direction.valueOf(sort.uppercase()), column) else Sort.unsorted()
        return match.offset(offset.toLong())
            .limit(limit)
            .sort(sorted)
    }

    companion object {
        fun fromServerResponse(request: ServerRequest): QueryPage {
            val map = request.queryParams()
            val queryPage = QueryPage(
                page = map["page"]?.first()?.toInt() ?: 1,
                size =  map["size"]?.first()?.toInt() ?: 10,
                column = map["column"]?.firstOrNull(),
                sort = map["sort"]?.firstOrNull(),
            )
            validate(queryPage)
            return queryPage
        }
    }

}
```
최종적으로는 다음과 같이 `fromServerResponse`함수내에서 처리하도록 변경을 한다.

그리고 `fromPageable`과 `pagination`함수에서는 스트링을 `Sort.Direction`으로 캐스팅해주자.

이 때 우리가 만든 `@EnumCheck`의 검증 로직에서는 대소문자를 무시하고 비교하기 때문에 여기서는 대문자로 변경해서 매핑을 하자.

어짜피 `QueryPage`에 대한 유효성 검증을 통과해야하기 때문에 이 부분에서만 수정을 해주면 크게 바꿀 일이 없어진다.

그러면 뮤지션, 레코드 부분에서 추가적인 작업을 거치지 않아도 될 것이다.

뮤지션/레코드의 생성과 업데이트 정보는 폼 데이터로 넘어온 값을 `bodyToMono`를 통해서 `Mono`로 얻게 되는데 이 부분은 유효성 검증을 할 수 있는 방법은 다음과 같다.

이 중에 레코드 부분만 설명한다. 

전체 적용한 것은 코드를 확인해 보시면 될 것이다.
```kotlin
data class CreateRecord(
    @field:Min(1, message = "뮤지션 아이디가 누락되었습니다.")
    val musicianId: Long,
    @field:NotNull(message = "음반명이 누락되었습니다. 최소 한 글자 이상이어야 합니다.")
    @field:NotBlank(message = "음반명에 빈 공백은 허용하지 않습니다.")
    val title: String,
    @field:NotNull(message = "레이블 정보가 누락되었습니다.")
    @field:NotBlank(message = "레이블에 빈 공백은 허용하지 않습니다.")
    val label: String,
    @field:NotNull(message = "음반 형태 정보가 누락되었습니다.")
    @field:EnumCheck(enumClazz = ReleasedType::class, message = "releasedType 필드는 SINGLE, FULL, EP, OST, COMPILATION, LIVE, MIXTAPE 만 가능합니다.")
    val releasedType: String,
    @field:Min(0, message = "음반 발매일이 누락되었습니다.")
    val releasedYear: Int,
    @field:NotNull(message = "음반 포맷 형식이 누락되엇습니다.")
    @field:NotBlank(message = "음반 포맷 형식에 빈 공백은 허용하지 않습니다.")
    val format: String,
) {
    fun toEntity(): Record {
        return Record(
            musicianId = musicianId,
            title = title,
            label = label,
            releasedType = ReleasedType.valueOf(releasedType.uppercase()),
            releasedYear = releasedYear,
            format = format,
        )
    }
}

data class UpdateRecord(
    val title: String? = null,
    val label: String? = null,
    @field:EnumCheck(enumClazz = ReleasedType::class, permitNull = true, message = "releasedType 필드는 SINGLE, FULL, EP, OST, COMPILATION, LIVE, MIXTAPE 만 가능합니다.")
    val releasedType: String? = null,
    val releasedYear: Int? = null,
    val format: String? = null,
) {
    fun createAssignments(record: Record): Pair<Record, MutableMap<SqlIdentifier, Any>> {
        val assignments = mutableMapOf<SqlIdentifier, Any>()
        title?.let {
            isParamBlankThrow(it)
            assignments[SqlIdentifier.unquoted("title")] = it
            record.title = it
        }
        label?.let {
            isParamBlankThrow(it)
            assignments[SqlIdentifier.unquoted("label")] = it
            record.label = it
        }
        releasedType?.let {
            assignments[SqlIdentifier.unquoted("releasedType")] = it
            record.releasedType = ReleasedType.valueOf(it.uppercase())
        }
        releasedYear?.let {
            assignments[SqlIdentifier.unquoted("releasedYear")] = it
            record.releasedYear = it
        }
        format?.let {
            isParamBlankThrow(it)
            assignments[SqlIdentifier.unquoted("format")] = it
            record.format = it
        }
        return record to assignments
    }
}
```

이 후에는 다음과 같이 `flatMap`을 통해서 비지니스 로직을 타기 전에 검증 유틸 함수를 통해 먼저 검증을 진행하는 방식을 취할 수 있다.

```kotlin
@Service
class WriteMusicianHandler(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    fun insert(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(CreateMusician::class.java)
                      .flatMap {
                            validate(it)
                            write.create(it.toEntity())
                      }
                      .flatMap {
                            ServerResponse.created(URI.create("/api/v1/musicians/${it.id}"))
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .body(it.toMono(), Musician::class.java)
                      }
    }

    fun update(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id").toLong()
        return request.bodyToMono(UpdateMusician::class.java)
                      .flatMap {
                          validate(it)
                          read.musicianByIdOrThrow(id)
                              .flatMap { musician ->
                                  val (musician, assignments) = it.createAssignments(musician)
                                  write.update(musician, assignments)
                              }.then(read.musicianById(id))
                      }
                      .flatMap {
                            ServerResponse.ok()
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .body(it.toMono(), Musician::class.java)
                      }
    }

}
```
이렇게 하면 `bodyToMono`로 얻어 온 객체를 검증해서 유효성 검사를 진행하고 그 결과에 따라 필요한 정보를 클라이언트에 알려줄 수 있게 된다.

또한 이외에도 먼저 알수 없는 케이스를 만날 수 있다.

지금처럼 스트링으로 받고 `@EnumCheck`를 통해 유효성 검증을 하는 것이 아닌 말 그대로 기존의 방식을 취하겠다면 다음과 같이 설정해야 한다.

컨트롤러의 경우에는 `jackson`을 사용할 때 발생하는 에러를 `DecodingException`을 통해 보여준다.

그리고 이 에러의 원인을 `InvalidFormatException`에서 찾아서 메세지를 전달하는 방식이다.

하지만 `functional endpoints`에서는 좀 다르게 작동한다.

`ServerWebInputException`에러가 발생을 하게 되고 그 에러의 원인을 `DecodingException`에서 찾게 된다.

아래는 그것을 적용한 코드이다.

```kotlin
@Component
@Order(-2)
class GlobalExceptionHandler(
    errorAttributes: ErrorAttributes,
    serverCodecConfigurer: ServerCodecConfigurer,
    applicationContext: ApplicationContext,
): AbstractErrorWebExceptionHandler(errorAttributes, WebProperties.Resources(), applicationContext) {

    init {
        super.setMessageWriters(serverCodecConfigurer.writers);
        super.setMessageReaders(serverCodecConfigurer.readers);
    }

    private val log = logger<GlobalExceptionHandler>()

    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return RouterFunctions.route(RequestPredicates.all(), rendering)
    }

    val rendering = HandlerFunction { request ->
        when (val ex = super.getError(request)) {
            is NotFoundException, is MissingInformationException -> {
                log.error(ex.message)
                ServerResponse.status(HttpStatus.NOT_FOUND)
                              .bodyValue(ApiError(code = HttpStatus.NOT_FOUND.value(), message = ex.message!!))
            }
            is BadParameterException -> {
                log.error(ex.message)
                ServerResponse.status(HttpStatus.BAD_REQUEST)
                              .bodyValue(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = ex.message!!))
            }
            is ServerWebInputException -> {
                log.error(ex.message)
                val enumMessage = Pattern.compile("not one of the values accepted for Enum class: \\[([^\\]]+)]")
                if (ex.cause != null && ex.cause is DecodingException) {
                    val matcher: Matcher = enumMessage.matcher(ex.cause!!.message)
                    if (matcher.find()) {
                        return@HandlerFunction ServerResponse.status(HttpStatus.BAD_REQUEST)
                                                             .bodyValue(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = "enum value should be: " + matcher.group(1)))
                    }
                }
                ServerResponse.status(HttpStatus.BAD_REQUEST)
                              .bodyValue(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = ex.message!!))
            }
            else -> {
                log.error(ex.message, ex)
                ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                              .bodyValue(ApiError(code = HttpStatus.INTERNAL_SERVER_ERROR.value(), message = ex.message!!))
            }
        }
    }

}
```

# At a Glance
솔직히 `functional endpoints`의 장점은 잘 모르겠다.

물론 기존 방식의 `useCase`가 핸들러처럼 작동을 하고 `RouterFunction`을 빈으로 올려서 컨트롤러를 대처한다는 점이 존재한다.

단순하게 컨트롤러가 사라진 것을 장점이라고 해야 할지는 잘 모르겠다. 

그만큼 컨트롤러에서 처리할 수 있는 많은 부분들을 손수 작업을 해줘야 하는 불편함이 존재하기 때문이다.

어째든 어떤 방식을 채택할지는 뭐 선택의 자유이고 나름대로의 장점이 있을 것이다.

다음은 코루틴을 활용한 방식을 작업할 에정이다.