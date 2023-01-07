package net.termer.krestx.api.util

import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals

/**
 * Tests for API utils
 * @author termer
 */
class ApiUtilsTest {
	private val vertx = Vertx.vertx()

	private val host = "127.0.0.1"
	private val port = Random.nextInt(1024, 65535)
	private val currentApiVersion = "v2"
	private val supportedApiVersions = arrayOf("v1", "v2")

	private val router = Router.router(vertx)
	private val http = vertx.createHttpServer()
		.requestHandler(router)

	private val webClient = WebClient.create(vertx)

	@Test
	fun `Returns API info`() {
		runBlocking(vertx.dispatcher()) {
			router
				.defaultApiInfoHandler(currentApiVersion, supportedApiVersions)

			http.listen(port, host).await()

			val successJson = apiSuccess(
				jsonObjectOf(
					"currentVersion" to currentApiVersion,
					"supportedVersions" to supportedApiVersions
				)
			).toJson().toString()

			val resJson = webClient.get(port, host, "/api")
				.send().await()
				.bodyAsString()

			assertEquals(successJson, resJson)
		}
	}

	@Test
	fun `API routers are mounted properly`() {
		runBlocking(vertx.dispatcher()) {
			router
				.mountApiRouter("v1", Router.router(vertx).apply {
					get("/test").apiHandler { apiSuccess(jsonObjectOf("version" to "v1")) }
				})
				.mountApiRouter("v2", Router.router(vertx).apply {
					get("/test").apiHandler { apiSuccess(jsonObjectOf("version" to "v2")) }
				})
			http.listen(port, host).await()

			val versionsToTest = arrayOf("v1", "v2")
			for(version in versionsToTest) {
				val res = webClient.get(port, host, "/api/$version/test")
					.send().await()

				val body = res.bodyAsJsonObject()

				assertEquals(200, res.statusCode())
				assertEquals(version, body.getString("version"))
			}
		}
	}

	@Test
	fun `Default error handlers return correct statuses and payloads`() {
		runBlocking(vertx.dispatcher()) {
			router
				.mountApiRouter("v1", Router.router(vertx).apply {
					get("/path-that-does-not-work")
						.handler { ctx -> ctx.fail(500, Exception("Test error")) }
					get("/youre-not-allowed-here")
						.handler { ctx -> ctx.fail(403) }
					get("/your-request-is-bad")
						.handler { ctx -> ctx.fail(400) }
					get("/you-cant-use-this-method")
						.handler { ctx -> ctx.fail(405) }
				})
				.defaultApiNotFoundHandler()
				.defaultApiUnauthorizedHandler()
				.defaultMethodNotAllowedHandler()
				.defaultBadRequestHandler()
				.defaultApiInternalErrorHandler()

			http.listen(port, host).await()

			val pathsAndStatuses = mapOf(
				"/api/v1/path-that-does-not-exist" to 404,
				"/api/v1/youre-not-allowed-here" to 403,
				"/api/v1/you-cant-use-this-method" to 405,
				"/api/v1/your-request-is-bad" to 400,
				"/api/v1/path-that-does-not-work" to 500
			)
			for((path, status) in pathsAndStatuses) {
				val res = webClient.get(port, host, path)
					.send().await()
				val body = res.bodyAsJsonObject()

				assertEquals(status, res.statusCode())
				assert(body.getJsonArray("errors").size() > 0)
			}
		}
	}

	@Test
	fun `Internal error handler non-async code is called before returning a result`() {
		runBlocking(vertx.dispatcher()) {
			val exceptionMsg = "The route failed for some reason"

			// This will be written to by the internal error handler
			var routeException: Throwable? = null

			router
				.mountApiRouter("v1", Router.router(vertx).apply {
					get("/path-that-does-not-work")
						.handler { ctx -> ctx.fail(500, Exception(exceptionMsg)) }
				})
				.defaultApiInternalErrorHandler {
					routeException = it.failure()
				}

			http.listen(port, host).await()

			val res = webClient.get(port, host, "/api/v1/path-that-does-not-work")
				.send().await()

			assertEquals(500, res.statusCode())
			assertEquals(exceptionMsg, routeException?.message)
		}
	}
}