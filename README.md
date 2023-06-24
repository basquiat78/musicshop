# MusicShop integration jooQ

`jooq`와 `queryDSL`은 비슷한 경험을 제공하는 라이브러리이다.

`queryDSL`이 `jpa`와 연계해서 사용하기 용이하기 때문에 같이 묶이긴 하지만 엄연히 말하면 이 둘은 `orm`과는 무관한 쿼리 빌더이다.

믈론 `jooQ`와 `jpa`를 연계해서 사용할 수 있다.

`queryDSL`도 `jpa`와 연계해서 사용한다는 개념이 박혀서 그렇지 실제로 `queryDSL-sql`같은 코어 라이브러러를 통해 순수한 쿼리 빌더로 사용하기도 한다.

하지만 대부분 `orm`을 쓰지 않는 환경내에서 오라클 디비를 사용하지 않는다면 쿼리 빌더로는 `jooQ`가 좀 더 인기있는 듯 하다.

뭐...`jooQ`의 약자가 딱 봐도 `Java Object Oriented Querying`이니 어쩌면 `R2DBC`같은 환경에서는 더할 나위 없는 라이브러리가 아닐까 한다.

하지만 개인적인 생각이니 이 부분은 넘어가자.

# pre work

실제 [Reactive SQL with jOOQ 3.15 and R2DBC](https://blog.jooq.org/reactive-sql-with-jooq-3-15-and-r2dbc/)을 따라가보면 아쉬움 부분이 있다.

`DslContext`을 사용해 쿼리 빌더를 할 때 반환되는 타입은 발행자인 `Mono`, `Flux`로 감싸인 타입이 아니기 때문에 일일히 수작업을 해줘야 한다.

공식 블로그의 코드대로라면 

```
Flux.from(dslContext
        .insertInto(AUTHOR)
        .columns(AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
        .values("John", "Doe")
    .returningResult(AUTHOR.ID)
)
```
처럼 일일히 `Flux`로 변환해 줘야 한다는 것이다.

하지만 해당 프로젝트는 코틀린의 코루틴을 기반으로 사용할 예정이다.

공식 사이트에서 [jooQ Kotlin coroutine support](https://www.jooq.org/doc/latest/manual/sql-building/kotlin-sql-building/kotlin-coroutines/)를 하기 때문에 이런 불필요함이 사라진다.

따라서 위 사이트의 내용을 한번 훝어보면 좋다.

# gradle setting

다음 공식 사이트의 내용을 토대로 작성되었다.

[Running the code generator with Gradle](https://www.jooq.org/doc/latest/manual/code-generation/codegen-gradle/)

여기 보면 `nu.studer.jooq`라는 플러그인을 사용하고 있다.

해당 플로그인 깃헙 주소가 있는데 다음 깃헙의 가이드라인을 따른다.

[gradle-jooq-plugin](https://github.com/etiennestuder/gradle-jooq-plugin)

또한 `jooQ`를 통해서 엔티티 생성시 기존에 만든 엔티티 명과 충돌이 나기 때문에 빌드시 바꿔줘야 한다.

## 최악의 삽질

일반적으로 `queryDSL`의 경우에는 `QType`로 생성하는데 지금 `jooQ`에서는 미리 지정한 `sql_schema`읽어서 엔티티를 생성한다.

작성한 엔티티 객체와 `jooQ`가 생성한 객체명이 같아서 `jooQ`가 관련 객체 생성시 이름을 변경할려고 했다.

예전에 공식 깃헙에 보면 `buildSrc`같은 방식을 통해서 `CustomStrategy`클래스를 정의해서 사용하는 방법을 제시한다.

또는 [gradle-jooq-plugin](https://github.com/etiennestuder/gradle-jooq-plugin)에서 제공하는 멀티 모듈 방식이 있다.

하지만 지금 이 프로젝트의 버전으로는 `buildSrc`의 경우에는 되지 않는다.

물론 `jooQ`에서 예제로 제공하는 `org.jooq.codegen.example.JPrefixGeneratorStrategy`를 사용하면 된다.

하지만 언제 어떻게 될지 모르기 때문에 같은 코드라 할지라도 `CusomtStrategy`를 사용할수 있도록 하자.

따라서 다음과 같이 [configure_custom_generator_strategy](https://github.com/etiennestuder/gradle-jooq-plugin/tree/main/example/configure_custom_generator_strategy)을 활용해 최종적으로 설정하는 방식을 사용해야 한다.

먼저 `root project`에 `custom-strategy`모듈을 생성하자.

그리고 `CustomGeneratorStrategy`를 작성한다.

```java
public class CustomGeneratorStrategy extends DefaultGeneratorStrategy {

    @Override
    public String getJavaClassName(final Definition definition, final Mode mode) {
        return 'J' + super.getJavaClassName(definition, mode);
    }
}
```
`custom-steratey`내의 `build.gradle`은 다음과 같이 심플하다.

```groovy
plugins {
    id 'java'
}

def jooqVersion = "3.18.5"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jooq:jooq-codegen:${jooqVersion}")
}
```
루트의 `settings.gradle.kts`을 다음과 같이 설정한다.

```groovy
plugins {
    id("com.gradle.enterprise") version "3.13"
}

rootProject.name = "musicshop"

include(/* ...projectPaths = */ "custom-strategy")

```
그리고 `build.gradle.kts`는 [gradle-jooq-plugin](https://github.com/etiennestuder/gradle-jooq-plugin)에서 제공하는 예제를 따라한다.

```groovy
import nu.studer.gradle.jooq.JooqEdition
import nu.studer.gradle.jooq.JooqGenerate
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.Property

plugins {
    id("org.springframework.boot") version "3.1.0"
    id("io.spring.dependency-management") version "1.1.0"
    id("nu.studer.jooq") version "8.2.1"
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.spring") version "1.8.21"
}

group = "io.basquiat"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

val jooqVersion = "3.18.5"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-jooq")

	implementation("org.jooq:jooq-meta-extensions:${jooqVersion}")
	implementation("org.jooq:jooq-kotlin:${jooqVersion}")
	implementation("org.jooq:jooq-codegen:${jooqVersion}")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("io.r2dbc:r2dbc-proxy:1.1.1.RELEASE")

	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation("com.github.jasync-sql:jasync-r2dbc-mysql:2.1.24")

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	implementation("io.projectreactor.tools:blockhound:1.0.8.RELEASE")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(module = "junit-vintage-engine")
	}

	project("custom-strategy")

	jooqGenerator("org.jooq:jooq-meta-extensions:${jooqVersion}")
	jooqGenerator("mysql:mysql-connector-java:8.0.33")
	jooqGenerator("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
	jooqGenerator(project("custom-strategy"))
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

jooq {
	version.set("$jooqVersion")
	edition.set(JooqEdition.OSS)

	evaluationDependsOn(":custom-strategy")

	configurations {
		create("main") {
			generateSchemaSourceOnCompilation.set(true)

			jooqConfiguration.apply {
				logging = org.jooq.meta.jaxb.Logging.WARN
				jdbc = null

				generator.apply {
					name = "org.jooq.codegen.KotlinGenerator"
					strategy.apply {
						name = "io.basquiat.strategy.CustomGeneratorStrategy"
					}
					database.apply {
						name = "org.jooq.meta.extensions.ddl.DDLDatabase"
						properties.addAll(
							listOf(
								Property().apply {
									key = "scripts"
									value = "src/main/resources/sql/sql_schema.sql"
								},
								Property().apply {
									key = "sort"
									value = "semantic"
								},
								Property().apply {
									key = "unqualifiedSchema"
									value = "none"
								},
								Property().apply {
									key = "defaultNameCase"
									value = "lower"
								}
							)
						)
					}
					generate.apply {
						isPojosAsKotlinDataClasses = true
					}
					target.apply {
						packageName = "io.basquiat.musicshop.entity"
						directory = "build/generated/jooq/main"
					}
				}
			}
		}
	}
}

tasks.named<JooqGenerate>("generateJooq") {
	allInputsDeclared.set(true)
}

tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq") {
	(launcher::set)(javaToolchains.launcherFor {
		languageVersion.set(JavaLanguageVersion.of(17))
	})
}
```

여기서 확인할 부분이 몇가지 있다.

```groovy
project("custom-strategy") // custom-strategy 프로젝트를 디펜던시로 잡는다.

jooqGenerator("org.jooq:jooq-meta-extensions:${jooqVersion}")
jooqGenerator("mysql:mysql-connector-java:8.0.33")
jooqGenerator("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
jooqGenerator(project("custom-strategy")) // 컴파일시에 jooqGenerator가 해당 프로젝트를 사용할 수 있도록 한다.

// do configuration

strategy.apply {
    name = "io.basquiat.strategy.CustomGeneratorStrategy" // custom-strategy에서 작업한 custom strategy 클래스의 경로 포함 정보
}

database.apply {
    name = "org.jooq.meta.extensions.ddl.DDLDatabase"
    properties.addAll(
            listOf(
                    Property().apply {
                        key = "scripts"
                        value = "src/main/resources/sql/sql_schema.sql" // 정의한 테이블 생성 스키마 파일 경로
                    },
                    Property().apply {
                        key = "sort"
                        value = "semantic"
                    },
                    Property().apply {
                        key = "unqualifiedSchema"
                        value = "none"
                    },
                    Property().apply {
                        key = "defaultNameCase"
                        value = "lower"
                    }
            )
    )
}
generate.apply {
    isPojosAsKotlinDataClasses = true
}
target.apply {
    // generate시 패키지 경로와 build 다이렉트 경로 설정을 하낟.
    packageName = "io.basquiat.musicshop.entity"
    directory = "build/generated/jooq/main"
}
```
터미널에서 직접적으로 다음과 같이 명령어르 날려주던가

```
./gradlew generateJooq
```

또는 인텔리제이의 그레이들 항목에서 `project > Tasks > jooq`경로의 `generateJooq`를 클릭해서 빌드를 진행한다.

빌드가 완료되면 `build`폴더내에 위에서 `target.apply`에서 정의한 `packageName`, `directory`경로로 생성된 코드를 볼 수 있다.

# JooqConfiguration

`JooqConfiguration`을 통해서 `DslContext`을 빈으로 등록하도록 하자.

```kotlin
@Configuration
class JooqConfiguration {

    @Bean
    fun dslContext(connectionFactory: ConnectionFactory) =
        DSL.using(TransactionAwareConnectionFactoryProxy(connectionFactory), SQLDialect.MYSQL)

}
```
이제는 이것을 활용해서 `repositoy`의 코드들을 하나씩 수정해 나가자.

# CustomMusicianRepository 수정

`CustomMusicianRepository`에서 먼저 `updateMusician`을 수정해 보자.

먼저 `jooQ`의 `update`의 경우에는 과거 `PreparedStatement`의 방식을 따른다.

그래서 업데이트의 경과는 성공시 `1`로 넘어오게 되어 있다.

따라서 다음과 같이 수정한다.

```kotlin
interface CustomMusicianRepository {
    suspend fun updateMusician(musicianId: Long, assignments: MutableMap<Field<*>, Any>): Int
}

class CustomMusicianRepositoryImpl(
    private val query: DSLContext,
): CustomMusicianRepository {

    override suspend fun updateMusician(musicianId: Long, assignments: MutableMap<Field<*>, Any>): Int {
        val musician = JMusician.MUSICIAN
        return query.update(musician)
                    .set(assignments)
                    .where(musician.ID.`equal`(musicianId))
                    .awaitSingle()
    }
}

@Service
class WriteMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    suspend fun create(musician: Musician): Musician {
        return musicianRepository.save(musician)
    }
    suspend fun update(musicianId: Long, assignments: MutableMap<Field<*>, Any>): Int {
        return musicianRepository.updateMusician(musicianId, assignments)
    }
}
```
일반적으로 `jooQ`의 업데이트 문법은 다음과 같다.

```kotlin
// 1 or 0
return query.update(musician)
            .set(assignments)
            .where(musician.ID.`equal`(musicianId))
            .execute()
```
여기서 `set`의 경우에는 `key-value`형식으로 `Field`타입으로 감싸진 컬럼과 업데이트 정보로 담아서 보내도 된다.

또는

```
## field로 직접적으로 컬럼을 명시해서 사용하는 방법
query.update(musician)
     .set(field("name"), "Charlie PPPPP")
     .set(field("genre"), "HIPHOP")
     .where(musician.ID.`equal`(musicianId))

## field로 직접적으로 컬럼을 명시해서 사용하는 방법
query.update(musician)
     .set(musician.NAME, "Charlie PPPPP")
     .set(musician.GENRE, "HIPHOP")
     .where(musician.ID.`equal`(musicianId))
```
이 방법으로 처리해도 상관없다.

아무튼 리액티브 방식이 아닌 기존의 `dsl`방식에서 사용하는 `execute`나 `fetch`는 블록킹이 발생한다.

따라서 자바나 코루틴을 사용하는 방식이 아니면 아래에 서술한 `Flux.from`을 감싸서 사용해야 한다.

어째든 어떤 방식을 사용해도 유연하게 작업을 하기 쉬워진다. 

물론 타입 안정성을 가져가는 것은 덤이다.

하지만 여기서는 기존 방식을 그대로 유지하는 선에서 사용해 보고자 한다.

어째든 [reactive-sql-with-jooq-3-15-and-r2dbc](https://blog.jooq.org/reactive-sql-with-jooq-3-15-and-r2dbc/)의 코드를 보면

```kotlin
fun example() {
    Flux.from(
        query.update(musician)
             .set(assignments)
             .where(musician.ID.`equal`(musicianId))
    )
}
```
처럼 `Flux.from`을 활용해서 감싼 이후 `awaitSingle`을 사용해서 가져오는 방법을 택할 수도 있다.

이 방식으로 반환값이 `0`이라면 업데이트가 수행되지 않았다는 것을 알 수 있다.

`PreparedStatement`사용시 이 반환값의 의미가 뭔지 아시는 분은 아시겠지만 `1`인 경우에는 업데이트에 대한 영향을 받은 레코드의 수다.

그렇다는 것은 `0`이라면 업데이트가 수행되지 않았다고 볼 수 있다. 

즉 조건절에서 일치하는 로우가 없었기 때문이라고 보는게 맞다.

테스트 코드를 한번 실행해 보자.

```kotlin
@SpringBootTest
class WriteMusicianServiceTest @Autowired constructor(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    @Test
    @DisplayName("musician update using builder test")
    fun updateMusicianTEST() = runTest {
        // given
        val id = 1L
        val assignments = mutableMapOf<Field<*>, Any>()
        //assignments[field("name")] = "Charlie Parker"
        //assignments[field("genre")] = Genre.JAZZ.name
        assignments[JMusician.MUSICIAN.NAME] = "Charlie Parker"
        assignments[JMusician.MUSICIAN.GENRE] = Genre.JAZZ.name
        
        // when
        val update = write.update(1, assignments)
        
        // then
        assertThat(update).isEqualTo(1)
    }
}
```
맵을 생성할 때 키 값은 주석처리된 부분처럼 `jooQ`가 생성한 엔티티를 통해서도 가능하다.

설정이 잘 되었다면 업데이트 쿼리가 나가고 요상한 `jooQ`이모지가 뜨면서 `Thank you for using jOOQ 3.18.5`메세지를 보게 된다.

이제는 컨트롤러에서 받는 `UpdateMusician`는 다음과 같이 변경해야 한다.

```kotlin
data class UpdateMusician(
    val name: String? = null,
    @field:EnumCheck(enumClazz = Genre::class, permitNull = true, message = "genre 필드는 POP, ROCK, HIPHOP, JAZZ, CLASSIC, WORLDMUSIC, ETC 만 가능합니다.")
    val genre: String? = null,
) {
    fun createAssignments(): MutableMap<Field<*>, Any> {
        val assignments = mutableMapOf<Field<*>, Any>()
        name?.let {
            isParamBlankThrow(it)
            assignments[JMusician.MUSICIAN.NAME] = it
        }
        genre?.let {
            assignments[JMusician.MUSICIAN.GENRE] = it
        }
        if(assignments.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
        }
        return assignments
    }
}
```
어짜피 `1`일 텐데 앞서 먼저 넘어온 `id`으로 뮤지션의 정보를 가져온 이후 업데이트 처리를 하기 때문에 그대로 간다.

만일 이런 방식으로 하지 않는다면 업데이트가 제대로 수행되었는지 이 값을 통해서 후처리를 할 수 있다.

이것은 프로젝트의 성향 또는 요구 사항에 맞춰가는 부분이기 때문에 그에 맞는 방식을 취하는 것 역시 개발자의 몫일 것이다.

이 프로젝트는 기존의 것을 유지하는 선에서의 변경점을 찾을 것이다. 

하지만 다양한 방법이 있기 때문에 프로젝트에 맞춰서 작업할 수 있도록 알아두면 좋다.

`Write`부분에서 커스텀 쿼리는 `updateMusician`만 사용하기 때문에 기존의 것은 그대로 두도록 하자.

이제는 `CustomMusicianRepository`에 있는 기존 함수들을 수정해 보자.

```kotlin
interface CustomMusicianRepository {
    suspend fun updateMusician(id: Long, assignments: MutableMap<Field<*>, Any>): Int
    fun musiciansByQuery(match: Query): Flow<Musician>
    suspend fun totalCountByQuery(match: Query): Long
    suspend fun musicianWithRecords(id: Long): Musician?
}
```
하지만 기존에 작업한 함수들의 파라미터 부분들이 달라질 것이기 때문에 이 부분도 이제는 변경이 되어야 한다.

## 조건이 있는 totalCountByQuery 수정

이 방법은 `QueryDsl`과 유사한 부분이 있는데 먼저 `Where`조건에 대한 문법을 먼저 확인해 보자.

```kotlin
override suspend fun totalCountByQuery(): Long {
    val musician = JMusician.MUSICIAN
    val query =  query.selectCount()
                      .from(musician)
                      .where(
                            musician.GENRE.eq("HIPHOP").and(
                                field("id").ge(11)
                            )
                      )
    return query.awaitSingle().value1().toLong()
}

override suspend fun totalCountByQuery(): Long {
    val musician = JMusician.MUSICIAN
    val query =  query.selectCount()
                      .from(musician)
                      .where(musician.GENRE.eq("HIPHOP"))
                      .and(field("id").ge(11))
    return query.awaitSingle().value1().toLong()
}
```
위 두가지 방식은 결과가 똑같다. 

기존의 우리가 작업한 방식은 `R2DBC`의 `Query`객체에 조건을 담아서 보내줬다.

하지만 이 방식을 사용할 때는 다르게 보내야 한다.

```
musician.ID.eq(11) or field("id").ge(11)
```
이 코드는 `org.jooq.Condition`을 반환한다. 

이것을 이용해 보자.

`ConditionType`은 다음과 같이 변경한다.

```kotlin
enum class ConditionType(
    val code: String,
    private val condition: (WhereCondition) -> Condition
) {
    LTE("lte", { field(it.column).lessOrEqual(it.value) }),
    LT("lt", { field(it.column).lessThan(it.value) }),
    GTE("gte", { field(it.column).greaterOrEqual(it.value) }),
    GT("gt", { field(it.column).greaterThan(it.value) }),
    EQ("eq", { field(it.column).eq(it.value) }),
    LIKE("like", { field(it.column).like("%${it.value}%") });

    fun getCondition(condition: WhereCondition): Condition {
        return condition(condition)
    }

    companion object {
        /**
         * null이면 EQ를 던진다.
         * @param code
         * @return ConditionType
         */
        fun of(code: String): ConditionType = values().firstOrNull { conditionType-> conditionType.code.equals(code, ignoreCase = true) }
            ?: EQ
    }
}
```

`CriteriaBuilder`를 이 `Condtion`을 담은 리스트로 반환하도록 변경해 보자.

```kotlin
fun createQuery(matrixVariable: MultiValueMap<String, Any>): List<Condition> {
    if(matrixVariable.containsKey("all")) {
        return emptyList()
    }
    val list = matrixVariable.map { (key, value) ->
        try {
            ConditionType.of(value[0].toString()).getCondition(WhereCondition.from(key, value[1]))
        } catch(e: Exception) {
            throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
        }
    }
    return list
}
```

최종적으로 

````kotlin
override suspend fun totalCountByQuery(conditions: List<Condition>): Long {
    val musician = JMusician.MUSICIAN
    val query = query.selectCount()
                     .from(musician)
                     .where()
    if(conditions.isNotEmpty()) {
        conditions.forEach {
            query.and(it)
        }
    }
    return query.awaitSingle().value1().toLong()
}
````

테스트 코드를 작성해서 테스트 해보자.

```kotlin
@Test
@DisplayName("total musician count by query test")
fun totalCountByQueryTEST() = runTest{
    // given
    val multiValueMap = LinkedMultiValueMap<String, Any>()
    multiValueMap.add("genre", "like")
    multiValueMap.add("genre", "HIPH")
    println(multiValueMap)
    val conditions = createQuery(multiValueMap)
    
    // when
    val count = read.totalCountByQuery(conditions)
    
    // then
    assertThat(count).isEqualTo(10)
}
```
실제 결과는 다음과 같이

```
INFO 12208 --- [-netty-thread-2] org.jooq.Constants                       : 
                                      
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@  @@        @@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@        @@@@@@@@@@
@@@@@@@@@@@@@@@@  @@  @@    @@@@@@@@@@
@@@@@@@@@@  @@@@  @@  @@    @@@@@@@@@@
@@@@@@@@@@        @@        @@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@        @@        @@@@@@@@@@
@@@@@@@@@@    @@  @@  @@@@  @@@@@@@@@@
@@@@@@@@@@    @@  @@  @@@@  @@@@@@@@@@
@@@@@@@@@@        @@  @  @  @@@@@@@@@@
@@@@@@@@@@        @@        @@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@  @@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@  Thank you for using jOOQ 3.18.5
                                      
INFO 12208 --- [-netty-thread-2] org.jooq.Constants                       : 

jOOQ tip of the day: Views (and table valued functions) are the most underrated SQL feature! They work very well together with jOOQ and jOOQ's code generator! https://www.jooq.org/doc/latest/manual/code-generation/codegen-advanced/codegen-config-database/codegen-database-table-valued-functions/

INFO 12208 --- [-netty-thread-2] i.b.m.c.listener.QueryLoggingListener    : Result Row : +-----+
|count|
+-----+
|   10|
+-----+

INFO 12208 --- [-netty-thread-2] i.b.m.c.listener.QueryLoggingListener    : ConnectionId: 10 
Query:["select count(*) from `musician` where genre like ?"] 
Bindings:[(%HIPH%)] 
Result Count : 1
```
다음과 같이 나오게 된다.

# jooQ Column 체크
`jooQ`가 생성하는 엔티티 정보를 통해서 해당 컬럼의 정보를 얻을 수 있다.

예를 들면

```kotlin
val musician = JMusician.MUSICIAN
musician.field("name")
```
만일 저 키 값에 해당하는 컬럼이 없다면 `null`을 반환한다.

이것을 이용해서 `CriteriaBuilder`의 `createQuery`를 다음과 같이 수정을 하자.

```kotlin
fun <T: TableImpl<*>> createQuery(matrixVariable: MultiValueMap<String, Any>, jooqEntity: T): List<Condition> {
    if(matrixVariable.containsKey("all")) {
        return emptyList()
    }
    val conditions = matrixVariable.map { (key, value) ->
        try {
            val column = jooqEntity.field(key)?.let { it as TableField<*, Any?> } ?: throw BadParameterException("컬럼 [${key}]은 존재하지 않는 컬럼입니다. 확인하세요.")
            ConditionType.of(value[0].toString()).getCondition(WhereCondition.from(column, value[1]))
        } catch(e: Exception) {
            when(e) {
                is BadParameterException -> throw BadParameterException(e.message)
                else -> throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
            }
        }
    }
    return conditions
}
```
`jooQ`관련 스택오버플로우의 내용을 보면 다음과 같이 `Field<*>`를 `as`를 통해서 `TableFiled<*, Any?>`로 캐스팅이 가능한 것을 확인했다.

만일 `jooq`의 엔티티에 정의된 컬럼명이 아니라면 `BadParameterException`을 던질 것이다.

이제는 `WhereCondition`과 `ConditionType`을 다시 수정하자.

```kotlin
data class WhereCondition(
    val column: TableField<*, Any?>,
    val value: Any,
) {
    companion object {
        fun from(key: TableField<*, Any?>, value: Any): WhereCondition {
            return WhereCondition(
                column = key,
                value = value
            )
        }
    }
}

enum class ConditionType(
    val code: String,
    private val condition: (WhereCondition) -> Condition
) {
    LTE("lte", { it.column.lessOrEqual(it.value) }),
    LT("lt", { it.column.lessThan(it.value) }),
    GTE("gte", { it.column.greaterOrEqual(it.value) }),
    GT("gt", { it.column.greaterThan(it.value) }),
    EQ("eq", { it.column.eq(it.value) }),
    LIKE("like", { it.column.like("%${it.value}%") });

    fun getCondition(condition: WhereCondition): Condition {
        return condition(condition)
    }

    companion object {
        /**
         * null이면 EQ를 던진다.
         * @param code
         * @return ConditionType
         */
        fun of(code: String): ConditionType = values().firstOrNull { conditionType-> conditionType.code.equals(code, ignoreCase = true) }
            ?: EQ
    }

}
```
상당히 어거지처럼 보이긴 한다. 

하지만 이런 방식을 통해서 리퀘스트로 넘어온 정보를 체크하는 방식을 채택한다면 베스트라는 생각이 든다.

물론 이런 방식보다는 구현체에서 명확하게 처리하는게 차라리 나을 수 있지만 이런 방식도 가능하다는 것을 알리고 싶었다.

이제는 테스트 코드를 다시 한번 손을 보자.

```kotlin
@Test
@DisplayName("total musician count by query test")
fun totalCountByQueryTEST() = runTest{
    // given
    val multiValueMap = LinkedMultiValueMap<String, Any>()
    multiValueMap.add("genre", "like")
    multiValueMap.add("genre", "HIPHOP")
    println(multiValueMap)
    val conditions = createQuery(multiValueMap, JMusician.MUSICIAN)

    // when
    val count = read.totalCountByQuery(conditions)

    // then
    assertThat(count).isEqualTo(10)
}
```
이 때 위에 기존 쿼리와 비교를 해보자면

```
# 변경 전
"select count(*) from `musician` where genre like ?"

# 변경 후
"select count(*) from `musician` where `musician`.`genre` like ?"
```
차이가 보이는 것을 알 수 있다.

## 정렬과 페이징 처리

`queryDSL`을 해보신 분이라면 흡사한 걸 알 수 있다.

`orderBy`함수는 단일 정보, 또는 `vararg` 그리고 `Collection`타입을 받을 수 있다.

이런 특징을 사용하면 되고 `limit`, `offset`을 사용할 수 있다.

이제는 카운트가 아닌 `musiciansByQuery`부분을 수정해 보자.

이 때는 `queyrDSL`처럼 프로젝션 정보를 통해 `dto`로 변환해주는 `API`를 제공하지 않는다.

대신 프로젝션 정보를 `Record`객체로 받아와서 이것을 `POJO`, 우리에게는 이미 만들어진 엔티티로 매핑하도록 한다.

이 때 유의해야 할 것은 생성자의 정보와 위치가 일치해야 한다.

다음 아래는 여러분들도 확인할 수 있을 텐데

```kotlin
@Suppress("UNCHECKED_CAST")
open class JMusicianRecord() : UpdatableRecordImpl<JMusicianRecord>(JMusician.MUSICIAN), Record5<Long?, String?, String?, LocalDateTime?, LocalDateTime?> {

    open var id: Long?
        set(value): Unit = set(0, value)
        get(): Long? = get(0) as Long?

    open var name: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var genre: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open var createdAt: LocalDateTime?
        set(value): Unit = set(3, value)
        get(): LocalDateTime? = get(3) as LocalDateTime?

    open var updatedAt: LocalDateTime?
        set(value): Unit = set(4, value)
        get(): LocalDateTime? = get(4) as LocalDateTime?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Long?> = super.key() as Record1<Long?>

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row5<Long?, String?, String?, LocalDateTime?, LocalDateTime?> = super.fieldsRow() as Row5<Long?, String?, String?, LocalDateTime?, LocalDateTime?>
    override fun valuesRow(): Row5<Long?, String?, String?, LocalDateTime?, LocalDateTime?> = super.valuesRow() as Row5<Long?, String?, String?, LocalDateTime?, LocalDateTime?>
    override fun field1(): Field<Long?> = JMusician.MUSICIAN.ID
    override fun field2(): Field<String?> = JMusician.MUSICIAN.NAME
    override fun field3(): Field<String?> = JMusician.MUSICIAN.GENRE
    override fun field4(): Field<LocalDateTime?> = JMusician.MUSICIAN.CREATED_AT
    override fun field5(): Field<LocalDateTime?> = JMusician.MUSICIAN.UPDATED_AT
    override fun component1(): Long? = id
    override fun component2(): String? = name
    override fun component3(): String? = genre
    override fun component4(): LocalDateTime? = createdAt
    override fun component5(): LocalDateTime? = updatedAt
    override fun value1(): Long? = id
    override fun value2(): String? = name
    override fun value3(): String? = genre
    override fun value4(): LocalDateTime? = createdAt
    override fun value5(): LocalDateTime? = updatedAt

    override fun value1(value: Long?): JMusicianRecord {
        set(0, value)
        return this
    }

    override fun value2(value: String?): JMusicianRecord {
        set(1, value)
        return this
    }

    override fun value3(value: String?): JMusicianRecord {
        set(2, value)
        return this
    }

    override fun value4(value: LocalDateTime?): JMusicianRecord {
        set(3, value)
        return this
    }

    override fun value5(value: LocalDateTime?): JMusicianRecord {
        set(4, value)
        return this
    }

    override fun values(value1: Long?, value2: String?, value3: String?, value4: LocalDateTime?, value5: LocalDateTime?): JMusicianRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        return this
    }

    /**
     * Create a detached, initialised JMusicianRecord
     */
    constructor(id: Long? = null, name: String? = null, genre: String? = null, createdAt: LocalDateTime? = null, updatedAt: LocalDateTime? = null): this() {
        this.id = id
        this.name = name
        this.genre = genre
        this.createdAt = createdAt
        this.updatedAt = updatedAt
        resetChangedOnNotNull()
    }
}
```
여기를 보면 `Record5<Long?, String?, String?, LocalDateTime?, LocalDateTime?>`이런 부분을 알 수 있다.

즉, 이 생성자의 위치와 정보와 우리가 매핑하고자 하는 엔티티의 생성자의 위치가 같아야 제대로 된 정보를 매핑할 수 있다.

아래는 예제를 위해 수정한 `musiciansByQuery`함수이다.

```kotlin
override fun musiciansByQuery(conditions: List<Condition>): Flow<Musician> {
    val musician = JMusician.MUSICIAN
    val query = query.select(asterisk())
                     .from(musician)
                     .where()
    if(conditions.isNotEmpty()) {
        conditions.forEach { query.and(it) }
    }
    
    val list = listOf(musician.ID.asc(), musician.NAME.asc())
    
    query.orderBy(list)
         .limit(10)
         .offset(0)
    return query.asFlow()
                .map { it.into(Musician::class.java) }
}
```
지금까지는 `List<Condition>`부분만 파라미터로 받았지만 예제로 만든 코드에서 정렬, 페이징 정보를 받아서 동적으로 처리하도록 작업해야 한다.

그렇기 위해서는 `QueryPage`를 변경해야 한다.

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
        return PageRequest.of(offset, limit)
    }
    
    fun <T: TableImpl<*>> pagination(jooqEntity: T): Pair<List<SortField<*>>, PageRequest> {
        val sortFields = if (column != null && sort != null) {
            val field = jooqEntity.field(column)?.let { it as TableField<*, Any?> } ?: throw BadParameterException("컬럼 [${column}]은 존재하지 않는 컬럼입니다. 확인하세요.")
            when (Sort.Direction.valueOf(sort.uppercase())) {
                Sort.Direction.DESC -> {
                    listOf(field.desc())
                }
                else -> {listOf(field.asc())}
            }
        } else {
            emptyList()
        }
        return sortFields to PageRequest.of(offset, limit)
    }

}
```
이에 맞추서 파라미터를 맞춰주자.

```kotlin
@Service
class ReadMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    fun musicians(pageable: Pageable) = musicianRepository.findAllBy(pageable)
    fun musiciansByQuery(conditions: List<Condition>, pagination: Pair<List<SortField<*>>, PageRequest>) =
        musicianRepository.musiciansByQuery(conditions, pagination)

    suspend fun musicianById(id: Long) = musicianRepository.findById(id)
    suspend fun musicianByIdOrThrow(id: Long, message: String? = null) = musicianRepository.findByIdOrThrow(id, message)
    suspend fun totalCount() = musicianRepository.count()
    suspend fun totalCountByQuery(conditions: List<Condition>) = musicianRepository.totalCountByQuery(conditions)
}

interface CustomMusicianRepository {
    suspend fun updateMusician(musicianId: Long, assignments: MutableMap<Field<*>, Any>): Int
    fun musiciansByQuery(conditions: List<Condition>, pagination: Pair<List<SortField<*>>, PageRequest>): Flow<Musician>
    suspend fun totalCountByQuery(conditions: List<Condition>): Long
}
class CustomMusicianRepositoryImpl(
    private val query: DSLContext,
): CustomMusicianRepository {

    override suspend fun updateMusician(musicianId: Long, assignments: MutableMap<Field<*>, Any>): Int {
        val musician = JMusician.MUSICIAN
        return query.update(musician)
                    .set(assignments)
                    .where(musician.ID.`equal`(musicianId)).awaitSingle()
    }

    override fun musiciansByQuery(conditions: List<Condition>, pagination: Pair<List<SortField<*>>, PageRequest>): Flow<Musician> {
        val musician = JMusician.MUSICIAN
        val query = query.select(asterisk())
                         .from(musician)
                         .where()
        if(conditions.isNotEmpty()) {
            conditions.forEach { query.and(it) }
        }

        if(pagination.first.isNotEmpty()) {
            query.orderBy(pagination.first)
        }
        query.limit(pagination.second.pageSize)
             .offset(pagination.second.offset)
        return query.asFlow()
                    .map { it.into(Musician::class.java) }
    }

    override suspend fun totalCountByQuery(conditions: List<Condition>): Long {
        val musician = JMusician.MUSICIAN
        val query = query.selectCount()
                         .from(musician)
                         .where()
        if(conditions.isNotEmpty()) {
            conditions.forEach {
                query.and(it)
            }
        }
        return query.awaitSingle().value1().toLong()
    }

}

```
테스트 코드도 이에 맞춰서 수정하자.

```kotlin
@Test
@DisplayName("musicians list by query test")
fun musiciansByQueryTEST() = runTest{
    // given
    val multiValueMap = LinkedMultiValueMap<String, Any>()
    multiValueMap.add("genre", "like")
    multiValueMap.add("genre", "HIP")
    println(multiValueMap)
    val conditions = createQuery(multiValueMap, JMusician.MUSICIAN)

    //val queryPage = QueryPage(page = 1, size = 5, column = "id", sort = "DESC")
    val queryPage = QueryPage(page = 1, size = 5)

    // when
    val musicians = read.musiciansByQuery(conditions, queryPage.pagination(JMusician.MUSICIAN))
                        .toList()
    
    // then
    assertThat(musicians.size).isEqualTo(5)

}
```

# UseCase 및 컨트롤러 수정 및 테스트

여기까지 왔다면 컨트롤러와 `useCase`부분은 변경하는데 크게 어려움이 없을 것이다.

이 부분은 코드로 확인하자.

## Musician one-to-many Record

뮤지션과 레코드는 뮤지션을 기준으로 `one-to-many`관계이다.

특정 뮤지션에 대한 `one-to-mnay`적용을 위해서는 다음과 같이 수정해야 한다.

```kotlin
override suspend fun musicianWithRecords(id: Long): Musician? {
    val musician = JMusician.MUSICIAN
    val record = JRecord.RECORD

    val sqlBuilder =
        query.select(
            musician,
            record
        )
        .from(musician)
        .leftJoin(record).on(musician.ID.eq(record.MUSICIAN_ID))
        .where(musician.ID.eq(id))
    return Flux.from(sqlBuilder)
               .bufferUntilChanged { it.component1() }
               .map {
                    rows ->
                        val selectMusician = rows[0].component1().into(Musician::class.java)
                        val records = rows.map { it.component2().into(Record::class.java) }
                        selectMusician.records = records
                        selectMusician
               }.awaitSingle()
}
``` 












이 때는 `Flux.from()`으로 감싼다.

`bufferUntilChanged`를 통해서 넘어오는 정보는 `Record2<JMusicianRecord!, JRecordRecord!>!`에 담겨져 넘어온다.

뮤지션을 기준으로 버퍼링을 할 것이기 때문에 첫 번째, 즉 `it.component1()`로 `bufferUntilChanged`를 묶어서 맵 처리를 한다.

`rows`의 정보는 `(Mutable)List<Record2<JMusicianRecord!, JRecordRecord!>!>!`
다음과 같다.

이후로는 쉽게 처리할 수 있는데 첫 번째 로우로부터 `Record2<JMusicianRecord!, JRecordRecord!>!`를 얻고 `JMusicianRecord`를 다시 얻는다.

이것을 통해서 뮤지션 엔티티로 매핑한다. 

그 이후는 어떤 의미인지 알 수 있을 것이다. 

테스트 코드를 실행해보면 원하는 결과를 얻을 수 있게 된다.

# Record 수정

수정 방식은 뮤지션과 동일하다.

다만 `many-to-one`을 처리하는 함수는 다음과 같다.

```kotlin
class CustomRecordRepositoryImpl(
    private val query: DSLContext,
): CustomRecordRepository {

    override suspend fun updateRecord(id: Long, assignments: MutableMap<Field<*>, Any>): Int {
        val record = JRecord.RECORD
        return query.update(record)
                    .set(assignments)
                    .where(record.ID.`equal`(id))
                    .awaitSingle()
    }

    override fun findAllRecords(conditions: List<Condition>, pagination: Pair<List<SortField<*>>, PageRequest>): Flow<Record> {
        val record = JRecord.RECORD
        val musician = JMusician.MUSICIAN
        val sqlBuilder= query.select(
            record,
            musician
        )
        .from(record)
        .join(musician).on(record.MUSICIAN_ID.eq(musician.ID))
        .where()

        if(conditions.isNotEmpty()) {
            conditions.forEach { sqlBuilder.and(it) }
        }

        if(pagination.first.isNotEmpty()) {
            sqlBuilder.orderBy(pagination.first)
        }
        sqlBuilder.limit(pagination.second.pageSize)
                  .offset(pagination.second.offset)

        return sqlBuilder.asFlow()
                         .map {
                                val selectRecord = it.value1().into(Record::class.java)
                                val selectMusician = it.value2().into(Musician::class.java)
                                selectRecord.musician = selectMusician
                                selectRecord
                         }

    }


}

```
`Flow`객체로 변환 후 `map`을 통해 변환하는 작업을 거치는 것은 동일하다.

이전에는 `mapper`을 이용했는데 커스텀 매퍼를 만들어서 사용해도 무방하다.

변경한 부분은 코드를 확인한다.

# At a Glance

여기서 사용하는 방식은 매트릭스 변수를 받아서 동적으로 처리하고자 이 프로젝트에 맞추서 작업한 것이다.

개발자의 환경 또는 요구사항에 따라서 이런 방식보다는 아마도 실무에서는 `API`에 필요한 정보를 받아서 처리하는 방식일 확률이 높다.

어찌되었든 이 브랜치는 `jooQ`와 연계해서 어떻게 사용하는지에 대한 가이드라인에 가깝다.

`jooQ`문법을 위한 저장소는 아니기에 관련 내용은 공식 사이트의 스펙을 확인해 보자.