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
	version.set(jooqVersion)
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

tasks.named<JooqGenerate>("generateJooq") {
	(launcher::set)(javaToolchains.launcherFor {
		languageVersion.set(JavaLanguageVersion.of(17))
	})
}