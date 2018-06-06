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
import java.util.function.BiConsumer;

import com.arangodb.ArangoCollectionAsync;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionPropertiesEntity;
import com.arangodb.entity.CollectionRevisionEntity;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentImportEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.entity.Permissions;
import com.arangodb.internal.ArangoExecutor.ResponseDeserializer;
import com.arangodb.internal.velocystream.ConnectionAsync;
import com.arangodb.model.CollectionPropertiesOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentExistsOptions;
import com.arangodb.model.DocumentImportOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.GeoIndexOptions;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.arangodb.model.SkiplistIndexOptions;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocystream.Response;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoCollectionAsyncImpl extends
		InternalArangoCollection<ArangoDBAsyncImpl, ArangoDatabaseAsyncImpl, ArangoExecutorAsync, CompletableFuture<Response>, ConnectionAsync>
		implements ArangoCollectionAsync {

	protected ArangoCollectionAsyncImpl(final ArangoDatabaseAsyncImpl db, final String name) {
		super(db, name);
	}

	@Override
	public <T> CompletableFuture<DocumentCreateEntity<T>> insertDocument(final T value) {
		return executor.execute(insertDocumentRequest(value, new DocumentCreateOptions()),
			insertDocumentResponseDeserializer(value));
	}

	@Override
	public <T> CompletableFuture<DocumentCreateEntity<T>> insertDocument(
		final T value,
		final DocumentCreateOptions options) {
		return executor.execute(insertDocumentRequest(value, options), insertDocumentResponseDeserializer(value));
	}

	@Override
	public <T> CompletableFuture<MultiDocumentEntity<DocumentCreateEntity<T>>> insertDocuments(
		final Collection<T> values) {
		final DocumentCreateOptions params = new DocumentCreateOptions();
		return executor.execute(insertDocumentsRequest(values, params),
			insertDocumentsResponseDeserializer(values, params));
	}

	@Override
	public <T> CompletableFuture<MultiDocumentEntity<DocumentCreateEntity<T>>> insertDocuments(
		final Collection<T> values,
		final DocumentCreateOptions options) {
		final DocumentCreateOptions params = (options != null ? options : new DocumentCreateOptions());
		return executor.execute(insertDocumentsRequest(values, params),
			insertDocumentsResponseDeserializer(values, params));
	}

	@Override
	public CompletableFuture<DocumentImportEntity> importDocuments(final Collection<?> values) {
		return importDocuments(values, new DocumentImportOptions());
	}

	@Override
	public CompletableFuture<DocumentImportEntity> importDocuments(
		final Collection<?> values,
		final DocumentImportOptions options) {
		return executor.execute(importDocumentsRequest(values, options), DocumentImportEntity.class);
	}

	@Override
	public CompletableFuture<DocumentImportEntity> importDocuments(final String values) {
		return executor.execute(importDocumentsRequest(values, new DocumentImportOptions()),
			DocumentImportEntity.class);
	}

	@Override
	public CompletableFuture<DocumentImportEntity> importDocuments(
		final String values,
		final DocumentImportOptions options) {
		return executor.execute(importDocumentsRequest(values, options), DocumentImportEntity.class);
	}

	@Override
	public <T> CompletableFuture<T> getDocument(final String key, final Class<T> type) throws ArangoDBException {
		executor.validateDocumentKey(key);
		final CompletableFuture<T> result = new CompletableFuture<>();
		final CompletableFuture<T> execute = executor.execute(getDocumentRequest(key, new DocumentReadOptions()), type);
		execute.whenComplete((response, ex) -> result.complete(response));
		return result;
	}

	@Override
	public <T> CompletableFuture<T> getDocument(
		final String key,
		final Class<T> type,
		final DocumentReadOptions options) throws ArangoDBException {
		executor.validateDocumentKey(key);
		final CompletableFuture<T> result = new CompletableFuture<>();
		final CompletableFuture<T> execute = executor.execute(getDocumentRequest(key, options), type);
		execute.whenComplete((response, ex) -> result.complete(response));
		return result;
	}

	@Override
	public <T> CompletableFuture<MultiDocumentEntity<T>> getDocuments(
		final Collection<String> keys,
		final Class<T> type) {
		final DocumentReadOptions options = new DocumentReadOptions();
		return executor.execute(getDocumentsRequest(keys, options), getDocumentsResponseDeserializer(type, options));
	}

	@Override
	public <T> CompletableFuture<DocumentUpdateEntity<T>> replaceDocument(final String key, final T value) {
		return executor.execute(replaceDocumentRequest(key, value, new DocumentReplaceOptions()),
			replaceDocumentResponseDeserializer(value));
	}

	@Override
	public <T> CompletableFuture<DocumentUpdateEntity<T>> replaceDocument(
		final String key,
		final T value,
		final DocumentReplaceOptions options) {
		return executor.execute(replaceDocumentRequest(key, value, options),
			replaceDocumentResponseDeserializer(value));
	}

	@Override
	public <T> CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<T>>> replaceDocuments(
		final Collection<T> values) {
		final DocumentReplaceOptions params = new DocumentReplaceOptions();
		return executor.execute(replaceDocumentsRequest(values, params),
			replaceDocumentsResponseDeserializer(values, params));
	}

	@Override
	public <T> CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<T>>> replaceDocuments(
		final Collection<T> values,
		final DocumentReplaceOptions options) {
		final DocumentReplaceOptions params = (options != null ? options : new DocumentReplaceOptions());
		return executor.execute(replaceDocumentsRequest(values, params),
			replaceDocumentsResponseDeserializer(values, params));
	}

	@Override
	public <T> CompletableFuture<DocumentUpdateEntity<T>> updateDocument(final String key, final T value) {
		return executor.execute(updateDocumentRequest(key, value, new DocumentUpdateOptions()),
			updateDocumentResponseDeserializer(value));
	}

	@Override
	public <T> CompletableFuture<DocumentUpdateEntity<T>> updateDocument(
		final String key,
		final T value,
		final DocumentUpdateOptions options) {
		return executor.execute(updateDocumentRequest(key, value, options), updateDocumentResponseDeserializer(value));
	}

	@Override
	public <T> CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<T>>> updateDocuments(
		final Collection<T> values) {
		final DocumentUpdateOptions params = new DocumentUpdateOptions();
		return executor.execute(updateDocumentsRequest(values, params),
			updateDocumentsResponseDeserializer(values, params));
	}

	@Override
	public <T> CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<T>>> updateDocuments(
		final Collection<T> values,
		final DocumentUpdateOptions options) {
		final DocumentUpdateOptions params = (options != null ? options : new DocumentUpdateOptions());
		return executor.execute(updateDocumentsRequest(values, params),
			updateDocumentsResponseDeserializer(values, params));
	}

	@Override
	public CompletableFuture<DocumentDeleteEntity<Void>> deleteDocument(final String key) {
		return executor.execute(deleteDocumentRequest(key, new DocumentDeleteOptions()),
			deleteDocumentResponseDeserializer(Void.class));
	}

	@Override
	public <T> CompletableFuture<DocumentDeleteEntity<T>> deleteDocument(
		final String key,
		final Class<T> type,
		final DocumentDeleteOptions options) {
		return executor.execute(deleteDocumentRequest(key, options), deleteDocumentResponseDeserializer(type));
	}

	@Override
	public CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<Void>>> deleteDocuments(
		final Collection<?> values) {
		return executor.execute(deleteDocumentsRequest(values, new DocumentDeleteOptions()),
			deleteDocumentsResponseDeserializer(Void.class));
	}

	@Override
	public <T> CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<T>>> deleteDocuments(
		final Collection<?> values,
		final Class<T> type,
		final DocumentDeleteOptions options) {
		return executor.execute(deleteDocumentsRequest(values, options), deleteDocumentsResponseDeserializer(type));
	}

	@Override
	public CompletableFuture<Boolean> documentExists(final String key) {
		final CompletableFuture<Boolean> result = new CompletableFuture<>();
		executor.execute(documentExistsRequest(key, new DocumentExistsOptions()), new ResponseDeserializer<Response>() {
			@Override
			public Response deserialize(final Response response) throws VPackException {
				return response;
			}
		}).whenComplete(documentExistsResponseConsumer(result));
		return result;
	}

	@Override
	public CompletableFuture<Boolean> documentExists(final String key, final DocumentExistsOptions options) {
		final CompletableFuture<Boolean> result = new CompletableFuture<>();
		executor.execute(documentExistsRequest(key, options), new ResponseDeserializer<Response>() {
			@Override
			public Response deserialize(final Response response) throws VPackException {
				return response;
			}
		}).whenComplete(documentExistsResponseConsumer(result));
		return result;
	}

	private BiConsumer<? super Response, ? super Throwable> documentExistsResponseConsumer(
		final CompletableFuture<Boolean> result) {
		return (response, ex) -> {
			if (response != null) {
				result.complete(true);
			} else if (ex != null) {
				result.complete(false);
			} else {
				result.cancel(true);
			}
		};
	}

	@Override
	public CompletableFuture<IndexEntity> getIndex(final String id) {
		return executor.execute(getIndexRequest(id), IndexEntity.class);
	}

	@Override
	public CompletableFuture<String> deleteIndex(final String id) {
		return executor.execute(deleteIndexRequest(id), deleteIndexResponseDeserializer());
	}

	@Override
	@Deprecated
	public CompletableFuture<IndexEntity> createHashIndex(
		final Collection<String> fields,
		final HashIndexOptions options) {
		return executor.execute(createHashIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	public CompletableFuture<IndexEntity> ensureHashIndex(
		final Iterable<String> fields,
		final HashIndexOptions options) {
		return executor.execute(createHashIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	@Deprecated
	public CompletableFuture<IndexEntity> createSkiplistIndex(
		final Collection<String> fields,
		final SkiplistIndexOptions options) {
		return executor.execute(createSkiplistIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	public CompletableFuture<IndexEntity> ensureSkiplistIndex(
		final Iterable<String> fields,
		final SkiplistIndexOptions options) {
		return executor.execute(createSkiplistIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	@Deprecated
	public CompletableFuture<IndexEntity> createPersistentIndex(
		final Collection<String> fields,
		final PersistentIndexOptions options) {
		return executor.execute(createPersistentIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	public CompletableFuture<IndexEntity> ensurePersistentIndex(
		final Iterable<String> fields,
		final PersistentIndexOptions options) {
		return executor.execute(createPersistentIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	@Deprecated
	public CompletableFuture<IndexEntity> createGeoIndex(
		final Collection<String> fields,
		final GeoIndexOptions options) {
		return executor.execute(createGeoIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	public CompletableFuture<IndexEntity> ensureGeoIndex(final Iterable<String> fields, final GeoIndexOptions options) {
		return executor.execute(createGeoIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	@Deprecated
	public CompletableFuture<IndexEntity> createFulltextIndex(
		final Collection<String> fields,
		final FulltextIndexOptions options) {
		return executor.execute(createFulltextIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	public CompletableFuture<IndexEntity> ensureFulltextIndex(
		final Iterable<String> fields,
		final FulltextIndexOptions options) {
		return executor.execute(createFulltextIndexRequest(fields, options), IndexEntity.class);
	}

	@Override
	public CompletableFuture<Collection<IndexEntity>> getIndexes() {
		return executor.execute(getIndexesRequest(), getIndexesResponseDeserializer());
	}

	@Override
	public CompletableFuture<Boolean> exists() {
		return getInfo().thenApply(info -> info != null).exceptionally(e -> e == null);
	}

	@Override
	public CompletableFuture<CollectionEntity> truncate() {
		return executor.execute(truncateRequest(), CollectionEntity.class);
	}

	@Override
	public CompletableFuture<CollectionPropertiesEntity> count() {
		return executor.execute(countRequest(), CollectionPropertiesEntity.class);
	}

	@Override
	public CompletableFuture<Void> drop() {
		return executor.execute(dropRequest(null), Void.class);
	}

	@Override
	public CompletableFuture<Void> drop(final boolean isSystem) {
		return executor.execute(dropRequest(isSystem), Void.class);
	}

	@Override
	public CompletableFuture<CollectionEntity> load() {
		return executor.execute(loadRequest(), CollectionEntity.class);
	}

	@Override
	public CompletableFuture<CollectionEntity> unload() {
		return executor.execute(unloadRequest(), CollectionEntity.class);
	}

	@Override
	public CompletableFuture<CollectionEntity> getInfo() {
		return executor.execute(getInfoRequest(), CollectionEntity.class);
	}

	@Override
	public CompletableFuture<CollectionPropertiesEntity> getProperties() {
		return executor.execute(getPropertiesRequest(), CollectionPropertiesEntity.class);
	}

	@Override
	public CompletableFuture<CollectionPropertiesEntity> changeProperties(final CollectionPropertiesOptions options) {
		return executor.execute(changePropertiesRequest(options), CollectionPropertiesEntity.class);
	}

	@Override
	public CompletableFuture<CollectionEntity> rename(final String newName) {
		return executor.execute(renameRequest(newName), CollectionEntity.class);
	}

	@Override
	public CompletableFuture<CollectionRevisionEntity> getRevision() {
		return executor.execute(getRevisionRequest(), CollectionRevisionEntity.class);
	}

	@Override
	public CompletableFuture<Void> grantAccess(final String user, final Permissions permissions) {
		return executor.execute(grantAccessRequest(user, permissions), Void.class);
	}

	@Override
	public CompletableFuture<Void> revokeAccess(final String user) {
		return executor.execute(grantAccessRequest(user, Permissions.NONE), Void.class);
	}

	@Override
	public CompletableFuture<Void> resetAccess(final String user) {
		return executor.execute(resetAccessRequest(user), Void.class);
	}

	@Override
	public CompletableFuture<Permissions> getPermissions(final String user) throws ArangoDBException {
		return executor.execute(getPermissionsRequest(user), getPermissionsResponseDeserialzer());
	}
}
