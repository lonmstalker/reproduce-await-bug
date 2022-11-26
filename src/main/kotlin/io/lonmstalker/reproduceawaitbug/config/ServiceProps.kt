package io.lonmstalker.reproduceawaitbug.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "app")
data class ServiceProps(
    val serviceOneUrl: String,
    val serviceTwoUrl: String,
    val maxRequestPageSize: Double = 49.0
)