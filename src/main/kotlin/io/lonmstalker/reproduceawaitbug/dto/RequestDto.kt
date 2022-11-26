package io.lonmstalker.reproduceawaitbug.dto

import com.fasterxml.jackson.annotation.JsonCreator

data class RequestDto @JsonCreator constructor(
    val itemsPerPage: Int,
    val pageNum: Int,
    val hasFilters: Boolean
)