/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

import java.util.concurrent.CompletableFuture;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoSearchAsync;
import com.arangodb.entity.ViewEntity;
import com.arangodb.entity.arangosearch.ArangoSearchPropertiesEntity;
import com.arangodb.model.arangosearch.ArangoSearchCreateOptions;
import com.arangodb.model.arangosearch.ArangoSearchPropertiesOptions;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoSearchAsyncImpl
		extends InternalArangoSearch<ArangoDBAsyncImpl, ArangoDatabaseAsyncImpl, ArangoExecutorAsync>
		implements ArangoSearchAsync {

	protected ArangoSearchAsyncImpl(final ArangoDatabaseAsyncImpl db, final String name) {
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
	public synchronized CompletableFuture<ViewEntity> rename(final String newName) {
		return executor.execute(renameRequest(newName), ViewEntity.class);
	}

	@Override
	public CompletableFuture<ViewEntity> getInfo() {
		return executor.execute(getInfoRequest(), ViewEntity.class);
	}

	@Override
	public CompletableFuture<ViewEntity> create() throws ArangoDBException {
		return create(new ArangoSearchCreateOptions());
	}

	@Override
	public CompletableFuture<ViewEntity> create(final ArangoSearchCreateOptions options) throws ArangoDBException {
		return db().createArangoSearch(name(), options);
	}

	@Override
	public CompletableFuture<ArangoSearchPropertiesEntity> getProperties() throws ArangoDBException {
		return executor.execute(getPropertiesRequest(), ArangoSearchPropertiesEntity.class);
	}

	@Override
	public CompletableFuture<ArangoSearchPropertiesEntity> updateProperties(final ArangoSearchPropertiesOptions options)
			throws ArangoDBException {
		return executor.execute(updatePropertiesRequest(options), ArangoSearchPropertiesEntity.class);
	}

	@Override
	public CompletableFuture<ArangoSearchPropertiesEntity> replaceProperties(
		final ArangoSearchPropertiesOptions options) throws ArangoDBException {
		return executor.execute(replacePropertiesRequest(options), ArangoSearchPropertiesEntity.class);
	}

}
