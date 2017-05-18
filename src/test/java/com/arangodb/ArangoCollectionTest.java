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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionPropertiesEntity;
import com.arangodb.entity.CollectionRevisionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentImportEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.IndexType;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.CollectionPropertiesOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentExistsOptions;
import com.arangodb.model.DocumentImportOptions;
import com.arangodb.model.DocumentImportOptions.OnDuplicate;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoCollectionTest extends BaseTest {

	private static final String COLLECTION_NAME = "db_collection_test";

	@Before
	public void setup() throws InterruptedException, ExecutionException {
		db.createCollection(COLLECTION_NAME, null).get();
	}

	@After
	public void teardown() throws InterruptedException, ExecutionException {
		db.collection(COLLECTION_NAME).drop().get();
	}

	@Test
	public void insertDocument() throws InterruptedException, ExecutionException {
		final CompletableFuture<DocumentCreateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.insertDocument(new BaseDocument(), null);
		assertThat(f, is(notNullValue()));
		final DocumentCreateEntity<BaseDocument> doc = f.get();
		assertThat(doc, is(notNullValue()));
		assertThat(doc.getId(), is(notNullValue()));
		assertThat(doc.getKey(), is(notNullValue()));
		assertThat(doc.getRev(), is(notNullValue()));
		assertThat(doc.getNew(), is(nullValue()));
		assertThat(doc.getId(), is(COLLECTION_NAME + "/" + doc.getKey()));
	}

	@Test
	public void insertDocumentReturnNew() throws InterruptedException, ExecutionException {
		final DocumentCreateOptions options = new DocumentCreateOptions().returnNew(true);
		final CompletableFuture<DocumentCreateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.insertDocument(new BaseDocument(), options);
		assertThat(f, is(notNullValue()));
		final DocumentCreateEntity<BaseDocument> doc = f.get();
		assertThat(doc, is(notNullValue()));
		assertThat(doc.getId(), is(notNullValue()));
		assertThat(doc.getKey(), is(notNullValue()));
		assertThat(doc.getRev(), is(notNullValue()));
		assertThat(doc.getNew(), is(notNullValue()));
	}

	@Test
	public void insertDocumentWaitForSync() throws InterruptedException, ExecutionException {
		final DocumentCreateOptions options = new DocumentCreateOptions().waitForSync(true);
		final CompletableFuture<DocumentCreateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.insertDocument(new BaseDocument(), options);
		assertThat(f, is(notNullValue()));
		final DocumentCreateEntity<BaseDocument> doc = f.get();
		assertThat(doc, is(notNullValue()));
		assertThat(doc.getId(), is(notNullValue()));
		assertThat(doc.getKey(), is(notNullValue()));
		assertThat(doc.getRev(), is(notNullValue()));
		assertThat(doc.getNew(), is(nullValue()));
	}

	@Test
	public void insertDocumentAsJson() throws InterruptedException, ExecutionException {
		final CompletableFuture<DocumentCreateEntity<String>> f = db.collection(COLLECTION_NAME)
				.insertDocument("{\"_key\":\"docRaw\",\"a\":\"test\"}", null);
		assertThat(f, is(notNullValue()));
		final DocumentCreateEntity<String> doc = f.get();
		assertThat(doc, is(notNullValue()));
		assertThat(doc.getId(), is(notNullValue()));
		assertThat(doc.getKey(), is(notNullValue()));
		assertThat(doc.getRev(), is(notNullValue()));
	}

	@Test
	public void getDocument() throws InterruptedException, ExecutionException {
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.insertDocument(new BaseDocument(), null).get();
		assertThat(createResult.getKey(), is(notNullValue()));
		final CompletableFuture<BaseDocument> f = db.collection(COLLECTION_NAME).getDocument(createResult.getKey(),
			BaseDocument.class, null);
		assertThat(f, is(notNullValue()));
		final BaseDocument readResult = f.get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getId(), is(COLLECTION_NAME + "/" + createResult.getKey()));
	}

	@Test
	public void getDocumentIfMatch() throws InterruptedException, ExecutionException {
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.insertDocument(new BaseDocument(), null).get();
		assertThat(createResult.getKey(), is(notNullValue()));
		final DocumentReadOptions options = new DocumentReadOptions().ifMatch(createResult.getRev());
		final CompletableFuture<BaseDocument> f = db.collection(COLLECTION_NAME).getDocument(createResult.getKey(),
			BaseDocument.class, options);
		assertThat(f, is(notNullValue()));
		final BaseDocument readResult = f.get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getId(), is(COLLECTION_NAME + "/" + createResult.getKey()));
	}

	@Test
	public void getDocumentIfMatchFail() throws InterruptedException, ExecutionException {
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.insertDocument(new BaseDocument(), null).get();
		assertThat(createResult.getKey(), is(notNullValue()));
		final DocumentReadOptions options = new DocumentReadOptions().ifMatch("no");
		final CompletableFuture<BaseDocument> f = db.collection(COLLECTION_NAME).getDocument(createResult.getKey(),
			BaseDocument.class, options);
		assertThat(f, is(notNullValue()));
		final BaseDocument doc = f.get();
		assertThat(doc, is(nullValue()));
	}

	@Test
	public void getDocumentIfNoneMatch() throws InterruptedException, ExecutionException {
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.insertDocument(new BaseDocument(), null).get();
		assertThat(createResult.getKey(), is(notNullValue()));
		final DocumentReadOptions options = new DocumentReadOptions().ifNoneMatch("no");
		final CompletableFuture<BaseDocument> f = db.collection(COLLECTION_NAME).getDocument(createResult.getKey(),
			BaseDocument.class, options);
		assertThat(f, is(notNullValue()));
		final BaseDocument readResult = f.get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getId(), is(COLLECTION_NAME + "/" + createResult.getKey()));
	}

	@Test
	public void getDocumentIfNoneMatchFail() throws InterruptedException, ExecutionException {
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME)
				.insertDocument(new BaseDocument(), null).get();
		assertThat(createResult.getKey(), is(notNullValue()));
		final DocumentReadOptions options = new DocumentReadOptions().ifNoneMatch(createResult.getRev());
		final CompletableFuture<BaseDocument> f = db.collection(COLLECTION_NAME).getDocument(createResult.getKey(),
			BaseDocument.class, options);
		assertThat(f, is(notNullValue()));
		final BaseDocument doc = f.get();
		assertThat(doc, is(nullValue()));
	}

	@Test
	public void getDocumentAsJson() throws InterruptedException, ExecutionException {
		db.collection(COLLECTION_NAME).insertDocument("{\"_key\":\"docRaw\",\"a\":\"test\"}", null).get();
		final CompletableFuture<String> f = db.collection(COLLECTION_NAME).getDocument("docRaw", String.class, null);
		assertThat(f, is(notNullValue()));
		final String readResult = f.get();
		assertThat(readResult.contains("\"_key\":\"docRaw\""), is(true));
		assertThat(readResult.contains("\"_id\":\"db_collection_test\\/docRaw\""), is(true));
	}

	@Test
	public void getDocumentNotFound() throws InterruptedException, ExecutionException {
		final CompletableFuture<BaseDocument> f = db.collection(COLLECTION_NAME).getDocument("no", BaseDocument.class);
		assertThat(f, is(notNullValue()));
		final BaseDocument doc = f.get();
		assertThat(doc, is(nullValue()));
	}

	@Test(expected = ArangoDBException.class)
	public void getDocumentWrongKey() throws InterruptedException, ExecutionException {
		db.collection(COLLECTION_NAME).getDocument("no/no", BaseDocument.class).get();
	}

	@Test
	public void updateDocument() throws ArangoDBException, InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		doc.addAttribute("c", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		doc.updateAttribute("c", null);
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, null);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> updateResult = f.get();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getNew(), is(nullValue()));
		assertThat(updateResult.getOld(), is(nullValue()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.getDocument(createResult.getKey(), BaseDocument.class, null).get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getAttribute("a"), is(notNullValue()));
		assertThat(String.valueOf(readResult.getAttribute("a")), is("test1"));
		assertThat(readResult.getAttribute("b"), is(notNullValue()));
		assertThat(String.valueOf(readResult.getAttribute("b")), is("test"));
		assertThat(readResult.getRevision(), is(updateResult.getRev()));
		assertThat(readResult.getProperties().keySet(), hasItem("c"));
	}

	@Test
	public void updateDocumentIfMatch() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		doc.addAttribute("c", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		doc.updateAttribute("c", null);
		final DocumentUpdateOptions options = new DocumentUpdateOptions().ifMatch(createResult.getRev());
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> updateResult = f.get();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.getDocument(createResult.getKey(), BaseDocument.class, null).get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getAttribute("a"), is(notNullValue()));
		assertThat(String.valueOf(readResult.getAttribute("a")), is("test1"));
		assertThat(readResult.getAttribute("b"), is(notNullValue()));
		assertThat(String.valueOf(readResult.getAttribute("b")), is("test"));
		assertThat(readResult.getRevision(), is(updateResult.getRev()));
		assertThat(readResult.getProperties().keySet(), hasItem("c"));
	}

	@Test
	public void updateDocumentIfMatchFail() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		doc.addAttribute("c", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		doc.updateAttribute("c", null);
		try {
			final DocumentUpdateOptions options = new DocumentUpdateOptions().ifMatch("no");
			db.collection(COLLECTION_NAME).updateDocument(createResult.getKey(), doc, options).get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void updateDocumentReturnNew() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		final DocumentUpdateOptions options = new DocumentUpdateOptions().returnNew(true);
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> updateResult = f.get();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));
		assertThat(updateResult.getNew(), is(notNullValue()));
		assertThat(updateResult.getNew().getKey(), is(createResult.getKey()));
		assertThat(updateResult.getNew().getRevision(), is(not(createResult.getRev())));
		assertThat(updateResult.getNew().getAttribute("a"), is(notNullValue()));
		assertThat(String.valueOf(updateResult.getNew().getAttribute("a")), is("test1"));
		assertThat(updateResult.getNew().getAttribute("b"), is(notNullValue()));
		assertThat(String.valueOf(updateResult.getNew().getAttribute("b")), is("test"));
	}

	@Test
	public void updateDocumentReturnOld() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.updateAttribute("a", "test1");
		doc.addAttribute("b", "test");
		final DocumentUpdateOptions options = new DocumentUpdateOptions().returnOld(true);
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> updateResult = f.get();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));
		assertThat(updateResult.getOld(), is(notNullValue()));
		assertThat(updateResult.getOld().getKey(), is(createResult.getKey()));
		assertThat(updateResult.getOld().getRevision(), is(createResult.getRev()));
		assertThat(updateResult.getOld().getAttribute("a"), is(notNullValue()));
		assertThat(String.valueOf(updateResult.getOld().getAttribute("a")), is("test"));
		assertThat(updateResult.getOld().getProperties().keySet(), not(hasItem("b")));
	}

	@Test
	public void updateDocumentKeepNullTrue() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.updateAttribute("a", null);
		final DocumentUpdateOptions options = new DocumentUpdateOptions().keepNull(true);
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> updateResult = f.get();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.getDocument(createResult.getKey(), BaseDocument.class, null).get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getProperties().keySet(), hasItem("a"));
	}

	@Test
	public void updateDocumentKeepNullFalse() throws ArangoDBException, InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.updateAttribute("a", null);
		final DocumentUpdateOptions options = new DocumentUpdateOptions().keepNull(false);
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> updateResult = f.get();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.getDocument(createResult.getKey(), BaseDocument.class, null).get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getId(), is(createResult.getId()));
		assertThat(readResult.getRevision(), is(notNullValue()));
		assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void updateDocumentMergeObjectsTrue() throws ArangoDBException, InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		final Map<String, String> a = new HashMap<>();
		a.put("a", "test");
		doc.addAttribute("a", a);
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		a.clear();
		a.put("b", "test");
		doc.updateAttribute("a", a);
		final DocumentUpdateOptions options = new DocumentUpdateOptions().mergeObjects(true);
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> updateResult = f.get();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.getDocument(createResult.getKey(), BaseDocument.class, null).get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		final Object aResult = readResult.getAttribute("a");
		assertThat(aResult, instanceOf(Map.class));
		final Map<String, String> aMap = (Map<String, String>) aResult;
		assertThat(aMap.keySet(), hasItem("a"));
		assertThat(aMap.keySet(), hasItem("b"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void updateDocumentMergeObjectsFalse() throws ArangoDBException, InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		final Map<String, String> a = new HashMap<>();
		a.put("a", "test");
		doc.addAttribute("a", a);
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		a.clear();
		a.put("b", "test");
		doc.updateAttribute("a", a);
		final DocumentUpdateOptions options = new DocumentUpdateOptions().mergeObjects(false);
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.updateDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> updateResult = f.get();
		assertThat(updateResult, is(notNullValue()));
		assertThat(updateResult.getId(), is(createResult.getId()));
		assertThat(updateResult.getRev(), is(not(updateResult.getOldRev())));
		assertThat(updateResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.getDocument(createResult.getKey(), BaseDocument.class, null).get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		final Object aResult = readResult.getAttribute("a");
		assertThat(aResult, instanceOf(Map.class));
		final Map<String, String> aMap = (Map<String, String>) aResult;
		assertThat(aMap.keySet(), not(hasItem("a")));
		assertThat(aMap.keySet(), hasItem("b"));
	}

	@Test
	public void updateDocumentIgnoreRevsFalse() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.updateAttribute("a", "test1");
		doc.setRevision("no");
		try {
			final DocumentUpdateOptions options = new DocumentUpdateOptions().ignoreRevs(false);
			db.collection(COLLECTION_NAME).updateDocument(createResult.getKey(), doc, options).get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void replaceDocument() throws ArangoDBException, InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.replaceDocument(createResult.getKey(), doc, null);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> replaceResult = f.get();
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getNew(), is(nullValue()));
		assertThat(replaceResult.getOld(), is(nullValue()));
		assertThat(replaceResult.getRev(), is(not(replaceResult.getOldRev())));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.getDocument(createResult.getKey(), BaseDocument.class, null).get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getRevision(), is(replaceResult.getRev()));
		assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
		assertThat(readResult.getAttribute("b"), is(notNullValue()));
		assertThat(String.valueOf(readResult.getAttribute("b")), is("test"));
	}

	@Test
	public void replaceDocumentIfMatch() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final DocumentReplaceOptions options = new DocumentReplaceOptions().ifMatch(createResult.getRev());
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.replaceDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> replaceResult = f.get();
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getRev(), is(not(replaceResult.getOldRev())));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));

		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.getDocument(createResult.getKey(), BaseDocument.class, null).get();
		assertThat(readResult.getKey(), is(createResult.getKey()));
		assertThat(readResult.getRevision(), is(replaceResult.getRev()));
		assertThat(readResult.getProperties().keySet(), not(hasItem("a")));
		assertThat(readResult.getAttribute("b"), is(notNullValue()));
		assertThat(String.valueOf(readResult.getAttribute("b")), is("test"));
	}

	@Test
	public void replaceDocumentIfMatchFail() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		try {
			final DocumentReplaceOptions options = new DocumentReplaceOptions().ifMatch("no");
			db.collection(COLLECTION_NAME).replaceDocument(createResult.getKey(), doc, options).get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void replaceDocumentIgnoreRevsFalse() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		doc.setRevision("no");
		try {
			final DocumentReplaceOptions options = new DocumentReplaceOptions().ignoreRevs(false);
			db.collection(COLLECTION_NAME).replaceDocument(createResult.getKey(), doc, options).get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void replaceDocumentReturnNew() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final DocumentReplaceOptions options = new DocumentReplaceOptions().returnNew(true);
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.replaceDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> replaceResult = f.get();
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));
		assertThat(replaceResult.getNew(), is(notNullValue()));
		assertThat(replaceResult.getNew().getKey(), is(createResult.getKey()));
		assertThat(replaceResult.getNew().getRevision(), is(not(createResult.getRev())));
		assertThat(replaceResult.getNew().getProperties().keySet(), not(hasItem("a")));
		assertThat(replaceResult.getNew().getAttribute("b"), is(notNullValue()));
		assertThat(String.valueOf(replaceResult.getNew().getAttribute("b")), is("test"));
	}

	@Test
	public void replaceDocumentReturnOld() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		doc.getProperties().clear();
		doc.addAttribute("b", "test");
		final DocumentReplaceOptions options = new DocumentReplaceOptions().returnOld(true);
		final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.replaceDocument(createResult.getKey(), doc, options);
		assertThat(f, is(notNullValue()));
		final DocumentUpdateEntity<BaseDocument> replaceResult = f.get();
		assertThat(replaceResult, is(notNullValue()));
		assertThat(replaceResult.getId(), is(createResult.getId()));
		assertThat(replaceResult.getOldRev(), is(createResult.getRev()));
		assertThat(replaceResult.getOld(), is(notNullValue()));
		assertThat(replaceResult.getOld().getKey(), is(createResult.getKey()));
		assertThat(replaceResult.getOld().getRevision(), is(createResult.getRev()));
		assertThat(replaceResult.getOld().getAttribute("a"), is(notNullValue()));
		assertThat(String.valueOf(replaceResult.getOld().getAttribute("a")), is("test"));
		assertThat(replaceResult.getOld().getProperties().keySet(), not(hasItem("b")));
	}

	@Test
	public void deleteDocument() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		db.collection(COLLECTION_NAME).deleteDocument(createResult.getKey(), null, null);
		final CompletableFuture<BaseDocument> f = db.collection(COLLECTION_NAME).getDocument(createResult.getKey(),
			BaseDocument.class, null);
		assertThat(f, is(notNullValue()));
		final BaseDocument document = f.get();
		assertThat(document, is(nullValue()));
	}

	@Test
	public void deleteDocumentReturnOld() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		doc.addAttribute("a", "test");
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		final DocumentDeleteOptions options = new DocumentDeleteOptions().returnOld(true);
		final CompletableFuture<DocumentDeleteEntity<BaseDocument>> f = db.collection(COLLECTION_NAME)
				.deleteDocument(createResult.getKey(), BaseDocument.class, options);
		assertThat(f, is(notNullValue()));
		final DocumentDeleteEntity<BaseDocument> deleteResult = f.get();
		assertThat(deleteResult.getOld(), is(notNullValue()));
		assertThat(deleteResult.getOld(), instanceOf(BaseDocument.class));
		assertThat(deleteResult.getOld().getAttribute("a"), is(notNullValue()));
		assertThat(String.valueOf(deleteResult.getOld().getAttribute("a")), is("test"));
	}

	@Test
	public void deleteDocumentIfMatch() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		final DocumentDeleteOptions options = new DocumentDeleteOptions().ifMatch(createResult.getRev());
		db.collection(COLLECTION_NAME).deleteDocument(createResult.getKey(), null, options);
		final CompletableFuture<BaseDocument> f = db.collection(COLLECTION_NAME).getDocument(createResult.getKey(),
			BaseDocument.class, null);
		assertThat(f, is(notNullValue()));
		final BaseDocument document = f.get();
		assertThat(document, is(nullValue()));
	}

	@Test
	public void deleteDocumentIfMatchFail() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		final DocumentCreateEntity<BaseDocument> createResult = db.collection(COLLECTION_NAME).insertDocument(doc, null)
				.get();
		final DocumentDeleteOptions options = new DocumentDeleteOptions().ifMatch("no");
		try {
			db.collection(COLLECTION_NAME).deleteDocument(createResult.getKey(), null, options).get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void getIndex() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		final IndexEntity createResult = db.collection(COLLECTION_NAME).createHashIndex(fields, null).get();
		final CompletableFuture<IndexEntity> f = db.collection(COLLECTION_NAME).getIndex(createResult.getId());
		assertThat(f, is(notNullValue()));
		final IndexEntity readResult = f.get();
		assertThat(readResult.getId(), is(createResult.getId()));
		assertThat(readResult.getType(), is(createResult.getType()));
	}

	@Test
	public void getIndexByKey() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		final IndexEntity createResult = db.collection(COLLECTION_NAME).createHashIndex(fields, null).get();
		final CompletableFuture<IndexEntity> f = db.collection(COLLECTION_NAME)
				.getIndex(createResult.getId().split("/")[1]);
		assertThat(f, is(notNullValue()));
		final IndexEntity readResult = f.get();
		assertThat(readResult.getId(), is(createResult.getId()));
		assertThat(readResult.getType(), is(createResult.getType()));
	}

	@Test
	public void deleteIndex() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		final IndexEntity createResult = db.collection(COLLECTION_NAME).createHashIndex(fields, null).get();
		final CompletableFuture<String> f = db.collection(COLLECTION_NAME).deleteIndex(createResult.getId());
		assertThat(f, is(notNullValue()));
		final String id = f.get();
		assertThat(id, is(createResult.getId()));
		try {
			db.getIndex(id).get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void deleteIndexByKey() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		final IndexEntity createResult = db.collection(COLLECTION_NAME).createHashIndex(fields, null).get();
		final CompletableFuture<String> f = db.collection(COLLECTION_NAME)
				.deleteIndex(createResult.getId().split("/")[1]);
		assertThat(f, is(notNullValue()));
		final String id = f.get();
		assertThat(id, is(createResult.getId()));
		try {
			db.getIndex(id).get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void createHashIndex() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		fields.add("b");
		final CompletableFuture<IndexEntity> f = db.collection(COLLECTION_NAME).createHashIndex(fields, null);
		assertThat(f, is(notNullValue()));
		final IndexEntity indexResult = f.get();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint(), is(nullValue()));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getFields(), hasItem("b"));
		assertThat(indexResult.getGeoJson(), is(nullValue()));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated(), is(true));
		assertThat(indexResult.getMinLength(), is(nullValue()));
		assertThat(indexResult.getSelectivityEstimate(), is(1));
		assertThat(indexResult.getSparse(), is(false));
		assertThat(indexResult.getType(), is(IndexType.hash));
		assertThat(indexResult.getUnique(), is(false));
	}

	@Test
	public void createGeoIndex() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		final CompletableFuture<IndexEntity> f = db.collection(COLLECTION_NAME).createGeoIndex(fields, null);
		assertThat(f, is(notNullValue()));
		final IndexEntity indexResult = f.get();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint(), is(false));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getGeoJson(), is(false));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated(), is(true));
		assertThat(indexResult.getMinLength(), is(nullValue()));
		assertThat(indexResult.getSelectivityEstimate(), is(nullValue()));
		assertThat(indexResult.getSparse(), is(true));
		assertThat(indexResult.getType(), is(IndexType.geo1));
		assertThat(indexResult.getUnique(), is(false));
	}

	@Test
	public void createGeo2Index() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		fields.add("b");
		final CompletableFuture<IndexEntity> f = db.collection(COLLECTION_NAME).createGeoIndex(fields, null);
		assertThat(f, is(notNullValue()));
		final IndexEntity indexResult = f.get();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint(), is(false));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getFields(), hasItem("b"));
		assertThat(indexResult.getGeoJson(), is(nullValue()));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated(), is(true));
		assertThat(indexResult.getMinLength(), is(nullValue()));
		assertThat(indexResult.getSelectivityEstimate(), is(nullValue()));
		assertThat(indexResult.getSparse(), is(true));
		assertThat(indexResult.getType(), is(IndexType.geo2));
		assertThat(indexResult.getUnique(), is(false));
	}

	@Test
	public void createSkiplistIndex() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		fields.add("b");
		final CompletableFuture<IndexEntity> f = db.collection(COLLECTION_NAME).createSkiplistIndex(fields, null);
		assertThat(f, is(notNullValue()));
		final IndexEntity indexResult = f.get();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint(), is(nullValue()));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getFields(), hasItem("b"));
		assertThat(indexResult.getGeoJson(), is(nullValue()));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated(), is(true));
		assertThat(indexResult.getMinLength(), is(nullValue()));
		assertThat(indexResult.getSparse(), is(false));
		assertThat(indexResult.getType(), is(IndexType.skiplist));
		assertThat(indexResult.getUnique(), is(false));
	}

	@Test
	public void createPersistentIndex() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		fields.add("b");
		final CompletableFuture<IndexEntity> f = db.collection(COLLECTION_NAME).createPersistentIndex(fields, null);
		assertThat(f, is(notNullValue()));
		final IndexEntity indexResult = f.get();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint(), is(nullValue()));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getFields(), hasItem("b"));
		assertThat(indexResult.getGeoJson(), is(nullValue()));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated(), is(true));
		assertThat(indexResult.getMinLength(), is(nullValue()));
		assertThat(indexResult.getSparse(), is(false));
		assertThat(indexResult.getType(), is(IndexType.persistent));
		assertThat(indexResult.getUnique(), is(false));
	}

	@Test
	public void createFulltextIndex() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		final CompletableFuture<IndexEntity> f = db.collection(COLLECTION_NAME).createFulltextIndex(fields, null);
		assertThat(f, is(notNullValue()));
		final IndexEntity indexResult = f.get();
		assertThat(indexResult, is(notNullValue()));
		assertThat(indexResult.getConstraint(), is(nullValue()));
		assertThat(indexResult.getFields(), hasItem("a"));
		assertThat(indexResult.getGeoJson(), is(nullValue()));
		assertThat(indexResult.getId(), startsWith(COLLECTION_NAME));
		assertThat(indexResult.getIsNewlyCreated(), is(true));
		assertThat(indexResult.getSelectivityEstimate(), is(nullValue()));
		assertThat(indexResult.getSparse(), is(true));
		assertThat(indexResult.getType(), is(IndexType.fulltext));
		assertThat(indexResult.getUnique(), is(false));
	}

	@Test
	public void getIndexes() throws InterruptedException, ExecutionException {
		final Collection<String> fields = new ArrayList<>();
		fields.add("a");
		db.collection(COLLECTION_NAME).createHashIndex(fields, null);
		final CompletableFuture<Collection<IndexEntity>> f = db.collection(COLLECTION_NAME).getIndexes();
		assertThat(f, is(notNullValue()));
		final Collection<IndexEntity> indexes = f.get();
		assertThat(indexes, is(notNullValue()));
		assertThat(indexes.size(), is(2));
		for (final IndexEntity i : indexes) {
			assertThat(i.getType(), anyOf(is(IndexType.primary), is(IndexType.hash)));
			if (i.getType() == IndexType.hash) {
				assertThat(i.getFields().size(), is(1));
				assertThat(i.getFields(), hasItem("a"));
			}
		}
	}

	@Test
	public void truncate() throws InterruptedException, ExecutionException {
		final BaseDocument doc = new BaseDocument();
		db.collection(COLLECTION_NAME).insertDocument(doc, null).get();
		final BaseDocument readResult = db.collection(COLLECTION_NAME)
				.getDocument(doc.getKey(), BaseDocument.class, null).get();
		assertThat(readResult.getKey(), is(doc.getKey()));
		final CompletableFuture<CollectionEntity> f = db.collection(COLLECTION_NAME).truncate();
		assertThat(f, is(notNullValue()));
		final CollectionEntity truncateResult = f.get();
		assertThat(truncateResult, is(notNullValue()));
		assertThat(truncateResult.getId(), is(notNullValue()));
		final BaseDocument document = db.collection(COLLECTION_NAME).getDocument(doc.getKey(), BaseDocument.class, null)
				.get();
		assertThat(document, is(nullValue()));
	}

	@Test
	public void getCount() throws InterruptedException, ExecutionException {
		{
			final CompletableFuture<CollectionPropertiesEntity> f = db.collection(COLLECTION_NAME).count();
			assertThat(f, is(notNullValue()));
			final CollectionPropertiesEntity countEmpty = f.get();
			assertThat(countEmpty, is(notNullValue()));
			assertThat(countEmpty.getCount(), is(0L));
		}
		db.collection(COLLECTION_NAME).insertDocument("{}", null).get();
		{
			final CompletableFuture<CollectionPropertiesEntity> f = db.collection(COLLECTION_NAME).count();
			assertThat(f, is(notNullValue()));
			final CollectionPropertiesEntity count = f.get();
			assertThat(count.getCount(), is(1L));

		}
	}

	@Test
	public void documentExists() throws InterruptedException, ExecutionException {
		{
			final CompletableFuture<Boolean> f = db.collection(COLLECTION_NAME).documentExists("no", null);
			assertThat(f, is(notNullValue()));
			final Boolean existsNot = f.get();
			assertThat(existsNot, is(false));
		}
		db.collection(COLLECTION_NAME).insertDocument("{\"_key\":\"abc\"}", null).get();
		{
			final CompletableFuture<Boolean> f = db.collection(COLLECTION_NAME).documentExists("abc", null);
			assertThat(f, is(notNullValue()));
			final Boolean exists = f.get();
			assertThat(exists, is(true));
		}
	}

	@Test
	public void documentExistsIfMatch() throws InterruptedException, ExecutionException {
		final DocumentCreateEntity<String> createResult = db.collection(COLLECTION_NAME)
				.insertDocument("{\"_key\":\"abc\"}", null).get();
		final DocumentExistsOptions options = new DocumentExistsOptions().ifMatch(createResult.getRev());
		final CompletableFuture<Boolean> f = db.collection(COLLECTION_NAME).documentExists("abc", options);
		assertThat(f, is(notNullValue()));
		final Boolean exists = f.get();
		assertThat(exists, is(true));
	}

	@Test
	public void documentExistsIfMatchFail() throws InterruptedException, ExecutionException {
		db.collection(COLLECTION_NAME).insertDocument("{\"_key\":\"abc\"}", null).get();
		final DocumentExistsOptions options = new DocumentExistsOptions().ifMatch("no");
		final CompletableFuture<Boolean> f = db.collection(COLLECTION_NAME).documentExists("abc", options);
		assertThat(f, is(notNullValue()));
		final Boolean exists = f.get();
		assertThat(exists, is(false));
	}

	@Test
	public void documentExistsIfNoneMatch() throws InterruptedException, ExecutionException {
		db.collection(COLLECTION_NAME).insertDocument("{\"_key\":\"abc\"}", null).get();
		final DocumentExistsOptions options = new DocumentExistsOptions().ifNoneMatch("no");
		final CompletableFuture<Boolean> f = db.collection(COLLECTION_NAME).documentExists("abc", options);
		assertThat(f, is(notNullValue()));
		final Boolean exists = f.get();
		assertThat(exists, is(true));
	}

	@Test
	public void documentExistsIfNoneMatchFail() throws InterruptedException, ExecutionException {
		final DocumentCreateEntity<String> createResult = db.collection(COLLECTION_NAME)
				.insertDocument("{\"_key\":\"abc\"}", null).get();
		final DocumentExistsOptions options = new DocumentExistsOptions().ifNoneMatch(createResult.getRev());
		final CompletableFuture<Boolean> f = db.collection(COLLECTION_NAME).documentExists("abc", options);
		assertThat(f, is(notNullValue()));
		final Boolean exists = f.get();
		assertThat(exists, is(false));
	}

	@Test
	public void insertDocuments() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument());
		values.add(new BaseDocument());
		values.add(new BaseDocument());
		final CompletableFuture<MultiDocumentEntity<DocumentCreateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).insertDocuments(values, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentCreateEntity<BaseDocument>> docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getDocuments(), is(notNullValue()));
		assertThat(docs.getDocuments().size(), is(3));
		assertThat(docs.getErrors(), is(notNullValue()));
		assertThat(docs.getErrors().size(), is(0));
	}

	@Test
	public void insertDocumentsOne() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument());
		final CompletableFuture<MultiDocumentEntity<DocumentCreateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).insertDocuments(values, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentCreateEntity<BaseDocument>> docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getDocuments(), is(notNullValue()));
		assertThat(docs.getDocuments().size(), is(1));
		assertThat(docs.getErrors(), is(notNullValue()));
		assertThat(docs.getErrors().size(), is(0));
	}

	@Test
	public void insertDocumentsEmpty() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		final CompletableFuture<MultiDocumentEntity<DocumentCreateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).insertDocuments(values, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentCreateEntity<BaseDocument>> docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getDocuments(), is(notNullValue()));
		assertThat(docs.getDocuments().size(), is(0));
		assertThat(docs.getErrors(), is(notNullValue()));
		assertThat(docs.getErrors().size(), is(0));
	}

	@Test
	public void insertDocumentsReturnNew() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument());
		values.add(new BaseDocument());
		values.add(new BaseDocument());
		final DocumentCreateOptions options = new DocumentCreateOptions().returnNew(true);
		final CompletableFuture<MultiDocumentEntity<DocumentCreateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).insertDocuments(values, options);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentCreateEntity<BaseDocument>> docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getDocuments(), is(notNullValue()));
		assertThat(docs.getDocuments().size(), is(3));
		assertThat(docs.getErrors(), is(notNullValue()));
		assertThat(docs.getErrors().size(), is(0));
		for (final DocumentCreateEntity<BaseDocument> doc : docs.getDocuments()) {
			assertThat(doc.getNew(), is(notNullValue()));
			final BaseDocument baseDocument = doc.getNew();
			assertThat(baseDocument.getKey(), is(notNullValue()));
		}
	}

	@Test
	public void insertDocumentsFail() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument("1"));
		values.add(new BaseDocument("2"));
		values.add(new BaseDocument("2"));
		final CompletableFuture<MultiDocumentEntity<DocumentCreateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).insertDocuments(values);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentCreateEntity<BaseDocument>> docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getDocuments(), is(notNullValue()));
		assertThat(docs.getDocuments().size(), is(2));
		assertThat(docs.getErrors(), is(notNullValue()));
		assertThat(docs.getErrors().size(), is(1));
		assertThat(docs.getErrors().iterator().next().getErrorNum(), is(1210));
	}

	@Test
	public void importDocuments() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument());
		values.add(new BaseDocument());
		values.add(new BaseDocument());
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values);
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(values.size()));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(0));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsDuplicateDefaultError() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument("1"));
		values.add(new BaseDocument("2"));
		values.add(new BaseDocument("2"));
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values);
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(1));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsDuplicateError() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument("1"));
		values.add(new BaseDocument("2"));
		values.add(new BaseDocument("2"));
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().onDuplicate(OnDuplicate.error));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(1));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsDuplicateIgnore() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument("1"));
		values.add(new BaseDocument("2"));
		values.add(new BaseDocument("2"));
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().onDuplicate(OnDuplicate.ignore));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(0));
		assertThat(docs.getIgnored(), is(1));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsDuplicateReplace() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument("1"));
		values.add(new BaseDocument("2"));
		values.add(new BaseDocument("2"));
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().onDuplicate(OnDuplicate.replace));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(0));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(1));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsDuplicateUpdate() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument("1"));
		values.add(new BaseDocument("2"));
		values.add(new BaseDocument("2"));
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().onDuplicate(OnDuplicate.update));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(0));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(1));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsCompleteFail() {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument("1"));
		values.add(new BaseDocument("2"));
		values.add(new BaseDocument("2"));
		try {
			db.collection(COLLECTION_NAME).importDocuments(values, new DocumentImportOptions().complete(true)).get();
			fail();
		} catch (InterruptedException | ExecutionException e) {
			assertThat(e.getMessage(), containsString("1210"));
		}
	}

	@Test
	public void importDocumentsDetails() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument("1"));
		values.add(new BaseDocument("2"));
		values.add(new BaseDocument("2"));
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().details(true));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(1));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails().size(), is(1));
		assertThat(docs.getDetails().iterator().next(), containsString("unique constraint violated"));
	}

	@Test
	public void importDocumentsOverwriteFalse() throws InterruptedException, ExecutionException {
		final ArangoCollectionAsync collection = db.collection(COLLECTION_NAME);
		collection.insertDocument(new BaseDocument()).get();
		assertThat(collection.count().get().getCount(), is(1L));

		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument());
		values.add(new BaseDocument());
		collection.importDocuments(values, new DocumentImportOptions().overwrite(false)).get();
		assertThat(collection.count().get().getCount(), is(3L));
	}

	@Test
	public void importDocumentsOverwriteTrue() throws InterruptedException, ExecutionException {
		final ArangoCollectionAsync collection = db.collection(COLLECTION_NAME);
		collection.insertDocument(new BaseDocument()).get();
		assertThat(collection.count().get().getCount(), is(1L));

		final Collection<BaseDocument> values = new ArrayList<>();
		values.add(new BaseDocument());
		values.add(new BaseDocument());
		collection.importDocuments(values, new DocumentImportOptions().overwrite(true)).get();
		assertThat(collection.count().get().getCount(), is(2L));
	}

	@Test
	public void importDocumentsFromToPrefix() throws InterruptedException, ExecutionException {
		db.createCollection(COLLECTION_NAME + "_edge", new CollectionCreateOptions().type(CollectionType.EDGES)).get();
		final ArangoCollectionAsync collection = db.collection(COLLECTION_NAME + "_edge");
		try {
			final Collection<BaseEdgeDocument> values = new ArrayList<>();
			final String[] keys = { "1", "2" };
			for (int i = 0; i < keys.length; i++) {
				values.add(new BaseEdgeDocument(keys[i], "from", "to"));
			}
			assertThat(values.size(), is(keys.length));

			final DocumentImportEntity importResult = collection
					.importDocuments(values, new DocumentImportOptions().fromPrefix("foo").toPrefix("bar")).get();
			assertThat(importResult, is(notNullValue()));
			assertThat(importResult.getCreated(), is(values.size()));
			for (int i = 0; i < keys.length; i++) {
				BaseEdgeDocument doc;
				try {
					doc = collection.getDocument(keys[i], BaseEdgeDocument.class).get();
					assertThat(doc, is(notNullValue()));
					assertThat(doc.getFrom(), is("foo/from"));
					assertThat(doc.getTo(), is("bar/to"));
				} catch (ArangoDBException | InterruptedException | ExecutionException e) {
					fail();
				}
			}
		} finally {
			collection.drop().get();
		}
	}

	@Test
	public void importDocumentsJson() throws InterruptedException, ExecutionException {
		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"}]";
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values);
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(0));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsJsonDuplicateDefaultError() throws InterruptedException, ExecutionException {
		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"},{\"_key\":\"2\"}]";
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values);
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(1));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsJsonDuplicateError() throws InterruptedException, ExecutionException {
		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"},{\"_key\":\"2\"}]";
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().onDuplicate(OnDuplicate.error));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(1));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsJsonDuplicateIgnore() throws InterruptedException, ExecutionException {
		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"},{\"_key\":\"2\"}]";
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().onDuplicate(OnDuplicate.ignore));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(0));
		assertThat(docs.getIgnored(), is(1));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsJsonDuplicateReplace() throws InterruptedException, ExecutionException {
		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"},{\"_key\":\"2\"}]";
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().onDuplicate(OnDuplicate.replace));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(0));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(1));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsJsonDuplicateUpdate() throws InterruptedException, ExecutionException {
		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"},{\"_key\":\"2\"}]";
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().onDuplicate(OnDuplicate.update));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(0));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(1));
		assertThat(docs.getDetails(), is(empty()));
	}

	@Test
	public void importDocumentsJsonCompleteFail() {
		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"},{\"_key\":\"2\"}]";
		try {
			db.collection(COLLECTION_NAME).importDocuments(values, new DocumentImportOptions().complete(true)).get();
			fail();
		} catch (InterruptedException | ExecutionException e) {
			assertThat(e.getMessage(), containsString("1210"));
		}
	}

	@Test
	public void importDocumentsJsonDetails() throws InterruptedException, ExecutionException {
		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"},{\"_key\":\"2\"}]";
		final CompletableFuture<DocumentImportEntity> f = db.collection(COLLECTION_NAME).importDocuments(values,
			new DocumentImportOptions().details(true));
		assertThat(f, is(notNullValue()));
		final DocumentImportEntity docs = f.get();
		assertThat(docs, is(notNullValue()));
		assertThat(docs.getCreated(), is(2));
		assertThat(docs.getEmpty(), is(0));
		assertThat(docs.getErrors(), is(1));
		assertThat(docs.getIgnored(), is(0));
		assertThat(docs.getUpdated(), is(0));
		assertThat(docs.getDetails().size(), is(1));
		assertThat(docs.getDetails().iterator().next(), containsString("unique constraint violated"));
	}

	@Test
	public void importDocumentsJsonOverwriteFalse() throws InterruptedException, ExecutionException {
		final ArangoCollectionAsync collection = db.collection(COLLECTION_NAME);
		collection.insertDocument(new BaseDocument()).get();
		assertThat(collection.count().get().getCount(), is(1L));

		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"}]";
		collection.importDocuments(values, new DocumentImportOptions().overwrite(false)).get();
		assertThat(collection.count().get().getCount(), is(3L));
	}

	@Test
	public void importDocumentsJsonOverwriteTrue() throws InterruptedException, ExecutionException {
		final ArangoCollectionAsync collection = db.collection(COLLECTION_NAME);
		collection.insertDocument(new BaseDocument()).get();
		assertThat(collection.count().get().getCount(), is(1L));

		final String values = "[{\"_key\":\"1\"},{\"_key\":\"2\"}]";
		collection.importDocuments(values, new DocumentImportOptions().overwrite(true)).get();
		assertThat(collection.count().get().getCount(), is(2L));
	}

	@Test
	public void importDocumentsJsonFromToPrefix() throws InterruptedException, ExecutionException {
		db.createCollection(COLLECTION_NAME + "_edge", new CollectionCreateOptions().type(CollectionType.EDGES)).get();
		final ArangoCollectionAsync collection = db.collection(COLLECTION_NAME + "_edge");
		try {
			final String[] keys = { "1", "2" };
			final String values = "[{\"_key\":\"1\",\"_from\":\"from\",\"_to\":\"to\"},{\"_key\":\"2\",\"_from\":\"from\",\"_to\":\"to\"}]";

			final DocumentImportEntity importResult = collection
					.importDocuments(values, new DocumentImportOptions().fromPrefix("foo").toPrefix("bar")).get();
			assertThat(importResult, is(notNullValue()));
			assertThat(importResult.getCreated(), is(2));
			for (int i = 0; i < keys.length; i++) {
				BaseEdgeDocument doc;
				try {
					doc = collection.getDocument(keys[i], BaseEdgeDocument.class).get();
					assertThat(doc, is(notNullValue()));
					assertThat(doc.getFrom(), is("foo/from"));
					assertThat(doc.getTo(), is("bar/to"));
				} catch (ArangoDBException | InterruptedException | ExecutionException e) {
					fail();
				}
			}
		} finally {
			collection.drop();
		}
	}

	@Test
	public void deleteDocumentsByKey() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("1");
			values.add(e);
		}
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("2");
			values.add(e);
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final Collection<String> keys = new ArrayList<>();
		keys.add("1");
		keys.add("2");
		final CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<Object>>> f = db.collection(COLLECTION_NAME)
				.deleteDocuments(keys, null, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentDeleteEntity<Object>> deleteResult = f.get();
		assertThat(deleteResult, is(notNullValue()));
		assertThat(deleteResult.getDocuments().size(), is(2));
		for (final DocumentDeleteEntity<Object> i : deleteResult.getDocuments()) {
			assertThat(i.getKey(), anyOf(is("1"), is("2")));
		}
		assertThat(deleteResult.getErrors().size(), is(0));
	}

	@Test
	public void deleteDocumentsByDocuments() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("1");
			values.add(e);
		}
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("2");
			values.add(e);
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<Object>>> f = db.collection(COLLECTION_NAME)
				.deleteDocuments(values, null, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentDeleteEntity<Object>> deleteResult = f.get();
		assertThat(deleteResult, is(notNullValue()));
		assertThat(deleteResult.getDocuments().size(), is(2));
		for (final DocumentDeleteEntity<Object> i : deleteResult.getDocuments()) {
			assertThat(i.getKey(), anyOf(is("1"), is("2")));
		}
		assertThat(deleteResult.getErrors().size(), is(0));
	}

	@Test
	public void deleteDocumentsByKeyOne() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("1");
			values.add(e);
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final Collection<String> keys = new ArrayList<>();
		keys.add("1");
		final CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<Object>>> f = db.collection(COLLECTION_NAME)
				.deleteDocuments(keys, null, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentDeleteEntity<Object>> deleteResult = f.get();
		assertThat(deleteResult, is(notNullValue()));
		assertThat(deleteResult.getDocuments().size(), is(1));
		for (final DocumentDeleteEntity<Object> i : deleteResult.getDocuments()) {
			assertThat(i.getKey(), is("1"));
		}
		assertThat(deleteResult.getErrors().size(), is(0));
	}

	@Test
	public void deleteDocumentsByDocumentOne() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("1");
			values.add(e);
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<Object>>> f = db.collection(COLLECTION_NAME)
				.deleteDocuments(values, null, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentDeleteEntity<Object>> deleteResult = f.get();
		assertThat(deleteResult, is(notNullValue()));
		assertThat(deleteResult.getDocuments().size(), is(1));
		for (final DocumentDeleteEntity<Object> i : deleteResult.getDocuments()) {
			assertThat(i.getKey(), is("1"));
		}
		assertThat(deleteResult.getErrors().size(), is(0));
	}

	@Test
	public void deleteDocumentsEmpty() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final Collection<String> keys = new ArrayList<>();
		final CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<Object>>> f = db.collection(COLLECTION_NAME)
				.deleteDocuments(keys, null, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentDeleteEntity<Object>> deleteResult = f.get();
		assertThat(deleteResult, is(notNullValue()));
		assertThat(deleteResult.getDocuments().size(), is(0));
		assertThat(deleteResult.getErrors().size(), is(0));
	}

	@Test
	public void deleteDocumentsByKeyNotExisting() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final Collection<String> keys = new ArrayList<>();
		keys.add("1");
		keys.add("2");
		final CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<Object>>> f = db.collection(COLLECTION_NAME)
				.deleteDocuments(keys, null, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentDeleteEntity<Object>> deleteResult = f.get();
		assertThat(deleteResult, is(notNullValue()));
		assertThat(deleteResult.getDocuments().size(), is(0));
		assertThat(deleteResult.getErrors().size(), is(2));
	}

	@Test
	public void deleteDocumentsByDocumentsNotExisting() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("1");
			values.add(e);
		}
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("2");
			values.add(e);
		}
		final CompletableFuture<MultiDocumentEntity<DocumentDeleteEntity<Object>>> f = db.collection(COLLECTION_NAME)
				.deleteDocuments(values, null, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentDeleteEntity<Object>> deleteResult = f.get();
		assertThat(deleteResult, is(notNullValue()));
		assertThat(deleteResult.getDocuments().size(), is(0));
		assertThat(deleteResult.getErrors().size(), is(2));
	}

	@Test
	public void updateDocuments() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("1");
			values.add(e);
		}
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("2");
			values.add(e);
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null);
		final Collection<BaseDocument> updatedValues = new ArrayList<>();
		for (final BaseDocument i : values) {
			i.addAttribute("a", "test");
			updatedValues.add(i);
		}
		final CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).updateDocuments(updatedValues, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>> updateResult = f.get();
		assertThat(updateResult.getDocuments().size(), is(2));
		assertThat(updateResult.getErrors().size(), is(0));
	}

	@Test
	public void updateDocumentsOne() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("1");
			values.add(e);
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final Collection<BaseDocument> updatedValues = new ArrayList<>();
		final BaseDocument first = values.iterator().next();
		first.addAttribute("a", "test");
		updatedValues.add(first);
		final CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).updateDocuments(updatedValues, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>> updateResult = f.get();
		assertThat(updateResult.getDocuments().size(), is(1));
		assertThat(updateResult.getErrors().size(), is(0));
	}

	@Test
	public void updateDocumentsEmpty() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		final CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).updateDocuments(values, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>> updateResult = f.get();
		assertThat(updateResult.getDocuments().size(), is(0));
		assertThat(updateResult.getErrors().size(), is(0));
	}

	@Test
	public void updateDocumentsWithoutKey() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			values.add(new BaseDocument("1"));
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final Collection<BaseDocument> updatedValues = new ArrayList<>();
		for (final BaseDocument i : values) {
			i.addAttribute("a", "test");
			updatedValues.add(i);
		}
		updatedValues.add(new BaseDocument());
		final CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).updateDocuments(updatedValues, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>> updateResult = f.get();
		assertThat(updateResult.getDocuments().size(), is(1));
		assertThat(updateResult.getErrors().size(), is(1));
	}

	@Test
	public void replaceDocuments() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			values.add(new BaseDocument("1"));
			values.add(new BaseDocument("2"));
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final Collection<BaseDocument> updatedValues = new ArrayList<>();
		for (final BaseDocument i : values) {
			i.addAttribute("a", "test");
			updatedValues.add(i);
		}
		final CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).replaceDocuments(updatedValues, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>> updateResult = f.get();
		assertThat(updateResult.getDocuments().size(), is(2));
		assertThat(updateResult.getErrors().size(), is(0));
	}

	@Test
	public void replaceDocumentsOne() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			final BaseDocument e = new BaseDocument();
			e.setKey("1");
			values.add(e);
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final Collection<BaseDocument> updatedValues = new ArrayList<>();
		final BaseDocument first = values.iterator().next();
		first.addAttribute("a", "test");
		updatedValues.add(first);
		final CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).updateDocuments(updatedValues, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>> updateResult = f.get();
		assertThat(updateResult.getDocuments().size(), is(1));
		assertThat(updateResult.getErrors().size(), is(0));
	}

	@Test
	public void replaceDocumentsEmpty() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		final CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).updateDocuments(values, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>> updateResult = f.get();
		assertThat(updateResult.getDocuments().size(), is(0));
		assertThat(updateResult.getErrors().size(), is(0));
	}

	@Test
	public void replaceDocumentsWithoutKey() throws InterruptedException, ExecutionException {
		final Collection<BaseDocument> values = new ArrayList<>();
		{
			values.add(new BaseDocument("1"));
		}
		db.collection(COLLECTION_NAME).insertDocuments(values, null).get();
		final Collection<BaseDocument> updatedValues = new ArrayList<>();
		for (final BaseDocument i : values) {
			i.addAttribute("a", "test");
			updatedValues.add(i);
		}
		updatedValues.add(new BaseDocument());
		final CompletableFuture<MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>>> f = db
				.collection(COLLECTION_NAME).updateDocuments(updatedValues, null);
		assertThat(f, is(notNullValue()));
		final MultiDocumentEntity<DocumentUpdateEntity<BaseDocument>> updateResult = f.get();
		assertThat(updateResult.getDocuments().size(), is(1));
		assertThat(updateResult.getErrors().size(), is(1));
	}

	@Test
	public void load() throws InterruptedException, ExecutionException {
		final CompletableFuture<CollectionEntity> f = db.collection(COLLECTION_NAME).load();
		assertThat(f, is(notNullValue()));
		final CollectionEntity result = f.get();
		assertThat(result.getName(), is(COLLECTION_NAME));
	}

	@Test
	public void unload() throws InterruptedException, ExecutionException {
		final CompletableFuture<CollectionEntity> f = db.collection(COLLECTION_NAME).unload();
		assertThat(f, is(notNullValue()));
		final CollectionEntity result = f.get();
		assertThat(result.getName(), is(COLLECTION_NAME));
	}

	@Test
	public void getInfo() throws InterruptedException, ExecutionException {
		final CompletableFuture<CollectionEntity> f = db.collection(COLLECTION_NAME).getInfo();
		assertThat(f, is(notNullValue()));
		final CollectionEntity result = f.get();
		assertThat(result.getName(), is(COLLECTION_NAME));
	}

	@Test
	public void getPropeties() throws InterruptedException, ExecutionException {
		final CompletableFuture<CollectionPropertiesEntity> f = db.collection(COLLECTION_NAME).getProperties();
		assertThat(f, is(notNullValue()));
		final CollectionPropertiesEntity result = f.get();
		assertThat(result.getName(), is(COLLECTION_NAME));
		assertThat(result.getCount(), is(nullValue()));
	}

	@Test
	public void changeProperties() throws InterruptedException, ExecutionException {
		final CollectionPropertiesEntity properties = db.collection(COLLECTION_NAME).getProperties().get();
		assertThat(properties.getWaitForSync(), is(notNullValue()));
		final CollectionPropertiesOptions options = new CollectionPropertiesOptions();
		options.waitForSync(!properties.getWaitForSync());
		final CompletableFuture<CollectionPropertiesEntity> f = db.collection(COLLECTION_NAME)
				.changeProperties(options);
		assertThat(f, is(notNullValue()));
		final CollectionPropertiesEntity changedProperties = f.get();
		assertThat(changedProperties.getWaitForSync(), is(notNullValue()));
		assertThat(changedProperties.getWaitForSync(), is(not(properties.getWaitForSync())));
	}

	@Test
	public void rename() throws InterruptedException, ExecutionException {
		try {
			final CompletableFuture<CollectionEntity> f = db.collection(COLLECTION_NAME).rename(COLLECTION_NAME + "1");
			assertThat(f, is(notNullValue()));
			final CollectionEntity result = f.get();
			assertThat(result, is(notNullValue()));
			assertThat(result.getName(), is(COLLECTION_NAME + "1"));
			final CollectionEntity info = db.collection(COLLECTION_NAME + "1").getInfo().get();
			assertThat(info.getName(), is(COLLECTION_NAME + "1"));
			try {
				db.collection(COLLECTION_NAME).getInfo().get();
				fail();
			} catch (final Exception e) {
			}
		} finally {
			db.collection(COLLECTION_NAME + "1").rename(COLLECTION_NAME).get();
		}
	}

	@Test
	public void getRevision() throws InterruptedException, ExecutionException {
		final CompletableFuture<CollectionRevisionEntity> f = db.collection(COLLECTION_NAME).getRevision();
		assertThat(f, is(notNullValue()));
		final CollectionRevisionEntity result = f.get();
		assertThat(result, is(notNullValue()));
		assertThat(result.getName(), is(COLLECTION_NAME));
		assertThat(result.getRevision(), is(notNullValue()));
	}

}
