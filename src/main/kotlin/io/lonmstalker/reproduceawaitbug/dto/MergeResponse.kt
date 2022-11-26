package io.lonmstalker.reproduceawaitbug.dto

import com.fasterxml.jackson.annotation.JsonCreator
import io.lonmstalker.reproduceawaitbug.dto.EmployeeServiceTwo.AnotherData
import java.util.UUID

data class MergeResponse @JsonCreator constructor(
    val id: UUID,
    val username: String,
    val firstName: String,
    val anotherData: AnotherData
)