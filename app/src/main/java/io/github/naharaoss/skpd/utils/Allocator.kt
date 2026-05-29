package io.github.naharaoss.skpd.utils

/**
 * Manage and allocate resources.
 */
abstract class Allocator<T : AutoCloseable, P> {
    private val unallocated = mutableListOf<T>()

    protected abstract fun onResourceCreate(params: P): T

    protected abstract fun onResourceRecycle(params: P, resource: T)

    protected abstract fun onResourceRecall(resource: T)

    fun allocate(params: P): T = unallocated.removeFirstOrNull() ?: onResourceCreate(params)
    fun recall(resource: T) = unallocated.add(resource.also(::onResourceRecall))

    /**
     * Perform clean up of unused resources.
     *
     * This method is called at the end of rendering pipeline to look for resources to remove. In
     * some cases where user want to reclaim as much memory as possible, [force] may be set to
     * `true`.
     *
     * @param [force] Whether to force clearing all resources
     */
    fun cleanUp(force: Boolean = false) {
        unallocated.removeIf { resource ->
            // TODO: Time-limited resources
            val remove = force

            if (remove) {
                resource.close()
                true
            } else {
                false
            }
        }
    }
}