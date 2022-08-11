# krestx-api
Opinionated Vert.x REST library built with Kotlin and coroutines. API portion.

This project includes a collection of utils for building REST APIs using Vert.x and Kotlin

# Setup
You need Java 8 or higher to use this library.

To use in your project, include the following in your `build.gradle` file:
```kotlin
implementation("net.termer.krestx:krestx-api:<current version>")
```

Additionally, you must add the latest 4.X.X versions of the following Vert.x dependencies:

 - vertx-core
 - vertx-web
 - vertx-lang-kotlin
 - vertx-lang-kotlin-coroutines

The library includes them as compile-only dependencies, so it will not work unless you include them in your own project as implementation dependencies.
This is to ensure the Vert.x version this library was built against doesn't interfere with the version you're using in your project.

# Usage
Most methods in this library are extension methods to provide Kotlin-specific and uniform REST functionality to various `vertx-web` components.

## Router Extensions
For example, here is an API request handler for `/api/v1/ping`:

```kotlin
val apiV1Router = Router.router(vertx)
    .get("/ping")
    .apiHandler { apiSuccess(
        jsonObjectOf(
            "time" to Instant.now().toString()
		)
	) }

appRouter.mountApiRouter("v1", apiV1Router)
```

For a default API setup (API info on "/api", default error handlers), you can use default API handler methods on a Router:

```kotlin
val currentApiVersion = "v2"
val supportedApiVersions = arrayOf("v1", "v2")

appRouter
    .defaultApiInfoHandler(currentApiVersion, supportedApiVersions)
    .defaultApiNotFoundHandler()
    .defaultApiUnauthorizedHandler()
    .defaultApiInternalErrorHandler()
```

## API Responses
API response handlers need to return an instance of a `ApiResponse` implementation, which can either be ApiSuccessResponse or ApiErrorResponse.
Success responses can optionally include additional JSON data. Error responses contain a status code (which should match the HTTP status code), an error of zero or more ApiError objects, which include a computer-readable error name (e.g. "internal_error"), a message, and optionally additional JSON data.

A success response (with no data in this case) will look something like this:

```json
{
  "success": true,
  "data": null
}
```

An error response (with 2 errors containing additional data) will look something like this:

```json
{
  "success": false,
  "statusCode": 400,
  "errors": [
    {
      "name": "missing_param",
      "message": "Request body is missing \"title\" parameter",
      "data": {
        "name": "title",
        "location": "body"
      }
    },
    {
      "name": "missing_param",
      "message": "Request body is missing \"description\" parameter",
      "data": {
        "name": "description",
        "location": "body"
      }
    }
  ]
}
```

# KDoc, Javadoc
Both are available on MavenCentral under the same artifact that was stated at the beginning of the `Usage` section.