# 세션은 메모리다.

처음 스프링 시큐리티를 사용했을 때 이런 궁금증이 생겼다.

```
도대체 SecurityContexHolder의 SecurityContext로부터 인증 정보를 어떻게 가져오지?????
```
이런 생각이 먼저 들었다.

하지만 곰곰히 생각해 보면 `어딘가`로부터 정보를 가져온다는 것은 `그 어딘가`에 정보를 저장했기 때문이다.

`MVC`에서는 `HttpSessionSecurityContextRepository` 그리고 `WebFlux`에서는 `WebSessionServerSecurityContextRepository`에서 그 힌트를 얻을 수 있다.

```java
public class WebSessionServerSecurityContextRepository implements ServerSecurityContextRepository {
 
    // more...
    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return exchange.getSession().doOnNext((session) -> {
            if (context == null) {
                session.getAttributes().remove(this.springSecurityContextAttrName);
                logger.debug(LogMessage.format("Removed SecurityContext stored in WebSession: '%s'", session));
            }
            else {
                session.getAttributes().put(this.springSecurityContextAttrName, context);
                logger.debug(LogMessage.format("Saved SecurityContext '%s' in WebSession: '%s'", context, session));
            }
        }).flatMap(WebSession::changeSessionId);
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        Mono<SecurityContext> result = exchange.getSession().flatMap((session) -> {
            SecurityContext context = (SecurityContext) session.getAttribute(this.springSecurityContextAttrName);
            logger.debug((context != null)
                    ? LogMessage.format("Found SecurityContext '%s' in WebSession: '%s'", context, session)
                    : LogMessage.format("No SecurityContext found in WebSession: '%s'", session));
            return Mono.justOrEmpty(context);
        });
        return (this.cacheSecurityContext) ? result.cache() : result;
    }
}
```
결국 세션 정보는 캐시 메모리에 저장된다.

우리가 토이 프로젝트를 하다보면 느낄 수 없겠지만 만일 동접자가 많다면 그만큼 저장된 세션 정보도 많을 것이다.

이건 서버에 부담이 된다.

그래서 스프링 시큐리티에서도 이 세션를 담는 시간을 설정하게 된다.

게다가 여러 개의 서버를 띄운다고 생각한다면 세션 정보를 공유해야 한다.

그렇지 않으면 각각의 서버에 똑같은 세션 정보가 생성될 것이다.

이런 생각을 해 볼 수 있다.

```
그럼 그 세션 정보를 DB에 저장하면 되잖아??
```

신입 시절에는 딱 저 생각을 했다.

# DB로부터 정보를 가져온다는 것은 I/O가 발생한다.

이건 생각안하는거냐?????

~~응 처음에는 생각 안했어~~

이럴거면 차라리 세션을 쓰는게 더 이득같다는 생각이 든다.     

# 변경된 사항

일반적인 경우 이전 버전에서는 `WebSecurityConfigurerAdapter`를 상속해서 사용했는데 이제는 `Deprecated`가 되었다.

`WebFlux` 역시 `WebFluxConfigurer`를 사용했지만 이 역시 `Deprecated`가 되었다.

지금은 `SecurityFilterChain`으로 통합되었다.

스프링 시큐리티는 `SpringBootWebSecurityConfiguration`을 통해서 `SecurityFilterChain`을 빈으로 등록한다.

하지만 `WebFlux`에서는 `ReactiveManagementWebSecurityAutoConfiguration`을 통해서 `SecurityWebFilterChain`을 빈으로 등록하는 차이가 있다.

두 개를 비교해보면 `SecurityWebFilterChain`는 발행자인 `Mono/Flux`로 반환하는 것을 알 수 있게 된다.

최종적으로 `WebFlux`를 기반으로 하기 때문에 `SecurityWebFilterChain`을 통해 설정들을 세팅하고 빈으로 다시 등록하는 것이다.

방식 자체는 `MVC`에서도 마찬가지이다.

더 `딥다이브`하는 건 차후 스프링 시큐리티는 기회가 된다면 따로 상세하게 풀어보고자 한다.

# Spring Security 설정

```kotlin
@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration(
    private val authenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
) {

    private val noAuthUri = arrayOf(
        "/api/v1/members/signin",
        "/api/v1/members/signup",
        "/musicshop",
        "/api-docs",
        "/webjars/swagger-ui/index.html",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/swagger-resources",
        "/v3/api-docs/**",
        "/proxy/**",
        "/webjars/**"
    )


    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.cors { it.disable() } // (0)
                   .httpBasic { it.disable() } // (1)
                   .csrf { it.disable() } // (2)
                   .formLogin { it.disable() } // (3)
                   .logout { it.disable() } // (4)
                   .authorizeExchange { exchanges ->
                        exchanges.pathMatchers(*noAuthUri).permitAll() // (5)
                                 .anyExchange()
                                 .authenticated() // (6)
                   }
                   .exceptionHandling {
                       it.authenticationEntryPoint(authenticationEntryPoint) // (7)
                       it.accessDeniedHandler(customAccessDeniedHandler) // (8)
                   }
                   .build()
    }
}
```
기존의 `MVC`에서 `formLogin`, `httpBasic`, `csrf`, `cors`에 대한 옵션들을 넣게 되는데 현재 우리는 `jwt`를 통해서 인증하고자 한다.

따라서 이 옵션들은 전부 사용하지 않는 방식으로 진행한다.

최근 스프링 시큐리티를 작업하면서 `and`같은 것들은 `deprecated`된 상태로 그냥 위에서처럼 체이닝을 통해서 설정하도록 되어 있다.

`(5)`의 경우에는 우리가 스웨거를 쓰고 있고 회원가입, 로그인의 경우에는 체크하지 않도록 설정한다.

그 외에는 `(6)`을 통해서 전체 인증을 하도록 설정한다.

`(7)`처럼 인증 관련 오류시 그리고 `(8)`은 롤과 관련된 인가에 대해 익셉션 처리를 하도록 한다.

이때 `WebFlux`는 `ServerAuthenticationEntryPoint`, `ServerAccessDeniedHandler`를 받도록 되어 있다.

`MVC`에서는 `AuthenticationEntryPoint`, `AccessDeniedHandler`를 받도록 되어 있다.


```java
public interface ServerAuthenticationEntryPoint {
    Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex);
}

public interface ServerAccessDeniedHandler {
    Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied);
}
```
클래스를 보면 `Mono<Void>`를 반환한다.

이것을 상속받은 커스텀를 작성한다.

코틀린에서는 `mono`라는 코루틴 빌더를 이용할 수 있다.

이것을 이용해서 두개의 커스텀 핸들러를 작성한다.

```kotlin
@Component
class CustomAuthenticationEntryPoint: ServerAuthenticationEntryPoint {

    override fun commence(exchange: ServerWebExchange, ex: AuthenticationException): Mono<Void> = mono {
        with(exchange.response) {
            statusCode = HttpStatus.UNAUTHORIZED
            headers.contentType = MediaType.APPLICATION_JSON
            val error = ApiError(
                code = HttpStatus.UNAUTHORIZED.value(),
                message = ex.message!!,
                timestamp = now(),
            )
            val buffer = bufferFactory().wrap(toByte(error))
            writeWith(buffer.toMono()).awaitSingle()
        }
    }
}

@Component
class CustomAccessDeniedHandler: ServerAccessDeniedHandler {
    override fun handle(exchange: ServerWebExchange, denied: AccessDeniedException): Mono<Void> = mono {
        with(exchange.response) {
            statusCode = HttpStatus.FORBIDDEN
            headers.contentType = MediaType.APPLICATION_JSON
            val error = ApiError(
                code = HttpStatus.FORBIDDEN.value(),
                message = denied.message!!,
                timestamp = now(),
            )
            val buffer = bufferFactory().wrap(toByte(error))
            writeWith(buffer.toMono()).awaitSingle()
        }
    }
}
```
`response`를 통해서 클라이언트에 보내고자 할 때는 에러 객체를 `ByteArray`로 보내야 한다.

다음과 같이 함수를 하나 만들어보자.

```kotlin
/**
 * kotlin jackson object mapper
 */
val mapper = jacksonObjectMapper().also {
    it.registerModule(JavaTimeModule())
    it.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

/**
 * 객체를 받아서 json 스트링으로 반환한다.
 *
 * @param any
 * @return String
 */
fun <T> toJson(any: T): String = mapper.writeValueAsString(any)

/**
 * json 스트링을 해당 객체로 매핑해서 반환한다.
 *
 * @param json
 * @param valueType
 * @return T
 */
fun <T> fromJson(json: String, valueType: Class<T>): T = mapper.readValue(json, valueType)

fun <T> toByte(any: T): ByteArray = mapper.writeValueAsBytes(any)
```

`CustomAuthenticationEntryPoint`은 인증에 대한 부분이기 때문에 상태값을 `UNAUTHORIZED`로 보낸다.

당연히 `CustomAccessDeniedHandler`은 접근에 대한 인가에 대한 부분이기 때문에 상태값은 `FORBIDDEN`이다.

현재 에러 메세지는 자체 메세지를 받도록 하고 있는데 이 부분은 커스텀 메세지를 작성해도 무방한다.

# 일단 이렇게 기동을 해보자.

스웨거의 경우에는 캐쉬때문에 폼로긴 팝업창이 뜨는데 이 부분은 캐시를 지우면 해결된다.

설정에서 로그인하는 부분은 제외했기 때문에 회원가입 역시 그대로 진행된다.

또한 로그인을 하게 되면 토큰을 잘 발급받을 수 있다.

하지만 뮤지션이나 레코드의 정보를 가져오는 `API`를 호출하게 되면

```json
{
    "code": 401,
    "message": "Not Authenticated"
}
```
과 같은 에러를 볼 수 있다.

# 난 어떤 설정도 하지 않았는데 왜????

면접 볼 때 자주 물어보는 질문이 있다.

```
Filter와 Interceptor의 차이가 뭔지 아세요?
```
`SecurityWebFilterChain`라는 객체 이름에서 눈치를 채신 분들도 있을 것이다.

단순한 면접 질문으로 치부할 수 있겠지만 답을 안다면 스프링 시큐리티를 이해하는데 도움이 된다.

앞서 작성한 커스텀 리졸버를 디버깅을 해보면 바로 알 수 있는데 이 리졸버를 타기 전에 이미 스프링 시큐리티가 먼저 관여를 한다는 것을 알 수 있다.

`Dispatcher Servlet`에 대한 개념은 찾아보도록 하자.

마치 스프링 컨텍스트 밖에서 처리되는 것처럼 작동하면서 디버깅이 걸리지 않는다는 것을 알 수 있다.

바로 스프링 시큐리티가 필터같은 역할을 한다는 것을 알 수 있다. 

이 필터를 통해 어떤 행위를 정의해야 한다.

이때 이 필터에 등록해서 이런 행위를 정의하는 방법은 `securityContextRepository`와 `authenticationManager`을 이용하는 것이다.

```java
public interface ServerSecurityContextRepository {
    Mono<Void> save(ServerWebExchange exchange, SecurityContext context);
    Mono<SecurityContext> load(ServerWebExchange exchange);
}

@FunctionalInterface
public interface ReactiveAuthenticationManager {
    Mono<Authentication> authenticate(Authentication authentication);
}
```
앞서 살펴본 `WebSessionServerSecurityContextRepository`은 바로 `ServerSecurityContextRepository`를 상속구현하고 있다.

여기서 `ServerSecurityContextRepository`는 두 개의 메소드가 존재하는데 `save`는 세션 정보를 저장하는 역할을 한다.

`MVC`에서는 `stateless`하게 설정을 하기 위해서는 다음과 같이 정책을 정의할 수 있다.

```kotlin
fun securityFilterChain(http: ServerHttpSecurity): SecurityFilterChain {
    return http
        // more..
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        // more..
        .build()
}
```


하지만 `WebFlux`에서는 이같은 방식을 사용할 수 없다.

여러 기술 블로그에서는 `NoOpServerSecurityContextRepository`를 사용하면 된다고 한다.

그럼 이 녀석은 어떻게 생겨먹었나?

```java
public final class NoOpServerSecurityContextRepository implements ServerSecurityContextRepository {

    private static final NoOpServerSecurityContextRepository INSTANCE = new NoOpServerSecurityContextRepository();

    private NoOpServerSecurityContextRepository() {
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        return Mono.empty();
    }

    public static NoOpServerSecurityContextRepository getInstance() {
        return INSTANCE;
    }

}
```
인스턴스만 반환하고 나머지는 전부 `Mono.empty()`로 처리하고 있다.

어떤 세션 정보도 저장하지 않고 어떤 것도 검증하지 않는 독특한 녀석이다.

만일 기술 블로그처럼 이 녀석을 사용한다면  `ReactiveAuthenticationManager`로 `Authentication`객체를 넘기는 방식을 바꿔야 한다.

즉, 필터를 등록해서 처리하는 방식으로 처리해야 한다.

여기서는 필터를 등록하는 방식보다는 다른 방식으로 처리할 예정이다.

먼저 `ReactiveAuthenticationManager`를 구현한 커스텀 객체를 만들어보자.

```kotlin
@Component
class CustomAuthenticationManager(
    private val memberCacheService: MemberCacheService,
): ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> = mono {
        val authToken = authentication.credentials.toString()
        memberCacheService.memberByJWT(authToken)
        authentication.toMono().awaitSingle()
    }
}
```
`authentication`로부터 토큰 정보를 가져온 다음에 캐시로부터 멤버 정보를 가져온다.

이때 여기를 통과하지 못하면 에러가 발생할 것이다.

하지만 지금 우리는 기존에 `@RestControllerAdvice`를 통해서 전역 처리를 하고 있다.

여기서 발생하는 에러는 컨트롤러로 타기 전에 발생하는 에러로 이 방식으로는 전역 처리를 할 수가 없다.

이유는 위에서 언급한 것처럼 스프링 컨텍스트 영역 밖의 필터처럼 작동하기 때문이다.

따라서 기존의 `ExceptionAdvice`클래스를 수정해야 한다.

이것을 해결하기 위해서는 다음과 같이 `ErrorWebExceptionHandler`을 구현해서 사용해야 한다.

```kotlin
@Component
class ExceptionAdvice: ErrorWebExceptionHandler {

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> = mono {
        val pairs = when (ex) {
            is NotFoundException -> HttpStatus.NOT_FOUND to (ex.message ?: "조회된 정보가 없습니다.")
            is BadParameterException -> HttpStatus.BAD_REQUEST to (ex.message ?: "파라미터 정보가 잘못되었습니다.")
            is DuplicatedMemberException -> HttpStatus.NO_CONTENT to (ex.message ?: "중복된 사용자입니다.")
            is BadAuthorizeTokenException -> HttpStatus.UNAUTHORIZED to (ex.message ?: "Not AuthorizeToken")
            is AuthenticationException -> HttpStatus.UNAUTHORIZED to (ex.message ?: "Not Authenticated")
            else -> HttpStatus.INTERNAL_SERVER_ERROR to "Internal Server Error"
        }

        with(exchange.response) {
            statusCode = pairs.first
            headers.contentType = MediaType.APPLICATION_JSON
            val error = ApiError(code = pairs.first.value(), message = pairs.second, timestamp = now())
            val dataBuffer = bufferFactory().wrap(toByte(error))
            writeWith(dataBuffer.toMono()).awaitSingle()
        }

    }

}

@RestControllerAdvice
class RestApiAdvice {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(MissingInformationException::class)
    fun handleMissingInformationException(ex: MissingInformationException): Mono<ApiError> {
        return Mono.just(ApiError(
            code = HttpStatus.NOT_FOUND.value(),
            message = ex.message!!,
            timestamp = now(),
        ))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleException(ex: WebExchangeBindException): Mono<ApiError> {
        val errors = ex.bindingResult.allErrors.first()
        return Mono.just(ApiError(
            code = HttpStatus.BAD_REQUEST.value(),
            message = errors.defaultMessage!!,
            timestamp = now(),
        ))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DecodingException::class)
    fun handleJsonParserErrors(ex: DecodingException): Mono<ApiError> {
        val enumMessage = Pattern.compile("not one of the values accepted for Enum class: \\[([^\\]]+)]")
        if (ex.cause != null && ex.cause is InvalidFormatException) {
            val matcher: Matcher = enumMessage.matcher(ex.cause!!.message)
            if (matcher.find()) {
                return Mono.just(ApiError(
                    code = HttpStatus.BAD_REQUEST.value(),
                    message = "enum value should be: " + matcher.group(1),
                    timestamp = now(),
                ))
            }
        }
        return Mono.just(ApiError(
            code = HttpStatus.BAD_REQUEST.value(),
            message = ex.message!!,
            timestamp = now(),
        ))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TypeMismatchException::class)
    fun handleValidationExceptions(ex: TypeMismatchException): Mono<ApiError> {
        val enumMessage = Pattern.compile(".*Sort.*")
        if (ex.cause != null && ex.cause is ConversionFailedException) {
            val matcher: Matcher = enumMessage.matcher(ex.cause!!.message)
            if (matcher.matches()) {
                return Mono.just(ApiError(
                    code = HttpStatus.BAD_REQUEST.value(),
                    message = "Sort Direction should be: [DESC, ASC]",
                    timestamp = now(),
                ))
            }
        }
        return Mono.just(ApiError(
            code = HttpStatus.BAD_REQUEST.value(),
            message = ex.message!!,
            timestamp = now(),
        ))
    }

}
```
기존의 만들어 놨던 전역 에러 처리 클래스는 `RestApiAdvice`라는 클래스로 따로 뺴놔야 한다.

대부분 스프링 시큐리티를 지나 컨트롤러로 들어오는 리퀘스트 정보에 대한 `Validation`에러 처리를 위해서이다.

이제는 `CustomAuthenticationManager`객체로 토큰 정보를 넘겨주는 `ServerSecurityContextRepository`를 커스텀해야한다.

다음과 같이 `ServerSecurityContextRepository`를 상속 구현하는 `CustomSecurityContextRepository`를 만들자.

세션을 사용하지 않기 때문에 `NoOpServerSecurityContextRepository`처럼 `save`함수는 아무것도 하지 않도록 처리한다.

여기서 스프링 부트에서 제공하는 `UsernamePasswordAuthenticationToken`을 사용할 수 있다.

`UsernamePasswordAuthenticationToken`은 좀 특이한 구석이 있다.

일반적으로 어떤 인증 처리이후 `Authentication`을 인증했다는 정보를 설정하도록 되어 있다.

하지만 코드를 보면 `UsernamePasswordAuthenticationToken`에서는 설정할 수 없도록 되어 있다.

```kotlin
@Override
public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    Assert.isTrue(!isAuthenticated,
            "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
    super.setAuthenticated(false);
}
```
내용을 보면 `Authentication`을 인증이 된 믿을 만한 객체로 만들고 싶으면 `GrantedAuthority`리스트를 받는 생성자를 사용하라고 친절하게 알려주고 있다.

실제로 `UsernamePasswordAuthenticationToken`는 두 개의 생성자를 제공한다.

```kotlin
/**
 * This constructor can be safely used by any code that wishes to create a
 * <code>UsernamePasswordAuthenticationToken</code>, as the {@link #isAuthenticated()}
 * will return <code>false</code>.
 *
 */
public UsernamePasswordAuthenticationToken(Object principal, Object credentials) {
    super(null);
    this.principal = principal;
    this.credentials = credentials;
    setAuthenticated(false);
}

/**
 * This constructor should only be used by <code>AuthenticationManager</code> or
 * <code>AuthenticationProvider</code> implementations that are satisfied with
 * producing a trusted (i.e. {@link #isAuthenticated()} = <code>true</code>)
 * authentication token.
 * @param principal
 * @param credentials
 * @param authorities
 */
public UsernamePasswordAuthenticationToken(Object principal, Object credentials,
        Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.credentials = credentials;
    super.setAuthenticated(true); // must use super, as we override
}
```
전략은 처음 `load`시에는 두 개의 파라미터를 받는 생성자를 통해서 아직 인증되지 않는 상태로 생성한다.

그리고 이 값을 `CustomAuthenticationManager`로 넘겨줄 것이다. 

```kotlin
@Component
class CustomSecurityContextRepository(
    private val props: JwtProperties,
    private val authenticationManager: ReactiveAuthenticationManager,
): ServerSecurityContextRepository {

    override fun save(exchange: ServerWebExchange, context: SecurityContext): Mono<Void> = mono {
        Mono.empty<Void>().awaitSingle()
    }

    override fun load(exchange: ServerWebExchange): Mono<SecurityContext> = mono {
        exchange.request.headers["Authorization"]?.first()?.let { bearerToken ->
            val token = extractToken(bearerToken, props)
            val auth = CustomAuthenticationToken(token, token)
            authenticationManager.authenticate(auth)
                                 .toMono()
                                 .map(::SecurityContextImpl).awaitSingle()
        }
    }

}
```

`load`를 통해서 토큰 정보를 `SecurityContext`에 담아 인증 매니저에 넘겨줄 생각이기 때문에 위와 같이 처리하도록 하자.

최종적으로 다음과 같이 `CustomAuthenticationManager`을 처리하도록 하자.

```kotlin
@Component
class CustomAuthenticationManager(
    private val memberCacheService: MemberCacheService,
): ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = mono {
        val authToken = authentication.credentials.toString()
        memberCacheService.memberByJWT(authToken)
        toAuthentication(authentication).toMono().awaitSingle()
    }

    private fun toAuthentication(
        authentication: Authentication,
        authorities: List<out GrantedAuthority>? = null
    ): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(
            authentication.principal,
            authentication.credentials,
            authorities
        )
    }

}
```
위에서처럼 파라미터를 3개를 받는 생성자를 통해서 `Authentication`객체를 생성하면 인증 상태를 `true`로 변경해 주게 된다.

지금은 `Role`에 대해 딱히 처리한 적이 없기 때문에 다음과 같이 `null`로 세팅한다.

지금까지 기본적인 설정이 끝나면 포스트맨이나 스웨거를 통해서 토큰을 발급받고 `API`테스트를 해보도록 하자.

이것을 토대로 토큰이 없으면 `API`를 호출하지 못하도록 설정이 끝났다.

스프링 시큐리티에서 이미 토큰 검증은 끝났다.

따라서 이 전에 작성했던 커스텀 리졸버를 통해 토큰 정보를 받아와서 유효성 검사를 하는 부분은 삭제하도록 하자.

# create/update api 접근 권한 설정

이제는 `RBAC`, 즉 `Role-Based Access Control`를 적용해 보자.

현재 만들어 놓은 `Member`테이블에 `role`정보를 담은 컬럼을 추가하는 방법도 있고 `role`테이블을 만들어 볼 수 있다.

```roomsql
-- musicshop.role definition
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `role_name` varchar(10) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `member_FK` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

```kotlin
@Table("member")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Member(
    @Id
    var id: Long? = null,
    val name: String,
    val email: String,
    val password: String,
    @Column("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    @Column("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var updatedAt: LocalDateTime? = null,
) {
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var roles: List<RoleCode>? = null
}

@Table("role")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Role(
    @Id
    var id: Long? = null,
    val memberId: Long,
    val roleName: String,
    @Column("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var createdAt: LocalDateTime? = LocalDateTime.now(),
    @Column("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var updatedAt: LocalDateTime? = null,
)

enum class RoleCode {
    USER,
    ADMIN
}
```
멤버는 롤 리스트를 가질 수 있기 때문에 위와 같이 설정을 한다.

```kotlin
interface CustomMemberRepository {
    suspend fun existsByEmail(email: String): Boolean
    suspend fun memberWithRoles(id: Long, email: String): Member?
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

    override suspend fun memberWithRoles(id: Long, email: String): Member? {
        val member = JMember.MEMBER
        val role = JRole.ROLE

        val sqlBuilder = query.select(
            member,
            role
        )
        .from(member)
        .leftJoin(role).on(member.ID.eq(role.MEMBER_ID))
        .where(member.ID.eq(id)
                        .and(member.EMAIL.eq(email))
        )
        return Flux.from(sqlBuilder)
                   .bufferUntilChanged { it.component1() }
                   .map { rows ->
                        val targetMember = rows[0].component1().into(Member::class.java)

                        val roleCodes = mutableListOf<RoleCode>()

                        val roles = rows.filter { it.component2().memberId != null && it.component2().memberId!! > 0 }
                        if(roles.isEmpty()) {
                            roleCodes.add(RoleCode.USER)
                        } else {
                            roles.mapTo(roleCodes) {
                                val role = it.component2().into(Role::class.java)
                                RoleCode.valueOf(role.roleName)
                            }
                        }
                        targetMember.roles = roleCodes.toList()
                        targetMember
                   }.awaitSingle()
    }

}
```
멤버 정보에서 롤 정보를 가질 수 있도록 위와 같이 `API`를 하나 만들어서 `MemberCacheService`에 적용을 해 보자.

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
            memberRepository.memberWithRoles(id, encrypt(email)) ?: notFound("아이디 [$id]와 이메일 [$email]로 조회된 멤버가 없습니다.")
        }
    }

    suspend fun removeCache(token: String) {
        cacheManager.cacheEvict(token, Member::class.java)
    }

}
```

# Role Save API & 회원가입 UseCase 변경

최초 회원가입시 롤을 부여하고 저장하는 로직을 추가해야 한다.

해당 코드는 전형적인 `CRUD`이기 때문에 전체 코드를 확인하자.

# @PreAuthorize 적용

일단 이것을 사용하기 위해서는 `SecurityConfiguration`에 추가적인 어노테이션을 추가해 줘야 한다.

```kotlin
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfiguration
```
다음과 같이 추가해 주자.

[EnableReactiveMethodSecurity](https://docs.spring.io/spring-security/reference/reactive/authorization/method.html)

일단 위 사이트를 보면 코틀린 코루틴을 이용할 때는 일반적인 방식으로는 사용할 수 없다.     

실제로 이것을 적용하게 되면 아래와 같은 에러가 찍힌다.

```
java.lang.IllegalStateException: The returnType class java.lang.Object on public java.lang.Object...
more...
```

위 링크의 공식 사이트에서도 `Note`부분에 보면 이런 이야기가 있다.

```
For this example to work, the return type of the method must be a org.reactivestreams.Publisher (that is, a Mono or a Flux). 
This is necessary to integrate with Reactor’s Context.
```
즉, 발행자인 `Mono`, `Flux`를 반환하는 컨트롤러에만 사용해야 한다고 말하고 있다.

따라서 `@PreAuthorize`를 적용하기 위해서는 다음과 같은 방식으로 변경해야 한다.

```kotlin
@PatchMapping("/{id}")
@PreAuthorize("hasAuthority('ADMIN')")
@ResponseStatus(HttpStatus.OK)
fun updateRecord(@PathVariable("id") id: Long, @RequestBody @Valid command: UpdateRecord): Mono<Record> = mono {
    writeRecordUseCase.update(id, command).toMono().awaitSingle()
}
```
`mono`코루틴 빌더를 사용해서 변경가능하다.

일단 공식사이트에서는 이 부분에 대한 지원을 위해 대기중이라고 하는데 이게 2019년에 올라온 글이다.

아직까지 지원하지는 않는 것 같다.

~~4년이 지나도록 감감무소식...~~

# hasAuthority or hasRole

`@PreAuthorize("hasAuthority('ADMIN')")`를 보면 `hasAuthority`를 사용하고 있다.

하지만 `hasRole`과 동작 방식은 똑같은데 차이점이 하나 있다.

`hasRole`은 프리픽스로 `ROLE_`을 붙여주어야 한다.

`hasRole`를 꼭 쓰겠다면 `CustomAuthenticationManager`에서 `ROLE_`을 붙여서 처리하도록 하자.

```kotlin
@Component
class CustomAuthenticationManager(
    private val memberCacheService: MemberCacheService,
): ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = mono {
        val authToken = authentication.credentials.toString()
        val member = memberCacheService.memberByJWT(authToken)
        val authorities = member.roles?.let { roles ->
            roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }
        }
        toAuthentication(authentication, authorities).toMono().awaitSingle()
    }

    private fun toAuthentication(
        authentication: Authentication,
        authorities: List<out GrantedAuthority>? = null
    ): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(
            authentication.principal,
            authentication.credentials,
            authorities
        )
    }

}
```
위와 같이 처리하면 `@PreAuthorize("hasRole('ADMIN')")`로 처리하면 된다.

지금은 롤이 없다면 `memberWithRoles`함수에서 `USER`롤을 부여하도록 만들어 놨다.

따라서 `create/update`컨트롤러에 위와 같이 롤을 설정한다면

```json
{
    "code": 403,
    "message": "Access Denied"
}
```
처럼 에러 메세지를 보게 된다.

`CustomAuthenticationEntryPoint`가 아닌 `CustomAccessDeniedHandler`가 작동하는 것을 알 수 있다.

디비에서 해당 멤버에 `ADMIN`롤을 추가해서 테스트를 해보자.

그렇다면 잘 실행될 것이다.

# At a Glance

스프링 시큐리티와 `JWT`적용해 보는 시간을 가졌다.

스프링 시큐리티는 확실히 막강한 기능을 가진 라이브러리이다.

기회가 된다면 스프링 시큐리티에 대한 저장소를 진행할 생각이다.
