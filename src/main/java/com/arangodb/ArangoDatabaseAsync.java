/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.arangodb.entity.AqlExecutionExplainEntity;
import com.arangodb.entity.AqlFunctionEntity;
import com.arangodb.entity.AqlParseEntity;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.DatabaseEntity;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.Permissions;
import com.arangodb.entity.QueryCachePropertiesEntity;
import com.arangodb.entity.QueryEntity;
import com.arangodb.entity.QueryTrackingPropertiesEntity;
import com.arangodb.entity.TraversalEntity;
import com.arangodb.entity.ViewEntity;
import com.arangodb.entity.ViewType;
import com.arangodb.model.AqlFunctionCreateOptions;
import com.arangodb.model.AqlFunctionDeleteOptions;
import com.arangodb.model.AqlFunctionGetOptions;
import com.arangodb.model.AqlQueryExplainOptions;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.CollectionsReadOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.GraphCreateOptions;
import com.arangodb.model.TransactionOptions;
import com.arangodb.model.TraversalOptions;
import com.arangodb.model.arangosearch.ArangoSearchCreateOptions;

/**
 * Interface for operations on ArangoDB database level.
 * 
 * @see <a href="https://docs.arangodb.com/current/HTTP/Database/">Databases API Documentation</a>
 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQuery/">Query API Documentation</a>
 * @author Mark Vollmary
 */
public interface ArangoDatabaseAsync extends ArangoSerializationAccessor {

	/**
	 * Return the main entry point for the ArangoDB driver
	 * 
	 * @return main entry point
	 */
	ArangoDBAsync arango();

	/**
	 * Returns the name of the database
	 * 
	 * @return database name
	 */
	String name();

	/**
	 * Returns the server name and version number.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/MiscellaneousFunctions/index.html#return-server-version">API
	 *      Documentation</a>
	 * @return the server version, number
	 */
	CompletableFuture<ArangoDBVersion> getVersion();

	/**
	 * Checks whether the database exists
	 * 
	 * @return true if the database exists, otherwise false
	 */
	CompletableFuture<Boolean> exists();

	/**
	 * Retrieves a list of all databases the current user can access
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#list-of-accessible-databases">API
	 *      Documentation</a>
	 * @return a list of all databases the current user can access
	 */
	CompletableFuture<Collection<String>> getAccessibleDatabases();

	/**
	 * Returns a handler of the collection by the given name
	 * 
	 * @param name
	 *            Name of the collection
	 * @return collection handler
	 */
	ArangoCollectionAsync collection(final String name);

	/**
	 * Creates a collection
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Creating.html#create-collection">API
	 *      Documentation</a>
	 * @param name
	 *            The name of the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the collection
	 */
	CompletableFuture<CollectionEntity> createCollection(final String name);

	/**
	 * Creates a collection
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Creating.html#create-collection">API
	 *      Documentation</a>
	 * @param name
	 *            The name of the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the collection
	 */
	CompletableFuture<CollectionEntity> createCollection(final String name, final CollectionCreateOptions options);

	/**
	 * Returns all collections
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Getting.html#reads-all-collections">API
	 *      Documentation</a>
	 * @return list of information about all collections
	 */
	CompletableFuture<Collection<CollectionEntity>> getCollections();

	/**
	 * Returns all collections
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Getting.html#reads-all-collections">API
	 *      Documentation</a>
	 * @param options
	 *            Additional options, can be null
	 * @return list of information about all collections
	 */
	CompletableFuture<Collection<CollectionEntity>> getCollections(final CollectionsReadOptions options);

	/**
	 * Returns an index
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Indexes/WorkingWith.html#read-index">API Documentation</a>
	 * @param id
	 *            The index-handle
	 * @return information about the index
	 */
	CompletableFuture<IndexEntity> getIndex(final String id);

	/**
	 * Deletes an index
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Indexes/WorkingWith.html#delete-index">API Documentation</a>
	 * @param id
	 *            The index handle
	 * @return the id of the index
	 */
	CompletableFuture<String> deleteIndex(final String id);

	/**
	 * Creates the database
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#create-database">API
	 *      Documentation</a>
	 * @return true if the database was created successfully.
	 */
	CompletableFuture<Boolean> create();

	/**
	 * Drop an existing database
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#drop-database">API
	 *      Documentation</a>
	 * @return true if the database was dropped successfully
	 */
	CompletableFuture<Boolean> drop();

	/**
	 * Grants access to the database dbname for user user. You need permission to the _system database in order to
	 * execute this call.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/index.html#grant-or-revoke-database-access">
	 *      API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param permissions
	 *            The permissions the user grant
	 * @return void
	 */
	CompletableFuture<Void> grantAccess(final String user, final Permissions permissions);

	/**
	 * Grants access to the database dbname for user user. You need permission to the _system database in order to
	 * execute this call.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/index.html#grant-or-revoke-database-access">
	 *      API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return void
	 */
	CompletableFuture<Void> grantAccess(final String user);

	/**
	 * Revokes access to the database dbname for user user. You need permission to the _system database in order to
	 * execute this call.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/index.html#grant-or-revoke-database-access">
	 *      API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return void
	 */
	CompletableFuture<Void> revokeAccess(final String user);

	/**
	 * Clear the database access level, revert back to the default access level.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/index.html#grant-or-revoke-database-access">
	 *      API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @since ArangoDB 3.2.0
	 * @return void
	 */
	CompletableFuture<Void> resetAccess(final String user);

	/**
	 * Sets the default access level for collections within this database for the user <code>user</code>. You need
	 * permission to the _system database in order to execute this call.
	 * 
	 * @param user
	 *            The name of the user
	 * @param permissions
	 *            The permissions the user grant
	 * @since ArangoDB 3.2.0
	 */
	CompletableFuture<Void> grantDefaultCollectionAccess(final String user, final Permissions permissions);

	/**
	 * Get specific database access level
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/#get-the-database-access-level"> API
	 *      Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return permissions of the user
	 * @since ArangoDB 3.2.0
	 */
	CompletableFuture<Permissions> getPermissions(final String user);

	/**
	 * Performs a database query using the given {@code query} and {@code bindVars}, then returns a new
	 * {@code ArangoCursor} instance for the result list.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQueryCursor/AccessingCursors.html#create-cursor">API
	 *      Documentation</a>
	 * @param query
	 *            contains the query string to be executed
	 * @param bindVars
	 *            key/value pairs representing the bind parameters
	 * @param options
	 *            Additional options, can be null
	 * @param type
	 *            The type of the result (POJO class, VPackSlice, String for Json, or Collection/List/Map)
	 * @return cursor of the results
	 */
	<T> CompletableFuture<ArangoCursorAsync<T>> query(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options,
		final Class<T> type);

	/**
	 * Performs a database query using the given {@code query}, then returns a new {@code ArangoCursor} instance for the
	 * result list.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQueryCursor/AccessingCursors.html#create-cursor">API
	 *      Documentation</a>
	 * @param query
	 *            contains the query string to be executed
	 * @param options
	 *            Additional options, can be null
	 * @param type
	 *            The type of the result (POJO class, VPackSlice, String for Json, or Collection/List/Map)
	 * @return cursor of the results
	 */
	<T> CompletableFuture<ArangoCursorAsync<T>> query(
		final String query,
		final AqlQueryOptions options,
		final Class<T> type);

	/**
	 * Performs a database query using the given {@code query} and {@code bindVars}, then returns a new
	 * {@code ArangoCursor} instance for the result list.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQueryCursor/AccessingCursors.html#create-cursor">API
	 *      Documentation</a>
	 * @param query
	 *            contains the query string to be executed
	 * @param bindVars
	 *            key/value pairs representing the bind parameters
	 * @param type
	 *            The type of the result (POJO class, VPackSlice, String for Json, or Collection/List/Map)
	 * @return cursor of the results
	 */
	<T> CompletableFuture<ArangoCursorAsync<T>> query(
		final String query,
		final Map<String, Object> bindVars,
		final Class<T> type);

	/**
	 * Performs a database query using the given {@code query}, then returns a new {@code ArangoCursor} instance for the
	 * result list.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQueryCursor/AccessingCursors.html#create-cursor">API
	 *      Documentation</a>
	 * @param query
	 *            contains the query string to be executed
	 * @param type
	 *            The type of the result (POJO class, VPackSlice, String for Json, or Collection/List/Map)
	 * @return cursor of the results
	 */
	<T> CompletableFuture<ArangoCursorAsync<T>> query(final String query, final Class<T> type);

	/**
	 * Return an cursor from the given cursor-ID if still existing
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQueryCursor/AccessingCursors.html#read-next-batch-from-cursor">API
	 *      Documentation</a>
	 * @param cursorId
	 *            The ID of the cursor
	 * @param type
	 *            The type of the result (POJO class, VPackSlice, String for Json, or Collection/List/Map)
	 * @return cursor of the results
	 */
	<T> CompletableFuture<ArangoCursorAsync<T>> cursor(final String cursorId, final Class<T> type);

	/**
	 * Explain an AQL query and return information about it
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#explain-an-aql-query">API
	 *      Documentation</a>
	 * @param query
	 *            the query which you want explained
	 * @param bindVars
	 *            key/value pairs representing the bind parameters
	 * @param options
	 *            Additional options, can be null
	 * @return information about the query
	 */
	CompletableFuture<AqlExecutionExplainEntity> explainQuery(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryExplainOptions options);

	/**
	 * Parse an AQL query and return information about it This method is for query validation only. To actually query
	 * the database, see {@link ArangoDatabaseAsync#query(String, Map, AqlQueryOptions, Class)}
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#parse-an-aql-query">API
	 *      Documentation</a>
	 * @param query
	 *            the query which you want parse
	 * @return imformation about the query
	 */
	CompletableFuture<AqlParseEntity> parseQuery(final String query);

	/**
	 * Clears the AQL query cache
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQueryCache/index.html#clears-any-results-in-the-aql-query-cache">API
	 *      Documentation</a>
	 * @return void
	 */
	CompletableFuture<Void> clearQueryCache();

	/**
	 * Returns the global configuration for the AQL query cache
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQueryCache/index.html#returns-the-global-properties-for-the-aql-query-cache">API
	 *      Documentation</a>
	 * @return configuration for the AQL query cache
	 */
	CompletableFuture<QueryCachePropertiesEntity> getQueryCacheProperties();

	/**
	 * Changes the configuration for the AQL query cache. Note: changing the properties may invalidate all results in
	 * the cache.
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQueryCache/index.html#globally-adjusts-the-aql-query-result-cache-properties">API
	 *      Documentation</a>
	 * @param properties
	 *            properties to be set
	 * @return current set of properties
	 */
	CompletableFuture<QueryCachePropertiesEntity> setQueryCacheProperties(final QueryCachePropertiesEntity properties);

	/**
	 * Returns the configuration for the AQL query tracking
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#returns-the-properties-for-the-aql-query-tracking">API
	 *      Documentation</a>
	 * @return configuration for the AQL query tracking
	 */
	CompletableFuture<QueryTrackingPropertiesEntity> getQueryTrackingProperties();

	/**
	 * Changes the configuration for the AQL query tracking
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#changes-the-properties-for-the-aql-query-tracking">API
	 *      Documentation</a>
	 * @param properties
	 *            properties to be set
	 * @return current set of properties
	 */
	CompletableFuture<QueryTrackingPropertiesEntity> setQueryTrackingProperties(
		final QueryTrackingPropertiesEntity properties);

	/**
	 * Returns a list of currently running AQL queries
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#returns-the-currently-running-aql-queries">API
	 *      Documentation</a>
	 * @return a list of currently running AQL queries
	 */
	CompletableFuture<Collection<QueryEntity>> getCurrentlyRunningQueries();

	/**
	 * Returns a list of slow running AQL queries
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#returns-the-list-of-slow-aql-queries">API
	 *      Documentation</a>
	 * @return a list of slow running AQL queries
	 */
	CompletableFuture<Collection<QueryEntity>> getSlowQueries();

	/**
	 * Clears the list of slow AQL queries
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#clears-the-list-of-slow-aql-queries">API
	 *      Documentation</a>
	 * @return void
	 */
	CompletableFuture<Void> clearSlowQueries();

	/**
	 * Kills a running query. The query will be terminated at the next cancelation point.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#kills-a-running-aql-query">API
	 *      Documentation</a>
	 * @param id
	 *            The id of the query
	 * @return void
	 */
	CompletableFuture<Void> killQuery(final String id);

	/**
	 * Create a new AQL user function
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlUserFunctions/index.html#create-aql-user-function">API
	 *      Documentation</a>
	 * @param name
	 *            the fully qualified name of the user functions
	 * @param code
	 *            a string representation of the function body
	 * @param options
	 *            Additional options, can be null
	 * @return void
	 */
	CompletableFuture<Void> createAqlFunction(
		final String name,
		final String code,
		final AqlFunctionCreateOptions options);

	/**
	 * Remove an existing AQL user function
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlUserFunctions/index.html#remove-existing-aql-user-function">API
	 *      Documentation</a>
	 * @param name
	 *            the name of the AQL user function
	 * @param options
	 *            Additional options, can be null
	 * @return number of deleted functions (since ArangoDB 3.4.0)
	 */
	CompletableFuture<Integer> deleteAqlFunction(final String name, final AqlFunctionDeleteOptions options);

	/**
	 * Gets all reqistered AQL user functions
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlUserFunctions/index.html#return-registered-aql-user-functions">API
	 *      Documentation</a>
	 * @param options
	 *            Additional options, can be null
	 * @return all reqistered AQL user functions
	 */
	CompletableFuture<Collection<AqlFunctionEntity>> getAqlFunctions(final AqlFunctionGetOptions options);

	/**
	 * Returns a handler of the graph by the given name
	 * 
	 * @param name
	 *            Name of the graph
	 * @return graph handler
	 */
	ArangoGraphAsync graph(final String name);

	/**
	 * Create a new graph in the graph module. The creation of a graph requires the name of the graph and a definition
	 * of its edges.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#create-a-graph">API
	 *      Documentation</a>
	 * @param name
	 *            Name of the graph
	 * @param edgeDefinitions
	 *            An array of definitions for the edge
	 * @return information about the graph
	 */
	CompletableFuture<GraphEntity> createGraph(final String name, final Collection<EdgeDefinition> edgeDefinitions);

	/**
	 * Create a new graph in the graph module. The creation of a graph requires the name of the graph and a definition
	 * of its edges.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#create-a-graph">API
	 *      Documentation</a>
	 * @param name
	 *            Name of the graph
	 * @param edgeDefinitions
	 *            An array of definitions for the edge
	 * @param options
	 *            Additional options, can be null
	 * @return information about the graph
	 */
	CompletableFuture<GraphEntity> createGraph(
		final String name,
		final Collection<EdgeDefinition> edgeDefinitions,
		final GraphCreateOptions options);

	/**
	 * Lists all graphs known to the graph module
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#list-all-graphs">API
	 *      Documentation</a>
	 * @return graphs stored in this database
	 */
	CompletableFuture<Collection<GraphEntity>> getGraphs();

	/**
	 * Execute a server-side transaction
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Transaction/index.html#execute-transaction">API
	 *      Documentation</a>
	 * @param action
	 *            the actual transaction operations to be executed, in the form of stringified JavaScript code
	 * @param type
	 *            The type of the result (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return the result of the transaction if it succeeded
	 */
	<T> CompletableFuture<T> transaction(final String action, final Class<T> type, final TransactionOptions options);

	/**
	 * Retrieves information about the current database
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#information-of-the-database">API
	 *      Documentation</a>
	 * @return information about the current database
	 */
	CompletableFuture<DatabaseEntity> getInfo();

	/**
	 * Execute a server-side traversal
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/Traversal/index.html#executes-a-traversal">API
	 *      Documentation</a>
	 * @param vertexClass
	 *            The type of the vertex documents (POJO class, VPackSlice or String for Json)
	 * @param edgeClass
	 *            The type of the edge documents (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options
	 * @return Result of the executed traversal
	 */
	<V, E> CompletableFuture<TraversalEntity<V, E>> executeTraversal(
		final Class<V> vertexClass,
		final Class<E> edgeClass,
		final TraversalOptions options);

	/**
	 * Reads a single document
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#read-document">API
	 *      Documentation</a>
	 * @param id
	 *            The id of the document
	 * @param type
	 *            The type of the document (POJO class, VPackSlice or String for Json)
	 * @return the document identified by the id
	 */
	<T> CompletableFuture<T> getDocument(final String id, final Class<T> type) throws ArangoDBException;

	/**
	 * Reads a single document
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#read-document">API
	 *      Documentation</a>
	 * @param id
	 *            The id of the document
	 * @param type
	 *            The type of the document (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return the document identified by the id
	 */
	<T> CompletableFuture<T> getDocument(final String id, final Class<T> type, final DocumentReadOptions options)
			throws ArangoDBException;

	/**
	 * Reload the routing table.
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AdministrationAndMonitoring/index.html#reloads-the-routing-information">API
	 *      Documentation</a>
	 * @return void
	 */
	CompletableFuture<Void> reloadRouting();

	/**
	 * Returns a new {@link ArangoRouteAsync} instance for the given path (relative to the database) that can be used to
	 * perform arbitrary requests.
	 * 
	 * @param path
	 *            The database-relative URL of the route
	 * @return {@link ArangoRouteAsync}
	 */
	ArangoRouteAsync route(String... path);

	/**
	 * Fetches all views from the database and returns an list of view descriptions.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Views/Getting.html#reads-all-views">API Documentation</a>
	 * @return list of information about all views
	 * @since ArangoDB 3.4.0
	 */
	CompletableFuture<Collection<ViewEntity>> getViews();

	/**
	 * Returns a {@code ArangoViewAsync} instance for the given view name.
	 * 
	 * @param name
	 *            Name of the view
	 * @return view handler
	 * @since ArangoDB 3.4.0
	 */
	ArangoViewAsync view(String name);

	/**
	 * Returns a {@code ArangoSearchAsync} instance for the given ArangoSearch view name.
	 * 
	 * @param name
	 *            Name of the view
	 * @return ArangoSearch view handler
	 * @since ArangoDB 3.4.0
	 */
	ArangoSearchAsync arangoSearch(String name);

	/**
	 * Creates a view of the given {@code type}, then returns view information from the server.
	 * 
	 * @param name
	 *            The name of the view
	 * @param type
	 *            The type of the view
	 * @return information about the view
	 * @since ArangoDB 3.4.0
	 */
	CompletableFuture<ViewEntity> createView(String name, ViewType type);

	/**
	 * Creates a ArangoSearch view with the given {@code options}, then returns view information from the server.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Views/ArangoSearch.html#create-arangosearch-view">API
	 *      Documentation</a>
	 * @param name
	 *            The name of the view
	 * @param options
	 *            Additional options, can be null
	 * @return information about the view
	 * @since ArangoDB 3.4.0
	 */
	CompletableFuture<ViewEntity> createArangoSearch(String name, ArangoSearchCreateOptions options);

}
