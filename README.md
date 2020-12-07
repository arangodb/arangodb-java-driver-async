# [DEPRECATED]: `arangodb-java-driver-async` has been merged into [arangodb-java-driver](https://github.com/arangodb/arangodb-java-driver)



![ArangoDB-Logo](https://www.arangodb.com/docs/assets/arangodb_logo_2016_inverted.png)

# arangodb-java-driver-async

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.arangodb/arangodb-java-driver-async/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.arangodb/arangodb-java-driver-async)

The asynchronous variant of the official ArangoDB Java Driver.

The following documentation links to the documentation of the synchronous variant, because this driver provides a nearly identical API with some exceptions:

- The main access point is the class `ArangoDBAsync` instead of `ArangoDB`
- Each method, which performs a database operation, returns a `CompletableFuture` instead of the normal result.
- Only the transport protocol `VST` is supported (no `HTTP`).

## Documentation

- [Getting Started](https://github.com/arangodb/arangodb-java-driver/blob/master/docs/Drivers/Java/GettingStarted/README.md)
- [Reference](https://github.com/arangodb/arangodb-java-driver/blob/master/docs/Drivers/Java/Reference/README.md)

## Learn more

- [ArangoDB](https://www.arangodb.com/)
- [ChangeLog](ChangeLog.md)
- [Examples](src/test/java/com/arangodb/example)
- [JavaDoc](http://arangodb.github.io/arangodb-java-driver-async/javadoc-5_1)
