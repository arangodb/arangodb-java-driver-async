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

package com.arangodb.internal;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.arangodb.ArangoEdgeCollectionAsync;
import com.arangodb.ArangoGraphAsync;
import com.arangodb.ArangoVertexCollectionAsync;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.internal.velocystream.ConnectionAsync;
import com.arangodb.velocystream.Response;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoGraphAsyncImpl extends
		InternalArangoGraph<ArangoDBAsyncImpl, ArangoDatabaseAsyncImpl, ArangoExecutorAsync, CompletableFuture<Response>, ConnectionAsync>
		implements ArangoGraphAsync {

	protected ArangoGraphAsyncImpl(final ArangoDatabaseAsyncImpl db, final String name) {
		super(db, name);
	}

	@Override
	public CompletableFuture<Boolean> exists() {
		return getInfo().thenApply(info -> info != null).exceptionally(e -> e == null);
	}

	@Override
	public CompletableFuture<Void> drop() {
		return executor.execute(dropRequest(), Void.class);
	}

	@Override
	public CompletableFuture<GraphEntity> getInfo() {
		return executor.execute(getInfoRequest(), getInfoResponseDeserializer());
	}

	@Override
	public CompletableFuture<Collection<String>> getVertexCollections() {
		return executor.execute(getVertexCollectionsRequest(), getVertexCollectionsResponseDeserializer());
	}

	@Override
	public CompletableFuture<GraphEntity> addVertexCollection(final String name) {
		return executor.execute(addVertexCollectionRequest(name), addVertexCollectionResponseDeserializer());
	}

	@Override
	public ArangoVertexCollectionAsync vertexCollection(final String name) {
		return new ArangoVertexCollectionAsyncImpl(this, name);
	}

	@Override
	public ArangoEdgeCollectionAsync edgeCollection(final String name) {
		return new ArangoEdgeCollectionAsyncImpl(this, name);
	}

	@Override
	public CompletableFuture<Collection<String>> getEdgeDefinitions() {
		return executor.execute(getEdgeDefinitionsRequest(), getEdgeDefinitionsDeserializer());
	}

	@Override
	public CompletableFuture<GraphEntity> addEdgeDefinition(final EdgeDefinition definition) {
		return executor.execute(addEdgeDefinitionRequest(definition), addEdgeDefinitionResponseDeserializer());
	}

	@Override
	public CompletableFuture<GraphEntity> replaceEdgeDefinition(final EdgeDefinition definition) {
		return executor.execute(replaceEdgeDefinitionRequest(definition), replaceEdgeDefinitionResponseDeserializer());
	}

	@Override
	public CompletableFuture<GraphEntity> removeEdgeDefinition(final String definitionName) {
		return executor.execute(removeEdgeDefinitionRequest(definitionName),
			removeEdgeDefinitionResponseDeserializer());
	}

}
