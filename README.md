# MusicShop Using Controller Version

먼저 가장 익숙한 컨트롤러 방식으로 진행한다.

## create Musician API

음반 가게를 운영한다는 가정하에 작성할 것이다.

먼저 뮤지션 정보를 담을 필요가 있다. 

뮤지션의 기본적인 정보는 이름과 해당 뮤지션의 음악 장르를 담는다.

해당 뮤지션에 대한 유니크 키는 `Long`으로 잡는다. 물론 `uuid`도 무방하다.

```roomsql
-- musicshop.musician definition
CREATE TABLE `musician` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `genre` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```
많은 정보들을 담을 수 있겠지만 확장은 언제든지 할 수 있기 때문에 가장 기본적인 정보만을 담는 간략한 테이블로 설정한다.     

### QueryLoggingListener 설정

`WebFlux`에서 일반적인 방식으로는 쿼리 로깅을 할 수 없다.

물론 

```yml
logging:
  level:
    root: info
    org:
      springframework:
        r2dbc: DEBUG
```
처럼 하면 가능하긴 하지만 원하는 쿼리 로깅을 할 수 없다. 

이 부분은 다음 저장소에서 확인 가능하다.

[r2dbc proxy](https://github.com/basquiat78/r2dbc-proxy-and-mysql)

### Repository 설정

`JPA`와 유사하다. 

이미 이전에도 이와 관련 저장소를 통해서 소개했던 만큼 그 구조를 가져갈 생각이다.

```kotlin
@Table("musician")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Musician(
    @Id
    var id: Long? = null,
    var name: String,
    var genre: Genre?,
    @Column("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var createdAt: LocalDateTime? = now(),
    @Column("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var updatedAt: LocalDateTime? = null,
)

interface MusicianRepository: ReactiveCrudRepository<Musician, Long> {
    fun findAllBy(pageable: Pageable): Flux<Musician>
}
```
그중에 뮤지션 리스트를 가져올 수 있기 때문에 페이징 처리 가능한 `API`도 설정한다.

### CustomCrudRepositoryExtensions

코틀린의 확장 함수를 활용해서 몇가지 커스텀 가능한 `API`를 제공할 수 있다.

```kotlin
fun <T, ID> CrudRepository<T, ID>.findByIdOrThrow(id: ID, message: String? = null): T = this.findByIdOrNull(id) ?: throw IllegalArgumentException(message)
```
만일 어떤 데이터를 조회했을 때 `null`을 반환하는 것이 아니라 에러를 던져 전역 에러 핸들러를 통해서 클라이언트에 관련 내용을 전달 할 필요가 있다.

예를 들면 어떤 정보를 업데이트하기 위해 해당 정보를 가져올 때 비어있다면 정보의 아이디값이 잘못되었거나 없을 수 있다.

이때는 클라이언트에 메세지를 던질 필요가 있다.

하지만 `WebFlux`에서 `reactor`상에서 에러를 던질 때 방식이 달라진다.

이것을 토대로 다음과 같이 작성 가능하다.

```kotlin
class NotFoundException(message: String? = "조회된 정보가 없습니다.") : RuntimeException(message)

fun <T, ID> ReactiveCrudRepository<T, ID>.findByIdOrThrow(id: ID): Mono<T> {
    return this.findById(id)
               .switchIfEmpty { Mono.error(NotFoundException("your error message")) }

}
```
즉 비어 있다면 `Mono.error()`를 통해서 던지도록 작성한다.

이 때 `jpa`처럼 `findByIdOrNull`을 제공하지 않기 때문에 `WebFlux`에서 제공하는 `switchIfEmpty`를 통해 처리하도록 작성하면 된다.     

### 전역 에러 핸들러

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): Mono<ApiError> {
        return Mono.just(ApiError(code = HttpStatus.NOT_FOUND.value(), message = ex.message!!))
    }
}
```
지금처럼 `MVC`패턴에서 사용하는 `@RestControllerAdvice`를 통해서 전역 에러 처리를 할 수 있다.

### 가벼운 방식의 CQRS를 통한 서비스 분리

서비스를 `CRUD`의 `R`과 `CUD`를 분리해서 작성한다.

```kotlin
@Service
class ReadMusicianService(
    private val musicianRepository: MusicianRepository,
) {

    fun musicians(pageable: Pageable) = musicianRepository.findAllBy(pageable)

    fun musicianById(id: Long) = musicianRepository.findById(id)

    fun musicianByIdOrThrow(id: Long) = musicianRepository.findByIdOrThrow(id)

    fun totalCount() = musicianRepository.count()

}

@Service
class WriteMusicianService(
    private val musicianRepository: MusicianRepository,
) {

    fun create(musician: Musician): Mono<Musician> {
        return musicianRepository.save(musician)
    }

    fun update(musician: Musician): Mono<Musician> {
        return musicianRepository.save(musician)
    }

}
```

`ReadMusicianService`에서 `findById`와 확장 함수로 생성한 `findByIdOrThrow`로 나눈 이유는 하나이다.

기본적으로 해당 `API`를 사용할 때 전역으로 처리해야 하는 경우 외에 비지니스 로직내에서 던지지 말아야 하는 경우도 발생할 수 있다.

그것을 고려해 2개의 `API`를 제공하는 방식을 취한다.

## Create Musician Test

`WebFlux`상에서 테스트 코드 작업시 `insert/update`의 경우에는 테스트 이후 롤백을 해야 하는 경우가 발생한다.

하지만 일반적인 방식으로 `@Transactional`을 사용해서는 불가능하다.

성공 여부를 떠나 에러가 떨어지기 때문이다.

[Reactive Transactions with Spring](https://spring.io/blog/2019/05/16/reactive-transactions-with-spring)

[Introduce reactive @Transactional support in the TestContext framework #24226](https://github.com/spring-projects/spring-framework/issues/24226)

위 내용들을 토대로 보면 테스트 코드가 끝날 때 이벤트를 통해서 롤백을 하는 방식을 취하고 있다.    

언젠가는 `@Transactional`같은 것을 통해서 위와 같이 하지 않고 되기를 기대하면서 깃헙에서 답변에 제공된 코드를 한번 살펴보자.

```kotlin
@Component
class Transaction(
    transactionalOperator: TransactionalOperator
) {
    init {
        Companion.transactionalOperator = transactionalOperator
    }
    companion object {
        lateinit var transactionalOperator: TransactionalOperator
        fun <T> withRollback(publisher: Mono<T>): Mono<T> {
            return transactionalOperator.execute { tx ->
                tx.setRollbackOnly()
                publisher
            }.next()
        }
        fun <T> withRollback(publisher: Flux<T>): Flux<T> {
            return transactionalOperator.execute { tx ->
                tx.setRollbackOnly()
                publisher
            }
        }
    }
}
```
코드를 보면 `ReactiveTransaction`를 통해 롤백 세팅을 하고 발행자를 통해 이벤트를 발생하는 형식이다.     

```kotlin
@SpringBootTest
@TestExecutionListeners(
	listeners = [TransactionalTestExecutionListener::class],
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class WriteMusicianServiceTest @Autowired constructor(
	private val write: WriteMusicianService,
) {

    @Test
    @DisplayName("musician create test")
    fun createMusicianTEST() {
        // given
        val createdMusician = Musician(name = "Charlie Parker", genre = Genre.JAZZ)
    
        // when
        val mono = write.create(createdMusician)
    
        // then
        mono.`as`(Transaction::withRollback)
            .`as`(StepVerifier::create)
            .assertNext {
                assertThat(it.id).isGreaterThan(0L)
            }
            .verifyComplete()
    }

}
```
다음과 같이 코드를 작성하면 테스트 이후 롤백이 잘 수행되는 것을 알 수 있다.

## fetch Musician API Test

뮤지션 정보를 생성하는 `API`를 테스트 했으니 저장된 뮤지션 정보를 가져오는 `API`를 테스트하는 코드를 작성해 보자.


```kotlin
@SpringBootTest
class ReadMusicianServiceTest @Autowired constructor(
private val read: ReadMusicianService,  
) {

    @Test
    @DisplayName("fetch musician by id")
    fun musicianByIdTEST() {
        // given
        val id = 1L
        
        // when
        val selected = read.musicianById(id)
        
        // then
        selected.`as`(StepVerifier::create)
                .assertNext {
                    assertThat(it.name).isEqualTo("Charlie Parker")
                }
                .verifyComplete()
    }
    
    @Test
    @DisplayName("fetch musician by id or throw")
    fun musicianByIdOrThrowTEST() {
        // given
        //val id = 1L
        val id = 1111L
        
        // when
        val selected = read.musicianByIdOrThrow(id)
        
        // then
        selected.`as`(StepVerifier::create)
                .assertNext {
                    assertThat(it.name).isEqualTo("Charlie Parker")
                }
                .verifyComplete()
    }

}
```
2개의 `API`를 테스트해보자. 

그 중에 `musicianByIdOrThrow`의 경우에는 의도적으로 존재하지 않을 아이디 값을 넘겨서 에러가 제대로 던져지는지 확인하는 테스트도 진행해 본다.

## update Musician API

위 테스트에서 롤백하는 부분을 주석처리 이후 몇개의 데이터를 넣어두고 업데이트를 위한 `API`를 생성해 보자.

업데이트의 경우에는 `ReactiveCrudRepository`에서 제공하는 방식과 `queryDSL`처럼 다른 방식으로 처리를 하고자 다음과 같이 클래스를 생성한다.

```kotlin

interface CustomMusicianRepository {
    fun updateMusician(user: Musician): Mono<Musician>
}

class CustomMusicianRepositoryImpl(
    private val query: R2dbcEntityTemplate,
): CustomMusicianRepository {
    override fun updateMusician(musician: Musician): Mono<Musician> {
        return query.update(musician)
    }
}

interface MusicianRepository: ReactiveCrudRepository<Musician, Long>, CustomMusicianRepository {
    override fun findById(id: Long): Mono<Musician>
    fun findAllBy(pageable: Pageable): Flux<Musician>
}

@Service
class WriteMusicianService(
    private val musicianRepository: MusicianRepository,
) {

    fun create(musician: Musician): Mono<Musician> {
        return musicianRepository.save(musician)
    }

    fun update(musician: Musician): Mono<Musician> {
        return musicianRepository.save(musician)
    }

    fun updateTemplate(musician: Musician): Mono<Musician> {
        return musicianRepository.updateMusician(musician)
    }

}
```
먼저 그냥 엔티티 자체로 업데이트하는 코드를 먼저 작성한다.

기존에 만든 `MusicianRepository`에 `implement`를 하고 서비스쪽에도 `API`를 작성한다.      

### update Musician Test

그렇다면 이제 해당 서비스를 검증해 보자.

```kotlin
@SpringBootTest
@TestExecutionListeners(
    listeners = [TransactionalTestExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class WriteMusicianServiceTest @Autowired constructor(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    @Test
    @DisplayName("musician create test")
    fun createMusicianTEST() {
        // given
        val createdMusician = Musician(name = "스윙스1123", genre = Genre.HIPHOP)

        // when
        val mono = write.create(createdMusician)
        
        // then
        mono.`as`(Transaction::withRollback)
            .`as`(StepVerifier::create)
            .assertNext {
                assertThat(it.id).isGreaterThan(0L)
            }
            .verifyComplete()
    }

    @Test
    @DisplayName("musician update test")
    fun updateMusicianTEST() {
        // given
        val id = 1L
        val selected = read.musician(1)
        
        // when
        val updated = selected.flatMap {
        it.name = "Charlie Parker"
        it.updatedAt = now() // test : 1
            write.update(it)
        }
    
        // then
        updated.`as`(StepVerifier::create)
               .assertNext {
                    assertThat(it.updatedAt).isNotNull()
               }
               .verifyComplete()
    }
}
```

주어진 아이디 값으로 타겟 뮤지션 정보를 가져온 이후 업데이트할 정보를 세팅 이후 단순하게 `MusicianRepository`의 `save`함수를 통해서 업데이트 하는 방식이다.

### more update code

하지만 위 코드에서 업데이트시 몇가지 문제가 발생한다.

테이블 생성시 `updated_at`컬럼은 업데이트시 생성되도록 되어 있다.      

하지만 쿼리가 날아가는 것을 보게 되면 

```
Query:["UPDATE musician SET name = ?, genre = ?, created_at = ?, updated_at = ? WHERE musician.id = ?"] 
```
다음과 같이 날아가기 때문에 `test : 1`같이 처리하지 않으면 `null`로 들어가기 때문에 `updated_at`에 `null`로 들어간다.

물론 그렇다면 `created_at`에 `LocalDateTime.now()`로 세팅해주면 되겠지만 DB의 기능을 사용하고 싶은 욕망이 생긴다.

`jpa`처럼 `@DynamicUpdate`같은 것을 제공하지 않기 때문에 다른 방식으로 접근해야 한다.

이번 작업은 빌더를 이용해서 업데이트 쿼리를 짤 생각이다.

그 중에 `Update`를 활용한 로직을 짤 생각이다.

하지만 다음과 같이

```kotlin
// 그냥 예제코드
fun update() {
    Update.update("name", musician.name).set("genre", musician.genre)
}
```
위와 같은 경우에는 동적으로 작업하기가 조금 불편하다.

```kotlin
val updateBuilder = Update.builder()
updateBuilder.set("column", value)
```
이렇게 제공이 된다면 좋겠지만 아쉽게도 제공하지 않는다.

어째든 `Update`클래스는 `private`생성자를 사용하는 불변함수이기 때문에 주어진 메소드만 사용이 가능하다.

따라서 `from`메소드를 통해 동적으로 맵 정보를 넘겨서 처리할 생각이다.

```kotlin
interface CustomMusicianRepository {
    fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician>
}

class CustomMusicianRepositoryImpl(
    private val query: R2dbcEntityTemplate,
): CustomMusicianRepository {

    override fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician> {
        return query.update(Musician::class.java)
                    .matching(query(where("id").`is`(musician.id!!)))
                    .apply(Update.from(assignments))
                    .thenReturn(musician)
    }
}
```
다음과 같이 인터페이스를 하나 정의하고 이것을 구현한 구현체를 작성한다.

테스트 코드를 통해서 이것을 이제 확인해 보자.

```kotlin
@SpringBootTest
@TestExecutionListeners(
    listeners = [TransactionalTestExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class WriteMusicianServiceTest @Autowired constructor(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    @Test
    @DisplayName("musician update test")
    fun updateTemplateMusicianTEST() {
        // given
        val id = 1L
        //		val name: String? = "Charlie Parkers"
        //		val genre: Genre? = Genre.JAZZ
        val name: String? = null
        val genre: Genre? = null
        
        val assignments = mutableMapOf<SqlIdentifier, Any>()
        name?.let { assignments[SqlIdentifier.unquoted("name")] = it }
        genre?.let { assignments[SqlIdentifier.unquoted("genre")] = it }
    
        if(assignments.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다.")
        }
    
        val target = read.musician(1)
        
        // when
        val updated = target.flatMap {
            write.updateTemplate(it, assignments)
        }.then(read.musician(1))
        
        // then
        updated.`as`(StepVerifier::create)
               .assertNext {
                    assertThat(it.genre).isEqualTo(Genre.JAZZ)
               }
               .verifyComplete()
    }
}
```

다만 `jpa`처럼 업데이트 이후에 엔티티 정보가 같이 변경되는 형식이 아니기 때문에 한번 더 셀렉트를 하는 방식으로 처리를 한다.

당연히 다음과 같이 인터페이스 구현체에서 업데이트 이후 업데이트 된 정보를 쿼리 셀렉트 해서 처리할 수 있다.

예를 들면 다음과 같이

```kotlin
class CustomMusicianRepositoryImpl(
    private val query: R2dbcEntityTemplate,
): CustomMusicianRepository {

    override fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician> {
        return query.update(Musician::class.java)
                    .matching(query(where("id").`is`(musician.id!!)))
                    .apply(Update.from(assignments))
                    .then()
                    .then(
                        query.select(Musician::class.java)
                             .matching(query(where("id").`is`(musician.id!!)))
                             .one()
                    )
    }

}
```

`then()`을 통해서 업데이트 이후 `then(Mono<V> other)`를 통해서 업데이트한 쿼리를 위처럼 처리할 수 있다.

하지만 때론 업데이트 이후 업데이트가 왼료되었다는 단순 메세지를 보내는 경우도 있고 업데이트가 완료된 엔티티 정보를 보낼 수 있다.

만일 업데이트 성공 여부 단순 메세지만를 보내는 경우에도 셀렉트를 한번 더 날릴 필요가 있을까?

정답은 없다.

다만 여기서는 필요에 따라서 가져오거나 할 수 있도록 하기 위에서 `thenReturn`으로 처리한다.

그리고 비지니스 로직단에서 필요하다면 테스트 케이스처럼 `then(read.musician(1))`코드로 처리하도록 한다.

이렇게 하면 불필요한 쿼리를 꼭 날리지 않게 처리할 수 있다.

### advanced Update Test Code

업데이트를 하기 위해 다음과 같이 리퀘스트를 날린다고 생각해 보자.

```kotlin
PATCH '/v1/musicians/1'
```
이 때 업데이터 정보를 `@RequestBody`로 받을 것이기 때문에 `UpdateMusician`클래스를 하나 정의하자.

```kotlin
data class UpdateMusician(
    val name: String?,
    val genre: Genre?,
) {
    fun createAssignments(): MutableMap<SqlIdentifier, Any> {
        val assignments = mutableMapOf<SqlIdentifier, Any>()
        name?.let { assignments[SqlIdentifier.unquoted("name")] = it }
        genre?.let { assignments[SqlIdentifier.unquoted("genre")] = it }
        if(assignments.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
        }
        return assignments
    }
}
```
객체내에서 처리할 수 있도록 하고 로직단에서는 함수를 가져와 쓰도록 처리하자.

뮤지션 테이블의 컬럼이 확장되거나 할 때 좀 더 유연하게 대처할 수 있도록 한다.


```kotlin
@SpringBootTest
@TestExecutionListeners(
    listeners = [TransactionalTestExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class WriteMusicianServiceTest @Autowired constructor(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    @Test
    @DisplayName("musician update test")
    fun updateTemplateMusicianTEST() {
        // given
        val id = 1L
        
        val command = UpdateMusician(name = "Charlie Parker", genre = Genre.POP)
        val assignments = command.createAssignments()
        
        val target = read.musicianByIdOrThrow(1)
        
        // when
        val updated = target.flatMap {
            write.updateTemplate(it, assignments)
        }.then(read.musicianById(1))
    
        // then
        updated.`as`(StepVerifier::create)
               .assertNext {
                    assertThat(it.genre).isEqualTo(Genre.POP)
               }
               .verifyComplete()
    }

}
```
`assignments`정보를 먼저 체크해서 오류가 있으면 에러를 던지자. 

그래야 불필요한 쿼리를 타지 않고 클라이언트로 에러 메세지를 전달할 수 있기 때문이다.

또한 타겟 뮤지션 정보를 가져올때 `musicianByIdOrThrow`를 사용하자.

아이디 정보는 있지만 디비에 조회되는 내용이 없다면 이것 역시 에러 처리를 통해 클라이언트에 알려주도록 처리하자.

최종적으로 업데이트에 대해서는 빌더를 활용한 커스텀 레파지토리를 사용하도록 한다.

```kotlin
@Service
class WriteMusicianService(
    private val musicianRepository: MusicianRepository,
) {

    fun create(musician: Musician): Mono<Musician> {
        return musicianRepository.save(musician)
    }

    fun update(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician> {
        return musicianRepository.updateMusician(musician, assignments)
    }
}
```

## WriteMusicianUseCase 작성하기

이제는 비지니스 로직을 처리할 `usecase`를 작성해보자.

```kotlin
data class CreateMusician(
    @field:NotNull
    @field:Size(min = 2, message = "뮤지션의 이름이 누락되었습니다. 최소 한 글자 이상이어야 합니다.")
    val name: String,
    @field:NotNull(message = "장르 정보가 누락되었습니다.")
    val genre: Genre?,
)

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
        // 셀렉트 이전에 command 정보를 확인해서 유효성 검사를 하자.
        val assignments = command.createAssignments()
        val targetSource = read.musicianOrThrow(id)
        return targetSource.flatMap { musician ->
            write.update(musician, assignments)
        }.then(read.musician(id))
    }
}
```
업데이트 코드에서 `flatMap`을 사용하는 이유가 궁금한 분이 있을지 모르겠다.

예를 들면 `lodash`같은 `js`라이브러리나 `Stream API`를 사용하다 보면 자주 볼 수 있는데 도대체 어떤 상황에서 사용할지 모르는 경우를 자주 봐왔다.

```java
public final <R> Mono<R> map(Function<? super T, ? extends R> mapper) {
    if (this instanceof Fuseable) {
        return onAssembly(new MonoMapFuseable<>(this, mapper));
    }
    return onAssembly(new MonoMap<>(this, mapper));
}

public final <R> Mono<R> flatMap(Function<? super T, ? extends Mono<? extends R>> transformer) {
    return onAssembly(new MonoFlatMap<>(this, transformer));
}
```
이 두 메소드를 가만히 살펴보면 `map`의 `mapper`의 형태와 `flatMap`의 `transformer`의 형식이 다르다는 것을 알 수 있다.

`mapper`의 경우에는 그 결과가 반환되는 객체라는 점이다.

근데 만일 `map`내부의 어떤 `API`를 호출할때 해당 결과가 `Mono<Object>`라면 어떤 일이 벌어질까?

최종적인 결과는 우리의 예상과는 달리 `Mono<Mono<Object>>`같은 형식이 되게 된다.

위 코드에서 보면

```kotlin
fun update(id: Long, command: UpdateMusician): Mono<Musician> {
    // 셀렉트 이전에 command 정보를 확인해서 유효성 검사를 하자.
    val assignments = command.createAssignments()
    val targetSource = read.musicianOrThrow(id)
    return targetSource.flatMap { musician ->
        write.update(musician, assignments)
    }.then(read.musician(id))
}
```
요 부분에서 `write.update(musician, assignments)`가 반환하는 값은 `Mono<Musician>`이다.

만일 `flatMap`을 사용하지 않는다면 이 결과는 `Mono<Mono<Musician>>`이라는 요상한 형식이 된다.

실제로 저 위의 코드를 아래처럼 하면

```kotlin
fun update(id: Long, command: UpdateMusician): Mono<Musician> {
    // 셀렉트 이전에 command 정보를 확인해서 유효성 검사를 하자.
    val assignments = command.createAssignments()
    val targetSource = read.musicianOrThrow(id)
    val result: Mono<Mono<Musician>> = targetSource.map { musician ->
        write.update(musician, assignments)
    }
//
//
//
//    return targetSource.flatMap { musician ->
//        write.update(musician, assignments)
//    }.then(read.musician(id))
}
```
여러분의 `IDE`에서는 위와 같이 보여준게 된다. 

`Musician`의 요소를 접근하고자 한다면 한번 더 타고 들어가는 코드를 만들 상황이 생긴다.

따라서 이것을 평탄화해 줄 필요가 있는데 이 때 `flatMap`을 사용하는 것이다.

`Stream API`를 사용하다 보면 `map`내부에서 `API`를 호출해 정보를 가져오는 경우가 있다.

이 때 `API`의 반환값에 따라서 `List<List<String>>`같은 형식을 가지는 경우가 있다.

이런 이유로 `List<String>`을 원소로 갖는 길이 1의 리스트가 생길 수 있는데 생각해 봐라. 

리스트의 첫번 째 요소를 가져와서 해당 리스트를 다시 순회해야 하는 이상한 코드를 짜야 하는 경우가 생긴다.

만일 이런 `flatMap`의 기능을 모른다면 여러분은 1개의 원소를 갖는 리스트에서 첫 번째 인덱스를 가져와 순회하는 코드를 짤 수 밖에 없다.

어째든 앞서 작업한 `UpdateMusician`처럼 뮤지션을 생성할 `CreateMusician`객체를 정의한다.

여기서 `enum`필드와 관련된 유효성 처리를 위해서 다음과 같은 로직을 사용할 수 있다는 것을 기술 블로그에서 찾아볼 수 있었다.

```kotlin
@Documented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_SETTER)
@ReportAsSingleViolation
@Constraint(validatedBy = [EnumValueValidator::class])
annotation class EnumCheck(
    val enumClazz: KClass<out Enum<*>>,
    val values: Array<String>,
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class EnumValueValidator: ConstraintValidator<EnumCheck, Enum<*>> {

    private var valueList: Array<Enum<*>> = arrayOf()

    override fun isValid(value: Enum<*>, context: ConstraintValidatorContext): Boolean {
        return valueList.contains(value)
    }

    override fun initialize(constraintAnnotation: EnumCheck) {
        val enumClass: Class<out Enum<*>> = constraintAnnotation.enumClazz.java
        val enumValueList = constraintAnnotation.values.toList()
        val enumValues: Array<Enum<*>> = enumClass.enumConstants as Array<Enum<*>>
        try {
            valueList = enumValues.filter { enum -> enumValueList.contains(enum.name) }
                                  .toTypedArray()
        } catch (e: Exception) {
            throw BadParameterException()
        }
    }
}
```
자바와 일반적인 Spring Boot MVC 패턴 내에서 가능한지 체크해 보지 않았지만 현재 이 프로젝트에서는 해당 유효성 체크를 타지 않는다.

그 이전에 이미 `jackon`라이브러리에서 `decode`에러가 발생한다.

곰곰히 생각해보면 당연한 것이 아닐까 생각이 든다.   

해당 어노테이션을 이용해서 `@Valid`를 사용한다 해도 그 전에 `enum`필드와 관련해 `deserialize`할 때 에러가 발생하지 않을까?

먼저 리퀘스트로 들어온 json 정보를 `deserialize`한 이후 유효성 검사를 진행할 텐데 이미 유효성 검사를 타기 이전에 에러가 발생할 것이 자명하다.

스택오버플로우에서도 이와 관련 내용을 찾아볼 수 있는데 고전적인 방식이지만 필요한 부분을 정규식으로 찾아서 처리하는 방법으로 할수 밖에 없다.

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): Mono<ApiError> {
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
}
```
`DecodingException`이 발생할 때 위 코드와 같이 관련 에러가 날때 발생하는 특정 에러 부분을 정규화해서 처리한다.

차후 포스트맨을 통해서 테스트할 때 이 부분도 체크할 예정이다.

어째든 `WriteMusicianUseCase`는 결국 컨트롤러로 넘어온 파라미터를 처리하는 로직이 담길 것이다.

### WriteMusicianUseCase Test

```kotlin
@SpringBootTest
@TestExecutionListeners(
    listeners = [TransactionalTestExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class WriteMusicianUseCaseTest @Autowired constructor(
    private val writeUseCase: WriteMusicianUseCase,
    private val readMusicianService: ReadMusicianService,
) {
    
    @Test
    @DisplayName("musician insert useCase test")
    fun insertUseCaseTEST() {
        // given
        val command = CreateMusician(name = "Charlie Mingus", genre = Genre.JAZZ)
        
        // when
        val mono = writeUseCase.insert(command)
        // then
        mono.`as`(StepVerifier::create)
            .assertNext {
                assertThat(it.genre).isEqualTo(Genre.JAZZ)
            }
            .verifyComplete()
    }
    
    @Test
    @DisplayName("musician update useCase test")
    fun updateUseCaseTEST() {
        // given
        val id = 9L
        val command = UpdateMusician(name = "Charles Mingus", genre = null)
        
        // when
        val mono = writeUseCase.update(id, command)
        
        // then
        mono.`as`(StepVerifier::create)
            .assertNext {
                assertThat(it.name).isEqualTo(command.name)
            }
            .verifyComplete()
    }
    
}
```
### Update UseCase Modify

코틀린의 `tuple`를 이용해서 다른 방식으로 코드를 한번 작성해 볼까 한다.

이유는 현재 `update`시 `thenReturn`으로 넘어온 뮤지션 정보는 어떤 정보도 변경되지 않은 정보이다.      

물론 `then`을 통해서 갱신된 뮤지션 정보를 가져오면 되거나 단순 업데이트 성공 메세지를 보내주면 그만이다.

하지만 이걸 좀 다른 방식으로 접근해 보자.

```kotlin
data class UpdateMusician(
    val name: String?,
    val genre: Genre?,
) {
    fun createAssignments(musician: Musician): Pair<Musician, MutableMap<SqlIdentifier, Any>> {
        val assignments = mutableMapOf<SqlIdentifier, Any>()
        name?.let {
            assignments[SqlIdentifier.unquoted("name")] = it
            musician.name = it
        }
        genre?.let {
            assignments[SqlIdentifier.unquoted("genre")] = it
            musician.genre = it
        }
        if(assignments.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
        }
        return musician to assignments
    }
}
```
기존 `UpdateMusician`객체의 `createAssignments`함수를 수정하자.     

뮤지션 정보를 받고 파라미터 정보를 체크하면서 같이 값을 세팅해 준다.

이때 `tuple`로 값을 넘겨주자.

```kotlin
@Service
class WriteMusicianUseCase(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {
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
물론 이 방식은 일단 갱신할 뮤지션 정보를 가져오고 난 이후 리퀘스트로 넘어온 정보의 유효성 검사를 하는 방식이다.      

튜플로 뮤지션의 정보와 업데이트 쿼리 부분의 정보를 받아 업데이트를 하는 방식이다.

이때 `assignments`의 검증 여부에 따라 에러를 던지게 되면 `onErrorResume`을 통해 에러를 받아 처리하는 방식이다.

둘 중의 어느 방식을 사용하더라도 개인적으로 괜찮은 방식이라고 생각한다.       

## Musician Controller

이제는 뮤지션을 생성하고 업데이트하는 컨트롤러를 만들어 보자.

가장 익숙한 방법인 만큼 크게 특별하진 않다.

```kotlin
@Validated
@RestController
@RequestMapping("/api/v1/musicians")
class MusicianController(
    private val writeMusicianUseCase: WriteMusicianUseCase,
) {

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMusician(@RequestBody @Valid command: CreateMusician): Mono<Musician> {
        return writeMusicianUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateMusician(@PathVariable("id") id: Long, @RequestBody command: UpdateMusician): Mono<Musician> {
        return writeMusicianUseCase.update(id, command)
    }

}
```

### Controller Test

물론 서버를 띄우고 포스트맨을 통해서 충분히 테스트를 해볼 수 있지만 `WebClient`를 사용해서 테스트를 한번 작성해 보자.

```kotlin
@SpringBootTest
@AutoConfigureWebTestClient
class MusicianControllerTest @Autowired constructor(
    private val webTestClient: WebTestClient,
) {

    @Test
    @DisplayName("musician create test")
    fun createMusicianTEST() {
        val createMusician = CreateMusician(name = "스윙스", genre = Genre.JAZZ)
        webTestClient.post()
                     .uri("/api/v1/musicians")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .body(Mono.just(createMusician), CreateMusician::class.java)
                     .exchange()
                     .expectStatus().isCreated
                     .expectHeader().contentType(MediaType.APPLICATION_JSON)
                     .expectBody()
                     .jsonPath("$.name").isEqualTo("스윙스")
    }

    @Test
    @DisplayName("musician update test")
    fun updateMusicianTEST() {
        val update = UpdateMusician(name = null, genre = Genre.HIPHOP)
        webTestClient.patch()
                     .uri("/api/v1/musicians/10")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .body(Mono.just(update), UpdateMusician::class.java)
                     .exchange()
                     .expectStatus().isOk
                     .expectHeader().contentType(MediaType.APPLICATION_JSON)
                     .expectBody()
                     .jsonPath("$.genre").isEqualTo("HIPHOP")
    }

}
```
이 때 `WebTestClient`를 주입받기 위해서는 위 코드에서 볼 수 있듯이 `@AutoConfigureWebTestClient`설정을 해주면 된다.

## musician 리스트 페이징 처리

이제는 하나 남은 뮤지션 리스트 정보를 페이징 처리하는 법을 알아보자.

먼저 `jpa`처럼 `Pageable`을 이용하는 방식을 먼저 알아보자. 

하지만 `jpa`와는 다른 방식으로 작동한다.

예를 들면 `jpa`의 경우에는 `count`쿼리를 알아서 날려준다.     

몰룬 이 `count`쿼리를 튜닝할 수 있는 여지가 있다면 

```kotlin
@Query(countQuery = "{튜닝가능한 카운트 쿼리}")
```
처럼 처리가능하다.

하지만 `R2DBC`에서는 이러한 방식을 지원하지 않는다.      

결국 대응하는 카운트 쿼리를 작성해서 따로 날려줘야 한다.     

먼저 우리는 어떤 검색 조건없이 전체 뮤지션 리스트를 가져오고자 한다.

앞서 작업했던 `ReadMusicianService`를 살펴보자.

```kotlin
@Service
class ReadMusicianService(
    private val musicianRepository: MusicianRepository,
) {

    fun musicians(pageable: Pageable) = musicianRepository.findAllBy(pageable)

    fun musicianById(id: Long) = musicianRepository.findById(id)

    fun musicianByIdOrThrow(id: Long) = musicianRepository.findByIdOrThrow(id)

    fun totalCount() = musicianRepository.count()

}

interface MusicianRepository: ReactiveCrudRepository<Musician, Long>, CustomMusicianRepository {
    override fun findById(id: Long): Mono<Musician>
    fun findAllBy(pageable: Pageable): Flux<Musician>
}
```

물론 컨트롤러 단에서 쿼리파람으로 넘어온 정보를 `Pageable`객체로 직접 매핑하는 방법도 있다.

이 때는 설정을 통해 `ReactivePageableHandlerMethodArgumentResolver`을 등록해야 한다.      

하지만 나의 경우에는 `page`정보를 0이 아닌 1를 적용해서 객체내에서 0으로 처리하도록 하는 방식을 선택할 것이다.

만일 첫 번째 페이지를 0으로 하자는 약속이 있다면 위 방식을 통해서 처리할 수 있지만 코드로 직접 다루는게 좋은 나의 경우에는 객체를 직접 정의 했다.

또한 정렬 정보도 받아서 처리하고자 한다.

먼저 우리는 이와 관련 나머지 테스트를 한번 작성해 보기 전에 페이징 관련 리퀘스트 객체를 하나 작성해 볼까 한다.

```kotlin
data class Query(
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

}
```
그리고 기존의 테스트 코드를 추가하자.

```kotlin
@SpringBootTest
class ReadMusicianServiceTest @Autowired constructor(
    private val read: ReadMusicianService,
) {

    @Test
    @DisplayName("fetch musician by id")
    fun musicianByIdTEST() {
        // given
        val id = 1L
        
        // when
        val selected = read.musicianById(id)
        
        // then
        selected.`as`(StepVerifier::create)
                .assertNext {
                    assertThat(it.name).isEqualTo("Charlie Parker")
                }
                .verifyComplete()
    }
    
    @Test
    @DisplayName("fetch musician by id or throw")
    fun musicianByIdOrThrowTEST() {
        // given
        //val id = 1L
        val id = 1111L
        
        // when
        val selected = read.musicianByIdOrThrow(id)
        
        // then
        selected.`as`(StepVerifier::create)
                .assertNext {
                    assertThat(it.name).isEqualTo("Charlie Parker")
                }
                .verifyComplete()
    }
    
    @Test
    @DisplayName("total musician count test")
    fun totalCountTEST() {
        // when
        val count: Mono<Long> = read.totalCount()
        
        // then
        count.`as`(StepVerifier::create)
             .assertNext {
                 // 현재 5개의 row가 있다.
                 assertThat(it).isEqualTo(5)
             }
             .verifyComplete()
    }
    
    @Test
    @DisplayName("musicians list test")
    fun musiciansTEST() {
        // given
        // 2개의 정보만 가져와 보자.
        val query = Query(1, 2)
        
        // when
        val musicians: Flux<String> = read.musicians(query.fromPageable())
                                          .map { it.name }
        
        // then
        musicians.`as`(StepVerifier::create)
                 .expectNext("Charlie Parker")
                 .expectNext("John Coltrane")
    
    }
    
}
```

## ReadMusicianUseCase 작성하기

이제는 이와 관련 비지니스 로직을 처리할 `usecase`를 작성하자.

```kotlin
@Service
class ReadMusicianUseCase(
    private val read: ReadMusicianService,
) {

    fun all(query: Query): Mono<Page<Musician>> {
        val pageable = query.fromPageable()
        return read.musicians(pageable)
                   .collectList()
                   .zipWith(read.totalCount())
                   .map { tuple -> PageImpl(tuple.t1, pageable, tuple.t2) }
    }

    fun musicianById(id: Long): Mono<Musician> {
        return read.musicianByIdOrThrow(id)
    }
}
```
클라이언트 요청에 의해 페이징 처리된 리스트 정보를 받을 때는 페이징 관련 정보도 같이 주는 것이 일반적이다.   

이 때 `Reactive Stream`의 흐름을 따라서 이것을 작성하도록 한다.

이 코드에서 보면 페이징 처리한 결과값에서 `collectList`를 통해서 하나의 `Flux`를 모으고 `Flux`연산자가 제공하는 `zipWith`를 통해서 뮤지션의 전체 카운트를 가져온다.

`zipWith`는 처음 작업한 뮤지션의 `Flux`와 해당 메소드에서 실행된 전체 카운트 `Mono`를 튜플 형식으로 반환한다.

코드를 보면 알겠지만 `map`에서 `tuple`을 받아서 `PageImpl`을 통해서 `Mono<Page<Musician>>`형태로 반환한다.

일반적으로 `Page`의 결과 형태는 다음과 같다.

```json
{
    "content": [
    ],
    "pageable": {
        "sort": {
            "empty": true,
            "sorted": false,
            "unsorted": true
        },
        "offset": 0,
        "pageNumber": 0,
        "pageSize": 10,
        "paged": true,
        "unpaged": false
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 5,
    "first": true,
    "size": 10,
    "number": 0,
    "sort": {
        "empty": true,
        "sorted": false,
        "unsorted": true
    },
    "numberOfElements": 5,
    "empty": false
}
```
와 같은 형태로 반환한다.

그렇다면 실제로 이 `usecase`에 작성한 2개의 `API`를 한번 테스트 코드를 통해서 검증하자.

```kotlin
@SpringBootTest
class ReadMusicianUseCaseTest @Autowired constructor(
    private val readUseCase: ReadMusicianUseCase,
) {

    @Test
    @DisplayName("musicianById test")
    fun musicianByIdTEST() {
        // given
        val id = 1L
        
        // when
        val mono = readUseCase.musicianById(id)
        
        // then
        mono.`as`(StepVerifier::create)
            .assertNext {
                assertThat(it.name).isEqualTo("Charlie Parker")
            }
            .verifyComplete()
    }
    
    @Test
    @DisplayName("musicians list test")
    fun allTEST() {
        // given
        val query = QueryPagination(1, 10)
        
        // when
        val mono = readUseCase.all(query)
        
        // then
        mono.`as`(StepVerifier::create)
            .assertNext {
                // 전체 row는 5개이므로 
                assertThat(it.totalElements).isEqualTo(5)
            }
            .verifyComplete()
    }
}
```
참고로 테스트는 작성한 개인의 따라 테스트 하는 값을 달라질 것이다.

## 나머지 Controller 작성

```kotlin
@Validated
@RestController
@RequestMapping("/api/v1/musicians")
class MusicianController(
    private val readMusicianUseCase: ReadMusicianUseCase,
    private val writeMusicianUseCase: WriteMusicianUseCase,
) {

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    fun fetchMusicians(@Valid query: Query): Mono<Page<Musician>> {
        return readMusicianUseCase.all(query)
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchMusician(@PathVariable("id") id: Long): Mono<Musician> {
        return readMusicianUseCase.musicianById(id)
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMusician(@RequestBody @Valid command: CreateMusician): Mono<Musician> {
        return writeMusicianUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateMusician(@PathVariable("id") id: Long, @RequestBody command: UpdateMusician): Mono<Musician> {
        return writeMusicianUseCase.update(id, command)
    }

}
```
이제 거의 다 왔다.

마지막으로 나머지 컨트롤러의 `API`에 대한 테스트 코드를 작성하자.

```kotlin
@SpringBootTest
@AutoConfigureWebTestClient
class MusicianControllerTest @Autowired constructor(
    private val webTestClient: WebTestClient,
) {

    @Test
    @DisplayName("musician create test")
    fun createMusicianTEST() {
        // given
        val createMusician = CreateMusician(name = "스윙스", genre = Genre.JAZZ)
        
        // when
        webTestClient.post()
                     .uri("/api/v1/musicians")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .body(Mono.just(createMusician), CreateMusician::class.java)
                     .exchange()
                     .expectStatus().isCreated
                     .expectHeader().contentType(MediaType.APPLICATION_JSON)
                     .expectBody()
                     // then
                     .jsonPath("$.name").isEqualTo("스윙스")
    }

    @Test
    @DisplayName("musician update test")
    fun updateMusicianTEST() {
        // given
        val update = UpdateMusician(name = null, genre = Genre.HIPHOP)

        // when
        webTestClient.patch()
                     .uri("/api/v1/musicians/10")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .body(Mono.just(update), UpdateMusician::class.java)
                     .exchange()
                     .expectStatus().isOk
                     .expectHeader().contentType(MediaType.APPLICATION_JSON)
                     .expectBody()
                     // then
                     .jsonPath("$.genre").isEqualTo("HIPHOP")
    }
    
    @Test
    @DisplayName("fetchMusician test")
    fun fetchMusicianTEST() {
        // given
        val id = 1L

        // when
        webTestClient.get()
                     .uri("/api/v1/musicians/$id")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus().isOk
                     .expectHeader().contentType(MediaType.APPLICATION_JSON)
                     .expectBody()
                     // then
                     .jsonPath("$.name").isEqualTo("Charlie Parker")
    }
    
    @Test
    @DisplayName("fetchMusicians test")
    fun fetchMusiciansTEST() {
        // given
        val page = 1
        val size = 10

        // when
        webTestClient.get()
                     .uri("/api/v1/musicians?page=$page&size=$size")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus().isOk
                     .expectHeader().contentType(MediaType.APPLICATION_JSON)
                     .expectBody()
                     // then
                     .jsonPath("$.last").isEqualTo(true) // row5 -> last is true
    }

}
```

## 빌더를 이용해 뮤지션 리스트를 가져오기

뮤직샵에서 뮤지션들의 정보 또는 앨범 정보를 가져온다면 단순하게 모든 뮤지션의 리스트를 보여줄 수도 있다.    

하지만 뮤지션명(동명 이인), 장르별 또는 한달내에 발행된 신보를 보여주거나 할 때는 지금 제공하는 방식으로는 버겁다.

따라서 이것을 빌더를 이용해서 가져올 생각이다. 

`QueryDSL`과 유사한 면이 있지만 그렇게 친절하진 않다. 

다음은 코드의 예시이다.

```kotlin
interface CustomMusicianRepository {
    fun musiciansByQuery(): Flux<Musician>
    fun totalCountByQuery(): Mono<Long>
}

class CustomMusicianRepositoryImpl(
    private val query: R2dbcEntityTemplate,
): CustomMusicianRepository {

    override fun musiciansByQuery(): Flux<Musician> {
        var init = Query.empty()
        val condition = where("name").like("%윙스%")
                        .and(
                            where("genre").isEqual(Genre.HIPHOP)
                        )
        init = query(condition).limit(10).offset(0)
        return query.select(Musician::class.java)
                    .matching(init)
                    .all()
    }

    override fun totalCountByQuery(): Mono<Long> {
        var init = Query.empty()
        val condition = where("name").isEqual("스윙스")
        init = query(condition)
        return query.select(Musician::class.java)
                    .matching(init)
                    .count()
    }

}
```
하지만 이것은 예시일뿐 우리는 저 `matching()`함수에 들어갈 정보를 파라미터로 받을 예정이다.

따라서 코드는 다음과 같이 간결하게 변경한다.

```kotlin
interface CustomMusicianRepository {
    fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician>
    fun musiciansByQuery(query: Query): Flux<Musician>
    fun totalCountByQuery(query: Query): Mono<Long>
}

class CustomMusicianRepositoryImpl(
    private val query: R2dbcEntityTemplate,
): CustomMusicianRepository {

    override fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician> {
        return query.update(Musician::class.java)
                    .matching(query(where("id").`is`(musician.id!!)))
                    .apply(Update.from(assignments))
                    .thenReturn(musician)
// jpa와 달리 리턴된 엔티티는 업데이트된 정보가 아니기 때문에 한번 더 실렉트를 해오는 방식
//                    .then()
//                    .then(
//                        query.select(Musician::class.java)
//                             .matching(query(where("id").`is`(musician.id!!)))
//                             .one()
//                    )
    }

    override fun musiciansByQuery(match: Query): Flux<Musician> {
        return query.select(Musician::class.java)
                    .matching(match)
                    .all()
    }

    override fun totalCountByQuery(match: Query): Mono<Long> {
        return query.select(Musician::class.java)
                    .matching(match)
                    .count()
    }

}
```
다만 카운트 쿼리의 경우에는 넘겨줄 때 페이징 부분은 제외해서 넘겨주면 된다.


`ReadMusicianService`에 `API`를 추가하자.

```kotlin
@Service
class ReadMusicianService(
    private val musicianRepository: MusicianRepository,
) {

    fun musicians(pageable: Pageable) = musicianRepository.findAllBy(pageable)

    fun musicianById(id: Long) = musicianRepository.findById(id)

    fun musicianByIdOrThrow(id: Long) = musicianRepository.findByIdOrThrow(id)

    fun totalCount() = musicianRepository.count()

    fun musiciansByQuery(match: Query) = musicianRepository.musiciansByQuery(match)

    fun totalCountByQuery(match: Query) = musicianRepository.totalCountByQuery(match)

}
```
먼저 테스트 코드를 여기에 맞춰서 작성을 해보자.

```kotlin
@SpringBootTest
class ReadMusicianServiceTest @Autowired constructor(
    private val read: ReadMusicianService,
) {
    
    @Test
    @DisplayName("musicians list by query test")
    fun musiciansByQueryTEST() {
        // given
        val match = query(where("genre").isEqual(Genre.JAZZ))
        
        // when
        val musicians: Flux<String> = read.musiciansByQuery(
            // page 0, size 2
            match.limit(2)
                 .offset(0)
        )
        .map { it.name }
        
        // then
        musicians.`as`(StepVerifier::create)
                 .expectNext("Charlie Parker")
                 .expectNext("John Coltrane")
                 .verifyComplete()
    
    }
    
    @Test
    @DisplayName("total musician count by query test")
    fun totalCountByQueryTEST() {
        // given
        val match = query(where("name").like("%윙스%"))
        
        // when
        val count: Mono<Long> = read.totalCountByQuery(match)
        
        // then
        count.`as`(StepVerifier::create)
             .assertNext {
                 // 현재 1개의 row가 있다.
                 assertThat(it).isEqualTo(1)
             }
             .verifyComplete()
    }

}
```
참고로 코틀린에서는 `CriteriaStepExtensions.kt`를 제공한다.

예를 들면 `is`, `as`, `in`의 경우에는 코틀린내에서는 그대로 사용이 불가능하다.

`QueryDSL`를 코틀린에서 해본 경험이 있다면 별칭을 주는 `as`의 경우에는 백틱 '`'으로 감싸게 된다.

실제로 여기서도 뮤지션의 정보를 업데이트할때 아이디 값으로 검색할 때 사용하기도 했는데 이것이 은근 불편하다.     

그래서 `isEqual`, `isIn`을 제공한다.

실제 코드로 보면 다음과 같다.

```kotlin
infix fun Criteria.CriteriaStep.isEqual(value: Any): Criteria =
        `is`(value)

fun Criteria.CriteriaStep.isIn(vararg value: Any): Criteria =
        `in`(value)

fun Criteria.CriteriaStep.isIn(values: Collection<Any>): Criteria =
        `in`(values)
```

## @MatrixVariable 를 이용한 조건 검색 사용해 보기

지금같은 검색 조건은 `@ReqeustParam`을 통해서 구현할 수 있다.

또는 `@PostMapping`요청으로 바디에 정보를 담는 경우도 존재한다.

하지만 여기서는 매트릭스 항렬 변수를 사용해 볼까 한다.

다만 생각과는 다르게 `@MatrixVariable`를 사용하기 위해서는 `@PathVariable`과 같이 사용해야 한다.

예를 들면

```
/api/v2/musicians/query;name=like,윙스?page=1&size=10
```
처럼 사용하고 싶은데 되지 않는다는 것을 테스트를 통해서 알게 되엇다.

아쉽긴 하지만 다음과 같은 리퀘스트 `uri`로 받을 수 있도록 해야 한다.

```kotlin
@GetMapping("/query/{queryCondition}")
fun fetchMusicians(@Valid queryPage: QueryPage,
                   @MatrixVariable(pathVar = "queryCondition", required = false) matrixVariable: MultiValueMap<String, Any>): Mono<Page<Musician>> {
    return readMusicianUseCase.musiciansByQuery(queryPage, matrixVariable)
}
```

여기서 `{queryCondition}`이 부분은 요청시 `search`로 고정한다.

다만 `MultiValueMap<String, Any>`을 사용하고 있는데 이 경우에는 매트릭스 항렬 변수가 없으면 맵을 생성하다 `argument mismatch`에러가 발생한다.

따라서 다음과 같은 방식으로 넘기도록 프론트엔드 개발자와 규약을 만들어야 한다.

```
1. 전체 조건없이 페이징처리된 뮤지션 리스트를 가져오고 싶다면

/api/vi/musicians/query/search;all?page=1&size=10

2. 조건 검색을 하고 싶다면 column=조건,값으로 넘긴다.

예를 들면 name을 like검색으로 윙스, genre를 eq검색으로 HIPHOP을 검색하고 싶다면

/api/v1/musicians/query/search;name=like,윙스;genre=eq,HIPHOP?size=10&page=1

처럼 날린다.

위 예제에서 볼 수 있듯이 `;`으로 여러개의 검색 조건을 넘긴다.
;name=like,윙스;genre=eq,HIPHOP
```

`ConditionType`이라는 `enum`클래스를 하나 생성하자.

```kotlin
enum class ConditionType(
    val code: String,
    private val criteria: Function<WhereCondition, Criteria>
) {
    LTE("lte", Function { condition: WhereCondition -> where(condition.column).lessThanOrEquals(condition.value)}),
    LT("lt", Function { condition: WhereCondition -> where(condition.column).lessThan(condition.value)}),
    GTE("gte", Function { condition: WhereCondition -> where(condition.column).greaterThanOrEquals(condition.value)}),
    GT("gt", Function { condition: WhereCondition -> where(condition.column).greaterThan(condition.value)}),
    EQ("eq", Function { condition: WhereCondition -> where(condition.column).isEqual(condition.value)}),
    LIKE("like", Function { condition: WhereCondition -> where(condition.column).like("%${condition.value}%")});

    fun create(condition: WhereCondition): Criteria {
        return criteria.apply(condition)
    }

    companion object {
        /**
         * null이면 illegalArgumentException을 던지고 있지만 ETC를 던져도 상관없다.
         * @param code
         * @return ConditionType
         */
        fun of(code: String): ConditionType = values().firstOrNull { conditionType-> conditionType.code.equals(code, ignoreCase = true) }
            ?: EQ
    }

}
```
기존에 만들어 둔 `Query`클래스를 `QueryPage`로 만들고 다음과 같이 수정을 한다.

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
}

data class WhereCondition(
    val column: String,
    val value: Any,
) {
    companion object {
        fun from(key: String, value: Any): WhereCondition {
            return WhereCondition(
                column = key,
                value = value
            )
        }
    }
}
```
그리고 `MutiValueMap`으로 넘어온 정보를 담을 `WhereCondition`에 만든다.

```kotlin
fun createQuery(matrixVariable: MultiValueMap<String, Any>): Query {
    if(matrixVariable.containsKey("all")) {
        return empty()
    }
    val list = matrixVariable.map { (key, value) ->
        try {
            ConditionType.of(value[0].toString()).create(WhereCondition.from(key, value[1]))
        } catch(e: Exception) {
            throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
        }
    }
    return query(Criteria.from(list))
}
```
최종적으로 `R2DBC`의 빌더에 넘겨줄 `Query`객체를 담을 빌더 하나도 만들어 보자.

위 코드는 

```
;name=like,윙스;genre=eq,HIPHOP
```
처럼 넘어오게 되면 `=`을 기준으로 `key`와 `value`로 담는다.

하지만 `MutiValueMap`사용시 `value`부분을 `,`로 구분하면 `List<Any>`로 담게 되는데 이것을 이용해서 만든 것이 `createQuery`함수이다.

이제는 이것을 통해서 최종적으로 컨트롤러와 `usecase`를 수정해보자.

```kotlin
@Validated
@RestController
@RequestMapping("/api/v1/musicians")
class MusicianController(
    private val readMusicianUseCase: ReadMusicianUseCase,
    private val writeMusicianUseCase: WriteMusicianUseCase,
) {

    @GetMapping("/query/{queryCondition}")
    fun fetchMusicians(@Valid queryPage: QueryPage,
                       @MatrixVariable(pathVar = "queryCondition", required = false) matrixVariable: MultiValueMap<String, Any>): Mono<Page<Musician>> {
        return readMusicianUseCase.musiciansByQuery(queryPage, matrixVariable)
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchMusician(@PathVariable("id") id: Long): Mono<Musician> {
        return readMusicianUseCase.musicianById(id)
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMusician(@RequestBody @Valid command: CreateMusician): Mono<Musician> {
        return writeMusicianUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateMusician(@PathVariable("id") id: Long, @RequestBody command: UpdateMusician): Mono<Musician> {
        return writeMusicianUseCase.update(id, command)
    }

}

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
사실 이 코드에서 `Mono<Page<Musician>>`의 경우에는 페이지 정보가 좀 헤비한 편이다.

여기서는 이대로 사용하고 마지막으로 이 코드에서 페이징 정보를 필요한 정보만 담을 객체로 바꿀 예정이다.

어째든 서버를 띄우고 포스트맨을 통해서 다음과 같이 테스트를 해보자.

```
127.0.0.1:8080/api/v1/musicians/query/search;name=like,윙스;genre=eq,HIPHOP?size=10&page=1

쿼리 로깅
Query:["SELECT COUNT(musician.id) FROM musician WHERE (musician.name LIKE ? AND (musician.genre = ?))"] 
Bindings:[(%윙스%,HIPHOP)] 

{
    "content": [
        {
            "id": 10,
            "name": "스윙스",
            "genre": "HIPHOP",
            "createdAt": "2023-06-05T19:28:31",
            "updatedAt": "2023-06-05T19:34:20"
        }
    ],
    "pageable": {
        "sort": {
            "empty": true,
            "sorted": false,
            "unsorted": true
        },
        "offset": 0,
        "pageNumber": 0,
        "pageSize": 10,
        "paged": true,
        "unpaged": false
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 1,
    "first": true,
    "size": 10,
    "number": 0,
    "sort": {
        "empty": true,
        "sorted": false,
        "unsorted": true
    },
    "numberOfElements": 1,
    "empty": false
}

조건 없이 요청
127.0.0.1:8080/api/v1/musicians/query/search;all?size=10&page=1&column=id&sort=DESC

쿼리 로깅
Query:["SELECT musician.* FROM musician ORDER BY musician.id DESC LIMIT 10"] 

{
    "content": [
        {
            "id": 10,
            "name": "스윙스",
            "genre": "HIPHOP",
            "createdAt": "2023-06-05T19:28:31",
            "updatedAt": "2023-06-05T19:34:20"
        },
        {
            "id": 9,
            "name": "Charles Mingus",
            "genre": "JAZZ",
            "createdAt": "2023-06-04T19:54:00",
            "updatedAt": "2023-06-04T19:57:15"
        },
        {
            "id": 3,
            "name": "Miles Davis",
            "genre": "JAZZ",
            "createdAt": "2023-06-02T17:30:31"
        },
        {
            "id": 2,
            "name": "John Coltrane",
            "genre": "JAZZ",
            "createdAt": "2023-06-02T17:30:21"
        },
        {
            "id": 1,
            "name": "Charlie Parker",
            "genre": "JAZZ",
            "createdAt": "2023-06-02T18:25:37",
            "updatedAt": "2023-06-04T18:19:34"
        }
    ],
    "pageable": {
        "sort": {
            "empty": false,
            "sorted": true,
            "unsorted": false
        },
        "offset": 0,
        "pageNumber": 0,
        "pageSize": 10,
        "paged": true,
        "unpaged": false
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 5,
    "first": true,
    "size": 10,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "numberOfElements": 5,
    "empty": false
}
```
원하는 대로 쿼리가 날아가고 결과값을 받은 것을 알 수 있다.

### enum 체크

포스트맨에서 다음과 같이 

```
1. create musician

POst 127.0.0.1:8080/api/v1/musicians
{
    "name": "Miles Davis",
    "genre": "DESS"
 
}

result
{
    "code": 400,
    "message": "enum value should be: POP, CLASSIC, WORLDMUSIC, ETC, ROCK, JAZZ, HIPHOP"
}


127.0.0.1:8080/api/v1/musicians/query/search;name=like,윙스;?size=10&page=1&column=id&sort=DESCs

result

{
    "code": 400,
    "message": "Sort Direction should be: [DESC, ASC]"
}
```
`enum`과 관련된 에러 처리에 대한 테스트도 진행해보면 에러 전역 처리한 부분이 잘 작동되는 것이 확인된다.

# At a Glance
1차적으로 기본적인 `API`를 작성해 봤다.

다음은 뮤지션들의 음반과 관련된 `API`를 작성해 볼까 한다. 

이때 `converter`등을 활용한 작업들 역시 같이 진행해 볼까 한다.     

