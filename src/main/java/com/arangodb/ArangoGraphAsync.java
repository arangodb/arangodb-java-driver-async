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
import java.util.concurrent.CompletableFuture;

import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.internal.ArangoExecutorAsync;
import com.arangodb.internal.InternalArangoGraph;
import com.arangodb.internal.velocystream.ConnectionAsync;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoGraphAsync
		extends InternalArangoGraph<ArangoExecutorAsync, CompletableFuture<Response>, ConnectionAsync> {

	protected ArangoGraphAsync(final ArangoDatabaseAsync db, final String name) {
		super(db.executor(), db.name(), name);
	}

	protected ArangoExecutorAsync executor() {
		return executor;
	}

	/**
	 * Delete an existing graph
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#drop-a-graph">API Documentation</a>
	 * @return void
	 */
	public CompletableFuture<Void> drop() {
		return executor.execute(dropRequest(), Void.class);
	}

	/**
	 * Get a graph from the graph module
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#get-a-graph">API Documentation</a>
	 * @return the definition content of this graph
	 */
	public CompletableFuture<GraphEntity> getInfo() {
		return executor.execute(getInfoRequest(), getInfoResponseDeserializer());
	}

	/**
	 * Lists all vertex collections used in this graph
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#list-vertex-collections">API
	 *      Documentation</a>
	 * @return all vertex collections within this graph
	 */
	public CompletableFuture<Collection<String>> getVertexCollections() {
		return executor.execute(getVertexCollectionsRequest(), getVertexCollectionsResponseDeserializer());
	}

	/**
	 * Adds a vertex collection to the set of collections of the graph. If the collection does not exist, it will be
	 * created.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#add-vertex-collection">API
	 *      Documentation</a>
	 * @param name
	 *            The name of the collection
	 * @return information about the graph
	 */
	public CompletableFuture<GraphEntity> addVertexCollection(final String name) {
		return executor.execute(addVertexCollectionRequest(name), addVertexCollectionResponseDeserializer());
	}

	/**
	 * Returns a handler of the vertex collection by the given name
	 * 
	 * @param name
	 *            Name of the vertex collection
	 * @return collection handler
	 */
	public ArangoVertexCollectionAsync vertexCollection(final String name) {
		return new ArangoVertexCollectionAsync(this, name);
	}

	/**
	 * Returns a handler of the edge collection by the given name
	 * 
	 * @param name
	 *            Name of the edge collection
	 * @return collection handler
	 */
	public ArangoEdgeCollectionAsync edgeCollection(final String name) {
		return new ArangoEdgeCollectionAsync(this, name);
	}

	/**
	 * Lists all edge collections used in this graph
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#list-edge-definitions">API
	 *      Documentation</a>
	 * @return all edge collections within this graph
	 */
	public CompletableFuture<Collection<String>> getEdgeDefinitions() {
		return executor.execute(getEdgeDefinitionsRequest(), getEdgeDefinitionsDeserializer());
	}

	/**
	 * Add a new edge definition to the graph
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#add-edge-definition">API
	 *      Documentation</a>
	 * @param definition
	 * @return information about the graph
	 */
	public CompletableFuture<GraphEntity> addEdgeDefinition(final EdgeDefinition definition) {
		return executor.execute(addEdgeDefinitionRequest(definition), addEdgeDefinitionResponseDeserializer());
	}

	/**
	 * Change one specific edge definition. This will modify all occurrences of this definition in all graphs known to
	 * your database
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#replace-an-edge-definition">API
	 *      Documentation</a>
	 * @param definition
	 *            The edge definition
	 * @return information about the graph
	 */
	public CompletableFuture<GraphEntity> replaceEdgeDefinition(final EdgeDefinition definition) {
		return executor.execute(replaceEdgeDefinitionRequest(definition), replaceEdgeDefinitionResponseDeserializer());
	}

	/**
	 * Remove one edge definition from the graph. This will only remove the edge collection, the vertex collections
	 * remain untouched and can still be used in your queries
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Gharial/Management.html#remove-an-edge-definition-from-the-graph">API
	 *      Documentation</a>
	 * @param definitionName
	 *            The name of the edge collection used in the definition
	 * @return information about the graph
	 */
	public CompletableFuture<GraphEntity> removeEdgeDefinition(final String definitionName) {
		return executor.execute(removeEdgeDefinitionRequest(definitionName),
			removeEdgeDefinitionResponseDeserializer());
	}

}
