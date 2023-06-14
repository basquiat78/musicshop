package io.basquiat.musicshop.common.configuration

import io.basquiat.musicshop.api.router.musician.ReadMusicianHandler
import io.basquiat.musicshop.api.router.musician.WriteMusicianHandler
import io.basquiat.musicshop.api.router.record.ReadRecordHandler
import io.basquiat.musicshop.api.router.record.WriteRecordHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class RouterConfiguration {
    @Bean
    fun readMusicianRouter(handler: ReadMusicianHandler): RouterFunction<ServerResponse> {
         return coRouter {
            //path("/api/v1/musicians").nest {
            "/api/v1/musicians".nest {
                accept(APPLICATION_JSON).nest {
                    GET("/{id}", handler::musicianById)
                    GET("/query/{queryCondition}", handler::musiciansByQuery)
                }
            }
        }
    }

    @Bean
    fun writeMusicianRouter(handler: WriteMusicianHandler): RouterFunction<ServerResponse> {
        return coRouter {
            "/api/v1/musicians".nest {
                accept(APPLICATION_JSON).nest {
                    POST("", handler::insert)
                    PATCH("/{id}", handler::update)
                }
            }
        }
    }

    @Bean
    fun readRecordRouter(handler: ReadRecordHandler): RouterFunction<ServerResponse> {
        return coRouter {
            "/api/v1/records".nest {
                accept(APPLICATION_JSON).nest {
                    GET("/{id}", handler::recordById)
                    GET("/musician/{musicianId}", handler::recordByMusicianId)
                    GET("/query/{queryCondition}", handler::allRecords)
                }
            }
        }
    }

    @Bean
    fun writeRecordRouter(handler: WriteRecordHandler): RouterFunction<ServerResponse> {
        return coRouter {
            "/api/v1/records".nest {
                accept(APPLICATION_JSON).nest {
                    POST("", handler::insert)
                    PATCH("/{id}", handler::update)
                }
            }
        }
    }

}

