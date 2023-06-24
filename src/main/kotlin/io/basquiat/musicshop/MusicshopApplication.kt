package io.basquiat.musicshop

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux
import java.util.*

@EnableWebFlux
@SpringBootApplication
@ConfigurationPropertiesScan
class MusicshopApplication

@PostConstruct
fun initTimezone() {
	TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
}

fun main(args: Array<String>) {
	runApplication<MusicshopApplication>(*args)
}
