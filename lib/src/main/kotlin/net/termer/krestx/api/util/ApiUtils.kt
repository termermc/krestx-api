package net.termer.krestx.api.util

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import net.termer.krestx.api.handler.SuspendHandler

/**
 * An API request handler.
 * Alias to [SuspendHandler]<[RoutingContext], [ApiResponse]>.
 * @since 1.0.0
 */
typealias ApiRequestHandler = SuspendHandler<RoutingContext, ApiResponse>

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
	fun toJson() = jsonObjectOf(
		"name" to name,
		"message" to message,
		"data" to data
	)
}

/**
 * An API response
 * @since 1.1.0
 */
sealed interface ApiResponse {
	/**
	 * Returns a JSON representation of the response
	 * @return A JSON representation of the response
	 * @since 1.1.0
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
	val data: JsonObject = JsonObject()
) : ApiResponse {
	override fun toJson() = data
}

/**
 * An error API response.
 * Should contain at least one or more error objects.
 * @throws IllegalArgumentException If 0 errors were provided
 * @since 1.0.0
 */
data class ApiErrorResponse(
	/**
	 * The errors
	 * @since 1.0.0
	 */
	val errors: Array<ApiError>,

	/**
	 * The HTTP status code to send along with the response (defaults to 500).
	 * Not serialized in [toJson].
	 * @since 1.0.0
	 */
	val statusCode: Int = 500
) : ApiResponse {
	init {
		if (errors.isEmpty())
			throw IllegalArgumentException("At least one ApiError must be provided")
	}

	override fun toJson() = jsonObjectOf(
		"errors" to errors.map { it.toJson() }
	)

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
 * Sends an API response
 * @param res The API response
 * @since 1.0.0
 */
suspend fun RoutingContext.send(res: ApiResponse) {
	response().putHeader("content-type", "application/json; charset=UTF-8")

	when (res) {
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
 * Returns a successful API response
 * @param data Any additional data to include in the response (optional)
 * @return The successful API response
 * @since 1.1.0
 */
inline fun apiSuccess(data: JsonObject = JsonObject()) = ApiSuccessResponse(data)

/**
 * Returns an error API response.
 * Contains only one error. To include multiple, use [apiErrors].
 * @param name The computer-readable error name (e.g. "internal_error")
 * @param message The human-readable error message (e.g. "Internal error")
 * @param data Any additional data to include with the error (optional)
 * @param statusCode The status code to send along with the error response (defaults to 500)
 * @return The error API response
 * @since 1.0.0
 */
inline fun apiError(name: String, message: String, data: JsonObject? = null, statusCode: Int = 500) = ApiErrorResponse(
	errors = arrayOf(ApiError(name, message, data)),
	statusCode = statusCode
)

/**
 * Returns an error API response with multiple errors.
 * To return only one error, use [apiError].
 * @param errors The errors to send
 * @param statusCode The status code to send along with the error response (defaults to 500)
 * @return The error API response
 * @since 1.0.0
 */
inline fun apiErrors(errors: Array<ApiError>, statusCode: Int = 500) = ApiErrorResponse(
	errors = errors,
	statusCode = statusCode
)

/**
 * Returns a default API info success API response.
 * Returns the current API version and a list of supported versions.
 * @return A default API info success API response.
 * @since 1.1.1
 */
inline fun apiInfoSuccess(currentApiVersion: String, supportedApiVersions: Array<String>) = apiSuccess(
	jsonObjectOf(
		"currentVersion" to currentApiVersion,
		"supportedVersions" to supportedApiVersions
	)
)

/**
 * Returns a "Not found" error API response
 * @return A "Not found" error API response
 * @since 1.1.1
 */
inline fun apiNotFoundError() = apiError(
	name = "not_found",
	message = "Not found",
	statusCode = 404
)

/**
 * Returns an "Unauthorized" error API response
 * @return An "Unauthorized" error API response
 * @since 1.1.1
 */
inline fun apiUnauthorizedError() = apiError(
	name = "unauthorized",
	message = "Unauthorized",
	statusCode = 403
)

/**
 * Returns a "Method not allowed" error API response
 * @return A "Method not allowed" error API response
 * @since 1.1.1
 */
inline fun apiMethodNotAllowedError() = apiError(
	name = "method_not_allowed",
	message = "Method not allowed",
	statusCode = 405
)

/**
 * Returns a "Bad request" error API response
 * @return A "Bad request" error API response
 * @since 1.1.1
 */
inline fun apiBadRequestError() = apiError(
	name = "bad_request",
	message = "Bad request",
	statusCode = 400
)

/**
 * Returns an "Internal error" error API response
 * @return An "Internal error" error API response
 * @since 1.1.1
 */
inline fun apiInternalError() = apiError(
	name = "internal_error",
	message = "Internal error",
	statusCode = 500
)
