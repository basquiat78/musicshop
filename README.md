# MusicShop Using Controller With Coroutine Extension

이 방식은 기존의 프로듀서인 `Flux`, `Mono`가 아닌 일반적인 `Web MVC`방식과 거의 흡사하다.

# Before

이런 방식을 사용하기 위해서는 코틀린에서 제공하는 라이브러리를 먼저 그레이들에 설정하자.

```groovy
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.spring") version "1.8.21"
}

group = "io.basquiat"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    implementation("io.r2dbc:r2dbc-proxy:1.1.1.RELEASE")
    
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.github.jasync-sql:jasync-r2dbc-mysql:2.1.24")
    
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit-vintage-engine")
    }

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

```
`io.projectreactor.kotlin:reactor-kotlin-extensions`, `org.jetbrains.kotlinx:kotlinx-coroutines-reactor`을 추가한다.

그리고 테스트를 위한 `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1`도 같이 설정하자.

# BaseRepository 설정

이 방식을 사용할 때 몇 가지 추가적인 부분이 있다.

기존 방식을 유지하기 위해서는 `R2dbcRepository`가 아닌 `CoroutineCrudRepository`을 사용해야 한다.

따라서 다음과 같이 공통으로 사용할 녀석을 정의하자.

```kotlin
@NoRepositoryBean
interface BaseRepository<M, ID>: CoroutineCrudRepository<M, ID>, CoroutineSortingRepository<M, ID>
```

# CustomCrudRepositoryExtensions 수정

기존에 우리가 사용하던 코드이다.

```kotlin
fun <T, ID> R2dbcRepository<T, ID>.findByIdOrThrow(id: ID, message: String? = null): Mono<T> {
    return this.findById(id)
               .switchIfEmpty { notFound(message?.let{ it } ?: "Id [$id]로 조회된 정보가 없습니다.") }

}
```

하지만 이제는 이것을 사용하지 않고 아래 것을 사용하자.

```kotlin
suspend fun <T, ID> CoroutineCrudRepository<T, ID>.findByIdOrThrow(id: ID, message: String? = null): T {
    return this.findById(id) ?: notFound(message)
}
```
여기서 `suspend`가 붙은 것을 볼 수 있다.

또한 `Mono`가 아닌 일반적인 `Web MVC`의 패턴을 따라간다.

실제로 `CoroutineCrudRepository`를 보면

```kotlin
@NoRepositoryBean
interface CoroutineCrudRepository<T, ID> : Repository<T, ID> {
	suspend fun <S : T> save(entity: S): T
	fun <S : T> saveAll(entities: Iterable<S>): Flow<S>
	fun <S : T> saveAll(entityStream: Flow<S>): Flow<S>
	suspend fun findById(id: ID): T?
	suspend fun existsById(id: ID): Boolean
	fun findAll(): Flow<T>
	fun findAllById(ids: Iterable<ID>): Flow<T>
	fun findAllById(ids: Flow<ID>): Flow<T>
	suspend fun count(): Long
	suspend fun deleteById(id: ID)
	suspend fun delete(entity: T)
	suspend fun deleteAllById(ids: Iterable<ID>)
	suspend fun deleteAll(entities: Iterable<T>)
	suspend fun <S : T> deleteAll(entityStream: Flow<S>)
	suspend fun deleteAll()
}
```
위 코드를 보면 `suspend`가 붙은 경우와 아닌 경우를 볼 수 있다.

이것은 반환되는 타입에 따라 달라지는데 컬렉션의 경우에는 `Flow`로 감싸는 것을 알 수 있다.

하지만 이것도 제공하는 `API`를 통해서 다루게 된다.

# 차라리 새로 만든다는 느낌으로 시작하자.

기존 코드가 변경되면서 대부분의 코드에서 에러가 나기 때문에 차라리 새로 작성한다는 마음으로 시작하는게 좋다.

하지만 기존의 작업했던 디비에 쌓인 데이터를 통해서 먼저 `Read`쪽을 수정하도록 한다.

# MusicianRepository 수정

```kotlin

// 기존 MusicianRepository
interface MusicianRepository: R2dbcRepository<Musician, Long>, CustomMusicianRepository {
    override fun findById(id: Long): Mono<Musician>
    fun findAllBy(pageable: Pageable): Flux<Musician>
}

// 변경된 MusicianRepository
interface MusicianRepository: BaseRepository<Musician, Long>, CustomMusicianRepository {
    override suspend fun findById(id: Long): Musician?
    fun findAllBy(pageable: Pageable): Flow<Musician>
}
```
여기서 `Pageable`을 파라미터로 받는 `findAllBy`의 경우에는 `suspend`가 없고 `Flow`로 감싸진 것을 알 수 있다.

내부적으로 이 경우에는 `suspend fun findAllBy(pageable: Pageable): List<Musician>`처럼 하게 되면

```
IllegalStateException: Method has to use a either 
multi-item reactive wrapper return type or a wrapped Page/Slice type.
```
위와 같은 에러가 난다.

아마도 이런 방식으로 지원을 하지 않는 듯 싶다.

이 때 리스트가 아닌 `Flow`로 감싸서 처리하면 된다.

기존의 만든 [CustomMusicianRepository](https://github.com/basquiat78/musicshop/blob/02-using-controller-record/src/main/kotlin/io/basquiat/musicshop/domain/musician/repository/custom/CustomMusicianRepository.kt)와 [CustomMusicianRepositoryImpl](https://github.com/basquiat78/musicshop/blob/02-using-controller-record/src/main/kotlin/io/basquiat/musicshop/domain/musician/repository/custom/impl/CustomMusicianRepositoryImpl.kt)도 변경하자.

```kotlin
interface CustomMusicianRepository {
    suspend fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Musician
    fun musiciansByQuery(match: Query): Flow<Musician>
    suspend fun totalCountByQuery(match: Query): Long
    suspend fun musicianWithRecords(id: Long): Musician?
}

class CustomMusicianRepositoryImpl(
    private val query: R2dbcEntityTemplate,
): CustomMusicianRepository {

    override suspend fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Musician {
        return query.update(Musician::class.java)
                    .matching(query(where("id").`is`(musician.id!!)))
                    .apply(Update.from(assignments))
                    .thenReturn(musician)
                    .awaitSingle()
    }

    override fun musiciansByQuery(match: Query): Flow<Musician> {
        return query.select(Musician::class.java)
                    .matching(match)
                    .flow()
    }

    override suspend fun totalCountByQuery(match: Query): Long {
        return query.select(Musician::class.java)
                    .matching(match)
                    .count()
                    .awaitSingle()
    }

    override suspend fun musicianWithRecords(id: Long): Musician? {
        var sql = """
            SELECT musician.id,
                   musician.name,
                   musician.genre,
                   musician.created_at,         
                   musician.updated_at,         
                   record.id AS recordId,
                   record.title,
                   record.label,
                   record.released_type,
                   record.released_year,
                   record.format,
                   record.created_at AS rCreatedAt,
                   record.updated_at AS rUpdatedAt
            FROM musician
            LEFT OUTER JOIN record ON musician.id = record.musician_id
            WHERE musician.id = :id
        """.trimIndent()

        return query.databaseClient
                    .sql(sql)
                    .bind("id", id)
                    .fetch()
                    .all()
                    .bufferUntilChanged { it["id"] }
                    .map { rows ->
                        val musician = Musician(
                            id = rows[0]["id"]!! as Long,
                            name = rows[0]["name"]!! as String,
                            genre = Genre.valueOf(rows[0]["genre"]!! as String),
                            createdAt = rows[0]["created_at"]?.let { it as LocalDateTime },
                            updatedAt = rows[0]["updated_at"]?.let { it as LocalDateTime },
                        )
                        val records = rows.map {
                            Record(
                                id = it["recordId"]!! as Long,
                                musicianId = rows[0]["id"]!! as Long,
                                title = it["title"]!! as String,
                                label = it["label"]!! as String,
                                releasedType = ReleasedType.valueOf(it["released_type"]!! as String),
                                releasedYear = it["released_year"]!! as Int,
                                format = it["format"]!! as String,
                                createdAt = it["rCreatedAt"]?.let { row -> row as LocalDateTime },
                                updatedAt = it["rUpdatedAt"]?.let { row -> row as LocalDateTime },
                            )
                        }
                        musician.records = records
                        musician
                    }
                    .awaitFirst()
    }

}

```

여기서도 마찬가지로 `suspend`를 붙여준다.

이 때 각 쿼리 로직 이후 `awaitXXX`같은 함수를 통해서 무언가를 처리하고 있다.

또한 `Flux`의 경우에는 `flow()`함수를 통해서 `Flow<T>`로 처리하는 것을 알 수 있다.

이와 관련 `Wawit.kt`에서 해답을 찾을 수 있다.
```kotlin
public suspend fun <T> Publisher<T>.awaitFirst(): T = awaitOne(Mode.FIRST)
public suspend fun <T> Publisher<T>.awaitFirstOrDefault(default: T): T = awaitOne(Mode.FIRST_OR_DEFAULT, default)
public suspend fun <T> Publisher<T>.awaitFirstOrNull(): T? = awaitOne(Mode.FIRST_OR_DEFAULT)
public suspend fun <T> Publisher<T>.awaitFirstOrElse(defaultValue: () -> T): T = awaitOne(Mode.FIRST_OR_DEFAULT) ?: defaultValue()
public suspend fun <T> Publisher<T>.awaitLast(): T = awaitOne(Mode.LAST)
public suspend fun <T> Publisher<T>.awaitSingle(): T = awaitOne(Mode.SINGLE)
@Deprecated public suspend fun <T> Publisher<T>.awaitSingleOrDefault(default: T): T = awaitOne(Mode.SINGLE_OR_DEFAULT, default)
@Deprecated public suspend fun <T> Publisher<T>.awaitSingleOrNull(): T? = awaitOne(Mode.SINGLE_OR_DEFAULT)
@Deprecated public suspend fun <T> Publisher<T>.awaitSingleOrElse(defaultValue: () -> T): T = awaitOne(Mode.SINGLE_OR_DEFAULT) ?: defaultValue()
// more private fun
```
`Publisher<T>`로 부터 `T`객체를 얻는 방식이 마치 코루틴 빌더 `async`와 유사하다.

즉 발행자인 `Mono`, `Flux`로부터 비동기적으로 객체를 가져온다고 생각하면 쉽다.

해당 코드를 따라가다보면 `suspendCancellableCoroutine`를 활용하고 있는데 방식은 `pub/sub`방식으로 처리하고 있는 것을 알 수 있다.

결국 마치 `jpa`나 `queryDSL`처럼 쿼리 이후 가져오는 타입에 따라 `fetch`, `fetchOne`처럼 사용하고 있기 때문에 크게 어려움이 없다.

이제는 서비스 레이어의 코드를 수정해 보자.

# ReadMusicianService 수정

```kotlin
@Service
class ReadMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    fun musicians(pageable: Pageable) = musicianRepository.findAllBy(pageable)
    suspend fun musicianById(id: Long) = musicianRepository.findById(id)
    suspend fun musicianByIdOrThrow(id: Long, message: String? = null) = musicianRepository.findByIdOrThrow(id, message)
    suspend fun totalCount() = musicianRepository.count()
    fun musiciansByQuery(match: Query) = musicianRepository.musiciansByQuery(match)
    suspend fun totalCountByQuery(match: Query) = musicianRepository.totalCountByQuery(match)
    suspend fun musicianWithRecords(id: Long) = musicianRepository.musicianWithRecords(id)
}
```
기존과의 차이점은 `suspend`가 붙는 다는 점과 `Mono`나 `Flux`아 아닌 객체를 다룰 수 있게 된다는 점이다.

다만 `Pageable`을 사용한 `findAllBy(pageable: Pageable)`함수의 경우에는 `Flow<T>`로 받는다.

이때는 `suspend`가 붙지 않아도 된다.

여기서 `toList()`를 통해 `Iterable<T>`형식으로 변환이 가능하지만 `domain`패키지의 경우에는 이대로 사용한다.

`useCase`작성시 `toList()`로 처리할지 또는 그냥 `Flow<T>`로 컨트롤러를 통해 클라이언트로 보내줄지 결정하도록 하자.

이제부터 이것이 잘 작동하는지 테스트 코드를 작성해 보자.

앞서 우리는 이 테스트를 위해서 그레이들에 `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")`을 설정했다.

최신 버전은 이 `README.md`이 작성된 시점에 `1.7.1`버전이 최신 버전이다.

여기서 제공하는 `runTest{}`을 통해서 테스팅을 진행한다.

```kotlin
@SpringBootTest
class ReadMusicianServiceTest @Autowired constructor(
    private val read: ReadMusicianService,
) {

    @Test
    @DisplayName("fetch musician by id")
    fun musicianByIdTEST() = runTest{
        // given
        val id = 1L

        // when
        val selected = read.musicianById(id)

        // then
        assertThat(selected!!.name).isEqualTo("Charlie Parker")
    }

    @Test
    @DisplayName("fetch musician by id or throw")
    fun musicianByIdOrThrowTEST() = runTest{
        // given
        //val id = 1L
        val id = 1111L

        // when
        val selected = read.musicianByIdOrThrow(id)

        // then
        assertThat(selected!!.name).isEqualTo("Charlie Parker")

    }

    @Test
    @DisplayName("fetch musicians pagination")
    fun musiciansTEST() = runTest{
        // given
        val pageable = PageRequest.of(0, 3)

        // when
        val musicians = read.musicians(pageable)
                            .toList()
                            .map { it.name }
        // then
        assertThat(musicians.size).isEqualTo(3)
        assertThat(musicians[0]).isEqualTo("Charlie Parker")
    }

    @Test
    @DisplayName("total musician count test")
    fun totalCountTEST() = runTest{
        // when
        val count = read.totalCount()

        // then
        assertThat(count).isEqualTo(10)
    }

    @Test
    @DisplayName("musicians list by query test")
    fun musiciansByQueryTEST() = runTest{

        val list = emptyList<Criteria>()

        // given
        val match = query(Criteria.from(list)).limit(2).offset(0)

        // when
        val musicians: List<String> = read.musiciansByQuery(match)
                                          .toList()
                                          .map { it.name }

        // then
        assertThat(musicians.size).isEqualTo(2)

    }

    @Test
    @DisplayName("total musician count by query test")
    fun totalCountByQueryTEST() = runTest{
        // given
        val match = query(where("genre").isEqual("JAZZ"))

        // when
        val count = read.totalCountByQuery(match)

        // then
        assertThat(count).isEqualTo(4)
    }

    @Test
    @DisplayName("musician with records test")
    fun musicianWithRecordsTEST() = runTest{
        // given
        val id = 10L

        // when
        val musician = read.musicianWithRecords(id) ?: notFound()

        // then
        assertThat(musician.name).isEqualTo("스윙스")
        assertThat(musician.records!!.size).isEqualTo(5)

    }

}

```
마치 일반 `Web MVC`을 사용할 때와 크게 다르지 않다는 것을 눈치챘을 것이다.

이제는 이것을 기반으로 `useCase`를 작성해 보자.

# ReadMusicianUseCase 수정

이제부터는 대충 느낌이 올것이다.

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
위 기존의 코드를 바꿔보도록 하자.

하지만 `musiciansByQuery`에서 사용하고 있는 `zipWith`는 `Mono`에서 제공하는 `API`로 일반적인 코틀린의 컬렉션 함수에서는 존재하지 않는다.

게다가 코틀린의 컬렉션 함수나 자바의 `Stream API`에서 제공하는 `zip`나 `zipWithNext`는 `zipWith`와는 작동 방식이 다르다.

우리는 이것을 이런 방식으로 처리하고 싶은 욕망이 생긴다.

따라서 확장 함수를 통해서 이것을 정의해서 사용하자.

이 방식에 특화된 커스텀 확장 함수를 하나 작성하자.

`CustomExtensions.kt`
```kotlin
fun <T, R> Iterable<T>.countZipWith(other: R): Pair<Iterable<T>, R> {
    return this to other
}

fun <T, R, S> Pair<Iterable<T>, R>.map(transformer: (Pair<Iterable<T>, R>) -> S): S {
    return transformer(this)
}
```
`countZipWith`는 페이징 처리를 위해서는 리스트 정보와 전체 카운트 정보를 통해 처리하고 자 할 테니 `Pair`로 반환한다.

따라서 `Pair`에 대해 확장 함수도 같이 작성을 해줘야 한다.

아래는 이것을 적용한 최종 `ReadMusicianUseCase`이다.

```kotlin
@Service
class ReadMusicianUseCase(
    private val read: ReadMusicianService,
) {

    suspend fun musicianById(id: Long): Musician {
        return read.musicianByIdOrThrow(id)
    }

    suspend fun musiciansByQuery(queryPage: QueryPage, matrixVariable: MultiValueMap<String, Any>): Page<Musician> {
        val match = createQuery(matrixVariable)
        return read.musiciansByQuery(queryPage.pagination(match))
                   .toList()
                   .countZipWith(read.totalCountByQuery(match))
                   .map { ( musicians, count) -> PageImpl(musicians.toList(), queryPage.fromPageable(), count)}
    }

}
```
테스트 코드는 기존과 크게 다르지 않기 때문에 완료된 테스트 코드는 확인해 보면 될 듯 싶다.

# WriteMusicianService 수정

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
이제는 너무 익숙해지기 시작한다.

```kotlin
@Service
class WriteMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    suspend fun create(musician: Musician): Musician {
        return musicianRepository.save(musician)
    }
    suspend fun update(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Musician {
        return musicianRepository.updateMusician(musician, assignments)
    }
}
```
당연히 우리는 테스트 코드를 수행해야 한다.

이 때 `rollBack`은 기존의 코드를 좀 수정해야 한다.

```kotlin
@Component
class Transaction (
    transactionalOperator: TransactionalOperator
) {
    init {
        Companion.transactionalOperator = transactionalOperator
    }
    companion object {
        lateinit var transactionalOperator: TransactionalOperator
        suspend fun <T, S> withRollback(value: T, receiver: suspend (T) -> S): S {
            return transactionalOperator.executeAndAwait {
                it.setRollbackOnly()
                receiver(value)
            }
        }

        suspend fun withRollback(receiver: suspend () -> Unit) {
            return transactionalOperator.executeAndAwait {
                it.setRollbackOnly()
                receiver()
            }
        }
    }
}
```
이것을 이용해 롤백 테스트를 진행하자.

```kotlin
@SpringBootTest
class WriteMusicianServiceTest @Autowired constructor(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    @Test
    @DisplayName("musician create test")
    fun createMusicianTEST() = runTest {
        // given
        val createdMusician = Musician(name = "taasaaa", genre = Genre.HIPHOP)
        
        // when
        val musician = Transaction.withRollback(createdMusician) { 
            write.create(it) 
        }
        
        // then
        assertThat(musician.id).isGreaterThan(0)
    }

    @Test
    @DisplayName("musician update using builder test")
    fun updateMusicianTEST() = runTest {
        // given
        val id = 1L
        
        val command = UpdateMusician(name = "Charlie Parker", genre = "POP")
        
        val target = read.musicianByIdOrThrow(1)
        
        val (musician, assignments) = command.createAssignments(target)
        
        // when
        val update = Transaction.withRollback(id) {
            write.update(musician, assignments)
            read.musicianById(id)!!
        }
        
        // then
        assertThat(update.genre).isEqualTo(Genre.POP)
    }

}
```

# WriteMusicianUseCase 수정 및 MusicianController 완성

```kotlin
@Service
class WriteMusicianUseCase(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    suspend fun insert(command: CreateMusician): Musician {
        val created = Musician(name = command.name, genre = Genre.valueOf(command.genre))
        return write.create(created)
    }

    suspend fun update(id: Long, command: UpdateMusician): Musician {
        val selected = read.musicianByIdOrThrow(id)
        val (musician, assignments) = command.createAssignments(selected)
        write.update(musician, assignments)
        return read.musicianById(id)!!
    }

}

@RestController
@RequestMapping("/api/v1/musicians")
class MusicianController(
    private val readMusicianUseCase: ReadMusicianUseCase,
    private val writeMusicianUseCase: WriteMusicianUseCase,
) {

    @GetMapping("/query/{queryCondition}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun fetchMusicians(
        @Valid queryPage: QueryPage,
        @MatrixVariable(pathVar = "queryCondition", required = false) matrixVariable: MultiValueMap<String, Any>
    ): Page<Musician> {
        return readMusicianUseCase.musiciansByQuery(queryPage, matrixVariable)
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun fetchMusician(@PathVariable("id") id: Long): Musician {
        return readMusicianUseCase.musicianById(id)
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createMusician(@RequestBody @Valid command: CreateMusician): Musician {
        return writeMusicianUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun updateMusician(@PathVariable("id") id: Long, @RequestBody command: UpdateMusician): Musician {
        return writeMusicianUseCase.update(id, command)
    }

}
```
테스트 코드는 전부 완료했으니 확인을 하면 될 것이다.

# Record 관련 수정 작업

아마도 똑같은 설명을 반복하게 될 것이다.

완성된 코드와 테스트 코드를 살펴보면 될것이라는 한줄로 마무리할까 한다.

# At a Glance

자바하시는 분들에게는 미안하지만 이런 이점을 누릴 수는 없다.

어째든 코틀린의 코루틴을 활용하는 이 방식은 `WebFlux`을 접하는데 상당한 이점을 제공한다.

기존의 방식을 그대로 누릴 수 있기 때문이다.

다음은 마지막으로 `functional endpoints`를 이용한 방식을 다루고자 한다.

이미 지금까지 잘 따라오신 분들이라면 이 방식 역시 크게 다르지 않을 것이다.

다만 `FunctionRouter`에서 살짝 변경될 것인데 이마저도 바꿀 게 없다.

그 내용은 다음 브랜치에서 확인해 보자.