package io.lonmstalker.reproduceawaitbug

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import io.lonmstalker.reproduceawaitbug.dto.EmployeeServiceOne
import io.lonmstalker.reproduceawaitbug.dto.EmployeeServiceTwo
import io.lonmstalker.reproduceawaitbug.dto.MergeResponse
import io.lonmstalker.reproduceawaitbug.dto.RequestDto
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors
import java.util.stream.IntStream

@SpringBootTest(
    classes = [ReproduceAwaitBugApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
class SuspendCallTest {
    private val webTestClient: WebTestClient = WebTestClient
        .bindToServer()
        .baseUrl("http://localhost:8080")
        .build()

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var wireMockServer: WireMockServer

    @BeforeEach
    fun startWiremock() {
        wireMockServer = WireMockServer(8082)
        wireMockServer.start()
        configureFor("localhost", 8082)
    }

    @AfterEach
    fun stopWiremock() {
        this.wireMockServer.stop()
    }

    @Test
    fun should_lost_data() {
        StepVerifier
            .create(
                Flux
                    .range(0, 5)
                    .publishOn(Schedulers.boundedElastic())
                    .flatMap {
                        Mono.fromRunnable<Void> {
                            println("------------------------------call service number=$it------------------------------")
                            this.oneCallService()
                        }
                    }
            )
            .thenAwait(Duration.ofSeconds(10))
            .verifyComplete()
    }

    fun oneCallService() {
        val generatedIds = IntStream
            .range(0, 250)
            .mapToObj { UUID.randomUUID() }
            .collect(Collectors.toList())
        val serviceOneEmployees = generatedIds.map { this.createEmployeeServiceOne(it) }
        val serviceTwoEmployees = generatedIds.map { this.createEmployeeServiceTwo(it) }

        this.stubServiceOne(serviceOneEmployees)
        this.stubServiceTwo(serviceTwoEmployees)
        setScenarioState("batch_request", "state-1")

        webTestClient
            .post()
            .uri("/suspend-call")
            .bodyValue(
                RequestDto(
                    itemsPerPage = 250,
                    pageNum = 0,
                    hasFilters = false
                )
            )
            .exchange()
            .expectStatus()
            .is2xxSuccessful
            .expectBody(object : ParameterizedTypeReference<List<MergeResponse>>() {})
            .consumeWith {
                Assertions.assertNotNull(it.responseBody)
                Assertions.assertEquals(250, it.responseBody!!.size)
            }
    }

    private fun stubServiceOne(serviceOneEmployees: List<EmployeeServiceOne>) {
        this.stubOne(serviceOneEmployees, 0, 50, 1)
        this.stubOne(serviceOneEmployees, 50, 100, 2)
        this.stubOne(serviceOneEmployees, 100, 150, 3)
        this.stubOne(serviceOneEmployees, 150, 200, 4)
        this.stubOne(serviceOneEmployees, 200, 250, 5)
    }

    private fun stubServiceTwo(serviceTwoEmployees: List<EmployeeServiceTwo>) {
        this.stubTwo(serviceTwoEmployees, 0, 50, 6)
        this.stubTwo(serviceTwoEmployees, 50, 100, 7)
        this.stubTwo(serviceTwoEmployees, 100, 150, 8)
        this.stubTwo(serviceTwoEmployees, 150, 200, 9)
        this.stubTwo(serviceTwoEmployees, 200, 250, 10)
    }

    private fun stubOne(serviceOneEmployees: List<EmployeeServiceOne>, startInt: Int, endInt: Int, state: Int) {
        stubFor(
            get(urlEqualTo("/service-one/employee/list"))
                .withScheme("http")
                .withHost(EqualToPattern("localhost"))
                .withPort(8082)
                .inScenario("batch_request")
                .whenScenarioStateIs("state-$state")
                .willSetStateTo("state-${state + 1}")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withLogNormalRandomDelay(90.0, 0.1)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(this.objectMapper.writeValueAsBytes(serviceOneEmployees.subList(startInt, endInt)))
                )
        )
    }

    private fun stubTwo(serviceOneEmployees: List<EmployeeServiceTwo>, startInt: Int, endInt: Int, state: Int) {
        stubFor(
            post(urlEqualTo("/service-two/employee/list"))
                .withScheme("http")
                .withHost(EqualToPattern("localhost"))
                .withPort(8082)
                .inScenario("batch_request")
                .whenScenarioStateIs("state-$state")
                .willSetStateTo("state-${state + 1}")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withLogNormalRandomDelay(50.0, 0.1)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(this.objectMapper.writeValueAsBytes(serviceOneEmployees.subList(startInt, endInt)))
                )
        )
    }

    private fun createEmployeeServiceOne(id: UUID) =
        EmployeeServiceOne.builder()
            .id(id)
            .firstName(RandomStringUtils.random(10))
            .secondName(RandomStringUtils.random(10))
            .middleName(RandomStringUtils.random(10))
            .username(RandomStringUtils.random(10))
            .age(ThreadLocalRandom.current().nextInt())
            .build()

    private fun createEmployeeServiceTwo(id: UUID) =
        EmployeeServiceTwo.builder()
            .id(id)
            .firstName(RandomStringUtils.random(10))
            .secondName(RandomStringUtils.random(10))
            .middleName(RandomStringUtils.random(10))
            .username(RandomStringUtils.random(10))
            .age(ThreadLocalRandom.current().nextInt())
            .someAnotherData(
                EmployeeServiceTwo.AnotherData.builder()
                    .someAnother(RandomStringUtils.random(10))
                    .someAnother1(RandomStringUtils.random(10))
                    .someAnother2(RandomStringUtils.random(10))
                    .build()
            )
            .build()
}