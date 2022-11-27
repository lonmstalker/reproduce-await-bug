package io.lonmstalker.reproduceawaitbug.controller

import io.lonmstalker.reproduceawaitbug.dto.MergeResponse
import io.lonmstalker.reproduceawaitbug.dto.RequestDto
import io.lonmstalker.reproduceawaitbug.service.MergeService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class RequestController(
    private val mergeService: MergeService
) {

    @PostMapping("/suspend-call")
    suspend fun suspendCallTwoServices(@RequestBody requestDto: RequestDto): List<MergeResponse> =
        this.mergeService.suspendCallTwoServices(requestDto)

    @PostMapping("/flux-call")
    fun fluxCallTwoServices(@RequestBody requestDto: RequestDto): Mono<List<MergeResponse>> =
        this.mergeService.fluxCallTwoServices(requestDto)
}