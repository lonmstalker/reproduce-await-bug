package io.lonmstalker.reproduceawaitbug

import io.lonmstalker.reproduceawaitbug.config.ServiceProps
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ServiceProps::class)
class ReproduceAwaitBugApplication

fun main(args: Array<String>) {
    runApplication<ReproduceAwaitBugApplication>(*args)
}
