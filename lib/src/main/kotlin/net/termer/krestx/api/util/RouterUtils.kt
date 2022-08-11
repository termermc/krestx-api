@file:OptIn(DelicateCoroutinesApi::class)

package net.termer.krestx.api.util

import net.termer.krestx.api.handler.SuspendHandler
import io.vertx.core.Handler
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * A suspending request handler.
 * Alias to [SuspendHandler]<[RoutingContext], [Unit]>.
 * @since 1.0.0
 */
typealias SuspendRequestHandler = SuspendHandler<RoutingContext, Unit>

/**
 * Generates a [Handler]<[RoutingContext]> from a suspending function
 * @param requestHandler The request handler suspending function
 * @return The generated [Handler]
 */
private fun genHandler(requestHandler: SuspendRequestHandler): Handler<RoutingContext> = Handler<RoutingContext> { ctx ->
	GlobalScope.launch(ctx.vertx().dispatcher()) {
		try {
			requestHandler.handle(ctx)
		} catch(e: Throwable) {
			ctx.fail(e)
		}
	}
}

/**
 * Append a suspending request handler to the route handlers list
 * @param requestHandler The suspending request handler
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Route.suspendHandler(requestHandler: SuspendRequestHandler): Route {
	handler(genHandler(requestHandler))
	return this
}

/**
 * Specify a suspending handler to handle an error for a particular status code.
 * You can use to manage general errors too using status code 500.
 * The handler will be called when the context fails and other failure handlers didn't write the reply or when an exception is thrown inside a handler.
 * You must not use [RoutingContext.next] inside the error handler This does not affect the normal failure routing logic.
 * @param statusCode The status code to handle
 * @param errorHandler The suspending error handler
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.suspendErrorHandler(statusCode: Int, errorHandler: SuspendRequestHandler): Router {
	errorHandler(statusCode, genHandler(errorHandler))
	return this
}
