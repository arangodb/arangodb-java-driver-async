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
import java.util.function.BiConsumer;

import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionPropertiesEntity;
import com.arangodb.entity.CollectionRevisionEntity;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentImportEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.internal.ArangoExecutorAsync;
import com.arangodb.internal.InternalArangoCollection;
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
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoCollectionAsync
		extends InternalArangoCollection<ArangoExecutorAsync, CompletableFuture<Response>, ConnectionAsync> {

	private final ArangoDatabaseAsync db;

	protected ArangoCollectionAsync(final ArangoDatabaseAsync db, final String name) {
		super(db.executor(), db.name(), name);
		this.db = db;
	}

	protected ArangoDatabaseAsync db() {
		return db;
	}

	/**
	 * Creates a new document from the given document, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#create-document">API
	 *      Documentation</a>
	 * @param value
	 *            A representation of a single document (POJO, VPackSlice or String for Json)
	 * @return information about the document
	 */
	public <T> CompletableFuture<DocumentCreateEntity<T>> insertDocument(final T value) {
		return executor.execute(insertDocumentRequest(value, new DocumentCreateOptions()),
			insertDocumentResponseDeserializer(value));
	}

	/**
	 * Creates a new document from the given document, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#create-document">API
	 *      Documentation</a>
	 * @param value
	 *            A representation of a single document (POJO, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 */
	public <T> CompletableFuture<DocumentCreateEntity<T>> insertDocument(
		final T value,
		final DocumentCreateOptions options) {
		return executor.execute(insertDocumentRequest(value, options), insertDocumentResponseDeserializer(value));
	}

	/**
	 * Creates new documents from the given documents, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#create-document">API
	 *      Documentation</a>
	 * @param values
	 *            A List of documents (POJO, VPackSlice or String for Json)
	 * @return information about the documents
	 */
	public <T> CompletableFuture<MultiDocumentEntity<DocumentCreateEntity<T>>> insertDocuments(
		final Collection<T> values) {
		final DocumentCreateOptions params = new DocumentCreateOptions();
		return executor.execute(insertDocumentsRequest(values, params),
			insertDocumentsResponseDeserializer(values, params));
	}

	/**
	 * Creates new documents from the given documents, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#create-document">API
	 *      Documentation</a>
	 * @param values
	 *            A List of documents (POJO, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 */
	public <T> CompletableFuture<MultiDocumentEntity<DocumentCreateEntity<T>>> insertDocuments(
		final Collection<T> values,
		final DocumentCreateOptions options) {
		final DocumentCreateOptions params = (options != null ? options : new DocumentCreateOptions());
		return executor.execute(insertDocumentsRequest(values, params),
			insertDocumentsResponseDeserializer(values, params));
	}

	/**
	 * Imports documents
	 * 
	 * @param values
	 *            a list of Objects that will be stored as documents
	 * @return information about the import
	 * @throws ArangoDBException
	 */
	public CompletableFuture<DocumentImportEntity> importDocuments(final Collection<?> values) {
		return importDocuments(values, new DocumentImportOptions());
	}

	/**
	 * Imports documents
	 * 
	 * @param values
	 *            a list of Objects that will be stored as documents
	 * @param options
	 *            Additional options, can be null
	 * @return information about the import
	 * @throws ArangoDBException
	 */
	public CompletableFuture<DocumentImportEntity> importDocuments(
		final Collection<?> values,
		final DocumentImportOptions options) {
		return executor.execute(importDocumentsRequest(values, options), DocumentImportEntity.class);
	}

	/**
	 * Imports documents
	 * 
	 * @param values
	 *            JSON-encoded array of objects that will be stored as documents
	 * @return information about the import
	 * @throws ArangoDBException
	 */
	public CompletableFuture<DocumentImportEntity> importDocuments(final String values) {
		return executor.execute(importDocumentsRequest(values, new DocumentImportOptions()),
			DocumentImportEntity.class);
	}

	/**
	 * Imports documents
	 * 
	 * @param values
	 *            JSON-encoded array of objects that will be stored as documents
	 * @param options
	 *            Additional options, can be null
	 * @return information about the import
	 * @throws ArangoDBException
	 */
	public CompletableFuture<DocumentImportEntity> importDocuments(
		final String values,
		final DocumentImportOptions options) {
		return executor.execute(importDocumentsRequest(values, options), DocumentImportEntity.class);
	}

	/**
	 * Reads a single document
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#read-document">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @param type
	 *            The type of the document (POJO class, VPackSlice or String for Json)
	 * @return the document identified by the key
	 */
	public <T> CompletableFuture<T> getDocument(final String key, final Class<T> type) throws ArangoDBException {
		executor.validateDocumentKey(key);
		final CompletableFuture<T> result = new CompletableFuture<>();
		final CompletableFuture<T> execute = executor.execute(getDocumentRequest(key, new DocumentReadOptions()), type);
		execute.whenComplete((response, ex) -> result.complete(response));
		return result;
	}

	/**
	 * Reads a single document
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#read-document">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @param type
	 *            The type of the document (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return the document identified by the key
	 */
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

	/**
	 * Replaces the document with key with the one in the body, provided there is such a document and no precondition is
	 * violated
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#replace-document">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @param value
	 *            A representation of a single document (POJO, VPackSlice or String for Json)
	 * @return information about the document
	 */
	public <T> CompletableFuture<DocumentUpdateEntity<T>> replaceDocument(final String key, final T value) {
		return executor.execute(replaceDocumentRequest(key, value, new DocumentReplaceOptions()),
			replaceDocumentResponseDeserializer(value));
	}

	/**
	 * Replaces the document with key with the one in the body, provided there is such a document and no precondition is
	 * violated
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#replace-document">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @param value
	 *            A representation of a single document (POJO, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 */
	public <T> CompletableFuture<DocumentUpdateEntity<T>> replaceDocument(
		final String key,
		final T value,
		final DocumentReplaceOptions options) {
		return executor.execute(replaceDocumentRequest(key, value, options),
			replaceDocumentResponseDeserializer(value));
	}

	/**
	 * Replaces multiple documents in the specified collection with the ones in the values, the replaced documents are
	 * specified by the _key attributes in the documents in values.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#replace-documents">API
	 *      Documentation</a>
	 * @param values
	 *            A List of documents (POJO, VPackSlice or String for Json)
	 * @return information about the documents
	 */
	public <T> CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<T>>> replaceDocuments(
		final Collection<T> values) {
		final DocumentReplaceOptions params = new DocumentReplaceOptions();
		return executor.execute(replaceDocumentsRequest(values, params),
			replaceDocumentsResponseDeserializer(values, params));
	}

	/**
	 * Replaces multiple documents in the specified collection with the ones in the values, the replaced documents are
	 * specified by the _key attributes in the documents in values.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#replace-documents">API
	 *      Documentation</a>
	 * @param values
	 *            A List of documents (POJO, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 */
	public <T> CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<T>>> replaceDocuments(
		final Collection<T> values,
		final DocumentReplaceOptions options) {
		final DocumentReplaceOptions params = (options != null ? options : new DocumentReplaceOptions());
		return executor.execute(replaceDocumentsRequest(values, params),
			replaceDocumentsResponseDeserializer(values, params));
	}

	/**
	 * Partially updates the document identified by document-key. The value must contain a document with the attributes
	 * to patch (the patch document). All attributes from the patch document will be added to the existing document if
	 * they do not yet exist, and overwritten in the existing document if they do exist there.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#update-document">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @param value
	 *            A representation of a single document (POJO, VPackSlice or String for Json)
	 * @return information about the document
	 */
	public <T> CompletableFuture<DocumentUpdateEntity<T>> updateDocument(final String key, final T value) {
		return executor.execute(updateDocumentRequest(key, value, new DocumentUpdateOptions()),
			updateDocumentResponseDeserializer(value));
	}

	/**
	 * Partially updates the document identified by document-key. The value must contain a document with the attributes
	 * to patch (the patch document). All attributes from the patch document will be added to the existing document if
	 * they do not yet exist, and overwritten in the existing document if they do exist there.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#update-document">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @param value
	 *            A representation of a single document (POJO, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 */
	public <T> CompletableFuture<DocumentUpdateEntity<T>> updateDocument(
		final String key,
		final T value,
		final DocumentUpdateOptions options) {
		return executor.execute(updateDocumentRequest(key, value, options), updateDocumentResponseDeserializer(value));
	}

	/**
	 * Partially updates documents, the documents to update are specified by the _key attributes in the objects on
	 * values. Vales must contain a list of document updates with the attributes to patch (the patch documents). All
	 * attributes from the patch documents will be added to the existing documents if they do not yet exist, and
	 * overwritten in the existing documents if they do exist there.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#update-documents">API
	 *      Documentation</a>
	 * @param values
	 *            A list of documents (POJO, VPackSlice or String for Json)
	 * @return information about the documents
	 */
	public <T> CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<T>>> updateDocuments(
		final Collection<T> values) {
		final DocumentUpdateOptions params = new DocumentUpdateOptions();
		return executor.execute(updateDocumentsRequest(values, params),
			updateDocumentsResponseDeserializer(values, params));
	}

	/**
	 * Partially updates documents, the documents to update are specified by the _key attributes in the objects on
	 * values. Vales must contain a list of document updates with the attributes to patch (the patch documents). All
	 * attributes from the patch documents will be added to the existing documents if they do not yet exist, and
	 * overwritten in the existing documents if they do exist there.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#update-documents">API
	 *      Documentation</a>
	 * @param values
	 *            A list of documents (POJO, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 */
	public <T> CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<T>>> updateDocuments(
		final Collection<T> values,
		final DocumentUpdateOptions options) {
		final DocumentUpdateOptions params = (options != null ? options : new DocumentUpdateOptions());
		return executor.execute(updateDocumentsRequest(values, params),
			updateDocumentsResponseDeserializer(values, params));
	}

	/**
	 * Removes a document
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#removes-a-document">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @param type
	 *            The type of the document (POJO class, VPackSlice or String for Json). Only necessary if
	 *            options.returnOld is set to true, otherwise can be null.
	 * @return information about the document
	 */
	public CompletableFuture<DocumentDeleteEntity<Void>> deleteDocument(final String key) {
		return executor.execute(deleteDocumentRequest(key, new DocumentDeleteOptions()),
			deleteDocumentResponseDeserializer(Void.class));
	}

	/**
	 * Removes a document
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#removes-a-document">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @param type
	 *            The type of the document (POJO class, VPackSlice or String for Json). Only necessary if
	 *            options.returnOld is set to true, otherwise can be null.
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 */
	public <T> CompletableFuture<DocumentDeleteEntity<T>> deleteDocument(
		final String key,
		final Class<T> type,
		final DocumentDeleteOptions options) {
		return executor.execute(deleteDocumentRequest(key, options), deleteDocumentResponseDeserializer(type));
	}

	/**
	 * Removes multiple document
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#removes-multiple-documents">API
	 *      Documentation</a>
	 * @param keys
	 *            The keys of the documents
	 * @param type
	 *            The type of the documents (POJO class, VPackSlice or String for Json). Only necessary if
	 *            options.returnOld is set to true, otherwise can be null.
	 * @return information about the documents
	 */
	public CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<Void>>> deleteDocuments(
		final Collection<String> keys) {
		return executor.execute(deleteDocumentsRequest(keys, new DocumentDeleteOptions()),
			deleteDocumentsResponseDeserializer(Void.class));
	}

	/**
	 * Removes multiple document
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#removes-multiple-documents">API
	 *      Documentation</a>
	 * @param keys
	 *            The keys of the documents
	 * @param type
	 *            The type of the documents (POJO class, VPackSlice or String for Json). Only necessary if
	 *            options.returnOld is set to true, otherwise can be null.
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 */
	public <T> CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<T>>> deleteDocuments(
		final Collection<String> keys,
		final Class<T> type,
		final DocumentDeleteOptions options) {
		return executor.execute(deleteDocumentsRequest(keys, options), deleteDocumentsResponseDeserializer(type));
	}

	/**
	 * Checks if the document exists by reading a single document head
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#read-document-header">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @return true if the document was found, otherwise false
	 */
	public CompletableFuture<Boolean> documentExists(final String key) {
		final CompletableFuture<Boolean> result = new CompletableFuture<>();
		executor.communication().execute(documentExistsRequest(key, new DocumentExistsOptions()))
				.whenComplete(documentExistsResponseConsumer(result));
		return result;
	}

	/**
	 * Checks if the document exists by reading a single document head
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#read-document-header">API
	 *      Documentation</a>
	 * @param key
	 *            The key of the document
	 * @param options
	 *            Additional options, can be null
	 * @return true if the document was found, otherwise false
	 */
	public CompletableFuture<Boolean> documentExists(final String key, final DocumentExistsOptions options) {
		final CompletableFuture<Boolean> result = new CompletableFuture<>();
		executor.communication().execute(documentExistsRequest(key, options))
				.whenComplete(documentExistsResponseConsumer(result));
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

	/**
	 * Creates a hash index for the collection, if it does not already exist.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Indexes/Hash.html#create-hash-index">API Documentation</a>
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 */
	public CompletableFuture<IndexEntity> createHashIndex(
		final Collection<String> fields,
		final HashIndexOptions options) {
		return executor.execute(createHashIndexRequest(fields, options), IndexEntity.class);
	}

	/**
	 * Creates a skip-list index for the collection, if it does not already exist.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Indexes/Skiplist.html#create-skip-list">API
	 *      Documentation</a>
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 */
	public CompletableFuture<IndexEntity> createSkiplistIndex(
		final Collection<String> fields,
		final SkiplistIndexOptions options) {
		return executor.execute(createSkiplistIndexRequest(fields, options), IndexEntity.class);
	}

	/**
	 * Creates a persistent index for the collection, if it does not already exist.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Indexes/Persistent.html#create-a-persistent-index">API
	 *      Documentation</a>
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 */
	public CompletableFuture<IndexEntity> createPersistentIndex(
		final Collection<String> fields,
		final PersistentIndexOptions options) {
		return executor.execute(createPersistentIndexRequest(fields, options), IndexEntity.class);
	}

	/**
	 * Creates a geo-spatial index for the collection, if it does not already exist.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Indexes/Geo.html#create-geospatial-index">API
	 *      Documentation</a>
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 */
	public CompletableFuture<IndexEntity> createGeoIndex(
		final Collection<String> fields,
		final GeoIndexOptions options) {
		return executor.execute(createGeoIndexRequest(fields, options), IndexEntity.class);
	}

	/**
	 * Creates a fulltext index for the collection, if it does not already exist.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Indexes/Fulltext.html#create-fulltext-index">API
	 *      Documentation</a>
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 */
	public CompletableFuture<IndexEntity> createFulltextIndex(
		final Collection<String> fields,
		final FulltextIndexOptions options) {
		return executor.execute(createFulltextIndexRequest(fields, options), IndexEntity.class);
	}

	/**
	 * Returns all indexes of the collection
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Indexes/WorkingWith.html#read-all-indexes-of-a-collection">API
	 *      Documentation</a>
	 * @return information about the indexes
	 */
	public CompletableFuture<Collection<IndexEntity>> getIndexes() {
		return executor.execute(getIndexesRequest(), getIndexesResponseDeserializer());
	}

	/**
	 * Removes all documents from the collection, but leaves the indexes intact
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Creating.html#truncate-collection">API
	 *      Documentation</a>
	 * @return information about the collection
	 */
	public CompletableFuture<CollectionEntity> truncate() {
		return executor.execute(truncateRequest(), CollectionEntity.class);
	}

	/**
	 * Counts the documents in a collection
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Collection/Getting.html#return-number-of-documents-in-a-collection">API
	 *      Documentation</a>
	 * @return information about the collection, including the number of documents
	 */
	public CompletableFuture<CollectionPropertiesEntity> count() {
		return executor.execute(countRequest(), CollectionPropertiesEntity.class);
	}

	/**
	 * Drops the collection
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Creating.html#drops-collection">API
	 *      Documentation</a>
	 * @return void
	 */
	public CompletableFuture<Void> drop() {
		return executor.execute(dropRequest(null), Void.class);
	}

	/**
	 * Drops the collection
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Creating.html#drops-collection">API
	 *      Documentation</a>
	 * @param isSystem
	 *            Whether or not the collection to drop is a system collection. This parameter must be set to true in
	 *            order to drop a system collection.
	 * @return void
	 */
	public CompletableFuture<Void> drop(final boolean isSystem) {
		return executor.execute(dropRequest(isSystem), Void.class);
	}

	/**
	 * Loads a collection into memory.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Modifying.html#load-collection">API
	 *      Documentation</a>
	 * @return information about the collection
	 */
	public CompletableFuture<CollectionEntity> load() {
		return executor.execute(loadRequest(), CollectionEntity.class);
	}

	/**
	 * Removes a collection from memory. This call does not delete any documents. You can use the collection afterwards;
	 * in which case it will be loaded into memory, again.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Modifying.html#unload-collection">API
	 *      Documentation</a>
	 * @return information about the collection
	 */
	public CompletableFuture<CollectionEntity> unload() {
		return executor.execute(unloadRequest(), CollectionEntity.class);
	}

	/**
	 * Returns information about the collection
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Collection/Getting.html#return-information-about-a-collection">API
	 *      Documentation</a>
	 * @return information about the collection
	 */
	public CompletableFuture<CollectionEntity> getInfo() {
		return executor.execute(getInfoRequest(), CollectionEntity.class);
	}

	/**
	 * Reads the properties of the specified collection
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Collection/Getting.html#read-properties-of-a-collection">API
	 *      Documentation</a>
	 * @return properties of the collection
	 */
	public CompletableFuture<CollectionPropertiesEntity> getProperties() {
		return executor.execute(getPropertiesRequest(), CollectionPropertiesEntity.class);
	}

	/**
	 * Changes the properties of a collection
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Collection/Modifying.html#change-properties-of-a-collection">API
	 *      Documentation</a>
	 * @param options
	 *            Additional options, can be null
	 * @return properties of the collection
	 */
	public CompletableFuture<CollectionPropertiesEntity> changeProperties(final CollectionPropertiesOptions options) {
		return executor.execute(changePropertiesRequest(options), CollectionPropertiesEntity.class);
	}

	/**
	 * Renames a collection
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Modifying.html#rename-collection">API
	 *      Documentation</a>
	 * @param newName
	 *            The new name
	 * @return information about the collection
	 */
	public CompletableFuture<CollectionEntity> rename(final String newName) {
		return executor.execute(renameRequest(newName), CollectionEntity.class);
	}

	/**
	 * Retrieve the collections revision
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Getting.html#return-collection-revision-id">API
	 *      Documentation</a>
	 * @return information about the collection, including the collections revision
	 */
	public CompletableFuture<CollectionRevisionEntity> getRevision() {
		return executor.execute(getRevisionRequest(), CollectionRevisionEntity.class);
	}

}
