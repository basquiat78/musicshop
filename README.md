# MusicShop Using Controller Version

## Before

`Repository`에서 현재 사용하는 `ReactiveCrudRepository`대신 `R2dbcRepository`로 교체한다.

```kotlin
@NoRepositoryBean
public interface R2dbcRepository<T, ID> extends ReactiveCrudRepository<T, ID>, ReactiveSortingRepository<T, ID>, ReactiveQueryByExampleExecutor<T> {}
```

## create Record API

뮤지션은 음반을 갖는다.

그것이 싱글이든 정규 음반이든 뭔든 존재할 것이다.     

관련 디비 스키마를 먼저 만들어 보자.

```roomsql
-- musicshop.record definition
CREATE TABLE `record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `musician_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `label` varchar(100) NOT NULL,
  `released_type` varchar(50) NOT NULL,
  `released_year` int NOT NULL, -- 2023
  `format` varchar(100) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `record_FK` FOREIGN KEY (`musician_id`) REFERENCES `musician` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```
기본적으로 음반이 어느 뮤지션의 음반인지 그리고 음반 타이틀과 발매일과 발매 타입 정보를 갖는다.

또한 음반은 LP, CD, 테이프, 디지털음원 같은 포맷을 갖을 수 있다.

피지컬 없이 디지털 음원으로만 발표하는 경우도 있기 때문에 이 부분을 코드로 대처한다.

```kotlin
enum class RecordFormat(
    val description: String,
) {
    
    TAPE("테이프"),
    CD("시디"),
    LP("엘피"),
    DIGITAL("디지털 음원"),
    DIGITALONLY("디지털 음원 온리");
    
}
enum class ReleasedType(
    val description: String,
) {
    SINGLE("싱글"),
    FULL("정규 음반"),
    EP("EP"),
    OST("o.s.t."),
    COMPILATION("컴필레이션 음반"),
    LIVE("라이브 음반"),
    MIXTAPE("믹스테잎");
}
```
엔티티를 정의해보자.

```kotlin
@Table("record")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Record(
    @Id
    var id: Long? = null,
    @Column("musician_id")
    var musicianId: Long,
    var title: String,
    var label: String,
    @Column("released_type")
    var releasedType: ReleasedType,
    @Column("released_year")
    var releasedYear: String,
    @Column("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var createdAt: LocalDateTime? = now(),
    @Column("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var updatedAt: LocalDateTime? = null,
)
```

실제 [discogs](https://www.discogs.com/)나 몇 몇 해외 음반 사이트를 보면 참여한 뮤지션들의 정보나 세세한 정보들을 담고 있다.

하지만 이 프로젝트는 먼저 심플한 음반들의 정보만을 담을 생각이다.

뮤지션과 관련된 `API`를 작성한 방식으로 먼저 기본적인 `API`와 테스트 코드는 미리 작업을 해 놓았다.

따라서 해당 테스트의 대한 방식은 이전 브랜치에서 확인하면 된다.

진행하는 방식은 그다지 크게 달라진 것은 없다.

해당 코드는 확인하면 될 것이다. 

이 후 진행하는 부분은 몇 가지 커스텀할 수 있는 부분에 대해서 진행할 생각이다.

## 변경점

`CustomCrudRepositoryExtensions`를 수정했다.

메세지가 필요하다면 메세지를 반을 수 있고 메세지가 없다면 정해진 메세지를 던질 수 있도록 처리한다.

```kotlin
fun <T, ID> R2dbcRepository<T, ID>.findByIdOrThrow(id: ID, message: String? = null): Mono<T> {
    return this.findById(id)
               .switchIfEmpty { notFound(message?.let{ it } ?: "Id [$id]로 조회된 정보가 없습니다.") }

}
```
이에 따른 각 `Read`쪽 서비스도 변경한다.

```kotlin
@Service
class WriteRecordUseCase(
    private val readMusician: ReadMusicianService,
    private val read: ReadRecordService,
    private val write: WriteRecordService,
) {

    fun insert(command: CreateRecord): Mono<Record> {
        val created = Record(
            musicianId = command.musicianId,
            title = command.title,
            label = command.label,
            releasedType = command.releasedType,
            releasedYear = command.releasedYear,
            format = command.format,
        )
        val musician = readMusician.musicianByIdOrThrow(command.musicianId, "해당 레코드의 뮤지션 정보가 조회되지 않습니다. 뮤지션 아이디를 확인하세요.")
        return musician.flatMap {
            write.create(created)
        }
    }

    fun update(id: Long, command: UpdateRecord): Mono<Record> {
        return read.recordByIdOrThrow(id).flatMap { record ->
            val (record, assignments) = command.createAssignments(record)
            write.update(record, assignments)
        }.onErrorResume {
            Mono.error(BadParameterException(it.message))
        }
        .then(read.recordById(id))
    }

}
```
레코드 정보를 생성할 때 몇가지 방법이 있지만 가장 이상적인 것은 넘어온 뮤지션의 정보가 디비상에 존재하는지 먼저 확인하는 방법이다.

보통 `record`테이블과 `musician`테이블의 구조를 볼 때 다음과 같이

```
CONSTRAINT `record_FK` FOREIGN KEY (`musician_id`) REFERENCES `musician` (`id`)
```
`record`테이블을 기준으로 뮤지션 아이디의 정보를 외래키로 잡는다.

만일 레코드 정보를 생성할 때 `musician`테이블의 키 존재 유무로 없는 뮤지션의 아이디로 넣을 경우 디비에서 `mysql Error 1452 - #23000` 에러를 뱉게되어 있다.

즉 외래키 제약 조건으로 인한 에러가 발생한다.

이렇게 에러를 던지는 방식보다는 먼저 뮤지션을 조회하고 없으면 클라이언트로 명확한 메세지를 던저주는 방식이 직관적일 수 있다.

```kotlin
val musician = readMusician.musicianByIdOrThrow(command.musicianId, "해당 레코드의 뮤지션 정보가 조회되지 않습니다. 뮤지션 아이디를 확인하세요.")
return musician.flatMap {
    write.create(created)
}
```
그렇기 때문에 위와 같이 처리를 하고 문제가 없다면 레코드 정보를 생성하는 방식으로 진행하는 것이 좋을 수 있다.

## ManyToOne Relation

레코드와 뮤지션의 관계는 `ManyToOne`의 관계이다.      

`jpa`를 예로 들면 객체 그래프 탐색을 위해서 `양방향 매핑`을 선호할 수도 있다.       

또는 정말 필요한 것이 아니라면 약간의 편의성을 포기하고 `@ManyToOne`단방향을 선호할 수 있다.

이것 역시 정답은 없다. 

개인적으로는 약간의 편의성을 포기하고 복잡성을 덜고 단방향을 선호하는 편이다.

이 저장소에서도 이런 단방향을 위주로 설계할 생각이다.

### Record entity 수정

```kotlin
@Table("record")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Record(
    @Id
    var id: Long? = null,
    var musicianId: Long,
    var title: String?,
    var label: String?,
    @Column("released_type")
    var releasedType: ReleasedType?,
    @Column("released_year")
    var releasedYear: Int?,
    var format: String?,
    @Column("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var createdAt: LocalDateTime? = now(),
    @Column("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var updatedAt: LocalDateTime? = null,
) {
    @Transient
    var musician: Musician? = null
}
```
레코드는 뮤지션 아이디의 값을 가지고 있다. 

하지만 `JPA`처럼 할 수 없기 때문에 생성자가 아닌 바디에 위와 같이 `@Transient`를 설정한다.     

먼저 그냥 이것을 토대로 그냥 무작정 해보는 방법을 사용해 보자.

### 1. UseCase에서 flatMapMany를 이용한 방법

먼저 만들어 놓은 `ReadRecordUseCase`은 다음과 같다.

```kotlin
@Service
class ReadRecordUseCase(
    private val read: ReadRecordService,
    private val readMusician: ReadMusicianService,
) {

    fun recordById(id: Long) = recordRepository.findById(id)
    fun recordByIdOrThrow(id: Long, message: String? = null) = recordRepository.findByIdOrThrow(id, message)

}

``` 
물론 뮤지션 아이디의 정보는 화면으로부터 정확하게 날아올 수 있을 것이다.     

하지만 좀 더 방어적인 코드를 짜보자면 `Write`에서 처럼 먼저 뮤지션의 정보를 가져온 이후 그에 따라 레코드 리스트를 가져온다고 해보자.

```kotlin
fun recordByMusicianId(musicianId: Long): Flux<Record> {
    val musician = readMusician.musicianByIdOrThrow(musicianId)
    return musician.flatMapMany { musician ->
        read.recordByMusicianId(musicianId)
            .map {
                it.musician = musician
                it
            }
    }
}
```
그렇다면 존재하는 뮤지션이라면 이미 뮤지션의 정보를 알 수 있다.

따라서 위와 같이 `flatMapMany`를 통해서 해당 뮤지션의 레코드 리스트를 가져오고 `map`을 통해 해당 뮤지션의 정보를 껴넣어 주는 방식을 고려해 볼 수 있다.

지금은 페이징처리도 없긴 하지만 뮤지션 아이디로 레코드 리스트를 가져온다면 이 방법에서 더 이상 손을 대지 않아도 좋아 보인다.

물론 최종적으로는 페이징처리를 해서 정보를 내려주는 방식이 가장 좋을 것이다.

일단 위 코드부터 페이징 처리를 해보자.

```kotlin
interface RecordRepository: R2dbcRepository<Record, Long>, CustomRecordRepository {
    override fun findById(id: Long): Mono<Record>
    fun findByMusicianId(id: Long, pageable: Pageable): Flux<Record>
    @Query("SELECT COUNT(id) FROM record WHERE musician_id = :musicianId")
    fun countByMusicianId(@Param("musicianId") musicianId: Long): Mono<Long>
}

@Service
class ReadRecordService(
    private val recordRepository: RecordRepository,
) {

    fun recordById(id: Long) = recordRepository.findById(id)
    fun recordByIdOrThrow(id: Long, message: String? = null) = recordRepository.findByIdOrThrow(id, message)
    fun recordByMusicianId(musicianId: Long, pageable: Pageable) = recordRepository.findByMusicianId(musicianId, pageable)
    fun recordCountByMusician(musicianId: Long) = recordRepository.countByMusicianId(musicianId)

}
```
기존의 `findByMusicianId`에서 `Pageable`정보를 받고 그에 상응하는 카운트 쿼리도 추가한다.

당연히 서비스쪽도 같이 변경한다.

이미 기존에도 같은 코드를 사용했기 때문에 형식은 크게 벗어나지 않는다.
```kotlin
fun recordByMusicianId(queryPage: QueryPage, musicianId: Long): Mono<Page<Record>> {
    val musician = readMusician.musicianByIdOrThrow(musicianId)
    return musician.flatMapMany { musician ->
        read.recordByMusicianId(musicianId, queryPage.fromPageable())
            .map {
                it.musician = musician
                it
            }
    }
    .collectList()
    .zipWith(read.recordCountByMusician(musicianId))
    .map { tuple -> PageImpl(tuple.t1, queryPage.fromPageable(), tuple.t2) }

}
```
공통으로 페이지 처리를 위해 만든 `QueryPage`를 재활용하고 테스트를 해보면 원하는 결과를 얻을 수 있다.

포스트맨의 경우에는 나의 테스트 데이타로 다음과 같은 결과를 얻을 수 있다.

```
GET 127.0.0.1:8080/api/v1/records/musician/10?page=1&size=10&column=releasedYear&sort=DESC
발매 연도로 내림차순

result
{
    "content": [
        {
            "id": 16,
            "musicianId": 10,
            "title": "Upgrade IV",
            "label": "린치핀뮤직",
            "releasedType": "FULL",
            "releasedYear": 2020,
            "format": "CD,LP,DIGITAL",
            "createdAt": "2023-06-12T14:02:02",
            "updatedAt": "2023-06-12T14:05:36",
            "musician": {
                "id": 10,
                "name": "스윙스",
                "genre": "HIPHOP",
                "createdAt": "2023-06-05T19:28:31",
                "updatedAt": "2023-06-05T19:34:20"
            }
        },
        {
            "id": 10,
            "musicianId": 10,
            "title": "Upgrade III",
            "label": "린치핀뮤직",
            "releasedType": "FULL",
            "releasedYear": 2018,
            "format": "CD",
            "createdAt": "2023-06-12T10:42:23",
            "musician": {
                "id": 10,
                "name": "스윙스",
                "genre": "HIPHOP",
                "createdAt": "2023-06-05T19:28:31",
                "updatedAt": "2023-06-05T19:34:20"
            }
        },
        {
            "id": 9,
            "musicianId": 10,
            "title": "파급효과 (Ripple Effect)",
            "label": "린치핀뮤직",
            "releasedType": "COMPILATION",
            "releasedYear": 2014,
            "format": "COMPILATION",
            "createdAt": "2023-06-12T10:42:23",
            "musician": {
                "id": 10,
                "name": "스윙스",
                "genre": "HIPHOP",
                "createdAt": "2023-06-05T19:28:31",
                "updatedAt": "2023-06-05T19:34:20"
            }
        }
    ],
    "pageable": {
        "sort": {
            "empty": false,
            "unsorted": false,
            "sorted": true
        },
        "offset": 0,
        "pageNumber": 0,
        "pageSize": 10,
        "paged": true,
        "unpaged": false
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 3,
    "first": true,
    "size": 10,
    "number": 0,
    "sort": {
        "empty": false,
        "unsorted": false,
        "sorted": true
    },
    "numberOfElements": 3,
    "empty": false
}
```
하지만 만일 레코드 리스트중에 뮤지션이 아닌 다른 조건의 검색을 통해 레코드 리스트를 가져오는 `API`라면 이 방식은 사용할 수 없다.

이 때는 나머지 두 방법을 고려해 볼 수 있다.

### 2. 네이티브 쿼리와 매퍼를 활용하는 방법

`jpa`와 `queryDSL`의 조합처럼 가능하다면 얼마나 좋을까만은 어째든 `R2DBC`는 `ORM`이 아니기 때문에 다른 방식으로 처리해야 한다.

그래서 확실히 손이 많이 가긴 하지만 현재는 이 방법이 가장 무난할 것이다.

먼저 다음과 같이 어떤 조건도 없이 `musician`과 `record`테이블을 조인한다.

```kotlin
interface CustomRecordRepository {
    fun updateRecord(record: Record, assignments: MutableMap<SqlIdentifier, Any>): Mono<Record>
    fun findAllRecords(pageable: Pageable): Flux<Record>
}

@Component
class RecordMapper: BiFunction<Row, RowMetadata, Record> {
    override fun apply(row: Row, rowMetadata: RowMetadata): Record {
        val musician = Musician(
            name = row.get("musicianName", String::class.java)!!,
            genre = Genre.valueOf(row.get("genre", String::class.java)!!),
            createdAt = row.get("mCreatedAt", LocalDateTime::class.java),
            updatedAt = row.get("mUpdatedAt", LocalDateTime::class.java),
        )

        val record = Record(
            id = row.get("id", Long::class.javaObjectType)!!,
            musicianId = row.get("musician_id", Long::class.javaObjectType)!!,
            title = row.get("title", String::class.java),
            label = row.get("label", String::class.java),
            releasedType = ReleasedType.valueOf(row.get("released_type", String::class.java)!!),
            releasedYear = row.get("released_year", Int::class.javaObjectType)!!,
            format = row.get("id", String::class.java),
            createdAt = row.get("created_at", LocalDateTime::class.java),
            updatedAt = row.get("updated_at", LocalDateTime::class.java),
        )
        record.musician = musician
        return record
    }
}

class CustomRecordRepositoryImpl(
    private val query: R2dbcEntityTemplate,
    private val recordMapper: RecordMapper,
): CustomRecordRepository {

    override fun updateRecord(record: Record, assignments: MutableMap<SqlIdentifier, Any>): Mono<Record> {
        return query.update(Record::class.java)
                    .matching(query(where("id").`is`(record.id!!)))
                    .apply(Update.from(assignments))
                    .thenReturn(record)
    }

    override fun findAllRecords(pageable: Pageable): Flux<Record> {
        val sql = """
            SELECT m.name AS musicianName,
                   m.genre,
                   m.created_at AS mCreatedAt,
                   m.updated_at AS mUpdatedAt,
                   r.*
              FROM record r
              INNER JOIN musician m
              ON r.musician_id = m.id 
        """.trimIndent()
        return query.databaseClient
                    .sql(sql)
                    .map(recordMapper::apply)
                    .all()
    }
}
```
`RecordMapper`를 보면 좀 불편한 느낌이 가득하다.

과거 `PrepareStatement`와 `ResultSet`을 이용한 방식과 상당히 유사하다.

이런 코드는 스프링 내에서도 `JdbcTemplate`을 이용한 코드와도 상당히 유사하다.     

이 때 주의점은 `Long`, `Int`의 경우에는 `javaObjectType`로 타입을 설정해 줘야 오류가 발생하지 않는다.

## Advanced All Records API

이번 작업은 `R2DBC`가 초창기 등장했을 때 생각했었던 코드로 사실 프로덕트 단계에서는 사용하기 좀 애매하긴 하지만 기존의 뮤지션의 조건 검색을 따라해 볼 생각이다.     

먼저 다음과 같이 `reflect`를 활용해서 넘어온 컬럼 정보가 맞는지 확인하는 함수를 하나 정의한다.

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
    return ParsingUtils.reconcatenateCamelCase(source, "_")
}
```
물론 이 부분을 뮤지션 부분에서도 사용하기 위해서는 엔티티의 변수명과 비교하는 부분에서도 활용할 수 있지만 네이티브 쿼리의 경우에는 몇가지 검사를 해야 한다.

먼저 리퀘스트로 넘어온 컬럼이 엔티티의 변수명과 같다면 `camel case by underscore`를 적용해 준다.

만일 `@Column`에 정의된 컬럼명이면 그대로 반환해 주는 역할을 한다.

```kotlin
interface CustomRecordRepository {
    fun updateRecord(record: Record, assignments: MutableMap<SqlIdentifier, Any>): Mono<Record>
    fun findAllRecords(whereClause: String = "", orderClause: String = "", limitClause: String = ""): Flux<Record>
}

class CustomRecordRepositoryImpl(
    private val query: R2dbcEntityTemplate,
    private val recordMapper: RecordMapper,
): CustomRecordRepository {

    override fun updateRecord(record: Record, assignments: MutableMap<SqlIdentifier, Any>): Mono<Record> {
        return query.update(Record::class.java)
                    .matching(query(where("id").`is`(record.id!!)))
                    .apply(Update.from(assignments))
                    .thenReturn(record)
    }

    override fun findAllRecords(whereClause: String, orderClause: String, limitClause: String): Flux<Record> {
        var sql = """
            SELECT musician.name AS musicianName,
                   musician.genre,
                   musician.created_at AS mCreatedAt,
                   musician.updated_at AS mUpdatedAt,
                   record.*
              FROM record
              INNER JOIN musician
              ON record.musician_id = musician.id
              WHERE 1 = 1 
              $whereClause
              $orderClause
              $limitClause
        """.trimIndent()
        return query.databaseClient
                    .sql(sql)
                    .map(recordMapper::apply)
                    .all()
    }
}
```
커스텀 레파지토리에서 모든 정보를 받고 조합하는 방법도 고려해 볼 수 있지만 `useCase`에서 리퀘스트 정보를 분석해서 쿼리를 넘겨주는 방식을 탹햤다.

앞서 정의한 `ConditionType`를 수정하자.

```kotlin
enum class ConditionType(
    val code: String,
    private val native: (String, WhereCondition) -> String,
    private val criteria: (WhereCondition) -> Criteria
) {
    LTE("lte", { prefix, it -> "AND ${prefix}.${it.column} <= '${it.value}'" }, { where(it.column).lessThanOrEquals(it.value)}),
    LT("lt", { prefix, it -> "AND ${prefix}.${it.column} < '${it.value}'" }, { where(it.column).lessThan(it.value)}),
    GTE("gte", { prefix, it -> "AND ${prefix}.${it.column} >= '${it.value}'" }, { where(it.column).greaterThanOrEquals(it.value)}),
    GT("gt", { prefix, it -> "AND ${prefix}.${it.column} > '${it.value}'" }, { where(it.column).greaterThan(it.value)}),
    EQ("eq", { prefix, it -> "AND ${prefix}.${it.column} = '${it.value}'" }, { where(it.column).isEqual(it.value)}),
    LIKE("like", { prefix, it -> "AND ${prefix}.${it.column} like '%${it.value}%'" }, { where(it.column).like("%${it.value}%")});

    fun getCriteria(condition: WhereCondition): Criteria {
        return criteria(condition)
    }

    fun getNativeSql(prefix: String, condition: WhereCondition): String {
        return native(prefix, condition)
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
또한 다음과 같이 `CritieraBuilder`에서도 리퀘스트 정보를 통해서 조건문과 페이징 소팅 관련 네이티브 쿼리 정보를 생성하는 함수도 추가한다.

```kotlin
fun createQuery(matrixVariable: MultiValueMap<String, Any>): Query {
    if(matrixVariable.containsKey("all")) {
        return empty()
    }
    val list = matrixVariable.map { (key, value) ->
        try {
            ConditionType.of(value[0].toString()).getCriteria(WhereCondition.from(key, value[1]))
        } catch(e: Exception) {
            throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
        }
    }
    return query(Criteria.from(list))
}

fun createNativeWhereClause(prefix: String, clazz: KClass<*>, matrixVariable: MultiValueMap<String, Any>): String {
    if(matrixVariable.containsKey("all")) {
        return ""
    }
    val list = matrixVariable.map { (key, value) ->
        try {
            val validColumn = getNativeColumn(key, clazz)
            ConditionType.of(value[0].toString()).getNativeSql(prefix, WhereCondition.from(validColumn, value[1]))
        } catch(e: Exception) {
            if (e is BadParameterException) {
                throw BadParameterException(e.message)
            } else {
                throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
            }
        }
    }
    return list.joinToString(separator = "\n")
}

fun createNativeSortLimitClause(prefix: String, clazz: KClass<*>, queryPage: QueryPage): Pair<String, String> {
    val nativeColumn = queryPage.column?.let { getNativeColumn(it, clazz) } ?: ""
    val sort = if (nativeColumn.isNotBlank() && queryPage.sort != null) {
        "ORDER BY ${prefix}.${nativeColumn} ${queryPage.sort.name}"
    } else {
        "ORDER BY ${prefix}.id"
    }
    val offset = ( queryPage.page!! - 1 ) * queryPage.size!!
    val limit = "LIMIT ${queryPage.size} OFFSET $offset"
    return sort to limit;
}
```


```kotlin
@Service
class ReadRecordService(
    private val recordRepository: RecordRepository,
) {

    fun recordById(id: Long) = recordRepository.findById(id)
    fun recordByIdOrThrow(id: Long, message: String? = null) = recordRepository.findByIdOrThrow(id, message)
    fun recordByMusicianId(musicianId: Long, pageable: Pageable) = recordRepository.findByMusicianId(musicianId, pageable)
    fun recordCountByMusician(musicianId: Long) = recordRepository.countByMusicianId(musicianId)
    fun allRecords(whereClause: String = "",
                   orderClause: String = "",
                   limitClause: String = "") = recordRepository.findAllRecords(whereClause, orderClause, limitClause)
}

@Service
class ReadRecordUseCase(
    private val read: ReadRecordService,
    private val readMusician: ReadMusicianService,
) {

    fun recordById(id: Long): Mono<Record> {
        return read.recordByIdOrThrow(id)
    }

    fun recordByMusicianId(queryPage: QueryPage, musicianId: Long): Mono<Page<Record>> {
        val musician = readMusician.musicianByIdOrThrow(musicianId)
        return musician.flatMapMany { musician ->
            read.recordByMusicianId(musicianId, queryPage.fromPageable())
                .map {
                    it.musician = musician
                    it
                }
        }
            .collectList()
            .zipWith(read.recordCountByMusician(musicianId))
            .map { tuple -> PageImpl(tuple.t1, queryPage.fromPageable(), tuple.t2) }

    }

    fun allRecords(queryPage: QueryPage, matrixVariable: MultiValueMap<String, Any>): Flux<Record> {
        val prefix = "record"
        val clazz = Record::class
        val whereClause = createNativeWhereClause(prefix, clazz, matrixVariable)
        val (orderSql, limitSql) = createNativeSortLimitClause(prefix, clazz, queryPage)
        return read.allRecords(whereClause = whereClause, orderClause = orderSql, limitClause = limitSql)
    }

}
```
`reflect`을 사용하는게 좀 걸리긴 하지만 나름대로 동적 쿼리를 작성하고자 했던 몸부림이라 생각하자.

최종적으로 컨트롤러 부분은 뮤지션처럼 `@MatrixVariable`를 활용한다.

```kotlin

@Validated
@RestController
@RequestMapping("/api/v1/records")
class RecordController(
    private val readRecordUseCase: ReadRecordUseCase,
    private val writeRecordUseCase: WriteRecordUseCase,
) {

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchRecord(@PathVariable("id") id: Long): Mono<Record> {
        return readRecordUseCase.recordById(id)
    }

    @GetMapping("/query/{queryCondition}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchRecord(
        @Valid queryPage: QueryPage,
        @MatrixVariable(pathVar = "queryCondition", required = false) matrixVariable: MultiValueMap<String, Any>
    ): Flux<Record> {
        return readRecordUseCase.allRecords(queryPage, matrixVariable)
    }

    @GetMapping("/musician/{musicianId}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchRecordByMusician(@Valid queryPage: QueryPage, @PathVariable("musicianId") musicianId: Long): Mono<Page<Record>> {
        return readRecordUseCase.recordByMusicianId(queryPage, musicianId)
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRecord(@RequestBody @Valid command: CreateRecord): Mono<Record> {
        return writeRecordUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateRecord(@PathVariable("id") id: Long, @RequestBody command: UpdateRecord): Mono<Record> {
        return writeRecordUseCase.update(id, command)
    }

}
```
물론 페이징 정보를 보내주는 것이 정확하겠지만 진행을 완료하게 되면 그 때 일괄적으로 적용해 볼까 한다.

포스트맨을 통해서 제대로 작동하는지 확인해 보자.

```
GET 127.0.0.1:8080/api/v1/records/query/where;all?page=1&size=20&column=musicianId&sort=DESC

Query:["SELECT musician.name AS musicianName,
       musician.genre,
       musician.created_at AS mCreatedAt,
       musician.updated_at AS mUpdatedAt,
       record.*
  FROM record
  INNER JOIN musician
  ON record.musician_id = musician.id
  WHERE 1 = 1 
  
  ORDER BY record.musician_id DESC
  LIMIT 20 OFFSET 0"

[
    {
        "id": 9,
        "musicianId": 10,
        "title": "파급효과 (Ripple Effect)",
        "label": "린치핀뮤직",
        "releasedType": "COMPILATION",
        "releasedYear": 2014,
        "format": "CD,DIGITAL",
        "createdAt": "2023-06-12T10:42:23",
        "musician": {
            "name": "스윙스",
            "genre": "HIPHOP",
            "createdAt": "2023-06-05T19:28:31",
            "updatedAt": "2023-06-05T19:34:20"
        }
    },
    {
        "id": 10,
        "musicianId": 10,
        "title": "Upgrade III",
        "label": "린치핀뮤직",
        "releasedType": "FULL",
        "releasedYear": 2018,
        "format": "CD,DIGITAL",
        "createdAt": "2023-06-12T10:42:23",
        "musician": {
            "name": "스윙스",
            "genre": "HIPHOP",
            "createdAt": "2023-06-05T19:28:31",
            "updatedAt": "2023-06-05T19:34:20"
        }
    },
    .
    .
]

GET 127.0.0.1:8080/api/v1/records/query/where;label=eq,Impulse!?page=1&size=20

Query:["SELECT musician.name AS musicianName,
       musician.genre,
       musician.created_at AS mCreatedAt,
       musician.updated_at AS mUpdatedAt,
       record.*
  FROM record
  INNER JOIN musician
  ON record.musician_id = musician.id
  WHERE 1 = 1 
  AND record.label = 'Impulse!'
  ORDER BY record.id
  LIMIT 20 OFFSET 0

[
    {
        "id": 4,
        "musicianId": 2,
        "title": "A Love Supreme",
        "label": "Impulse!",
        "releasedType": "FULL",
        "releasedYear": 1964,
        "format": "CD,LP",
        "createdAt": "2023-06-12T10:34:48",
        "musician": {
            "name": "John Coltrane",
            "genre": "JAZZ",
            "createdAt": "2023-06-02T17:30:21"
        }
    },
    {
        "id": 8,
        "musicianId": 9,
        "title": "The Black Saint And The Sinner Lady",
        "label": "Impulse!",
        "releasedType": "FULL",
        "releasedYear": 1963,
        "format": "CD,LP",
        "createdAt": "2023-06-12T10:38:22",
        "musician": {
            "name": "Charles Mingus",
            "genre": "JAZZ",
            "createdAt": "2023-06-04T19:54:00",
            "updatedAt": "2023-06-04T19:57:15"
        }
    }
]
```

물론 이런 복잡한 방식이 아니라 다음과 같이 조회 컬럼이 정해져 있다면
```kotlin
override fun findAllRecords(label: String, pageable: Pageable): Flux<Record> {
        var sql = """
            SELECT musician.name AS musicianName,
                   musician.genre,
                   musician.created_at AS mCreatedAt,
                   musician.updated_at AS mUpdatedAt,
                   record.*
              FROM record
              INNER JOIN musician
              ON record.musician_id = musician.id
              WHERE 1 = 1 
              AND record.label = :label
              LIMIT :limit OFFSET :offset
        """.trimIndent()
        return query.databaseClient
                    .sql(sql)
                    .bind("label")
                    .bind("limit", pageable.getPageSize())
                    .bind("offset", pageable.getOffset())
                    .map(recordMapper::apply)
                    .all()
    }
```
이런 방식으로도 처리할 수도 있다.

### 3. @Query와 CustomConverter를 활용하는 방법

이 방법은 `@Query`를 이용하기 때문에 다이나믹한 방식에는 적합하지 않다.

다만 특화된 케이스의 경우 활용하기 편하다.

우선 다음과 같이 예제용으로 레파지토리에 함수를 하나 추가하자.

```kotlin
interface RecordRepository: R2dbcRepository<Record, Long>, CustomRecordRepository {
    override fun findById(id: Long): Mono<Record>
    fun findByMusicianId(id: Long, pageable: Pageable): Flux<Record>
    @Query("SELECT COUNT(id) FROM record WHERE musician_id = :musicianId")
    fun countByMusicianId(@Param("musicianId") musicianId: Long): Mono<Long>
    @Query("""
            SELECT musician.name AS musicianName,
                   musician.genre,
                   musician.created_at AS mCreatedAt,
                   musician.updated_at AS mUpdatedAt,
                   record.*
              FROM record
              INNER JOIN musician
              ON record.musician_id = musician.id
        """)
    fun findRecords(): Flux<Record>
}
```
현재는 조건이 없지만 조건이 있다면 기존 `countByMusicianId`함수처럼 파라미터를 받아서 바인딩하면 된다.

다음과 같이 커스텀 컨버터를 만들자.

`R2DBC`에서는 `@ReadingConverter`, `@WritingConverter`두개를 지원한다.

보통 `@WritingConverter`의 경우에는 `insert/update`시 `@Query`를 이용해 네이티브 쿼리로 작업을 할 때 사용한다.

예를 들면 뮤지션이나 레코드처럼 장르의 경우에는 `enum`으로 처리하고 있는데 이것을 `@WritingConverter`를 활용해서 따로 매핑을 해 줄 수 있다.

그중에 우리는 `@ReadingConverter`를 활용한다.

```kotlin
@ReadingConverter
class RecordReadConverter: Converter<Row, Record> {

    override fun convert(row: Row): Record {
        val musician = Musician(
            name = row.get("musicianName", String::class.java)!!,
            genre = Genre.valueOf(row.get("genre", String::class.java)!!),
            createdAt = row.get("mCreatedAt", LocalDateTime::class.java),
            updatedAt = row.get("mUpdatedAt", LocalDateTime::class.java),
        )

        val record = Record(
            id = row.get("id", Long::class.javaObjectType)!!,
            musicianId = row.get("musician_id", Long::class.javaObjectType)!!,
            title = row.get("title", String::class.java),
            label = row.get("label", String::class.java),
            releasedType = ReleasedType.valueOf(row.get("released_type", String::class.java)!!),
            releasedYear = row.get("released_year", Int::class.javaObjectType)!!,
            format = row.get("format", String::class.java),
            createdAt = row.get("created_at", LocalDateTime::class.java),
            updatedAt = row.get("updated_at", LocalDateTime::class.java),
        )
        record.musician = musician
        return record
    }
}
```
앞서 구현한 형태와는 크게 다르지 않다.

다만 이 경우에는 `R2DBC`환경에서 만든 커스텀 컨버터를 사용할수 있도록 등록을 해줘야 한다.

```kotlin
@Configuration
@EnableR2dbcAuditing
class R2dbcConfiguration {

    @Bean
    fun r2dbcCustomConversions(databaseClient: DatabaseClient): R2dbcCustomConversions {
        val dialect = DialectResolver.getDialect(databaseClient.connectionFactory)
        val converters = ArrayList(dialect.converters)
        converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS)
        return R2dbcCustomConversions(
            CustomConversions.StoreConversions.of(dialect.simpleTypeHolder, converters),
            getCustomConverters()!!
        )
    }

    private fun getCustomConverters(): List<Any?>? {
        return listOf<Any?>(RecordReadConverter())
    }

}
```

`WebFluxConfigurer`를 상속받아서 사용할 수 있지만 현재 우리는 스프링 부트에서 `application.yml`을 통해서 커넥션 설정을 하고 있다.

하자만 `WebFluxConfigurer`를 사용하게 되면 불필요하게 `connectionFactory()`를 강제 구현해야 한다.

사용자가 설정한 값이 `primary`로 등록되기 때문에 코틀린의 `TODO()`를 통해 그냥 놔둬도 되지만 위 코드와 같이 설정을 할 수 있다.

그래서 `DatabaseClient`로부터 정보를 가져와서 처리하는 방식을 활용한다.  

만일 새로운 커스텀 컨버터를 생성하면 위 코드에서 추가해 주면 된다.

### OneToMany Relation
`one-to-many`의 경우에는 몇 가지 문제가 잇다. 

그 중에 하나가 페이징 처리이다. 

실제로 `jpa`에서는 이 경우에는 기존과는 다른 방식을 사용해야 한다.

곰곰히 생각해 보면 이는 당연한 결과이다.

예를 들어 뮤지션과 레코드의 관계에서 뮤지션을 기준으로 레코드와 조인을 하게 되면 데이터가 뻥튀기가 된다.    

쉽게 사용하고자 한다면 `@EntityGraph`를 활용하는 것이다.

물론 특정 뮤지션을 조건 검색으로 레코드를 가져온다면 가능하지만 그렇지 않다면 조인 이후 모든 정보를 조회해서 가져오게 된다.

그리고 로그에는 메모리 관련 경고가 뜰 것이다.

그래서 보통 `application.yml`에 전역 배치 사이즈를 설정하거나 해당 필드에 `@BatchSize`를 설정한다.

`jpa`에서는 이렇게 설정하면 좀 독특하게 작동하는데 먼저 뮤지션을 페이징처리로 가져온다.

그리고 리스트의 뮤지션 아이디값을 리스트로 묶어 레코드에서 `in`조건으로 가져온다.      

여기서는 예제로 특정 뮤지션을 조회할 때 음반도 같이 가져오도록 하는 `API`를 만들어 볼까 한다.

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
) {
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var records: List<Record>? = null
}
```
그리고 데모용으로 레파지토리에도 추가한다.

```kotlin
interface CustomMusicianRepository {
    fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician>
    fun musiciansByQuery(match: Query): Flux<Musician>
    fun totalCountByQuery(match: Query): Mono<Long>

    fun musicianWithRecord(id: Long): Mono<Musician>
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

    override fun musicianWithRecords(id: Long): Mono<Musician> {
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
                    .next()
    }

}
```
일단 코드가 너무 복잡한다.

`bufferUntilChanged`를 통해서 뮤지션 아이디로 `row`정보를 그룹핑하고 `map`부분을 매퍼로 정의해서 처리하면 깔끔해 질것이다.

이 경우에는 페이징 처리를 하는데 무리가 없다.      

하지만 이런 방식이라면 차라리 `record`에서 `many-to-one`으로 처리하는게 좀 더 나아보인다.

# At a Glance
이 방법을 사용한게 몇 년전이다.

그러다보니 이 프로젝트는 좀 지지부진한 감이 없지 않아 있다.

게다가 `jooQ`에서는 이와 관련 자체적으로 `R2DBC`를 지원하고 있는데 `queryDsl`는 그렇지 않다.    

그런데 `queryDsl`공식 사이트의 `Related projects`항목의 `Extensions`에 흥미로운 게 하나 있다.

여기에 `infobip-spring-data-querydsl`프로젝트 링크가 있어서 사용해 본 경험이 있는데 사실 이대로 사용하기에는 설정을 해야하는게 많다.

어째든 이 프로젝트와 관련해서 이 라이브러리를 사용하며 고통받앗던 경험을 살려서 한번 최종적으로 사용한 브랜치도 소개해 볼까 한다.

기회가 되면 `jooQ`와 연계된 프로젝트도 진행해 볼까 한다.

그 다음은 `functional endpoints`로 변경해 보고자 한다.
