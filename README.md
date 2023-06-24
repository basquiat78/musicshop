# Using JWT

`jooQ`를 이용한 브랜치를 기준으로 작성한다.

# springdoc 설정

```groovy
plugins {
    id("org.springdoc.openapi-gradle-plugin") version "1.6.0"
}

dependencies {
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.1.0")
}
```
웹플럭스용 `springdoc`은 작성 시점에서는 `org.springdoc:springdoc-openapi-starter-webflux-ui:2.1.0`이 버전을 사용한다.

이 때 다음과 같이 `id("org.springdoc.openapi-gradle-plugin") version "1.6.0"`플러그인 설정을 해줘야 한다.

`OpenApiConfiguration`설정 파일
```kotlin
@Configuration
class OpenApiConfiguration {

    @Bean
    fun groupedOpenApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
                             .group("musicshop")
                             .pathsToMatch("/api/**")
                             .build()
    }

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "bearerAuth"
        return OpenAPI().addSecurityItem(SecurityRequirement().addList(securitySchemeName))
                        .components(
                            Components().addSecuritySchemes(securitySchemeName, SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        )
                        .info(
                            Info().title("MusicShop with WebFlux API")
                                  .description("웹플럭스로 만든 뮤직샵 API")
                                  .version("v3")
                        )
    }

}
```

짧게 하고 싶어서 다음과 같이 줄였다.
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /musicshop
```

[http://localhost:8080/musicshop](http://localhost:8080/musicshop)

`api-docs`를 띄우는데만 포커스를 둔다. 

따라서 컨트롤러나 객체에 어노테이션을 덕지덕지 붙이지 않을 것이다.

# JWT Token

`jwt`토큰을 이용해 처음 가입하고 로그인을 할 때 토큰을 발급해서 그 토큰을 통해 사용자를 인증하는 방식을 사용해 보고자 한다.

먼저 `jwt`토큰을 생성할 때는 중요한 정보를 포함해서 만드는 것은 특별한 경우가 아니면 하지 않는 것이 좋다.

해당 어플리케이션으로부터 `jwt`토큰을 받는 것 자체가 디비로부터 특정 아이디 또는 이메일이나 전화번호를 유니크 아이디로 패스워드를 받아서 체크하고 생성하기 때문이다.

따라서 일반적으로 `jwt`토큰을 생성할 떄는 유니크 아이디와 `Role`이 있다면 `Role`정보 정도만 가지고 생성하는 것이 좋다. 

패스워드는 포함하지 않는 방식이 좀 더 안전한 방식일 것이다.

어째든 `jwt`토큰을 발급하고 특`uri`에 접근하거나 `API`를 호출할 때 이 토큰을 분석해서 접근 가능한 유저인지 체크하게 된다.

하지만 몇 가지 생각해 볼 것이 `uri`나 `API`를 호출할 때 마다 건당 디비에 접근해서 유효한 사용자인지 체크하는 것은 그다지 좋아 보이지 않는다.

그렇다고 세션을 쓰면 `Stateless`하게 운영하기 위해 `jwt`을 사용하는 이유가 사라진다.

따라서 보통 이런 방식을 적용할 때는 사용자 정보를 토큰를 키로 캐쉬 메모리에 저장하고 꺼내쓰는 방식을 사용한다.

가장 많이 사용하는 방식이 `redis`를 활용하는 것이다.

하지만 여기서는 `redis`를 깔고 사용하기에는 범위에서 벗어난다.

그래서 페이크 캐시 매니저를 작성해서 캐시 매니저를 두고 꺼내 쓰도록 할것이다.

~~그러나 번외로 작업을 할것이다.~~

다만 `redis`운영을 염두해 두고 인터페이스를 구현하도록 해서 구현체를 후에 커스텀으로 작성한 캐시 매니저에서 `redis`를 사용하는 구현체로 바꿀 수 있도록 구성한다.

# pre setting

그레이들에 다음과 같이

```groovy
dependencies {
    // ..
    implementation("org.springframework.security:spring-security-crypto")
    implementation("com.auth0:java-jwt:4.4.0")
    // ..
}
```
를 설정하자.

나중에 스프링 시큐리티를 쓰게 되면 `org.springframework.security:spring-security-crypto`가 포함되지만 지금은 사용하지 않고 있다.

이것은 사용자의 비밀번호를 암호화해서 저장할 때 쓸 것이다.

# 암복호화 및 비밀번호 암호화

`CryptoUtils`유틸리티 

```kotlin
class CryptoUtils {

    companion object {
        /** 임의의 secret key AES에서는 16비트 */
        private const val ALGORITHM = "AES"
        private const val ALGORITHM_MODE = "AES/CBC/PKCS5Padding"
        private const val KEY = "123457689basquiatisgreatartistok"
        private val IV = KEY.substring(0, 16)

        fun encrypt(text: String): String {
            val cipher = Cipher.getInstance(ALGORITHM_MODE)
            cipher.init(Cipher.ENCRYPT_MODE,
                        SecretKeySpec(KEY.toByteArray(), ALGORITHM),
                        IvParameterSpec(IV.toByteArray())
            )
            val encrypted = cipher.doFinal(text.toByteArray(charset(StandardCharsets.UTF_8.name())))
            return Base64.getEncoder().encodeToString(encrypted)
        }

        fun decrypt(cipherText: String): String {
            val cipher = Cipher.getInstance(ALGORITHM_MODE)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(KEY.toByteArray(), ALGORITHM),
                IvParameterSpec(IV.toByteArray())
            )
            return String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8)
        }

        fun encryptPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())

        fun matchPassword(password: String, encrypted: String) = BCrypt.checkpw(password, encrypted)

    }
}

```
굉장히 일반적인 코드로 흔하게 사용하는 양방향 방식은 `AES`채택한다. 

`이메일/핸드폰번호/주민번호`같은 개인정보의 경우에는 비지니스 로직상에서 복호화해서 사용해야 하는 경우가 있다.

따라서 디비에는 암호화해서 저장하고 필요한 경우에는 복화화해서 사용할 수 있도록 한다.

비밀번호의 경우에는 단방향으로 암호화만 하고 `matchPassword`를 통해 동일한 비밀번호인지 체크만 하도록 설정한다.

이런 이유로 비번찾기는 인증이후 새로 비밀번호를 설정하도록 하는 것이다.

# JWT Utils

`jwt`와 관련된 라이브러리는 상당히 많다.

가령 `jjwt`라든가 사용해본 적은 없지만 `nimbus-jose-jwt`같은 녀석들이 존재한다.

하지만 나의 경험은 `com.auth0:java-jwt`이라 기존에 익숙한 라이브러리를 사용할 것이다.

하지만 어떤 라이브러리를 사용하던지 대부분 사용하는 방식이 똑같기 때문에 무엇을 선택하든 그건 여러분의 몫이다.

어째든 저 라이브러리는 우리가 자주 만나게 되는 `baeldung`사이트에서 아주 친절하게 설명하고 있다.

[Managing JWT With Auth0 java-jwt](https://www.baeldung.com/java-auth0-jwt)      

아시는 분들은 패스해도 되고 처음 하시는 분들은 한번 읽어 보시면 될 것이다.

일단 `jwt`토큰 생성시 필요한 값들을 `application.yml`에 정의할 것이다.

```yaml
jwt:
  issuer: basquiat
  subject: musicshop
  # 초단위 3600 -> 1시간
  expires-at: 3600
  # Jean-Michel Basquiat가 사망한 날짜
  secret: basquiatdie19880812
  prefix: Bearer
```
이 값은 프로젝트에 맞춰서 사용하면 된다.

개인 프로젝트라 이렇게 심플하게 작성한다.

하지만 실무에서 적용하고자 한다면 `yml`보다는 프라이빗하게 관리하는 방식을 권장한다.

# JwtProperties

스프링 부트가 `yml`정보를 읽는 방식은 `spring-boot-configuration-processor`에 따른다.

그리고 우리도 위에 작성한 `jwt`정보를 객체에 바인딩 할 수 있다.

그러기 위해서는 코틀린의 그레이들에서 다음과 같이 설정을 추가해 줘야 한다.

```groovy
plugins {
    kotlin("kapt") version "1.8.21"
}

dependencies {
    kapt("org.springframework.boot:spring-boot-configuration-processor")
}
```

`JwtProperties`클래스
```kotlin
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties @ConstructorBinding constructor(
    val issuer: String,
    val subject: String,
    val expiredAt: Long,
    val secret: String,
    val prefix: String,
)
```
이 때 `yml`에 `expired-at`은 바인딩할때 카멜 방식으로 하게 된다.

또한 설정 방식이 `@Component`를 통해 설정이 가능했던 자바와 다르기 때문에 이 부분은 주의를 요한다.

마무리로 `MusicshopApplication`클래스에 `@ConfigurationPropertiesScan`을 붙여주면 끝!

또는 다른 설정 클래스를 선언하고 거기에 붙여도 된다.

```kotlin
@EnableWebFlux
@SpringBootApplication
@ConfigurationPropertiesScan
class MusicshopApplication

fun main(args: Array<String>) {
	runApplication<MusicshopApplication>(*args)
}
```

# JwtTokenUtils

[Managing JWT With Auth0 java-jwt](https://www.baeldung.com/java-auth0-jwt)의 내용을 참조하자.

`BadAuthorizeTokenException`를 만들고 `ExceptionAdvice`에서 전역으로 처리할 수 있도록 등록한다.

```kotlin
/**
 * generateAuthToken
 * @param jwtClaim
 * @param props
 * @return String
 */
fun generateAuthToken(jwtClaim: JwtClaim, props: JwtProperties): String {
    val algorithm = Algorithm.HMAC256(props.secret)
    return JWT.create()
              .withIssuer(props.issuer)
              .withSubject(props.subject)
              .withIssuedAt(from(now().atZone(systemDefault()).toInstant()))
              .withExpiresAt(from(now().plusSeconds(props.expiredAt).atZone(systemDefault()).toInstant()))
              .withClaim("memberId", jwtClaim.memberId)
              .withClaim("email", jwtClaim.email)
              .sign(algorithm)
}

/**
 * 토큰으로부터 프리픽스로 붙은 부분을 제거하고 jwt를 추출한다.
 * @param token
 * @return String
 */
fun extractToken(token: String, props: JwtProperties): String {
    return try {
        val prefix = token.split(" ")[0]
        if(props.prefix != prefix) {
            throw BadAuthorizeTokenException()
        }
        token.split(" ")[1]
    } catch (e: Exception) {
        throw BadAuthorizeTokenException("유효한 토큰 타입과 형식이 아닙니다.")
    }
}

/**
 * 토큰 검증 및 DecodedJWT반환
 * @param token
 * @param props
 * @return DecodedJWT
 * @throws BadAuthorizeTokenException
 */
fun decodedJWT(token: String, props: JwtProperties): DecodedJWT {
    val algorithm: Algorithm = Algorithm.HMAC256(props.secret)
    return try {
        JWT.require(algorithm)
           .withIssuer(props.issuer)
           .withSubject(props.subject)
           .build()
           .verify(token)
    } catch (e: Exception) {
        when (e) {
            is TokenExpiredException -> throw BadAuthorizeTokenException("토큰이 만료되엇습니다.")
            else -> throw BadAuthorizeTokenException()
        }
    }
}

/**
 * claim으로부터 userId 추출
 * @param decodedJWT
 * @return String
 */
fun memberIdFromJWT(decodedJWT: DecodedJWT): Long {
    //return decodedJWT.getClaim("memberId").asLong()
    return decodedJWT.claims["memberId"]?.asLong() ?: throw BadAuthorizeTokenException()
}

/**
 * claim으로부터 password 추출
 * @param decodedJWT
 * @return String
 */
fun emailFromJWT(decodedJWT: DecodedJWT): String {
    //return decodedJWT.getClaim("email").asString()
    return decodedJWT.claims["email"]?.asString() ?: throw BadAuthorizeTokenException()
}
```
여기서 각각의 함수들은 미리 만들어서 사용하고자 한다.

내용은 주석을 참조하자.

`generateAuthToken`함수의 경우에는 필요에 따라서 `withClaim`를 통해서 정보를 더 추가할 수도 있다.

소셜 로그인의 예를 들면 넘어오는 정보에는 사용자의 썸네일/유저이름 등의 추가적인 정보가 더 오기도 한다.

따라서 어플리케이션의 요구사항에 따라 맞추면 되는 부분이다.

여기서는 딱 멤버 아이디와 이메일만 정보를 담을 것이다.

`decodedJWT`에서 `try ~ catch`에서 에러를 잡을 때 해당 토큰이 만료되는 경우에는 `TokenExpiredException`를 던진다.

다른 부분은 그냥 처리하고 이 경우에는 메세지를 좀 더 세분화해서 보내주도록 하자.

# Custom Annotation 설정

`AuthorizeToken`어노테이션을 하나 생성을 하자.

이때 우리는 2가지 방식을 통해서 처리를 해 볼 수 있다.

그 중에 먼저 `HandlerMethodArgumentResolver`을 사용하는 방식을 먼저 체크해 보자.

# Using HandlerMethodArgumentResolver

`HandlerMethodArgumentResolver`은 컨트롤러에서 리퀘스트로 들어온 정보중 조건에 맞는 파라미터가 있다면 원하는 정보로 바인딩을 해주는 인터페이스이다.

가장 잘 알려진 인터페이스중 하나인데 보통 `jwt`토큰의 경우에는 헤더에 `Authorization`키로 보내주게 되어 있다.

이런 방식을 사용하는 대표적인 예가 컨트롤러 상에서 사용하는 `@RequestHeader`, `@PathVariable`, `@RequestParam`를 예로 들 수 있다.

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestHeader {
	@AliasFor("name")
	String value() default "";
	@AliasFor("value")
	String name() default "";
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
```
여기서 보면 타겟 부분이 `PARAMETER`로 이것을 처리하는 리졸버는 `RequestHeaderMethodArgumentResolver`이다.

이 클래스르 따라가다보면 최종적으로 `HandlerMethodArgumentResolver`를 구현하고 있는 것을 볼 수 있다.

친절하게도 어떻게 사용하는지 잘 보여주고 있으니 먼저 이것을 사용해 보고자 한다.

일단 위에서 언급한 `AuthorizeToken`어노테이션을 다음과 같이 만들자.


```kotlin
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class AuthorizeToken
```
코틀린에서 타겟부분은 `ElementType.PARAMETER`와 대응되는 것이 `AnnotationTarget.VALUE_PARAMETER`이다.

예제로 아이디로 뮤지션 정보를 가져오는 컨트롤로 함수에 다음과 같이 설정을 하자.

여기서 `token`값은 헤더에 담겨져 올 것이고 리졸버가 위임해서 해당 파라미터에 값을 바인딩 하게 된다.

따라서 스웨거문서에서는 `@Parameter(hidden = true)`을 붙여서 보이지 않도록 한다.

처음 `springdoc`를 설정할 때 `addSecurityItem`를 통해서 설정한 것이 있는데 스웨거에서 테스트 할때 이것을 활용하고자 한다.

```kotlin
@GetMapping("/{id}")
@ResponseStatus(HttpStatus.OK)
suspend fun fetchMusician(@PathVariable("id") id: Long, @AuthorizeToken @Parameter(hidden = true) token: String): Musician {
    return readMusicianUseCase.musicianById(id)
}
```
이제는 커스텀 리졸버를 만들어 보자.

`HandlerMethodArgumentResolver`는 `MVC`와 `WebFlux`용이 따로 존재한다. 

이에 맞춰 상속을 하고 구현을 하도록 하자.

```kotlin
@Component
class AuthorizeTokenMethodResolver(
    private val props: JwtProperties,
): HandlerMethodArgumentResolver {
    /** 컨트롤러의 파라미터에서 해당 어노테이션이 있는지 확인한다. */
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthorizeToken::class.java)
    }

    /** 만일 있다면 어노테이션이 붙어 있는 파라미터에 값을 바인드 하는 역할을 하게 된다. */
    override fun resolveArgument(
        parameter: MethodParameter,
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ): Mono<Any> {
        val bearerToken = exchange.request.headers["Authorization"]?.first() ?: throw BadAuthorizeTokenException("헤더에 Authorization 정보가 존재하지 않습니다.")
        return extractToken(bearerToken, props).toMono()
    }

}
```

이렇게 만든 커스텀 리졸버를 등록해야 한다.

`WebFluxConfiguration`클래스

```kotlin
@Configuration
class WebFluxConfiguration(
    private val authorizeTokenMethodResolver: AuthorizeTokenMethodResolver,
) : WebFluxConfigurer {

    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        super.configureArgumentResolvers(configurer)
        configurer.addCustomResolver(authorizeTokenMethodResolver)
    }
}
```
이제는 스웨거를 접속해 보자.

먼저 `/api/v1/musicians/{id}`를 실행하면 `Authorization`가 없기 때문에 

```json
{
    "code": 401,
    "message": "헤더에 Authorization 정보가 존재하지 않습니다."
}
```
이런 에러가 발생할 것이다.

`spring-docs`설정이 제대로 되었다면 다음과 깉아 상단에 아래와 같은 그지를 볼 수 있다.

![이미지](https://raw.githubusercontent.com/basquiat78/sns/master/swagger.png)

`Authorize`를 누르면 팝업창이 뜬다.

이것은 또한 각 `API`마다 🔒요렇게 생긴 잠금 표시를 볼 수 있다.

거기를 눌러서 설정을 해도 상관없다.

어째든 여기서 그냥 `xxxxxxxxxxx`같은 아무 값이나 넣는다.

그리고 실행을 하게 되면 

```
curl -X 'GET' \
  'http://localhost:8080/api/v1/musicians/1' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer xxxxxxxxxx'
```
다음과 같이 `Bearer `가 자동으로 붙어서 넘어간다.

실제로 스웨거 문서에는 `PathVariable`만 설정해도 헤더에 토큰 정보를 넘겨주고 이것을 리졸버가 받아서 처리한다.

그래서 토큰 값이 넘어오게 된다. 

디버깅을 해보면 바로 알 수도 있고 로그를 찍어 봐도 바로 알수 있다.

이제부터는 토큰이 필요한 부분에는 `@AuthorizeToken`을 사용할 것이다.

# 포스트맨으로 테스트한다면?

![포스트맨](https://raw.githubusercontent.com/basquiat78/sns/master/postman1.png)

그림에서 보면 `Headers`탭을 클릭해서 헤더에 키를 추가해서 저렇게 하는 걸 먼저 생각할 수 있다.

이 방법도 당연히 된다. 하지만 여러분들은 테스트하다보면 바로 불편함을 느끼게 된다.

토큰 만료이후 새로운 토큰을 발급받은 이후에 `value`항목에 복붙복이 힘들어지기 때문이다.

왜냐하면 `Bearer`를 토큰 앞에 붙여야 하기 때문이다.

위 그림에서 `Headers`탭 영역 왼쪽에 있는 `Authorization`을 사정없이 눌러보자.

![포스트맨](https://raw.githubusercontent.com/basquiat78/sns/master/postman2.png)

그림을 보면 바로 이해할 수 있을 것이다.

여기서 우리는 `Bearer Token`을 선택하고 우측 영역 `Token`에 값을 붙여서 테스트하면 된다.

# Member 도메인 생성

이제는 `가입 (signUp)`과 `로그인 (signIn)`행위가 벌어질 것이기 때문에 이와 관련 디비에 생성을 해야 한다.

`sql_schema`파일에 작업을 해놨다.

# 시나리오

먼저 가입과 로그인은 토큰과 관련이 없다. 

하지만 여기서 뮤지션의 정보와 레코드 정보를 가져오는 `API`의 경우에는 오픈하고 생성 및 업데이트 부분에는 토큰 정보를 가진 사용자만 가능하도록 한다.

물론 모든 `API`에 대해서 토큰 생성시 `read/write`권한 롤을 부여하는 방식으로 처리할 수도 있을 것이다.

하지만 여기서는 `write`부분에 대해서만 처리하도록 작업한다.

나머지는 지금까지 얻은 지식을 기반으로 응용단계이기 때문에 그 부분은 개인적으로 처리하면 될 것이다.

## 1. 회원가입

회원가입시 이메일/이름/비밀번호를 받을 것이다.

하지만 만일 이미 존재하는 이메일이 있는지 확인을 할 것이고 없다면 회원가입 진행을 할 것이다.

## 2. 로그인

먼저 심플한 방식부터 시작을 해 나가자.

로그인이 들어오면 디비로부터 정보를 확인하고 유저가 맞다면 토큰을 생성해서 응답객체에 담아주면 될 것이다.

## 3. 로그아웃

아직 캐시를 적용하지 않았기 때문에 지금은 빈 상태로 나둔다.

## 전체적인 시나리오

다음과 같은 플로우를 가질 것이다.

`회원가입 -> 로그인 -> 트큰 정보를 받는다. (클라이언트 또는 디바이스에 저장)` -> `토큰을 사용해 API에 접근한다`

```kotlin
@Service
class ReadMemberService(
    private val memberRepository: MemberRepository,
    private val props: JwtProperties,
) {

    suspend fun signIn(email: String, password: String): JwtTokenInfo {
        val member = memberRepository.findByEmail(email) ?: notFound("이데일 [$email]로 조회되는 멤버가 없습니다. 이메일을 다시 한번 확인하세요.")
        if(!matchPassword(password, member.password)) {
            throw BadParameterException("비밀번호가 일치하지 않습니다. 비밀번호를 다시 한번 확인하세요.")
        }
        val jwtClaim = JwtClaim(memberId = member.id!!, email = member.email)
        return JwtTokenInfo(memberId = member.id!!, email = member.email, token = generateAuthToken(jwtClaim, props))
    }
}

@Service
class WriteMemberService(
    private val memberRepository: MemberRepository,
) {
    suspend fun signUp(member: Member): Member {
        if(memberRepository.existsByEmail(decrypt(member.email))) {
            throw DuplicatedMemberException()
        }
        return memberRepository.save(member)
    }

    suspend fun logout() {
        TODO("구현 예정")
    }
}

class CustomMemberRepositoryImpl(
    private val query: DSLContext,
): CustomMemberRepository {

    override suspend fun existsByEmail(email: String): Boolean {
        val member = JMember.MEMBER
        val sqlBuilder = query.selectCount()
                              .from(member)
                              .where(member.EMAIL.eq(encrypt(email)))
        return sqlBuilder.awaitSingle().value1().toLong() > 0
    }

}

```
현재는 이 정도 선에서 처리를 한다.

먼저 넘어온 토큰값에 대해 유효성 검사를 하는 서비스를 하나 만든다.

```kotlin
@Service
class MemberCacheService(
    private val memberRepository: MemberRepository,
    private val props: JwtProperties,
) {

    suspend fun memberByJWT(token: String): Member {
        val decodedJWT = decodedJWT(token, props)
        return memberRepository.findByIdAndEmail(memberIdFromJWT(decodedJWT), emailFromJWT(decodedJWT))
            ?: throw BadAuthorizeTokenException()
    }

}
```
만일 백오피스를 염두해 둔다면 어드민이 뮤지션/레코드의 정보를 수정하거나 생성할 때 어떤 어드민 계정으로 생성 및 수정했는지 로그를 남길 필요가 있을 수 있다.

그것을 위해서 해당 함수는 `Member`엔티티를 반환하도록 만들었다.

하지만 지금 하고자 하는 작업은 토큰 정보를 통해 멤버를 가져오고 만일 토큰이 유효하지 않다면 `BadAuthorizeTokenException`을 던지도록 하자.

이제부터는 `Write`가 일어나는 `useCase`에서 이를 처리하도록 해보자.

전체 코드에서 완성된 코드를 살펴보면 될것이다.

# Token Validation

아래 코드는 생성/수정을 담당하는 엔트포인트만 보여주고 있는 코드이다.

```kotlin
@Validated
@RestController
@RequestMapping("/api/v1/records")
class RecordController(
    private val readRecordUseCase: ReadRecordUseCase,
    private val writeRecordUseCase: WriteRecordUseCase,
    private val memberCacheService: MemberCacheService,
) {

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createRecord(@RequestBody @Valid command: CreateRecord,
                             @AuthorizeToken @Parameter(hidden = true) token: String
    ): Record {
        memberCacheService.memberByJWT(token)
        return writeRecordUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun updateRecord(@PathVariable("id") id: Long,
                             @RequestBody @Valid command: UpdateRecord,
                             @AuthorizeToken @Parameter(hidden = true) token: String
    ): Record {
        memberCacheService.memberByJWT(token)
        return writeRecordUseCase.update(id, command)
    }

}

@Validated
@RestController
@RequestMapping("/api/v1/musicians")
class MusicianController(
    private val readMusicianUseCase: ReadMusicianUseCase,
    private val writeMusicianUseCase: WriteMusicianUseCase,
    private val memberCacheService: MemberCacheService,
) {

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createMusician(@RequestBody @Valid command: CreateMusician,
                               @AuthorizeToken @Parameter(hidden = true) token: String
    ): Musician {
        memberCacheService.memberByJWT(token)
        return writeMusicianUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun updateMusician(@PathVariable("id") id: Long,
                               @RequestBody @Valid command: UpdateMusician,
                               @AuthorizeToken @Parameter(hidden = true) token: String
    ): Musician {
        memberCacheService.memberByJWT(token)
        return writeMusicianUseCase.update(id, command)
    }

}
```
다음과 같이 `write`가 일어나기 전에 넘어온 토큰 정보를 통해 먼저 인증을 하는 방식이다.

먼저 우리가 `yml`에서 토큰의 만료기간을 3600초, 즉 1시간을 잡았는데 이것을 대충 60으로 1분으로 잡아서 테스트를 해보자.

그렇다면 1분 경과 후에 토큰이 만료되었다는 메세지를 보내 줄 것이다. 

# 하지만 이 방식은 문제가 있다.

토큰 검사를 할 때마다 해당 사용자의 정보를 디비로부터 가져오는 것은 좀 찜찜하다.

만일 `API`호출이 빈번하지 않다면 이게 큰 문제가 아닐수도 있지만 리소스 낭비인건 확실하다.

# MVC Web 처럼 @Cacheable 사용할 수 없는거야?

```
세팅은 되긴 하지만 
캐싱이 되진 않는다
```
~~개발자 라임 쥑이네~~

관련 스택오버플로우나 이런 곳을 찾아보면 기존의 이 방식은 블록킹이 된다는 얘기가 있다.

게다가 지금은 코틀린의 코루틴을 적용하기 때문에 `suspend`가 붙는 경우 자바로 컴파일시에 `Continuation<T>`가 추가되면서 작동을 하지 않는다고 한다.

이로 인해 키 매핑을 할 수 없다는 이야기를 한다.

[스택 오버플로우](https://stackoverflow.com/questions/64372602/how-to-use-cacheable-with-kotlin-suspend-funcion)

이와 관련해서 `baeldung`에서 몇가지 방안을 제시한다.

[Spring Webflux and @Cacheable Annotation](https://www.baeldung.com/spring-webflux-cacheable)

하지만 현재 코틀린의 코루틴을 사용하고 있기 때문에 이 방식으로 가능하진 않다.

실제로는 스프링에서는 이 `@Cacheable`은 `CacheInterceptor`에서 처리하게 되어 있다.

내부적으로 `CacheAspectSupport`가 이것을 지원하는데 일단 이것을 사용하기에는 좀 무리가 있어 보인다.

~~실력 문제 ㅠㅠ~~

일단 

```groovy
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.6")
}
```
이 두개를 사용하자.

`MemberCacheService`
```kotlin
@Service
class MemberCacheService(
    private val memberRepository: MemberRepository,
    private val props: JwtProperties,
) {

    private val cache = Caffeine.newBuilder()
                                .maximumSize(1_000)
                                .expireAfterAccess(15, TimeUnit.SECONDS)
                                .build(::memberByIdAsync)

    suspend fun memberByJWT(token: String): Member {
        val decodedJWT = decodedJWT(token, props)
        return this.memberByIdAsync(memberIdFromJWT(decodedJWT)).await()
    }

    @Cacheable(value = ["member"], key = "id")
    fun memberByIdAsync(id: Long): Deferred<Member> = CoroutineScope(Dispatchers.IO).async {
        cache.asMap()[id]?.await() ?: cached(id)
    }

    private suspend fun cached(id: Long): Member {
        val member = CoroutineScope(Dispatchers.IO).async {
            memberRepository.findByIdOrThrow(id)
        }
        cache.put(id, member)
        return member.await()
    }

}
```
`suspend`의 경우에는 위에 언급한 스택오버플로우에서 내용이 나와 있다.

물론 이 방식을 커스텀할 수 있는 방법이 있지만 가장 쉬운 방식은 위와 같이 직접 `LoadingCache`를 빌더로 생성하고 관련 정보를 캐쉬에 담는 것이다.

이 방식으로 하게 되면 15초를 만료시간으로 두었기 때문에 처음 `cache hit`를 할때는 없기 때문에 디비로부터 정보를 가져와 캐쉬에 담는다.

그 이후 캐쉬에 있는 정보를 가져오다가 15초이후 만료가 되면 다시 디비를 가져오게 된다.

하지만 이 방식은 지져분해질 수 밖에 없다. 또한 `suspend`로 인해 이것을 우회적으로 사용해서 `Deferred<Member>`객체를 저장한다.

여기서만 사용할 것이라면 나름 손을 크게 대지 않고 사용할 수 있다.

어째든 캐시 전략은 어쩔 수 없이 디비를 봐야 한다면 디비를 최소화하는 것을 목적으로 하기 때문에 현재 프로젝트에서는 이정도 선에서는 최선일 것이다.

게다가 멤버의 정보가 수시로 업데이트 될 일은 없기 때문에 15초는 테스트 용도로 잡은 것이기때문에 길게 잡고 가도 상관없을 것이다.

하지만 만일 웹플럭스 기반의 애플리케이션을 염두하고 캐시 전략을 가져간다고 한다면 이 방식은 사실 사용하기 힘들어질 수 있다.

또한 요구사항이 어떻게 변할지도 모르기 떄문이다.

`Caffeine`라이브러리가 내부적으로는 자바 1.5에 추가된 `ConcurrentHashMap`을 이용하기 때문에 이것을 직접 구현하는 방식도 고민해 볼 수 있다.

# 라이브러리에 기대고 싶지 않다면 직접 만들면 되지~

전통적인 `Mono/Flux`방식인 경우라면 좀 더 편하게 사용할 수 있을지도 모른다.

하지만 코틀린의 코루틴을 기반으로 하기 때문에 직접 캐시매니저를 만들어 보도록 하자.

시나리오는 대략 다음과 같다.

```
1. Caffeine처럼 멀티스레드 환경에서 동시성을 위해 제공되는 ConcurrentHashMap을 사용한다.
2. cache put
3. cache get
4. cache evict -> redis, caffeine에서 사용하는 용어
    - evict라는 말은 redis를 통해 캐시 전략을 가져간 분들이라면 흔히 보는 단어일 것이다.
    - remove라고 해도 상관없다.
5. cache duration
6. 확장을 고려해 어떤 정보를 담는 캐시매니저인지 제네릭하게 만들어야 한다.
```
캐시도 결국 메모리를 사용한다.

이 말은 캐시 전략을 사용할 때 만료기간을 고려해야 한다. 

벤치마킹 대상은 `Caffeine`이다.

완벽하게 똑같이 만들기보다는 기능을 구현하는데에 포커스를 맞추는 것이 목표이다.

```kotlin
@Component
class CustomCacheManager<T> {

    private val cache = ConcurrentHashMap<String, T>()

    suspend fun cached(key: String, value: T) {
        cache[key] = value
    }

    suspend fun cacheEvict(key: String) {
        cache.remove(key)
    }

    suspend fun cacheGet(key: String): T {
        return cache[key] ?: notFound()
    }

}
```
기본적인 것은 위와 같을 것이다. 

하지만 여기서 캐시의 만료기간을 설정할 수 있는 방법이 없다.

따라서 여기서 실제로 캐시에 저장해야 하는 경우에는 `value`쪽에 담아야 하는데 방법은 래퍼 클래스를 만들어서 담을 수 밖에 없다.

`Cached`라는 래퍼 클래스를 만들 것이다. 

속성은 객체 정보와 `LocalDateTime`을 가진다.

```kotlin
data class Cached<T>(
    val value: T,
    val expiredAt: LocalDateTime,
)
```

`application.yml`에
```yaml
cached:
  # 초로 받는다.
  expired-at: 20
```
를 설정하고 프로퍼티를 하나 생성하자.

```kotlin
@ConfigurationProperties(prefix = "cached")
data class CacheProperties @ConstructorBinding constructor(
    val expiredAt: Long,
)
```

`CustomCacheManager`는 다음과 같을 것이다.
```kotlin
@Component
class CustomCacheManager<T>(
    private val cacheProperties: CacheProperties,
) {

    private val cache = ConcurrentHashMap<String, Cached<T>>()

    suspend fun cached(key: String, value: T) {
        cache[key] = Cached(value, now().plusSeconds(cacheProperties.expiredAt))
    }

    suspend fun cacheEvict(key: String) {
        cache.remove(key)
    }

    suspend fun cacheGet(key: String): T {
        return cache[key]?.value ?: notFound()
    }

}
```
하지만 `cacheGet`이 문제다.

캐시가 없다면 다시 해당 정보를 받아야 하기 때문이다.

따라서 이것은 리시버를 받도록 하자.

아마도 이 리시버는 디비로부터 다시 조회하는 녀석일 테니까 익명 함수로 정의할 때

```kotlin
val receiver = () -> T
```
이런 형태일 것이다.

```kotlin
@Component
class CustomCacheManager<T>(
    private val cacheProperties: CacheProperties,
) {

    private val cache = ConcurrentHashMap<String, Cached<T>>()

    suspend fun cached(key: String, value: T) {
        cache[key] = Cached(value, now().plusSeconds(cacheProperties.expiredAt))
    }

    suspend fun cacheEvict(key: String) {
        cache.remove(key)
    }

    suspend fun cacheGet(key: String, receiver: suspend () -> T): T {
        val cached = cache[key]
        return cached?.let {
            it.value
        } ?: cacheIfEmpty(key, receiver)
    }

    private suspend fun cacheIfEmpty(key: String, receiver: suspend () -> T): T {
        val newCached = Cached(receiver(), now().plusSeconds(cacheProperties.expiredAt))
        cache[key] = newCached
        return newCached.value
    }

}
```
캐시에 정보가 있다면 가져오고 없다면 받은 리시버를 통해서 캐쉬에 저장이후 반환하는 방식이다.

하지만 여기서 우리는 만료기간을 체크하지 않는다.

```kotlin
@Component
class CustomCacheManager<T>(
    private val cacheProperties: CacheProperties,
) {

    private val log = logger<CustomCacheManager<T>>()

    init {
        start()
    }

    private val cache = ConcurrentHashMap<String, Cached<T>>()

    suspend fun cached(key: String, value: T) {
        cache[key] = Cached(value, now().plusSeconds(cacheProperties.expiredAt))
    }

    suspend fun cacheEvict(key: String) {
        cache.remove(key)
    }

    suspend fun cacheGet(key: String, receiver: suspend () -> T): T {
        val cached = cache[key]
        return cached?.let {
            it.value
        } ?: cacheIfEmpty(key, receiver)
    }

    private suspend fun cacheIfEmpty(key: String, receiver: suspend () -> T): T {
        val newCached = Cached(receiver(), now().plusSeconds(cacheProperties.expiredAt))
        cache[key] = newCached
        return newCached.value
    }

    private fun start() {
        Mono.defer {
            Flux.interval(ofSeconds(cacheProperties.expiredAt))
                .doOnSubscribe {
                    log.info("scheduled in interval ${cacheProperties.expiredAt}")
                }
                .subscribeOn(newSingle("custom-cache-monitor"))
                .doOnNext {
                    cache.forEach { (key, value) ->
                        if(value.expiredAt.isBefore(now())) {
                            log.info("cached remove by key [$key]")
                            cache.remove(key)
                        }
                    }
                }
                .then()
        }.subscribe()
    }

}
```
물론 비지니스 로직에서 캐시 만료시간을 체크해서 지우는 로직을 추가할 수 있다.

하지만 `Flux`의 `interval`을 이용해 스케쥴처럼 사용하는 방식으로 처리하도록 하자.

하지만 이것은 `redis`를 이용하게 되면 `redis`의 `expired`기능을 활용할 수 있다.

# redis를 활용해 보자.

`redis gui tool`은 최근 대부분이 유료로 변경되면서 무료를 찾아야 한다.

몇 가지가 있는데 그 중에 나는 그냥 심플하게 [redis gui](https://github.com/ekvedaras/redis-gui) 이것을 사용할 것이다.

`redis`에서는 `reactive driver`를 제공한다.

따라서 이와 관련해 많은 기술 블로그에서 이 방법에 대해 잘 알려져 있다.

여기서는 `redisson`을 이용해 볼까 한다.

[Redisson Spring Boot Starter](https://github.com/redisson/redisson/tree/master/redisson-spring-boot-starter)

여길 보면 스프링의 버전 별로 지원하는 것을 알 수 있다.

이 가이드라인에 따라 현재 `3.x`를 사용하기 때문에 버전 역시 `3.x`대로 설정한다.

```groovy
dependencies {
	implementation("org.redisson:redisson-spring-boot-starter:3.22.1")
}
```
작성 시점의 최신 버전은 `3.22.1`이다.

# redisson yml 적용

```yml
# Using common Spring Boot 3.x+ settings:
spring:
  data:
    redis:
      database: 
      host:
      port:
      password:
      ssl: 
      timeout:
      connectTimeout:
      clientName:
      cluster:
        nodes:
      sentinel:
        master:
        nodes:

--- 
# Using common Spring Boot up to 2.7.x settings:

spring:
  redis:
    database: 
    host:
    port:
    password:
    ssl: 
    timeout:
    connectTimeout:
    clientName:
    cluster:
      nodes:
    sentinel:
      master:
      nodes:
```
깃헙의 가이드라인에 따라 버전에 맞춰 설정 포맷을 선택해서 적용하면 된다.

# 인터페이스와 구현체로 분리

```kotlin
interface CustomCacheManager<T> {
    suspend fun cached(key: String, value: T)
    suspend fun cacheEvict(key: String)
    suspend fun cacheGet(key: String, receiver: suspend () -> T): T
}
```
`CustomCacheManager`는 인터페이스로 분리하고 기존의 구현체와 `redisson`을 사용한 구현체를 만들어 보자.

`redisson`의 경우에는 가이드라인을 따라 작업했기 때문에 내용을 보면 쉽게 알 수 있는 내용이다.

기존에 만든 `CustomCacheManagerImpl`클래스
```kotlin
@Component
@Profile("custom")
class CustomCacheManagerImpl<T> (
    private val cacheProperties: CacheProperties,
): CustomCacheManager<T> {

    private val log = logger<CustomCacheManagerImpl<T>>()

    init {
        start()
    }

    private val cache = ConcurrentHashMap<String, Cached<T>>()

    override suspend fun cached(key: String, value: T) {
        cache[key] = Cached(value, now().plusSeconds(cacheProperties.expiredAt))
    }

    override suspend fun cacheEvict(key: String) {
        cache.remove(key)
    }

    override suspend fun cacheGet(key: String, receiver: suspend () -> T): T {
        val cached = cache[key]
        return cached?.let {
            it.value
        } ?: cacheIfEmpty(key, receiver)
    }

    private suspend fun cacheIfEmpty(key: String, receiver: suspend () -> T): T {
        val newCached = Cached(receiver(), now().plusSeconds(cacheProperties.expiredAt))
        cache[key] = newCached
        return newCached.value
    }

    private fun start() {
        Mono.defer {
            Flux.interval(Duration.ofSeconds(cacheProperties.expiredAt))
                .doOnSubscribe {
                    log.info("scheduled in interval ${cacheProperties.expiredAt}")
                }
                .subscribeOn(Schedulers.newSingle("custom-cache-monitor"))
                .doOnNext {
                    cache.forEach { (key, value) ->
                        if(value.expiredAt.isBefore(now())) {
                            log.info("cached remove by key [$key]")
                            cache.remove(key)
                        }
                    }
                }
                .then()
        }.subscribe()
    }

}
```

`redisson`을 이용해 구현한 `RedissonCacheManagerImpl`클래스
```kotlin
@Component
@Profile("redisson")
class RedissonCacheManagerImpl<T> (
    private val redissonClient: RedissonReactiveClient,
    private val cacheProperties: CacheProperties,
): CustomCacheManager<T> {

    override suspend fun cached(key: String, value: T) {
        redissonClient.getBucket<String>(key, StringCodec.INSTANCE)
                      .set(toJson(value), cacheProperties.expiredAt, TimeUnit.SECONDS)
                      .awaitSingleOrNull()
    }

    override suspend fun cacheEvict(key: String) {
        redissonClient.getBucket<String>(key).delete()
    }

    override suspend fun cacheGet(key: String, receiver: suspend () -> T): T {
        return redissonClient.getBucket<String>(key, StringCodec.INSTANCE)
                             .get()
                             .awaitSingleOrNull()?.let {
                                    fromJson(it, Any::class.java) as T
                             } ?: cacheIfEmpty(key, receiver)
    }

    private suspend fun cacheIfEmpty(key: String, receiver: suspend () -> T): T {
        val value = receiver()
        cached(key, value)
        return value
    }

}
```
우리는 프로파일을 통해서 스프링이 빈을 등록할 때 프로파일 정보를 보고 해당 프로파일에 따라 빈으로 등록할 것이다.

이렇게 하면 두 개의 구현체를 상황에 따라서 선택해 사용할 수 있게 된다.

```yaml
spring:
  profiles:
    active: local, redisson
    # active: local, custom
```

이렇게 작업을 하게 되면 `MemberCacheService`에서는 어떤 변경점없이 프로파일 설정만으로 둘 중 하나를 선택해서 사용할 수 있다.

```kotlin
@Service
class MemberCacheService(
    private val memberRepository: MemberRepository,
    private val cacheManager: CustomCacheManager<Member>,
    private val props: JwtProperties,
) {

    suspend fun memberByJWT(token: String): Member {
        return cacheManager.cacheGet(token) {
            val decodedJWT = decodedJWT(token, props)
            val id = memberIdFromJWT(decodedJWT)
            val email = emailFromJWT(decodedJWT)
            memberRepository.findByIdAndEmail(id, encrypt(email)) ?: notFound("아이디 [$id]와 이메일 [$email]로 조회된 멤버가 없습니다.")
        }
    }

}
```

프로파일을 `redisson`으로 선택하게 되면 앞서 설치한 `redis gui`에서 캐시가 등록된 걸 확인 할 수 있다.

해당 툴에서는 설정한 만료시간인 20초가 지나면 자동으로 새로고침을 하면서 캐시가 삭제되었다는 것을 알려 줄 것이다.

# 좀 더 손을 보자.

지금 우리가 작업한 캐시는 단순하게 토큰값을 키로 멤버의 정보를 캐시에 담고 있다.

하지만 다양한 곳에서 사용하기 위해서는 이 방식은 좀 부족하다.

따라서 `CustomCacheManager`이 담는 객체의 이름을 소문자 형식으로 키와 조합하는게 좋아보인다.

예를 들면 `member:{token}`처럼 키를 생성해서 저장하도록 해보자.

먼저 `CustomCacheManager`가 제네릭하게 만들었기 때문에 내부적으로는 `타입 소거`가 이뤄진다.

좀 번거롭긴 하지만 외부로부터 `Class<T>`타압을 넘겨주도록 하자.

```kotlin
interface CustomCacheManager<T> {
    suspend fun cached(key: String, value: T, clazz: Class<T>)
    suspend fun cacheEvict(key: String, clazz: Class<T>)
    suspend fun cacheGet(key: String, clazz: Class<T>, receiver: suspend () -> T): T
}
```

수정된 구현체는 코드에서 직접 확인하면 될것이다.

최종적으로 `TokenValidateService`에서 다음과 같이 객체 타입 정보를 넘겨주면 된다.

```kotlin
@Service
class MemberCacheService(
    private val memberRepository: MemberRepository,
    private val cacheManager: CustomCacheManager<Member>,
    private val props: JwtProperties,
) {

    suspend fun memberByJWT(token: String): Member {
        return cacheManager.cacheGet(token, Member::class.java) {
            val decodedJWT = decodedJWT(token, props)
            val id = memberIdFromJWT(decodedJWT)
            val email = emailFromJWT(decodedJWT)
            memberRepository.findByIdAndEmail(id, encrypt(email)) ?: notFound("아이디 [$id]와 이메일 [$email]")
        }
    }

}
```
이것을 토대로 전체 코드에 약간의 리팩토링을 진행했다.

로그아웃 기능까지 추가했으니 해당 부분은 코드를 통해 확인해 보면 될것이다.

# 리졸버 말고 AOP

사실 커스텀 리졸버로 처리하는 방법이 가장 안정적이다.

예를 들면 커스텀 리졸버에서 `@AuthorizeToken @Parameter(hidden = true) token: String`로 헤더에 있는 값을 바인딩하고 있다.

실제로 헤더에 들어오는 값은 `Bearer xxxxxxx`같은 형식으로 커스텀 리졸버내에서 `resolveArgument`함수에서 앞부분을 잘라서 사용하고 있다.

하지만 `@RequestHeader("Authorization") @Parameter(hidden = true) token: String`의 경우에는 토큰 타입인 `Bearer`가 붙어서 넘어온다.

그렇다면 이것을 사용하는 곳에 앞단에서 일일히 `extractToken`함수를 사용해서 잘라주는 건 좀 귀찮다.

굳이 `@RequestHeader`를 사용하고 싶다면 다음과 같이

```groovy
dependencie {
    implementation("org.springframework.boot:spring-boot-starter-aop")
}
```
설정을 하고 함수에 붙는 커스텀 어노테이션을 만들자.

```kotlin
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Authorized
```

예제로 다음 뮤지션의 컨트롤러중 하나에 달아보다.

```kotlin
@Validated
@RestController
@RequestMapping("/api/v1/musicians")
class MusicianController(
    private val readMusicianUseCase: ReadMusicianUseCase,
    private val writeMusicianUseCase: WriteMusicianUseCase,
    private val readMemberUseCase: ReadMemberUseCase,
) {
    @PatchMapping("/{id}")
    @Authorized
    @ResponseStatus(HttpStatus.OK)
    suspend fun updateMusician(@PathVariable("id") id: Long,
                               @RequestBody @Valid command: UpdateMusician,
                               @RequestHeader("Authorization") @Parameter(hidden = true) token: String
    ): Musician {
        readMemberUseCase.memberByToken(token)
        return writeMusicianUseCase.update(id, command)
    }

}
```
이제부터 이 커스텀 어노테이션이 달린 함수를 체크하는 `AOP`클래스를 만들자.

```kotlin
@Aspect
@Component
class AuthorizedAOP(
    private val props: JwtProperties,
) {

    @Around("@annotation(io.basquiat.musicshop.common.aop.Authorized)")
    fun authorized(joinPoint: ProceedingJoinPoint): Any {
        val args = joinPoint.args.map {
            when(it) {
                is String -> {
                    if(it.startsWith(props.prefix)) {
                        extractToken(it, props)
                    } else {
                        it
                    }
                }
                else -> it
            }
        }
        return joinPoint.proceed(args.toTypedArray())
    }

}
```
이 때 `ProceedingJoinPoint`으로 넘어오는 함수의 시그니처를 순회하면서 그 중에 토큰인 파라미터가 있다면 여기서 앞단의 프리픽스 부분을 지워주자.

그리고 `ProceedingJoinPoint`에서 `proceed`함수에 변경된 함수의 시그니처 정보를 그대로 넘겨주도록 하자.

# 당신의 선택은?

뭐로 가도 로마로 가면 장땡이다 싶으면 나의 경우에는 커스텀 리졸버를 만들어서 처리하는게 좀 더 깔끔해 보인다.

# At a Glance

`jwt token`을 이용한 사용자 검증과 캐시까지 진행을 해보았다.

대부분 실제 운영에서는 `jwt token`을 사용하든 스프링 시큐리티의 세션을 사용하든 `redis`를 캐시 매니저로 사용하는 경우가 상당히 많다.

여기서는 이런 방법을 고려해 볼 수 있다는 것을 가이드라인으로 제시한다.
