package ru.kubsu.borshchevyk.core.model.domain

/**
 * Domain model representing a paginated response wrapper.
 *
 * Abstracts API pagination details and provides a list of data along with page metadata.
 *
 * @param T The type of content within the page (e.g., User, Message).
 * @property content The list of items on the current page.
 * @property pageNumber The current 0-indexed page number.
 * @property pageSize The maximum number of items requested per page.
 * @property totalElements The total number of available elements across all pages.
 * @property totalPages The total number of available pages.
 * @property last True if this is the final page of results, false otherwise.
 */
data class DomainPage<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean
)
