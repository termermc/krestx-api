package net.termer.krestx.api.handler

/**
 * A suspending version of [io.vertx.core.Handler], with a return type
 * @since 1.0.0
 */
fun interface SuspendHandler<E, R> {
	/**
	 * Something has happened, so handle it
	 * @param event The event to handle
	 * @return The result, if any
	 * @since 1.0.0
	 */
	suspend fun handle(event: E): R
}
