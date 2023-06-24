# MusicShop integration queryDSL

공식적으로 제공하는게 아니고 `infobip`이라는 깃헙에서 개발하고 있는 라이브러리를 쓰게 된다.

일단 [official QueryDsL](http://querydsl.com/)에 `Related projects`항목에 가보자.

그러면 `Extensions`에 관련 깃헙 링크가 존재한다.

[infobip-spring-data-querydsl](https://github.com/infobip/infobip-spring-data-querydsl)에 가보자.

먼저 여기서 눈여겨 볼 것은 [Annotation processor](https://github.com/infobip/infobip-spring-data-querydsl#annotation-processor)항목이다.

참고로 여기서는 `QueryDSL`을 안다는 가정하에 작성되었기 때문에 `QueryDSL`에 대한 문법은 건너 띌 확률이 높다.

최소한 `QueryDSL`에 대해 알아보는 방법을 추천한다.

# pre setting

깃헙 `README.md`를 보면 메이븐 최신 버전은 `8.1.2`이다.

`WebFlux`가 아닌 일반 `MVC`에서 `QueryDSL`사용시 `QClass`를 생성하는 플로그인 설정 관련해서는 그레이들에서 `kts`로 작업시에는 `kapt`를 사용한다.

따라서 다음과 같이 설정을 추가해 주자.

```groovy
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.1.0"
	id("io.spring.dependency-management") version "1.1.0"
	kotlin("jvm") version "1.8.21"
	kotlin("plugin.spring") version "1.8.21"
	kotlin("kapt") version "1.8.21"
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
	implementation("io.r2dbc:r2dbc-pool:1.0.0.RELEASE")


	implementation("com.infobip:infobip-spring-data-r2dbc-querydsl-boot-starter:8.1.2")
	kapt("com.infobip:infobip-spring-data-jdbc-annotation-processor:8.1.2")

	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation("mysql:mysql-connector-java:8.0.33")
	implementation("com.github.jasync-sql:jasync-r2dbc-mysql:2.1.24")

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
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
다만 이 항목에서 이런 내용이 있다.

```
R2DBC module:

Requirements:
Java 17 with parameter names preserved in byte code (used to map columns to constructor parameters)
Spring Data R2DBC
entities must have an all argument constructor (@AllArgsConstructor), can have others as well
entity class and all argument constructor must be public (limitation of Querydsl)
if you're not using Flyway, you need to provide a SQLTemplates bean
```
우리는 `Flyway`를 사용하지 않기 때문에 `SQLTemplates`을 빈으로 주입해줘야 한다.

그렇지 않으면 서버가 실행되지 않는다.

```kotlin
@Configuration
class QueryDslConfiguration {
    @Bean
    fun sqlTemplates() = MySQLTemplates()
}
```

다양한 템플릿을 제공하는데 여기선 `mySql`을 사용하고 있으니 `MySQLTemplates`로 설정한다.

# 아니 근데 왜 잘 돌아가던 코드들이 전부 에러가 난디여??? 

먼저 새로운 것을 도입할 때 가장 중요한 것은 바로 `side-effect`체크이다.

어떤 라이브러리 또는 프레임워크를 적용할 때 기존에 잘 작동하던 녀석들이 안되면 이것도 문제라는 것이다.

그래서 나의 경우에는 항상 뭔가 영향을 주는 라이브러리를 적용하면 기존의 테스트 코드를 무조건 실행해 본다.

가장 기본적은 `API`인 `musicianById`를 누르는 순간 이상한 에러를 만나게 된다.

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.1.0)
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
INFO 5601 --- [-netty-thread-2] i.b.m.c.listener.QueryLoggingListener    : Result Row : Optional[1]
INFO 5601 --- [-netty-thread-2] i.b.m.c.listener.QueryLoggingListener    : ConnectionId: 10 
Query:["SELECT 1"] 
Bindings:[] 
Result Count : 1
INFO 5601 --- [-netty-thread-2] i.b.m.c.listener.QueryLoggingListener    : Result Row : null
INFO 5601 --- [-netty-thread-2] i.b.m.c.listener.QueryLoggingListener    : ConnectionId: 10 
Query:["SELECT musician.id, musician.name, musician.genre, musician.created_at, musician.updated_at FROM musician WHERE musician.id = ?"] 
Bindings:[(1)] 
Result Count : 1
INFO 5601 --- [-netty-thread-2] i.b.m.c.listener.QueryLoggingListener    : ConnectionId: 10 
Query:["SELECT musician.id, musician.name, musician.genre, musician.created_at, musician.updated_at FROM musician WHERE musician.id = ?"] 
Bindings:[(1)] 
Result Count : 1

org.springframework.data.mapping.MappingException: Could not read property @org.springframework.data.annotation.Id()private java.lang.Long io.basquiat.musicshop.domain.musician.model.entity.Musician.id from column Id

	at |b|b|b(Coroutine boundary.|b(|b)
	at io.basquiat.musicshop.domain.musician.service.ReadMusicianServiceTest$musicianByIdTEST$1.invokeSuspend(ReadMusicianServiceTest.kt:29)
	at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt$runTestCoroutine$2.invokeSuspend(TestBuilders.kt:212)
Caused by: org.springframework.data.mapping.MappingException: Could not read property @org.springframework.data.annotation.Id()private java.lang.Long io.basquiat.musicshop.domain.musician.model.entity.Musician.id from column Id
	at org.springframework.data.r2dbc.convert.MappingR2dbcConverter.readFrom(MappingR2dbcConverter.java:188)
	at org.springframework.data.r2dbc.convert.MappingR2dbcConverter$RowParameterValueProvider.getParameterValue(MappingR2dbcConverter.java:716)
	at org.springframework.data.mapping.model.SpELExpressionParameterValueProvider.getParameterValue(SpELExpressionParameterValueProvider.java:49)
	at org.springframework.data.relational.core.conversion.BasicRelationalConverter$ConvertingParameterValueProvider.getParameterValue(BasicRelationalConverter.java:293)
	at org.springframework.data.mapping.model.KotlinClassGeneratingEntityInstantiator$DefaultingKotlinClassInstantiatorAdapter.extractInvocationArguments(KotlinClassGeneratingEntityInstantiator.java:222)
	at org.springframework.data.mapping.model.KotlinClassGeneratingEntityInstantiator$DefaultingKotlinClassInstantiatorAdapter.createInstance(KotlinClassGeneratingEntityInstantiator.java:196)
	at org.springframework.data.mapping.model.ClassGeneratingEntityInstantiator.createInstance(ClassGeneratingEntityInstantiator.java:98)
	at org.springframework.data.relational.core.conversion.BasicRelationalConverter.createInstance(BasicRelationalConverter.java:135)
	at org.springframework.data.r2dbc.convert.MappingR2dbcConverter.createInstance(MappingR2dbcConverter.java:328)
	...more
Caused by: java.util.NoSuchElementException: Key Id is missing in the map.
	at kotlin.collections.MapsKt__MapWithDefaultKt.getOrImplicitDefaultNullable(MapWithDefault.kt:24)
	at kotlin.collections.MapsKt__MapsKt.getValue(Maps.kt:349)
	at com.github.jasync.sql.db.general.ArrayRowData.get(ArrayRowData.kt:37)
	at com.github.jasync.r2dbc.mysql.JasyncRow.get(JasyncRow.kt:85)
	at com.github.jasync.r2dbc.mysql.JasyncRow.get(JasyncRow.kt:29)
	at com.github.jasync.r2dbc.mysql.JasyncRow.get(JasyncRow.kt:20)
	at io.r2dbc.spi.Readable.get(Readable.java:80)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:568)
	at io.r2dbc.proxy.callback.CallbackHandlerSupport.lambda$static$0(CallbackHandlerSupport.java:73)
	at io.r2dbc.proxy.callback.CallbackHandlerSupport.proceedExecution(CallbackHandlerSupport.java:182)
	at io.r2dbc.proxy.callback.RowCallbackHandler.lambda$invoke$0(RowCallbackHandler.java:73)
	... 81 more


Process finished with exit code 255

```
쿼리가 날아간 것까지는 로그로 확인할 수 있다.

근데 여기서 우리는 
```
org.springframework.data.mapping.MappingException: Could not read property @org.springframework.data.annotation.Id()private java.lang.Long io.basquiat.musicshop.domain.musician.model.entity.Musician.id from column Id
```
이 에러를 해결해야 한다.

# 문제 파악
현재 이 상태로 컴파일을 실행하면 `build > generated > source > kapt > main`경로로 `QClass`가 생성된 걸 볼 수 있다.


```java
/**
 * QMusician is a Querydsl query type for Musician
 */
@Generated("com.infobip.spring.data.jdbc.annotation.processor.CustomMetaDataSerializer")
public class QMusician extends com.querydsl.sql.RelationalPathBase<Musician> {

    private static final long serialVersionUID = -747127729;

    public static final QMusician musician = new QMusician("Musician");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final EnumPath<io.basquiat.musicshop.domain.musician.model.code.Genre> genre = createEnum("genre", io.basquiat.musicshop.domain.musician.model.code.Genre.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QMusician(String variable) {
        super(Musician.class, forVariable(variable), null, "musician");
        addMetadata();
    }

    public QMusician(String variable, String schema, String table) {
        super(Musician.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMusician(String variable, String schema) {
        super(Musician.class, forVariable(variable), schema, "musician");
        addMetadata();
    }

    public QMusician(Path<Musician> path) {
        super(path.getType(), path.getMetadata(), null, "musician");
        addMetadata();
    }

    public QMusician(PathMetadata metadata) {
        super(Musician.class, metadata, null, "musician");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(id, ColumnMetadata.named("Id").withIndex(0));
        addMetadata(name, ColumnMetadata.named("Name").withIndex(1));
        addMetadata(genre, ColumnMetadata.named("Genre").withIndex(2));
        addMetadata(createdAt, ColumnMetadata.named("created_at").withIndex(3));
        addMetadata(updatedAt, ColumnMetadata.named("updated_at").withIndex(4));
    }

}
```
다음은 `jpa`를 사용하는 다른 프로젝트에서 생성된 `QClass`이다.

```java
/**
 * QMeta is a Querydsl query type for Meta
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMeta extends EntityPathBase<Meta> {

    private static final long serialVersionUID = 1674230818L;

    public static final QMeta meta = new QMeta("meta");

    public final NumberPath<Long> balance = createNumber("balance", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath name = createString("name");

    public final StringPath owner = createString("owner");

    public final StringPath symbol = createString("symbol");

    public QMeta(String variable) {
        super(Meta.class, forVariable(variable));
    }

    public QMeta(Path<? extends Meta> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMeta(PathMetadata metadata) {
        super(Meta.class, metadata);
    }

}
```
차이가 보이는데 `public static final QMusician musician = new QMusician("Musician");`, `public static final QMeta meta = new QMeta("meta");`를 먼저 보자.

기존은 `meta`로 소문자로 표기되는데 여기서는 `Musician`으로 대문자로 시작한다.

근데 이건 사실 문제가 되는건 아니다.     

다만 `addMeate`에 정의된 것을 보면 `created_at`, `updated_at`을 제외하고 대문자로 시작한다.

[Annotation processor](https://github.com/infobip/infobip-spring-data-querydsl#annotation-processor)에서 그 힌트를 얻을 수 있는데

```kotlin
Annotation processor infobip-spring-data-jdbc-annotation-processor is used by R2DBC and JDBC modules to generate Querydsl Q classes. Without annotation processor this process can be quite cumbersome as connecting to database would be required during the build phase.

Annotation processor generates Q classes for all classes that have @Id annotated fields. Reason why @Id is used and not some custom annotation is for simplicity of use and implementation and because @Id is required by Spring Data JDBC:

Spring Data JDBC uses the ID to identify entities. The ID of an entity must be annotated with Spring Data’s @Id annotation.

여기 ! --> Current implementation of Annotation Processor uses pascal casing based naming strategy for table and column names.

To customize this behavior across whole project add following annotation to one of your classes:
```
`여기 !`부분을 보면 `pascal casing`이라고 되어 있다.

구글링으로 파스칼 방식이 뭔가 찾아봤더니 `단어의 첫 문자와 각 단어의 첫 글자를 대문자로 표기하는 명명 규칙`이라고 한다.

# 가장 심플한 방법

생성된 `QClass`에서 해답을 찾는 방법이다.

`created_at`, `updated_at`을 보면 `@Column`을 사용하는 경우에는 설정한 값을 세팅한 것을 알 수 있다.

이유는 `spring-data`에서 `@Column`에 설정한 값을 우선순위로 두기 때문이다.    

디비의 컬럼과 맞출기 위해서 사용했을 테니 당연하겠지.

번거롭더라도 다른 복잡한 방법을 사용하지 않고 해결하는 방법은 `@Column`을 일일히 설정하는 것이다.

# 더 가장 심플한 방법

처음 프로젝트 만들 때 엔티티 작업하면서 이 방법을 적용한다면 위 방식도 나아보인다.

하지만 이미 만들어진 엔티티, 더군다나 그 엔티티가 한두개가 아니면 저 방식으로 처리하는 건 바보같은 짓이다.

```kotlin
@Configuration
@ProjectTableCaseFormat(CaseFormat.LOWER_UNDERSCORE)
@ProjectColumnCaseFormat(CaseFormat.LOWER_UNDERSCORE)
class QueryDslConfiguration {
    @Bean
    fun sqlTemplates() = MySQLTemplates()
}
```
끗~~~

생성된 `QClass`를 보면 원하는 방식으로 적용된 걸 볼 수 있다.

## 그러나???? 이게 끝일거라고 생각했어????

결론적으로 `가장 심플한 방법`으로 결정했다면 이것만으로는 해결이 안된다.

~~이렇게 하면 해결이 될 수 있을거라 생각해써~~~

예를 들면 `@Column`을 통해 일일히 작업을 한 경우에는 문제가 없이 처리가 된다.

`가장 심플한 방법`을 적용하고 엔티티의 `@Column`을 지우고 컴파일된 `QClass`를 보면서 좋아라 한 내가 바보가 되는 순간이다.

결국 `도대체 왜`라는 질문을 가지고 디버깅을 시도하기 시작한다.

그냥 처음부터 할껄 그랬다는 후회 껄무새가 되버리면서

```
org.springframework.data.mapping.MappingException: Could not read property @org.springframework.data.annotation.Id()private java.lang.Long io.basquiat.musicshop.domain.musician.model.entity.Musician.id from column Id

	at |b|b|b(Coroutine boundary.|b(|b)
	at io.basquiat.musicshop.common.extensions.CustomCrudRepositoryExtensionsKt.findByIdOrThrow(CustomCrudRepositoryExtensions.kt:7)
	at io.basquiat.musicshop.domain.musician.service.ReadMusicianServiceTest$musicianByIdOrThrowTEST$1.invokeSuspend(ReadMusicianServiceTest.kt:43)
	at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt$runTestCoroutine$2.invokeSuspend(TestBuilders.kt:212)
Caused by: org.springframework.data.mapping.MappingException: Could not read property @org.springframework.data.annotation.Id()private java.lang.Long io.basquiat.musicshop.domain.musician.model.entity.Musician.id from column Id
	at org.springframework.data.r2dbc.convert.MappingR2dbcConverter.readFrom(MappingR2dbcConverter.java:188)
```
이 에러를 중심으로 `MappingR2dbcConverter`의 `readFrom`에 포인트를 잡고 디버깅 시작!!!

![갭쳐1](https://raw.githubusercontent.com/basquiat78/sns/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202023-06-30%20%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE%204.12.09.png)

이미지를 보면 `property`항목이 보이는데 어라? `columnName`이 `Id`로 되어 있다.

```java
@Nullable
private Object readFrom(Row row, @Nullable RowMetadata metadata, RelationalPersistentProperty property,
        String prefix) {

    String identifier = prefix + property.getColumnName().getReference();

    try {

        Object value = null;
        if (metadata == null || RowMetadataUtils.containsColumn(metadata, identifier)) {

            if (property.getType().equals(Clob.class)) {
                value = row.get(identifier, Clob.class);
            } else if (property.getType().equals(Blob.class)) {
                value = row.get(identifier, Blob.class);
            } else {
                value = row.get(identifier);
            }
        }

        if (value == null) {
            return null;
        }

        if (getConversions().hasCustomReadTarget(value.getClass(), property.getType())) {
            return readValue(value, property.getTypeInformation());
        }

        if (property.isEntity()) {
            return readEntityFrom(row, metadata, property);
        }

        return readValue(value, property.getTypeInformation());

    } catch (Exception o_O) {
        throw new MappingException(String.format("Could not read property %s from column %s", property, identifier), o_O);
    }
}
```
여기서 `value = row.get(identifier);`이 부분에서 에러가 난다.

디버깅 이미지에 의하면 `property`는 `BasicRelationalPersistentProperty`이다.

좋다. 

그럼 왜 이런 현상이 발생하는지 바로 따라가자.

```java
public class BasicRelationalPersistentProperty extends AnnotationBasedPersistentProperty<RelationalPersistentProperty>
		implements RelationalPersistentProperty {

	public BasicRelationalPersistentProperty(Property property, PersistentEntity<?, RelationalPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder, NamingStrategy namingStrategy) {

		super(property, owner, simpleTypeHolder);
		this.namingStrategy = namingStrategy;

		Assert.notNull(namingStrategy, "NamingStrategy must not be null");

		this.isEmbedded = Lazy.of(() -> Optional.ofNullable(findAnnotation(Embedded.class)).isPresent());

		this.embeddedPrefix = Lazy.of(() -> Optional.ofNullable(findAnnotation(Embedded.class)) //
				.map(Embedded::prefix) //
				.orElse(""));

		this.columnName = Lazy.of(() -> Optional.ofNullable(findAnnotation(Column.class)) //
				.map(Column::value) //
				.filter(StringUtils::hasText) //
				.map(this::createSqlIdentifier) //
				.orElseGet(() -> createDerivedSqlIdentifier(namingStrategy.getColumnName(this))));

		this.collectionIdColumnName = Lazy.of(() -> Optionals
				.toStream(Optional.ofNullable(findAnnotation(MappedCollection.class)) //
						.map(MappedCollection::idColumn), //
						Optional.ofNullable(findAnnotation(Column.class)) //
								.map(Column::value)) //
				.filter(StringUtils::hasText) //
				.findFirst() //
				.map(this::createSqlIdentifier)); //

		this.collectionKeyColumnName = Lazy.of(() -> Optionals //
				.toStream(Optional.ofNullable(findAnnotation(MappedCollection.class)).map(MappedCollection::keyColumn)) //
				.filter(StringUtils::hasText).findFirst() //
				.map(this::createSqlIdentifier) //
				.orElseGet(() -> createDerivedSqlIdentifier(namingStrategy.getKeyColumn(this))));
	}

}
```
나머지는 지우고 생성자 부분만 남겼는데 여기 보면 `columnName`부분에서 문제의 부분이 보이기 시작한다.

```java
this.columnName = Lazy.of(() -> Optional.ofNullable(findAnnotation(Column.class)) //
				.map(Column::value) //
				.filter(StringUtils::hasText) //
				.map(this::createSqlIdentifier) //
				.orElseGet(() -> createDerivedSqlIdentifier(namingStrategy.getColumnName(this))));
```
`namingStrategy.getColumnName(this)`이 코드가 눈에 떡하니 보인다.

![갭쳐2](https://raw.githubusercontent.com/basquiat78/sns/master/%E1%84%89%E1%85%B3%E1%84%8F%E1%85%B3%E1%84%85%E1%85%B5%E1%86%AB%E1%84%89%E1%85%A3%E1%86%BA%202023-06-30%20%E1%84%8B%E1%85%A9%E1%84%92%E1%85%AE%204.20.23.png)

이 이미지를 보면 `nameStrategy`를 지금 `PascalCaseNamingStrategy`로 위임하고 있다.!!!!!

아래는 `org.springframework.data.relational.core.mapping`패키지 내의 `NamingStrategy`클래스이다.

일단 다른 건 지우고 우리가 관심을 가질 부분만 보자.

```java
public interface NamingStrategy {

	
	default String getTableName(Class<?> type) {

		Assert.notNull(type, "Type must not be null");

		return ParsingUtils.reconcatenateCamelCase(type.getSimpleName(), "_");
	}

	default String getColumnName(RelationalPersistentProperty property) {

		Assert.notNull(property, "Property must not be null");

		return ParsingUtils.reconcatenateCamelCase(property.getName(), "_");
	}
}
```
이 코드를 보면 `camelCase`로 처리하고 있는데 이것을 `infobip-queryDSL`에서는 `PascalCaseNamingStrategy`를 사용하고 있는 것이다.

`infobip-queryDls`의 설정 파일을 한번 보자.

```kotlin
@Import(InfobipSpringDataCommonConfiguration.class)
@Configuration
public class QuerydslSqlQueryConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public NamingStrategy pascalCaseNamingStrategy() {
        return new PascalCaseNamingStrategy();
    }
}
```
나머지는 지우고 위 코드를 보자.

`PascalCaseNamingStrategy`클래스
```kotlin
public class PascalCaseNamingStrategy implements NamingStrategy {

    @Override
    public String getTableName(Class<?> type) {
        return type.getSimpleName();
    }

    @Override
    public String getColumnName(RelationalPersistentProperty property) {
        return property.getName().substring(0, 1).toUpperCase() + property.getName().substring(1);
    }
}
```
결국 우리가 `QClass`생성시에 제대로 컴파일했다고 생각해도 `BasicRelationalPersistentProperty`내에서 이 파스칼 방식으로 변환한다는 것을 알았다.

이전 브랜치에서 

```kotlin
fun getNativeColumn(columnName: String, clazz: KClass<*>): String {
    val members = clazz.java.declaredFields
    val annotationValues = members.mapNotNull { member ->
        val list = member.annotations
        val column = list.find { it.annotationClass == Column::class } as? Column
        column?.value?.let { it }
    }

    val fieldValues = clazz.memberProperties.mapNotNull { it.name }
    return if (annotationValues.contains(columnName)) {
        columnName
    } else if (fieldValues.contains(columnName)) {
        toSnakeCaseByUnderscore(columnName)
    } else {
        throw BadParameterException("${columnName}이 존재하지 않습니다.")
    }
}

fun toSnakeCaseByUnderscore(source: String): String {
    return reconcatenateCamelCase(source, "_")
}
```
이런 것을 만든 적이 있는데 스프링 내에서 `@Column`으로 정의된 컬럼 정보가 있으면 그것을 먼저 체크하도록 하고 있기 때문에 그것을 흉내내서 만든 것이다.

결국 모든 엔티티에 `@Column`을 전부 설정해야 하는가라는 의문이 들 수 있다.

하지만 스프링 내에서 빈을 등록할 때 우선 순위를 줄 수 있다. 

`@Primary`어노테이션을 설정해서 이것을 먼저 사용하도록 만들 수 있는데 이제는 이 방법을 이용해야 한다.

```kotlin
@Configuration
@ProjectTableCaseFormat(CaseFormat.LOWER_UNDERSCORE)
@ProjectColumnCaseFormat(CaseFormat.LOWER_UNDERSCORE)
class QueryDslConfiguration {
    @Bean
    fun sqlTemplates() = MySQLTemplates()

    @Bean
    @Primary
    fun namingStrategy(): NamingStrategy = object : NamingStrategy {
        override fun getTableName(type: Class<*>): String = reconcatenateCamelCase(type.simpleName, "_")
        override fun getColumnName(property: RelationalPersistentProperty) = reconcatenateCamelCase(property.name, "_")
    }
}
```
그냥 `org.springframework.data.relational.core.mapping`패키지 내의 `NameStrategy`에 있는 코드를 사용할 수 있도록 만들어 버리자.

그리고 모든 `API`를 전부 테스트 해보고 문제가 없는지 꼼꼼히 체크를 한다.

# Using QueryDSL

역시 깃헙의 `README.md`의 가이드 라인을 따른다.

기존의 `BaseRepository`을 이용해서 `NamedQuery`를 사용할 수 있는 것은 사용하고 특별한 경우에는 `R2dbcEntityTemplate`을 이용했다.

먼저 뮤지션의 `Update`부분을 손을 보자.

```kotlin
interface MusicianQueryDslRepository: QuerydslR2dbcRepository<Musician, Long>

class CustomMusicianRepositoryImpl(
    private val queryDsl: MusicianQueryDslRepository,
    private val query: R2dbcEntityTemplate,
): CustomMusicianRepository {

    override suspend fun updateMusician(id: Long, assignments: MutableMap<SqlIdentifier, Any>): Long {
        return queryDsl.update {
            it.set(musician.name, "parker")
                .where(musician.id.eq(id))
        }.awaitSingle()
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
위 코드는 일단 그냥 예제 코드로 `QuerydslR2dbcRepository`를 사용한 `Repository`를 만들고 주입을 받는다.

`jooQ`와 마찬가지로 업데이트의 경우에는 반환 결과가 `Long`으로 반환한다.

따라서 이것을 사용하는 서비스와 `useCase`도 변경해줘야 한다.

이 예제 코드에서는 컬럼당 `set`을 하는 구조를 가지고 있지만 이 부분도 리스트로 처리할 수 있도록 다음과 같이 `API`를 제공한다.

```kotlin
@Override
public C set(List<? extends Path<?>> paths, List<?> values) {
    for (int i = 0; i < paths.size(); i++) {
        if (values.get(i) instanceof Expression) {
            updates.put(paths.get(i), (Expression<?>) values.get(i));
        } else if (values.get(i) != null) {
            updates.put(paths.get(i), ConstantImpl.create(values.get(i)));
        } else {
            updates.put(paths.get(i), Null.CONSTANT);
        }
    }
    return (C) this;
}
```
따라서 다음과 같이

```kotlin
override suspend fun updateMusician(id: Long, assignments: MutableMap<SqlIdentifier, Any>): Long {
    val paths = listOf(musician.name, musician.genre)
    val values = listOf("Charlie Parker", Genre.JAZZ.name)
    return queryDsl.update {
        it.set(paths, values)
          .where(musician.id.eq(id))
    }.awaitSingle()
}
```
사용할 수 있다.

이것을 토대로 다음과 같이 함수의 시그니처와 로직 부분을 수정해야 한다.

```kotlin
// CustomMusicianRepository
override suspend fun updateMusician(id: Long, assignments: Pair<List<Path<*>>, List<*>>): Long {
    return queryDsl.update {
        it.set(assignments.first, assignments.second)
          .where(musician.id.eq(id))
    }.awaitSingle()
}

// WriteMusicianService
suspend fun update(id: Long, assignments: Pair<List<Path<*>>, List<*>>): Long {
    return musicianRepository.updateMusician(id, assignments)
}
```

테스트 코드를 수정해 보자.

```kotlin
@Test
@DisplayName("musician update using builder test")
fun updateMusicianTEST() = runTest {
    // given
    val id = 1L

    val paths
        = listOf(musician.name, musician.genre)
    val values
        = listOf("Charlie Parker", Genre.JAZZ.name)
    val assignments
        = paths to values
    // when
    val update = write.update(id, assignments)

    // then
    assertThat(update).isEqualTo(1)
}
```
사실 이 코드는 `useCase`부터 정보를 받기 때문에 테스트를 위해서는 서비스쪽만 변경하고 진입점의 코드들을 주석처리후 테스트해야 한다.

이제부터 주석처리한 부분도 단계적으로 수정하기 위해 기존에 `UpdateMusician`의 함수들을 수정하고 `useCase`와 컨트롤러 부분을 수정해 보자.

먼저 `UpdateMusician`는 다음과 같이 변경되어야 한다.

```kotlin
data class UpdateMusician(
    val name: String? = null,
    @field:EnumCheck(enumClazz = Genre::class, permitNull = true, message = "genre 필드는 POP, ROCK, HIPHOP, JAZZ, CLASSIC, WORLDMUSIC, ETC 만 가능합니다.")
    val genre: String? = null,
) {
    fun createAssignments(): Pair<List<Path<*>>, List<Any>> {
        val paths = mutableListOf<Path<*>>()
        val value = mutableListOf<Any>()
        name?.let {
            isParamBlankThrow(it)
            paths.add(musician.name)
            value.add(it)
        }
        genre?.let {
            paths.add(musician.genre)
            value.add(it)
        }
        if(paths.isEmpty() || value.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
        }
        return paths to value
    }
}
```

`useCase`는 다음과 같이 변경하자.

```kotlin
suspend fun update(id: Long, command: UpdateMusician): Musician {
    val assignments = command.createAssignments()
    val updated = write.update(id, assignments)
    return if(updated == 1L) {
        read.musicianByIdOrThrow(id)
    } else {
        notFound("id [$id]로 조회된 정보가 없습니다.")
    }
}
```
결과가 `1`이 나온다면 뮤지션 정보를 가져와 보여주고 `0`이라면 아이디에 해당하는 뮤지션정보가 없어 업데이트 수행이 이뤄지지 않기 때문에 에러를 던진다.

# select dynamic query
`jpa`와 사용할 때는 `QClass`생성시 `@QueryEntitiy`와 `@QueryDelegate`을 사용해서 구성해 볼 수 있다.

하지만 여기서는 그게 되지 않는다. 

아니면 설정방법을 몰라서일 수 있지만 해당 깃헙의 공식 가이드라인에 이런 정보가 없어서 찾지 못했다.

결국 우리는 `BooleanExpression`나 `BooleanBuilder` 또는 `Predicate functional interface`를 사용해야 한다.

여기서는 `BooleanBuilder`를 사용하고자 한다.

먼저 일단 예제 코드를 한번 만들어 보면

```kotlin
override fun musiciansByQuery(match: Query): Flow<Musician> {
    return queryDsl.query {
        it.select(musician)
            .from(musician)
    }.flow()
}
```
여러분이 `QueryDSL`을 안다면 이 후 어떤 방식으로 처리할 수 있을지 알 것이다.

어째든 `musiciansByQuery`의 파라미터 정보를 `BooleanBuilder`로 바꾼다면 끝날 것이라는 생각을 할 수 있다.

실제로 저 상태에서 기존에 만든 테스트 코드를 실행한다면 에러는 나겠지만 전체 뮤지션 정보를 가져오는 쿼리가 나가는 것을 볼 수 있다.

또한 정렬의 경우에는 `Q orderBy(OrderSpecifier<?>... o);`처럼 `vararg`로 처리할 수 있도록 리스트로 생성한다.

`jooQ`를 사용할 때처럼 처리하면 될 것이라는 생각을 할 수 있다.

히지만 조건절의 경우에는 `jooQ`처럼 처리하기 힘들다. 

따라서 이와 관련 이것을 처리할 수 있는 유틸을 만들어야 한다.

참고로 이 부분은 사용하고자 한다면 한번 살펴보는 것도 좋다. 

다만 조건 검색이 명확하다면 그다지 큰 내용은 아니다.

먼저 리퀘스트로 넘어온 컬럼 정보를 통해서 `Path`정보를 가져와야 한다.

방법은 `QClass`로부터 컬럼 정보를 가져와 루프를 돌면서 컬럼명과 같은 `Path`를 가져와야 한다.

```kotlne
val path = qClass.columns.firstOrNull { it.metadata.name == key } ?: throw BadParameterException("column [$key] 정보가 없습니다.")
```
즉 `QClass`의 `columns`에 있는 `metadata`정보를 통해서 가져온다.

이때 컬럼의 값은 엔티티에 정의된 변수명에 기인한다.

따라서 리퀘스트로부터 받아야 하는 정보는 엔티티의 변수명에 따라야 한다.

이것을 위해서 

```kotlin
fun snakeCaseToCamel(source: String): String {
    val firstCharLower = source[0].lowercase() + source.substring(1)
    if (source.indexOf("_") < 0) {
        return firstCharLower
    }
    return firstCharLower.split("_")
                         .mapIndexed { index, value ->
        if(index == 0) value else StringUtils.capitalize(value)
    }.joinToString(separator = "")

}
```
과 같은 함수를 통해서 넘어온 리퀘스트의 컬럼값을 변경해서 비교할 예정이다.

`QueryDsl`에서 `BooleanExpression`의 코드를 보면 다음과 같은 것을 통해 최종적으로 비교한다.

```java
public static BooleanOperation booleanOperation(Operator operator, Expression<?>... args) {
    return predicate(operator, args);
}
```
여기서 `vararg`로 넘어가는 값은 `QueryDsl`의 `Expression<T>`로 정의된 `mixin`과 비교값을 받게 되는데 이 방법을 이용해 공통으로 처리할 예정이다.

또한 위에 `path`를 `QClass`로부터 얻어올 수 있기 때문에 `WhereCondition`을 다음과 같이 변경한다.

즉, `Path<*>`를 받을 것이다.

```kotlin
data class WhereCondition(
    val column: Path<*>,
    val value: Any,
) {
    companion object {
        fun from(key: Path<*>, value: Any): WhereCondition {
            return WhereCondition(
                column = key,
                value = value
            )
        }
    }
}
```

`ConditionType`을 통해서 `BooleanExpression`정보를 얻어오기 때문에

```kotlin
enum class ConditionType(
    val code: String,
    private val booleanBuilder: (WhereCondition) -> BooleanExpression
) {
    LTE("lte", { Expressions.booleanOperation(Ops.LOE, it.column, ConstantImpl.create(it.value)) }),
    LT("lt", { Expressions.booleanOperation(Ops.LT, it.column, ConstantImpl.create(it.value)) }),
    GTE("gte", { Expressions.booleanOperation(Ops.GOE, it.column, ConstantImpl.create(it.value)) }),
    GT("gt", { Expressions.booleanOperation(Ops.GT, it.column, ConstantImpl.create(it.value)) }),
    EQ("eq", { Expressions.booleanOperation(Ops.EQ, it.column, ConstantImpl.create(it.value)) }),
    LIKE("like", { Expressions.booleanOperation(Ops.STRING_CONTAINS, it.column, ConstantImpl.create(it.value)) });

    fun getBooleanBuilder(condition: WhereCondition): BooleanExpression {
        return booleanBuilder(condition)
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
이제는 마지막 `CriteriaBuilder`를 마무리한다.

```kotlin
fun <T: RelationalPathBase<*>> pagination(qClass: T): Pair<List<OrderSpecifier<*>>, PageRequest> {
    val pathBuilder = PathBuilder<Any>(qClass.type.javaClass, qClass.toString())
    val sortFields = if (column != null && sort != null) {
        qClass.columns.firstOrNull { it.metadata.name == snakeCaseToCamel(column) }
            ?: throw BadParameterException("column [$column] 정보가 없습니다.")

        val field = pathBuilder.get(toSnakeCaseByUnderscore(column)) as Expression<out Comparable<*>>
        when (Sort.Direction.valueOf(sort.uppercase())) {
            Sort.Direction.DESC -> listOf(OrderSpecifier(Order.DESC, field))
            else -> listOf(OrderSpecifier(Order.ASC, field))
        }
    } else {
        emptyList()
    }
    return sortFields to PageRequest.of(offset, limit)
}
```
`QueryPage`도 수정한다.

다만 이때 컬럼 정보가 엔티티에 있는지 조회를 하고 `field`정보를 가져올 때 `toSnakeCaseByUnderscore`로 변경해서 가져와야 한다.

그렇지 않으면 `unknown field`에러가 발생하기 때문이다.

이 방법을 사용하면 최종적으로 다음과 같이

```kotlin
override fun musiciansByQuery(condition: BooleanBuilder, pagination: Pair<List<OrderSpecifier<*>>, PageRequest>): Flow<Musician> {
    return queryDsl.query {
        it.select(musician)
          .from(musician)
          .where(condition)
          .orderBy(*pagination.first.toTypedArray())
          .limit(pagination.second.pageSize.toLong())
          .offset(pagination.second.offset)
    }.flow()
}

override suspend fun totalCountByQuery(condition: BooleanBuilder): Long {
    return queryDsl.query {
        it.select(musician.id.count())
          .from(musician)
          .where(condition)
    }.awaitSingle()
}
```
사용할 수 있다.

# one-to-many or many-to-one
`jooQ`와 달리 이런 연관관계를 갖는 경우에는 `dto`를 생성해서 만들어야 한다.

원래는 `dto`로 반환하는 방식이 가장 무난하겠지만 여기서는 그런 방식을 딱히 고수하지 않을 것이다.

프로젝션 방식을 통해 `dto`로 생성하고 `dto`로부터 엔티티를 반환하도록 만들 예정이기 때문이다.

# 꼭 이렇게까지 만들 필요가 있을까?

이건 단지 이런 방식으로도 접근할 수 있다는 정도에서도 충분하다.

이보다는 구현 `API`, 즉 요구사항에 맞춰 잘 구현하면 되는 일일 것이다.

어째든 시도한 김에 끝까지 가보자.

## 삽질의 시작? 이것도??????

`MusicianDto`클래스는 정보를 받아서 엔티티로 보내기 위해 작성된 것이다.

이 방식을 꼭 따라할 필요가 없다는 것을 다시 한번 말씀드리면서
```kotlin
data class MusicianDto(
    val id: Long,
    val name: String,
    val genre: Genre,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val recordId: Long,
    val title: String,
    val label: String,
    val releasedType: ReleasedType,
    val releasedYear: Int,
    val format: String,
    val recordCreatedAt: LocalDateTime,
    val recordUpdatedAt: LocalDateTime?,
) {
    fun toMusician(): Musician {
        return Musician(
            id = id,
            name = name,
            genre = genre,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun toRecord(): Record {
        return Record(
            id = recordId,
            musicianId = id,
            title = title,
            label = label,
            releasedType = releasedType,
            releasedYear = releasedYear,
            format = format,
            createdAt = recordCreatedAt,
            updatedAt = recordUpdatedAt,
        )
    }

}
```

```kotlin


override suspend fun musicianWithRecords(id: Long): Musician {
    val dtoList =  queryDsl.query {
        it.select(
            Projections.constructor(MusicianDto::class.java,
                musician.id,
                musician.name,
                musician.genre,
                musician.createdAt,
                musician.updatedAt,
                record.id.`as`("record_id"),
                record.title,
                record.label,
                record.releasedType,
                record.releasedYear,
                record.format,
                record.createdAt.`as`("record_created_at"),
                record.updatedAt.`as`("record_updated_at")
            )
        )
        .from(musician)
        .innerJoin(record).on(musician.id.eq(record.musicianId))
        .where(musician.id.eq(id))
    }.flow().toList()
    if(dtoList.isEmpty()) notFound("뮤지션 아이디 [$id]로 조회된 정보가 없습니다.")
    val musician = dtoList[0].toMusician()
    val records = dtoList.map { it.toRecord() }
    musician.records = records
    return musician
}
```
뮤지션과 레코드의 아이디와 생성일과 수정일은 중복이 되기 때문에 별칭을 주게 되어 있다.

쿼리를 생성해 날릴 때는 별칭으로 날아간다.

하지만 실제 `R2dbcMappingContext`로부터 프로퍼티를 생성하고 `readFrom`을 통해서 매핑을 하게 되어 있다.

이 때도 이 별칭도 `NameStrategy`의 영향을 받기 때문에 관례적으로 `recordId`처럼 별칭을 주게 되면 매핑을 할 수 없어 `null`값으로 세팅이 되어 버린다.

따라서 별칭도 위와 같이 스네이크 언더스코어 방식으로 줘야 한다.


# Record 수정

방식은 똑같기 때문에 완성된 코드는 전체 코드를 살펴보면 될것이다.

최종적으로 수정된 코드는 전체 코드에서 확인해 보도록 하자!

# At a Glance

일단 `jooQ`에 비하면 사용하는 방식이 약간은 조잡하다는 느낌을 지울 수 없다. 

게다가 `NameStrategy`에 따른 컬럼 매핑을 염두해 둬야 하기 때문에 디테일한 부분에 손이 많이 간다.

아직은 공식 라이브러리가 아니기 때문에 그럴 수 있고 차후 좋아지거나 공식 서포터로 포함될 수 있는 부분을 염두해 두고 사용해 보면 좋을 듯 싶다.




