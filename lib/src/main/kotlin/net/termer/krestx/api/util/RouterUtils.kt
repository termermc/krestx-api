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
 * Wraps a [SuspendHandler] in a [Handler]<[RoutingContext]>
 * @param requestHandler The [SuspendHandler] to wrap
 * @return The generated [Handler]
 * @since 1.1.0
 */
fun wrapHandler(requestHandler: SuspendRequestHandler): Handler<RoutingContext> = Handler<RoutingContext> { ctx ->
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
	handler(wrapHandler(requestHandler))
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
	errorHandler(statusCode, wrapHandler(errorHandler))
	return this
}

/**
 * Append an API request handler to the route handlers list
 * @param requestHandler The API request handler
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Route.apiHandler(requestHandler: ApiRequestHandler) = suspendHandler { ctx ->
	ctx.send(requestHandler.handle(ctx))
}

/**
 * Specify an API handler to handle an error for a particular status code.
 * You can use to manage general errors too using status code 500.
 * The handler will be called when the context fails and other failure handlers didn't write the reply or when an exception is thrown inside a handler.
 * You must not use [RoutingContext.next] inside the error handler This does not affect the normal failure routing logic.
 * @param statusCode The status code to handle
 * @param errorHandler The API error handler
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.apiErrorHandler(statusCode: Int, errorHandler: ApiRequestHandler) = suspendErrorHandler(statusCode) { ctx ->
	ctx.send(errorHandler.handle(ctx))
}

/**
 * Mounts an API router for the API with the specified version
 * @param version The API version (e.g. "v1")
 * @param router The router to mount
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.mountApiRouter(version: String, router: Router): Router {
	route("/api/$version/*").subRouter(router)
	return this
}

/**
 * Attaches a default API info handler at "/api".
 * Returns the current API version and a list of supported versions.
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.defaultApiInfoHandler(currentApiVersion: String, supportedApiVersions: Array<String>) = this.apply {
	get("/api").apiHandler {
		apiInfoSuccess(currentApiVersion, supportedApiVersions)
	}
}

/**
 * Attaches a default API Not Found (404) error handler.
 * Returns an API error with the name "not_found" and message "Not found".
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.defaultApiNotFoundHandler() = apiErrorHandler(404) {
	apiNotFoundError()
}

/**
 * Attaches a default API Unauthorized (403) error handler.
 * Returns an API error with the name "unauthorized" and message "Unauthorized".
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.defaultApiUnauthorizedHandler() = apiErrorHandler(403) {
	apiUnauthorizedError()
}

/**
 * Attaches a default Method Not Allowed (405) error handler.
 * Returns an API error with the name "method_not_allowed" and message "Method not allowed".
 * @return This, to be used fluently
 * @since 1.1.1
 */
fun Router.defaultMethodNotAllowedHandler() = apiErrorHandler(405) {
	apiMethodNotAllowedError()
}

/**
 * Attaches a default Bad Request (400) error handler.
 * Returns an API error with the name "bad_request" and message "Bad request".
 * @return This, to be used fluently
 * @since 1.1.1
 */
fun Router.defaultBadRequestHandler() = apiErrorHandler(400) {
	apiBadRequestError()
}

/**
 * Attaches a default API Internal Error (500) error handler.
 * Returns an API error with the name "internal_error" and message "Internal error".
 * The handler does not log anything, so if you want to log errors or handle them in another way, pass a handler for [errorHandler].
 * Keep in mind that it is your responsibility to catch any exceptions that may occur inside your error handler.
 * @param errorHandler The error handler to execute before returning the default response, or null for none (defaults to null)
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.defaultApiInternalErrorHandler(errorHandler: SuspendHandler<RoutingContext, Unit>? = null) = apiErrorHandler(500) {
	if (errorHandler !== null)
		errorHandler.handle(it)

	apiInternalError()
}
