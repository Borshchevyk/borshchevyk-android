package ru.kubsu.borshchevyk.core.model.domain

import kotlinx.serialization.Serializable

/**
 * Value Object representing a Vector Clock for causal ordering.
 * Immutable class.
 */
@Serializable
data class VectorClock(
    val clocks: Map<String, Long> = emptyMap()
) {
    /**
     * Increments the clock for the given nodeId.
     *
     * @param nodeId the node identifier
     * @return a new VectorClock instance with the incremented value
     */
    fun increment(nodeId: String): VectorClock {
        val newClocks = clocks.toMutableMap()
        newClocks[nodeId] = (newClocks[nodeId] ?: 0L) + 1L
        return VectorClock(newClocks)
    }

    /**
     * Merges this vector clock with another, taking the maximum of each node's clock.
     *
     * @param other the other vector clock
     * @return a new merged VectorClock instance
     */
    fun merge(other: VectorClock?): VectorClock {
        if (other == null || other.clocks.isEmpty()) return this
        val merged = clocks.toMutableMap()
        for ((key, value) in other.clocks) {
            merged[key] = maxOf(merged[key] ?: 0L, value)
        }
        return VectorClock(merged)
    }

    /**
     * Checks if this vector clock is strictly before the other.
     *
     * @param other the other vector clock
     * @return true if this happens before the other
     */
    fun isBefore(other: VectorClock?): Boolean {
        if (other == null) return false
        return isLessOrEqual(this, other) && !isLessOrEqual(other, this)
    }

    /**
     * Checks if this vector clock is strictly after the other.
     *
     * @param other the other vector clock
     * @return true if this happens after the other
     */
    fun isAfter(other: VectorClock?): Boolean {
        if (other == null) return true
        return other.isBefore(this)
    }

    /**
     * Checks if this vector clock and the other are concurrent.
     *
     * @param other the other vector clock
     * @return true if they are concurrent
     */
    fun isConcurrent(other: VectorClock?): Boolean {
        if (other == null) return false
        val thisLeqOther = isLessOrEqual(this, other)
        val otherLeqThis = isLessOrEqual(other, this)
        return !thisLeqOther && !otherLeqThis
    }

    private fun isLessOrEqual(a: VectorClock, b: VectorClock): Boolean {
        val allKeys = a.clocks.keys + b.clocks.keys
        for (key in allKeys) {
            val valA = a.clocks[key] ?: 0L
            val valB = b.clocks[key] ?: 0L
            if (valA > valB) return false
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        val that = other as VectorClock
        
        val allKeys = clocks.keys + that.clocks.keys
        for (key in allKeys) {
            val valThis = clocks[key] ?: 0L
            val valThat = that.clocks[key] ?: 0L
            if (valThis != valThat) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = 0
        for ((key, value) in clocks) {
            if (value > 0) {
                result += java.util.Objects.hash(key, value)
            }
        }
        return result
    }
}