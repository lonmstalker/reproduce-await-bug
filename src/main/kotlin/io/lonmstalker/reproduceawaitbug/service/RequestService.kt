package io.lonmstalker.reproduceawaitbug.service

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.lonmstalker.reproduceawaitbug.config.ServiceProps
import io.lonmstalker.reproduceawaitbug.dto.EmployeeServiceOne
import io.lonmstalker.reproduceawaitbug.dto.EmployeeServiceTwo
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Service
class RequestService(
    private val webClient: WebClient,
    private val serviceProps: ServiceProps
) {

    // real request with sorting, filters and paging, because of it I use LinkedHashSet
    suspend fun callServiceOne(currentPage: Int, itemsPerPage: Int): LinkedHashSet<EmployeeServiceOne>? =
        this.webClient
            .get()
            .uri("${this.serviceProps.serviceOneUrl}/employee/list")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .awaitBodyOrNull<LinkedHashSet<EmployeeServiceOne>>()
            ?.apply {
                LOGGER.info(
                    "<<<<First service response. currentPage='{}', pageSize='{}', itemsPerPage='{}'",
                    currentPage,
                    this.size,
                    itemsPerPage
                )
            }

    // real request with sorting, filters and paging, because of it I use LinkedHashSet
    suspend fun callServiceTwo(currentPage: Int, itemsPerPage: Int): LinkedHashSet<EmployeeServiceTwo>? =
        this.webClient
            .post()
            .uri("${this.serviceProps.serviceTwoUrl}/employee/list")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .awaitBodyOrNull<LinkedHashSet<EmployeeServiceTwo>>()
            ?.apply {
                LOGGER.info(
                    "<<<<Second service response. currentPage='{}', pageSize='{}', itemsPerPage='{}'",
                    currentPage,
                    this.size,
                    itemsPerPage
                )
            }

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }
}