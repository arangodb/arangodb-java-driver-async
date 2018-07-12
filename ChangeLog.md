# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- added `ArangoCursorAsync.first()`
- added `java.util.stream.Stream` like methods for `ArangoCursor`
  - added `ArangoCursorAsync.foreach(Consumer)`
  - added `ArangoCursorAsync.map(Function)`
  - added `ArangoCursorAsync.filter(Predicate)`
  - added `ArangoCursorAsync.anyMatch(Predicate)`
  - added `ArangoCursorAsync.allMatch(Predicate)`
  - added `ArangoCursorAsync.noneMatch(Predicate)`
  - added `ArangoCursorAsync.collectInto(Collection)`

## [4.6.1] - 2018-07-12

### Added

- added convenience method `ArangoDatabase#query(String, Class)`
- added convenience method `ArangoDatabase#query(String, Map<String, Object>, Class)`
- added convenience method `ArangoDatabase#query(String, AqlQueryOptions, Class)`

### Fixed

- fixed missing `ArangoDBAsync.util() : ArangoSerialization`
- fixed missing `ArangoDatabaseAsync.util() : ArangoSerialization`
- fixed missing `ArangoCollectionAsync.util() : ArangoSerialization`
- fixed missing `ArangoGraphAsync.util() : ArangoSerialization`
- fixed missing `ArangoVertexCollectionAsync.util() : ArangoSerialization`
- fixed missing `ArangoEdgeCollectionAsync.util() : ArangoSerialization`

## [4.6.0] - 2018-07-02

### Added

- added convenience methods for arbitrary requests
  - added `ArangoDatabaseAsync.route(String...)`
- added `DocumentCreateOptions#silent(Boolean)`
- added `DocumentReplaceOptions#silent(Boolean)`
- added `DocumentUpdateOptions#silent(Boolean)`
- added `DocumentDeleteOptions#silent(Boolean)`
- added support for exclusive write operations
  - added `TransactionOptions#exclusiveCollections(String[])`

### Removed

- removed unnecessary deserializer for internal `_id` field

## [4.5.2] - 2018-06-25

### Added

- added support for custom serializer
  - added `ArangoDBAsync#Builder#serializer(ArangoSerialization)`

## [4.5.1] - 2018-06-21

### Fixed

- fixed `exists()` method in `ArangoDatabaseAsync`, `ArangoCollectionAsync`, `ArangoGraphAsync`: check for ArangoDB error num
- fixed `ArangoDBAsync#aquireHostList(true)` with authentication

## [4.5.0] - 2018-06-11

### Added

- added replace-insert support: `DocumentCreateOptions#overwrite(Boolean)`
- added support for satellite collections: `CollectionCreateOptions#satellite(Boolean)`
- added `AqlQueryOptions#stream(boolean)` for Streaming AQL Cursors
- added `ArangoDatabaseAsync#create()`
- added `ArangoCollectionAsync#create()`
- added `ArangoCollectionAsync#create(CollectionCreateOptions)`
- added `ArangoGraphAsync#create(Collection<EdgeDefinition>)`
- added `ArangoGraphAsync#create(Collection<EdgeDefinition>, GraphCreateOptions)`
- added return type for `ArangoDatabaseAsync#deleteAqlFunction()`
- added field `AqlFunctionEntity#isDeterministic`

### Changed

- upgraded dependency velocypack 1.2.0
  - replaced dependency json-simple with jackson
- extracted interfaces for ArangoDB API

### Removed

- removed deprecated `ArangoDBAsync#Builder#host(String)`
- removed deprecated `ArangoDBAsync#Builder#port(Integer)`
- removed deprecated `ArangoCollectionAsync#create[IndexType]Index()`
- removed deprecated `ArangoDatabaseAsync#updateUserDefaultCollectionAccess()`
- removed deprecated `ArangoDBAsync#updateUserDefaultDatabaseAccess()`
- removed deprecated `ArangoDBAsync#updateUserDefaultCollectionAccess()`
- removed several deprecated APIs

## Fixed

- fixed `aquireHostList` bug when using active failover

## [4.4.1] - 2018-06-04

### Fixed

- fixed concurrency bug in VST when using connectionTtl

## [4.4.0] - 2018-04-19

### Changed

- changed dependency com.arangodb:velocypack to 1.1.0
  - fixed DateUtil does incorrect conversion of UTC time
  - serialize `BigInteger`/`BigDecimal` as `String`

### Fixed

- fixed reconnecting after ArangoDB restarts
- fixed `ArangoCollectionAsync#updateDocuments()` ignoring `DocumentUpdateOptions#serializeNull`

## [4.3.7] - 2018-04-17

### Fixed

- fixed property loading
- fixed compatibility for ArangoDatabase.getAqlFunctions() for ArangoDB 3.4

## [4.3.6] - 2018-04-16

### Added

- added `ArangoDBAsync#Builder#maxConnectionTtl(Integer)`

## [4.3.4] - 2018-03-21

### Changed

- made `ErrorEntity` serializable

### Fixed

- fixed serialization of bind parameter with null values
- fixed VelocyStream multi-thread authentication bug
- fixed load balancing cursor stickiness bug

## [4.3.3] - 2018-02-01

### Added

- added `CollectionCreateOptions#distributeShardsLike(String)`
- added `AqlQueryOptions#memoryLimit(Long)`
- added `AqlQueryOptions#failOnWarning(Boolean)`
- added `AqlQueryOptions#maxTransactionSize(Long)`
- added `AqlQueryOptions#maxWarningCount(Long)`
- added `AqlQueryOptions#intermediateCommitCount(Long)`
- added `AqlQueryOptions#intermediateCommitSize(Long)`
- added `AqlQueryOptions#satelliteSyncWait(Double)`
- added `AqlQueryOptions#skipInaccessibleCollections(Boolean)`
- added `TransactionOptions#maxTransactionSize(Long)`
- added `TransactionOptions#intermediateCommitCount(Long)`
- added `TransactionOptions#intermediateCommitSize(Long)`
- added `QueryEntity#getBindVars(): Map<String, Object>`
- added `QueryEntity#getState(): QueryExecutionState`

### Fixed

- fixed inconsistency of `ArangoCollectionAsync#getDocument()` variants

## [4.3.2] - 2017-11-30

### Fixed

- fixed redirect header (uppercase)

## [4.3.1] - 2017-11-27

### Fixed

- fixed default Json parsing, include null values
- fixed Json parsing of negative long

## [4.3.0] - 2017-11-23

### Added

- added load balancing (`ArangoDBAsync#Builder#loadBalancingStrategy()`)
- added automatic acquiring of hosts for load balancing or as fallback (`ArangoDBAsync#Builder#acquireHostList()`)

## [4.2.7] - 2017-11-03

### Added

- added `ArangoGraphAsync.exists()`

### Fixed

- fixed de-/serialization of negative int values

## [4.2.5] - 2017-10-16

### Added

- added `ArangoCollectionAsync#exists()`
- added `ArangoDatabaseAsync#exists()`
- added `BaseDocument#setId(String)`
- added `GraphCreateOptions#replicationFactor(Integer)`

### Changed

- `ArangoDBAsync#shutdown()` now closes all connections

## [4.2.4] - 2017-09-04

### Added

- added properties validation `arangodb.host`
- added `ArangoCollectionAsync#ensure<IndexType>Index()`

### Deprecated

- deprecated `ArangoCollectionAsync#create<IndexType>Index()`

### Fixed

- fixed `ArangoDatabaseAsync#transaction()`: ignore null result
- fixed `ArangoCollectionAsync#updateDocument()`
- fixed `ArangoVertexCollectionAsync#updateVertex()`
- fixed `ArangoEdgeCollectionAsync#updateEdge()`

## [4.2.3] - 2017-07-31

### Added

- added `ArangoDatabaseAsync#getPermissions(String)`
- added `ArangoCollectionAsync#getPermissions(String)`
- added `ArangoDBAsync#grantDefaultDatabaseAccess(String, Permissions)`
- added `ArangoDBAsync#grantDefaultCollectionAccess(String, Permissions)`
- added `ArangoDatabaseAsync#grantDefaultCollectionAccess(String, Permissions)`

### Fixed

- fixed `DateUtil` (thread-safe)

## [4.2.2] - 2017-07-20

### Added

- added `ArangoDatabaseAsync#grantAccess(String, Permissions)`
- added `ArangoCollectionAsync#grantAccess(String, Permissions)`
- added `ArangoDatabaseAsync#resetAccess(String)`
- added `ArangoCollectionAsync#resetAccess(String)`
- added `ArangoDBAsync#updateUserDefaultDatabaseAccess(String, Permissions)`
- added `ArangoDBAsync#updateUserDefaultCollectionAccess(String, Permissions)`
- added `ArangoDatabaseAsync#updateUserDefaultCollectionAccess(String, Permissions)`
- added `ArangoCollectionAsync#getDocuments(Collection<String>, Class)`
- added connection/handshake retry on same host
- added deduplicate field for hash/skiplist index

## [4.2.1] - 2017-06-20

### Fixed

- fixed deserializing of internal field `_id`

## [4.2.0] - 2017-06-14

### Added

- added `ArangoDBVersion#getLicense()`
- added `ArangoDB#getRole()`
- added `ArangoDBException#getException()`

### Changed

- updated velocypack-module-jdk8
  - added support for deserializing null values into `Optional#empty()`
  - added support for deserializing null values into `OptionalDouble#empty()`
  - added support for deserializing null values into `OptionalInt#empty()`
  - added support for deserializing null values into `OptionalLong#empty()`

## [4.1.12] - 2017-04-21

### Added

- added `ArangoDatabaseAsync#cursor()`

### Changed

- optimized `ArangoDBAsync.Builder` for better multi thread support

## [4.1.11] - 2017-03-24

### Added

- added convenience methods (`ArangoDatabaseAsync#arango()`, `ArangoCollectionAsync#db()`, `ArangoGraphAsync#db()`)
- added convenience methods (`ArangoCollectionAsync#getIndex(String)`, `ArangoCollectionAsync#deleteIndex(key)`)
- added connection pooling
- added extension point for VelocyPack serialization (`ArangoDBAsync#registerModule()`)
- added dependency java-velocypack-module-jdk8
- added support for replacing build-in VelocyPack serializer/deserializer
- added `ArangoDatabaseAsync#getVersion()`, `ArangoDatabaseAsync#getAccessibleDatabases()`

### Changed

- extracted VelocyPack implementation to https://github.com/arangodb/java-velocypack

### Fixed

- fixed exception handling in Connection

## [4.1.10] - 2017-02-22

### Added

- added support for multiple hosts as fallbacks
- added support serializing collections with null elements
- added support serializing non-generic classes that extend collections
- added support serializing/deserializing byte and Byte
- added default value "root" for user

### Changed

- changed velocystream message sending to async
- changed return value of getVertex/getEdge to null if not exists

### Fixed

- fixed serialization of additionalFields for objects and maps
- fixed VPack parsing (arrays of specific length)

## [4.1.9] - 2017-02-10

### Added

- added missing IndexType.edge

### Fixed

- fixed URI encoding

## [4.1.8] - 2017-02-03

### Added

- added byte[] de-/serialization from/to VPack.string (Base64)
- added ArangoCollection.drop(isSystem)
- improved ArangoDBException with responseCode, errorNum, errorMessage

### Changed

- changed java.util.Date serialization from VPack.date to VPack.string (ISO 8601)
- changed java.sql.Date serialization from VPack.date to VPack.string (ISO 8601)
- changed java.sql.Timestamp serialization from VPack.date to VPack.string (ISO 8601)
- changed java.java.time.Instant serialization from VPack.date to VPack.string (ISO 8601)
- changed java.java.time.LocalDate serialization from VPack.date to VPack.string (ISO 8601)
- changed java.java.time.LocalDateTime serialization from VPack.date to VPack.string (ISO 8601)
- changed ArangoCollection.deleteDocuments() to work with keys and documents

### Fixed

- fixed URL encoding bug
- fixed update/replaceDocumets with Json

## [4.1.7] - 2017-01-26

### Fixed

- fixed importDocuments, insertDocuments to work with raw Jsons

## [4.1.6] - 2017-01-18

### Added

- added serializer support for enclosing types

## [4.1.5] - 2017-01-12

### Added

- added configuration for custom annotations within VPack de-/serialization
- added support of transient modifier within VPack de-/serialization

### Fixed

- fixed VPack String serialization (UTF-8 encoding)
- fixed VPack parsing of fields of type Object
- fixed VPack serializing of array with null values

## [4.1.4] - 2016-12-19

### Added

- added VPack serializer/de-serializer for java.util.UUID

### Fixed

- fixed VPack parsing

## [4.1.3] - 2016-11-22

### Added

- added bulk import API

### Fixed

- fixed error while serializing long values with VPackBuilder

## [4.1.2] - 2016-11-10

### Added

- added VelocyPack UTC_DATE parsing to Json String (ISO 8601)
- added configuration methods for VPackParser in ArangoDBAsync.Builder
- added VPackJsonSerializer for VPackParser

### Fixed

- fixed GraphEntity for ArangoDatabase.getGraphs() (field name is null)

## [4.1.1] - 2016-11-09

### Added

- added missing replicationFactor in CollectionCreateOptions
- added missing replicationFactor in CollectionPropertiesEntity
- added option serializeNull in DocumentUpdateOptions

### Changed

- changed json parsing of VelocyPack types not known in json

### Fixed

- fixed VelocyPack bug with non-ASCII characters

## [4.1.0] - 2016-10-28

### Added

- added VeloyPack serialization support for java.time.Instant, LocalDate, LocalDateTime
- added ArangoUtil for manually de-/serialization

### Changed

- changed VelocyStream communication (send protocol header)
