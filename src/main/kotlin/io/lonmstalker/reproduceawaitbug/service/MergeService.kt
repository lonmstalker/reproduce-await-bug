package io.lonmstalker.reproduceawaitbug.service

import io.lonmstalker.reproduceawaitbug.config.ServiceProps
import io.lonmstalker.reproduceawaitbug.converter.ConvertService
import io.lonmstalker.reproduceawaitbug.dto.EmployeeServiceOne
import io.lonmstalker.reproduceawaitbug.dto.EmployeeServiceTwo
import io.lonmstalker.reproduceawaitbug.dto.MergeResponse
import io.lonmstalker.reproduceawaitbug.dto.RequestDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max

@Service
class MergeService(
    private val serviceProps: ServiceProps,
    private val requestService: RequestService,
    private val convertService: ConvertService
) {

    suspend fun suspendCallTwoServices(requestDto: RequestDto): List<MergeResponse> {
        val responseOne =
            this.callBatchServiceOne(requestDto.itemsPerPage, requestDto.pageNum, requestDto.hasFilters)
        val responseTwo =
            this.callBatchServiceTwo(responseOne.size, requestDto.pageNum, requestDto.hasFilters)
        return this.convertService.mergeResponses(responseOne, responseTwo, requestDto.hasFilters)
    }

    fun fluxCallTwoServices(requestDto: RequestDto): Mono<List<MergeResponse>> =
        this.correctCallBatchServiceOne(requestDto.itemsPerPage, requestDto.pageNum, requestDto.hasFilters)
            .flatMap { responseOne ->
                this.correctCallBatchServiceTwo(responseOne.size, requestDto.pageNum, requestDto.hasFilters)
                    .map { responseTwo ->
                        this.convertService.mergeResponses(responseOne, responseTwo, requestDto.hasFilters)
                    }
            }

    // in this method, I transfer id users from callBatchServiceOne and filters only by these ids
    private suspend fun callBatchServiceTwo(
        userIdCount: Int,
        pageNum: Int,
        serviceOneMustGetAll: Boolean
    ): LinkedHashMap<UUID, EmployeeServiceTwo> {
        val responseEmployees2 = this.createNeededLinkedHashSet<UUID, EmployeeServiceTwo>(userIdCount)
        val pageCount = this.calculatePageCount(userIdCount)

        val neededPageSize = this.calculatePageSize(pageCount, userIdCount)
        val pageNumber = if (!serviceOneMustGetAll) 1 else pageNum

        LOGGER.info(
            ">>>>Start batching get from service two. pageCount='{}', neededPageSize='{}', pageNumber='{}'",
            pageCount,
            neededPageSize,
            pageNumber
        )
        for (currentPage in pageNumber..pageCount) {
            val response = this.requestService.callServiceTwo(currentPage, neededPageSize)
            if (!response.isNullOrEmpty()) {
                LOGGER.info("responseEmployees2 before addAll='{}'", responseEmployees2.size)
                response.forEach { responseEmployees2[it.id] = it }
                /**
                 * In real project here I have random size,
                 * but always the same data and log response from service two is okay
                 */
                LOGGER.info("responseEmployees2 after addAll='{}'", responseEmployees2.size)
            }
            if (response != null && response.size < neededPageSize) {
                break
            }
        }

        return responseEmployees2
            .apply { LOGGER.info(">>>>get count from service two='{}'", this.size) }
    }

    /*
    At first, I must filter by field from first service,
    and if I need other filters which second service have only, so in service two by other filters
     */
    private suspend fun callBatchServiceOne(
        itemsPerPage: Int,
        pageNum: Int,
        serviceOneMustGetAll: Boolean
    ): LinkedHashMap<UUID, EmployeeServiceOne> {
        val responseEmployees1 = this.createNeededLinkedHashSet<UUID, EmployeeServiceOne>(itemsPerPage)
        val pageCount = this.serviceOnePageCount(serviceOneMustGetAll, itemsPerPage)

        val neededPageSize = this.calculatePageSize(pageCount, itemsPerPage)
        val pageNumber = this.calculatePageNum(serviceOneMustGetAll, pageNum)

        LOGGER.info(
            ">>>>Start batching get from service one. pageCount='{}', neededPageSize='{}', pageNumber='{}'",
            pageCount,
            neededPageSize,
            pageNumber
        )
        for (currentPage in pageNumber..pageCount) {
            val response = this.requestService.callServiceOne(currentPage, neededPageSize)
            if (!response.isNullOrEmpty()) {
                LOGGER.info("responseEmployees1 before addAll='{}'", responseEmployees1.size)
                response.forEach { responseEmployees1[it.id] = it }
                LOGGER.info("responseEmployees1 after addAll='{}'", responseEmployees1.size)
            }
            if (response != null && response.size < neededPageSize) {
                break
            }
        }

        return responseEmployees1
            .apply { LOGGER.info(">>>>get count from service one='{}'", this.size) }
    }

    private fun correctCallBatchServiceTwo(
        userIdCount: Int,
        pageNum: Int,
        serviceOneMustGetAll: Boolean
    ): Mono<LinkedHashMap<UUID, EmployeeServiceTwo>> {
        val responseEmployees2 = this.createNeededLinkedHashSet<UUID, EmployeeServiceTwo>(userIdCount)
        val pageCount = this.calculatePageCount(userIdCount)
        val neededPageSize = this.calculatePageSize(pageCount, userIdCount)
        val pageNumber = this.calculatePageNum(serviceOneMustGetAll, pageNum)
        LOGGER.info(
            ">>>>Start batching get from service two. pageCount='{}', neededPageSize='{}', pageNumber='{}'",
            pageCount,
            neededPageSize,
            pageNumber
        )
        return Flux.range(pageNumber, pageCount)
            .concatMap { currentPage -> this.requestService.correctCallServiceTwo(currentPage, neededPageSize) }
            .takeWhile { response -> response != null && response.size >= neededPageSize }
            .collectList()
            .map { fullList ->
                this.createNeededLinkedHashSet<UUID, EmployeeServiceTwo>(userIdCount)
                    .apply {
                        fullList.forEach { nextList ->
                            LOGGER.info("responseEmployees1 before addAll='{}'", this.size)
                            nextList.forEach { employee -> this[employee.id] = employee }
                            LOGGER.info("responseEmployees1 after addAll='{}'", this.size)
                        }
                    }
            }
            .doOnNext { LOGGER.info(">>>>get count from service one='{}'", it.size) }
    }

    private fun correctCallBatchServiceOne(
        itemsPerPage: Int,
        pageNum: Int,
        serviceOneMustGetAll: Boolean
    ): Mono<LinkedHashMap<UUID, EmployeeServiceOne>> {
        val pageCount = this.serviceOnePageCount(serviceOneMustGetAll, itemsPerPage)
        val neededPageSize = this.calculatePageSize(pageCount, itemsPerPage)
        val pageNumber = if (!serviceOneMustGetAll) 1 else pageNum
        LOGGER.info(
            ">>>>Start batching get from service one. pageCount='{}', neededPageSize='{}', pageNumber='{}'",
            pageCount,
            neededPageSize,
            pageNumber
        )
        return Flux.range(pageNumber, pageCount)
            .concatMap { currentPage -> this.requestService.correctCallServiceOne(currentPage, neededPageSize) }
            .takeWhile { response -> response != null && response.size >= neededPageSize }
            .collectList()
            .map { fullList ->
                this.createNeededLinkedHashSet<UUID, EmployeeServiceOne>(itemsPerPage)
                    .apply {
                        fullList.forEach { nextList ->
                            LOGGER.info("responseEmployees1 before addAll='{}'", this.size)
                            nextList.forEach { employee -> this[employee.id] = employee }
                            LOGGER.info("responseEmployees1 after addAll='{}'", this.size)
                        }
                    }
            }
            .doOnNext { LOGGER.info(">>>>get count from service one='{}'", it.size) }
    }

    private fun serviceOnePageCount(serviceOneMustGetAll: Boolean, itemsPerPage: Int) =
        if (!serviceOneMustGetAll) {
            this.calculatePageCount(itemsPerPage)
        } else {
            Int.MAX_VALUE
        }

    private fun calculatePageSize(pageCount: Int, itemsPerPage: Int) =
        if (pageCount < 1) {
            itemsPerPage
        } else {
            this.serviceProps.maxRequestPageSize.toInt()
        }

    // if call service with jpa pagination should replace 1 with 0
    private fun calculatePageNum(serviceOneMustGetAll: Boolean, pageNum: Int) =
        if (!serviceOneMustGetAll) {
            1
        } else {
            pageNum
        }

    private fun calculatePageCount(itemsPerPage: Int) =
        if (itemsPerPage <= this.serviceProps.maxRequestPageSize) {
            1
        } else {
            ceil(itemsPerPage / this.serviceProps.maxRequestPageSize).toInt()
        }

    private fun <K, V> createNeededLinkedHashSet(itemsPerPage: Int) =
        LinkedHashMap<K, V>(max(2 * itemsPerPage, 11), 0.75f)

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }
}