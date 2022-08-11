package net.termer.krestx.api.util

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.await
import net.termer.krestx.api.handler.SuspendHandler

/**
 * The default API error HTTP status code
 */
private const val DEFAULT_API_ERROR_STATUS = 500

/**
 * An API request handler.
 * Alias to [SuspendHandler]<[RoutingContext], [ApiResponse]>.
 * @since 1.0.0
 */
typealias ApiRequestHandler = SuspendHandler<RoutingContext, ApiResponse?>

/**
 * An API error
 * @since 1.0.0
 */
data class ApiError(
	/**
	 * The computer-readable error name.
	 * Should be more or less formatted like the following string:
	 *
	 * "internal_error"
	 */
	val name: String,

	/**
	 * The human-readable error message
	 */
	val message: String,

	/**
	 * Any additional data to include with the error
	 */
	val data: JsonObject? = null
) {
	/**
	 * Returns a JSON representation of the error
	 * @return A JSON representation of the error
	 * @since 1.0.0
	 */
	fun toJson() = json {
		obj(
			"name" to name,
			"message" to message,
			"data" to data
		)
	}
}

/**
 * An API response
 * @since 1.0.0
 */
sealed interface ApiResponse {
	/**
	 * Returns a JSON representation of the response
	 * @return A JSON representation of the response
	 * @since 1.0.0
	 */
	fun toJson(): JsonObject
}

/**
 * A successful API response
 * @since 1.0.0
 */
data class ApiSuccessResponse(
	/**
	 * Any data to include with the response
	 * @since 1.0.0
	 */
	val data: JsonObject? = null
): ApiResponse {
	override fun toJson() = json {
		obj(
			"success" to true,
			"data" to data
		)
	}
}

/**
 * An error API response.
 * May contain zero or more error objects.
 * @since 1.0.0
 */
data class ApiErrorResponse(
	/**
	 * The errors
	 * @since 1.0.0
	 */
	val errors: Array<ApiError>,

	/**
	 * The status code to send along with the response (defaults to 500)
	 * @since 1.0.0
	 */
	val statusCode: Int = DEFAULT_API_ERROR_STATUS
): ApiResponse {
	override fun toJson() = json {
		obj(
			"success" to false,
			"statusCode" to statusCode,
			"errors" to errors.map { it.toJson() }
		)
	}

	override fun equals(other: Any?): Boolean {
		if(this === other)
			return true
		if(javaClass != other?.javaClass) return false

		other as ApiErrorResponse

		if(statusCode != other.statusCode)
			return false
		if(!errors.contentEquals(other.errors))
			return false

		return true
	}

	override fun hashCode(): Int {
		var result = statusCode
		result = 31*result+errors.contentHashCode()
		return result
	}
}

/**
 * Returns a successful API response
 * @param data Any additional data to include in the response (optional)
 * @return The successful API response
 * @since 1.0.0
 */
fun apiSuccess(data: JsonObject? = null) = ApiSuccessResponse(data = data)

/**
 * Returns an error API response.
 * Contains only one error. To include multiple, use [apiErrors].
 * @param name The computer-readable error name (e.g. "internal_error")
 * @param message The human-readable error message (e.g. "Internal error")
 * @param data Any additional data to include with the error (optional)
 * @param statusCode The status code to send along with the error response
 * @return The error API response
 * @since 1.0.0
 */
fun apiError(name: String, message: String, data: JsonObject? = null, statusCode: Int = DEFAULT_API_ERROR_STATUS) = ApiErrorResponse(
	errors = arrayOf(ApiError(name, message, data)),
	statusCode = statusCode
)

/**
 * Returns an error API response with multiple errors.
 * To return only one error, use [apiError].
 * @param errors The errrors to send
 * @param statusCode The status code to send along with the error response
 * @return The error API response
 * @since 1.0.0
 */
fun apiErrors(errors: Array<ApiError>, statusCode: Int = DEFAULT_API_ERROR_STATUS) = ApiErrorResponse(
	errors = errors,
	statusCode = statusCode
)

/**
 * Sends an API response
 * @param res The API response
 * @since 1.0.0
 */
suspend fun RoutingContext.send(res: ApiResponse) {
	response().putHeader("content-type", "application/json; charset=UTF-8")

	when(res) {
		is ApiSuccessResponse -> response()
			.setStatusCode(200)
			.end(res.toJson().toString())
			.await()
		is ApiErrorResponse -> response()
			.setStatusCode(res.statusCode)
			.end(res.toJson().toString())
			.await()
	}
}

/**
 * Append an API request handler to the route handlers list
 * @param requestHandler The API request handler
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Route.apiHandler(requestHandler: ApiRequestHandler) = suspendHandler { ctx ->
	val res = requestHandler.handle(ctx)

	// If a response is returned, send it
	if(res !== null)
		ctx.send(res)
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
	val res = errorHandler.handle(ctx)

	// If a response is returned, send it
	if(res !== null)
		ctx.send(res)
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
	get("/api").apiHandler { apiSuccess(
		jsonObjectOf(
			"currentVersion" to currentApiVersion,
			"supportedVersions" to supportedApiVersions
		)
	) }
}

/**
 * Attaches a default API Not Found (404) error handler.
 * Returns an API error with the name "not_found" and message "Not found".
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.defaultApiNotFoundHandler() = apiErrorHandler(404) { apiError(
	name = "not_found",
	message = "Not found",
	statusCode = 404
) }

/**
 * Attaches a default API Unauthorized (403) error handler.
 * Returns an API error with the name "unauthorized" and message "Unauthorized".
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.defaultApiUnauthorizedHandler() = apiErrorHandler(403) { apiError(
	name = "unauthorized",
	message = "Unauthorized",
	statusCode = 403
) }

/**
 * Attaches a default API Internal Error (500) error handler.
 * Returns an API error with the name "internal_error" and message "Internal error".
 * The handler does not log anything, so if you want to log errors or handle them in another way,
 * either place a 500 error handler for this and pass the error when it's done, or don't use this method at all.
 * @return This, to be used fluently
 * @since 1.0.0
 */
fun Router.defaultApiInternalErrorHandler() = apiErrorHandler(500) { apiError(
	name = "internal_error",
	message = "Internal error",
	statusCode = 500
) }