# 번외편

앞서 우리는 스프링 시큐리티와 `JWT`를 적용하면서 다음과 같은 흐름으로 적용했었다.

```
1. SecurityContextRepository를 구현한 CustomSecurityContextRepository
    - load함수에서 UsernamePasswordAuthenticationToken 생성한다.
    - 이 토큰을 SecurityContextImpl을 통해 SecurityContext로 반환한다. 

2. ReactiveAuthenticationManager를 구현한 CustomAuthenticationManager
    - authenticate함수에서 CustomSecurityContextRepository로부터 load된 SecurityContext를 받는다.
    - 여기서 인증 체크를 한다.
```

하지만 다른 기술 블로그에서에 말하는 

```kotlin
@Bean
fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
    return http.cors { it.disable() }
               .httpBasic { it.disable() }
               .csrf { it.disable() }
               .formLogin { it.disable() }
               .logout { it.disable() }
               .authorizeExchange { exchanges ->
                    exchanges.pathMatchers(*noAuthUri).permitAll()
                             .anyExchange()
                             .authenticated()
               }
               .exceptionHandling {
                   it.authenticationEntryPoint(authenticationEntryPoint)
                   it.accessDeniedHandler(customAccessDeniedHandler)
               }
              .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
              .authenticationManager(authenticationManager)
              .build()
}
```
이런 방식을 사용하게 된다면 `AuthenticationManager`자체를 타지 않는다.

이전 브랜치에서도 `NoOpServerSecurityContextRepository`가 어떻게 생겨먹었는지 봤기 때문에 예상할 수 있을 것이다.

`어떤 일도 하지 않으면 어떤 일도 벌어지지 않는다.` 딱 요것이다.

그래서 에러가 발생한다.

그렇다면 저걸 사용하겠다면 어떻게 처리해야 할 것인가라는 의문이 들 것이다.

`ServerHttpSecurity`에서는 `addFilterBefore`, `addFilterAfter`, `addFilterAt`필터를 제공한다.

```java
public ServerHttpSecurity addFilterAt(WebFilter webFilter, SecurityWebFiltersOrder order) {
    this.webFilters.add(new OrderedWebFilter(webFilter, order.getOrder()));
    return this;
}

public ServerHttpSecurity addFilterBefore(WebFilter webFilter, SecurityWebFiltersOrder order) {
    this.webFilters.add(new OrderedWebFilter(webFilter, order.getOrder() - 1));
    return this;
}

public ServerHttpSecurity addFilterAfter(WebFilter webFilter, SecurityWebFiltersOrder order) {
    this.webFilters.add(new OrderedWebFilter(webFilter, order.getOrder() + 1));
    return this;
}
```
`WebFlux`에서는 이 `WebFilter`를 구현하고 있는 `AuthenticationWebFilter`를 사용할 수 있다.

`AuthenticationWebFilter`

```kotlin
public class AuthenticationWebFilter implements WebFilter {

	private static final Log logger = LogFactory.getLog(AuthenticationWebFilter.class);

	private final ReactiveAuthenticationManagerResolver<ServerWebExchange> authenticationManagerResolver;

	private ServerAuthenticationSuccessHandler authenticationSuccessHandler = new WebFilterChainServerAuthenticationSuccessHandler();

	private ServerAuthenticationConverter authenticationConverter = new ServerHttpBasicAuthenticationConverter();

	private ServerAuthenticationFailureHandler authenticationFailureHandler = new ServerAuthenticationEntryPointFailureHandler(
			new HttpBasicServerAuthenticationEntryPoint());

	private ServerSecurityContextRepository securityContextRepository = NoOpServerSecurityContextRepository
			.getInstance();

	private ServerWebExchangeMatcher requiresAuthenticationMatcher = ServerWebExchangeMatchers.anyExchange();

	public AuthenticationWebFilter(ReactiveAuthenticationManager authenticationManager) {
		Assert.notNull(authenticationManager, "authenticationManager cannot be null");
		this.authenticationManagerResolver = (request) -> Mono.just(authenticationManager);
	}
	public AuthenticationWebFilter(
			ReactiveAuthenticationManagerResolver<ServerWebExchange> authenticationManagerResolver) {
		Assert.notNull(authenticationManagerResolver, "authenticationResolverManager cannot be null");
		this.authenticationManagerResolver = authenticationManagerResolver;
	}
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return this.requiresAuthenticationMatcher.matches(exchange).filter((matchResult) -> matchResult.isMatch())
				.flatMap((matchResult) -> this.authenticationConverter.convert(exchange))
				.switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
				.flatMap((token) -> authenticate(exchange, chain, token))
				.onErrorResume(AuthenticationException.class, (ex) -> this.authenticationFailureHandler
						.onAuthenticationFailure(new WebFilterExchange(exchange, chain), ex));
	}
	private Mono<Void> authenticate(ServerWebExchange exchange, WebFilterChain chain, Authentication token) {
		return this.authenticationManagerResolver.resolve(exchange)
				.flatMap((authenticationManager) -> authenticationManager.authenticate(token))
				.switchIfEmpty(Mono.defer(
						() -> Mono.error(new IllegalStateException("No provider found for " + token.getClass()))))
				.flatMap((authentication) -> onAuthenticationSuccess(authentication,
						new WebFilterExchange(exchange, chain)))
				.doOnError(AuthenticationException.class,
						(ex) -> logger.debug(LogMessage.format("Authentication failed: %s", ex.getMessage())));
	}
	protected Mono<Void> onAuthenticationSuccess(Authentication authentication, WebFilterExchange webFilterExchange) {
		ServerWebExchange exchange = webFilterExchange.getExchange();
		SecurityContextImpl securityContext = new SecurityContextImpl();
		securityContext.setAuthentication(authentication);
		return this.securityContextRepository.save(exchange, securityContext)
				.then(this.authenticationSuccessHandler.onAuthenticationSuccess(webFilterExchange, authentication))
				.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
	}
	public void setSecurityContextRepository(ServerSecurityContextRepository securityContextRepository) {
		Assert.notNull(securityContextRepository, "securityContextRepository cannot be null");
		this.securityContextRepository = securityContextRepository;
	}
	public void setAuthenticationSuccessHandler(ServerAuthenticationSuccessHandler authenticationSuccessHandler) {
		Assert.notNull(authenticationSuccessHandler, "authenticationSuccessHandler cannot be null");
		this.authenticationSuccessHandler = authenticationSuccessHandler;
	}
	@Deprecated
	public void setAuthenticationConverter(Function<ServerWebExchange, Mono<Authentication>> authenticationConverter) {
		Assert.notNull(authenticationConverter, "authenticationConverter cannot be null");
		setServerAuthenticationConverter(authenticationConverter::apply);
	}
	public void setServerAuthenticationConverter(ServerAuthenticationConverter authenticationConverter) {
		Assert.notNull(authenticationConverter, "authenticationConverter cannot be null");
		this.authenticationConverter = authenticationConverter;
	}
	public void setAuthenticationFailureHandler(ServerAuthenticationFailureHandler authenticationFailureHandler) {
		Assert.notNull(authenticationFailureHandler, "authenticationFailureHandler cannot be null");
		this.authenticationFailureHandler = authenticationFailureHandler;
	}
	public void setRequiresAuthenticationMatcher(ServerWebExchangeMatcher requiresAuthenticationMatcher) {
		Assert.notNull(requiresAuthenticationMatcher, "requiresAuthenticationMatcher cannot be null");
		this.requiresAuthenticationMatcher = requiresAuthenticationMatcher;
	}

}
```
이렇게 생겨먹은 놈이다.

그 중에 `AuthenticationFailureHandler`라든가 `AuthenticationSuccessHandler`을 등록하는 부분도 눈에 보인다.

하지만 일단 우리는 필터에서 할려고 하는 행위를 위에서 언급한 흐름으로 진행하는데 포커스를 맞춰보자.

이 흐름을 이 필터에서 할 수 있도록 작성해야 한다.

위 클래스를 보면 우리가 선택할 수 있는 `setServerAuthenticationConverter`를 통해서 처리를 할 수 있다.

`ServerAuthenticationConverter`는 인터페이스로 이것을 구현하면 된다.

```java
@FunctionalInterface
public interface ServerAuthenticationConverter {
    Mono<Authentication> convert(ServerWebExchange exchange);
}
```

`JwtAuthenticationConverter`를 하나 만들어 보자.

```kotlin
@Component
class JwtAuthenticationConverter(
    private val props: JwtProperties,
): ServerAuthenticationConverter {

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> = mono {
        exchange.request.headers["Authorization"]?.first()?.let { bearerToken ->
            val authToken = extractToken(bearerToken, props)
            UsernamePasswordAuthenticationToken(authToken, authToken)
        }.toMono().awaitSingle()
    }

}
```
이 컨버터는 우리가 앞서 작성했던 `CustomSecurityContextRepository`의 역할을 대신할 것이다.

이제부터 필터를 작성해 보자.

```kotlin
@Component
class CustomAuthenticationWebFilter(
    private val jwtAuthenticationConverter: JwtAuthenticationConverter,
    private val customAuthenticationManager: CustomAuthenticationManager,
) {

    fun authenticationWebFilter(): AuthenticationWebFilter {
        val authenticationWebFilter = AuthenticationWebFilter(customAuthenticationManager)
        authenticationWebFilter.setServerAuthenticationConverter(jwtAuthenticationConverter)
        return authenticationWebFilter
    }
}
```
`AuthenticationWebFilter`에 앞서 우리가 작성했던 `CustomAuthenticationManager`를 생성자를 통해 주입한다.

즉 이 필터는 커스텀 컨버터에서 생성한 `Authentication`정보를 `CustomAuthenticationManager`로 넘겨줄 것이다.

커스텀 컨버터를 등록해서 `AuthenticationWebFilter`을 반환하는 함수를 하나 만들자.

물론 이 방식은 `SecurityConfiguration`안에서 생성해도 상관없을 것이다.

하지만 다음과 같이 단순하게 필터를 적용하게 되면

```kotlin
@Bean
fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
    return http.cors { it.disable() }
               .httpBasic { it.disable() }
               .csrf { it.disable() }
               .formLogin { it.disable() }
               .logout { it.disable() }
               .authorizeExchange { exchanges ->
                    exchanges.pathMatchers(*noAuthUri).permitAll()
                             .anyExchange()
                             .authenticated()
               }
               .exceptionHandling {
                   it.authenticationEntryPoint(authenticationEntryPoint)
                   it.accessDeniedHandler(customAccessDeniedHandler)
               }
              .securityContextRepository(customSecurityContextRepository)
              .addFilterAt(customAuthenticationWebFilter.authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
              .build()
}
```
에서 `permitAll`부분이 적용되지 않는다.

그래서 스웨거 화면, 회원가입 및 로그인에서도 필터가 적용되면서 토큰 정보가 없다는 메세지를 보게 된다.

생각해 보면 당연한데 우리가 만든 필터는 바깥 영역에서 작동한다.

그래서 모든 리퀘스트에 대해서 필터가 걸리게 된다.

[Multiple Chains Support](https://docs.spring.io/spring-security/reference/reactive/configuration/webflux.html#jc-webflux-multiple-filter-chains)

위 사이트를 보면 `securityMatcher`함수를 사용하는 것을 볼 수 있는데 이것으로 필터가 적용될 `URL`을 작성해 주면 된다.

최종적으로

```kotlin
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfiguration(
    private val authenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
    private val customAuthenticationWebFilter: CustomAuthenticationWebFilter,
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.cors { it.disable() }
            .httpBasic { it.disable() }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }

        http.securityMatcher(PathPatternParserServerWebExchangeMatcher("/api/v1/members/logout"))
            .securityMatcher(PathPatternParserServerWebExchangeMatcher("/api/v1/musicians/**"))
            .securityMatcher(PathPatternParserServerWebExchangeMatcher("/api/v1/records/**"))

        http.addFilterAt(customAuthenticationWebFilter.authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(customAccessDeniedHandler)
            }
            .authorizeExchange { exchanges ->
                exchanges.anyExchange()
                         .authenticated()
            }

        return http.build()
    }

}
```
위와 같이 수정하면 된다.

# At A Glance

어떤 방식을 활용하든 동작은 똑같다.

하지만 이전 브랜치 방식으로 처리한다 해도 때론 필터를 적용할 케이스가 분명 생길 수 있다.

따라서 이 브랜치는 그에 대한 가이드라인이 될 것이다.
