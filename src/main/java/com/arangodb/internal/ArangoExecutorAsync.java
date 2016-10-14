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

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

import com.arangodb.ArangoDBException;
import com.arangodb.internal.ArangoExecutor;
import com.arangodb.internal.CollectionCache;
import com.arangodb.internal.DocumentCache;
import com.arangodb.internal.velocystream.Communication;
import com.arangodb.internal.velocystream.ConnectionAsync;
import com.arangodb.velocypack.VPack;
import com.arangodb.velocypack.VPackParser;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoExecutorAsync extends ArangoExecutor<CompletableFuture<Response>, ConnectionAsync> {

	public ArangoExecutorAsync(final Communication<CompletableFuture<Response>, ConnectionAsync> communication,
		final VPack vpacker, final VPack vpackerNull, final VPackParser vpackParser, final DocumentCache documentCache,
		final CollectionCache collectionCache) {
		super(communication, vpacker, vpackerNull, vpackParser, documentCache, collectionCache);
	}

	public <T> CompletableFuture<T> execute(final Request request, final Type type) {
		return execute(request, (response) -> createResult(vpacker, vpackParser, type, response));
	}

	public <T> CompletableFuture<T> execute(final Request request, final ResponseDeserializer<T> responseDeserializer) {
		final CompletableFuture<T> result = new CompletableFuture<>();
		communication.execute(request).whenComplete((response, ex) -> {
			if (response != null) {
				try {
					result.complete(responseDeserializer.deserialize(response));
				} catch (final VPackException | ArangoDBException e) {
					result.completeExceptionally(e);
				}
			} else if (ex != null) {
				result.completeExceptionally(ex);
			} else {
				result.cancel(true);
			}
		});
		return result;
	}

}
