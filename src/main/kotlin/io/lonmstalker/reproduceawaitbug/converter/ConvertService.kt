package io.lonmstalker.reproduceawaitbug.converter

import io.lonmstalker.reproduceawaitbug.dto.EmployeeServiceOne
import io.lonmstalker.reproduceawaitbug.dto.EmployeeServiceTwo
import io.lonmstalker.reproduceawaitbug.dto.MergeResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ConvertService {

    fun mergeResponses(
        one: LinkedHashMap<UUID, EmployeeServiceOne>,
        two: LinkedHashMap<UUID, EmployeeServiceTwo>,
        sortByTwo: Boolean
    ) =
        if (sortByTwo) {
            two
                .map { this.merge(one[it.key]!!, it.value) }
        } else {
            one
                .filter {
                    two.containsKey(it.key)
                        .apply {
                            if (!this) {
                                LOGGER.error("Response from service two lost user id='{}'", it.key)
                            }
                        }
                }
                .map { this.merge(it.value, two[it.key]!!) }
        }

    private fun merge(
        one: EmployeeServiceOne,
        two: EmployeeServiceTwo
    ) = MergeResponse(
        id = one.id,
        username = one.username ?: two.username,
        firstName = one.firstName ?: two.firstName,
        anotherData = two.someAnotherData
    )

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }
}