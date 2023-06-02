package io.basquiat.musicshop

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@EnableWebFlux
@SpringBootApplication
class MusicshopApplication

fun main(args: Array<String>) {
	runApplication<MusicshopApplication>(*args)
}
