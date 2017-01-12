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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;

public class A {

	public static class TestTaskResult {

		private int successNum = 0;
		private int exceptNum = 0;
		private int failNum = 0;

		public void addSuccessNum(final int i) {
			successNum += i;
		}

		public void addExceptNum(final int i) {
			exceptNum += i;
		}

		public void addFailNum(final int i) {
			failNum += i;
		}

	}

	private static final String ENTITY_COLLECTION = "test2017";
	private ArangoDBAsync arangoDB;
	private ArangoDatabaseAsync database;
	private final int propertyNum = 10;
	private final int threadTestCnt = 50000;
	private final String namePrefix = "test_";
	private final int nameIdOffset = 5;

	@Test
	public void a() {
		check("init", initArangoDB());

		try {
			check("insert", testArangoInsert());
			check("select", testArangoSelect());
			check("update", testArangoUpdate());
			check("select", testArangoSelect());
			check("delete", testArangoDelete());

		} finally {
			database.collection(ENTITY_COLLECTION).drop();
		}
	}

	private void check(final String phase, final TestTaskResult result) {
		System.out.println(String.format("%S: success:%s, fail:%s, exceptions:%s", phase, result.successNum,
			result.failNum, result.exceptNum));
		assertThat(result.exceptNum, is(0));
		assertThat(result.failNum, is(0));
	}

	private TestTaskResult initArangoDB() {
		final TestTaskResult result = new TestTaskResult();
		try {
			arangoDB = new ArangoDBAsync.Builder().build();
			database = arangoDB.db();

			try {
				arangoDB.db().collection(ENTITY_COLLECTION).drop().get();
			} catch (InterruptedException | ExecutionException e1) {
			}

			database.createCollection(ENTITY_COLLECTION).get();
			result.addSuccessNum(1);
		} catch (final ArangoDBException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
			result.addExceptNum(1);
			result.addFailNum(1);
		}
		return result;
	}

	private TestTaskResult testArangoInsert() {
		final TestTaskResult result = new TestTaskResult();
		final ArrayList<CompletableFuture<DocumentCreateEntity<BaseDocument>>> list = new ArrayList<>(threadTestCnt);
		final ArangoCollectionAsync entityCollection = database.collection(ENTITY_COLLECTION);

		try {
			for (int i = 0; i < threadTestCnt; i++) {
				final BaseDocument doc = new BaseDocument();
				doc.setKey(namePrefix + String.valueOf(i + 1 + nameIdOffset));
				for (int j = 0; j < propertyNum; j++) {
					final String nanoTime = String.valueOf(System.nanoTime());
					final StringBuilder randomValue = new StringBuilder();

					final String appendProp = "__" + nanoTime + "__" + String.valueOf(i + 1) + "__"
							+ String.valueOf(j + 1);
					randomValue.append(appendProp);
					doc.addAttribute("property" + String.valueOf(j), randomValue.toString());
				}

				final CompletableFuture<DocumentCreateEntity<BaseDocument>> f = entityCollection.insertDocument(doc);
				list.add(f);
			}
		} catch (final ArangoDBException e) {
			e.printStackTrace();
			result.addExceptNum(1);
		}

		for (int i = 0; i < threadTestCnt; i++) {
			try {
				final DocumentCreateEntity<BaseDocument> createEntity = list.get(i).get();
				if (createEntity.getKey() != null) {
					result.addSuccessNum(1);
				} else {
					result.addFailNum(1);
				}
			} catch (InterruptedException | ExecutionException e) {
				result.addFailNum(1);
			}
		}

		return result;
	}

	private TestTaskResult testArangoUpdate() {
		final TestTaskResult result = new TestTaskResult();
		int updatePropNum = propertyNum;
		if (propertyNum >= 10) {
			updatePropNum = propertyNum / 3;
		}

		final ArrayList<CompletableFuture<DocumentUpdateEntity<BaseDocument>>> list = new ArrayList<>(threadTestCnt);
		final ArangoCollectionAsync entityCollection = database.collection(ENTITY_COLLECTION);
		try {

			for (int i = 0; i < threadTestCnt; i++) {
				final BaseDocument doc = new BaseDocument();
				doc.setKey(namePrefix + String.valueOf(i + 1 + nameIdOffset));

				for (int j = 0; j < updatePropNum; j++) {
					final String nanoTime = String.valueOf(System.nanoTime());
					final StringBuilder randomValue = new StringBuilder();
					randomValue.append("_Update");

					final String appendProp = "__" + nanoTime + "__" + String.valueOf(i + 1) + "__"
							+ String.valueOf(j + 1);
					randomValue.append(appendProp);
					doc.addAttribute("property" + String.valueOf(j), randomValue.toString());
				}

				final CompletableFuture<DocumentUpdateEntity<BaseDocument>> f = entityCollection
						.updateDocument(doc.getKey(), doc);
				list.add(f);
			}
		} catch (final ArangoDBException e) {
			e.printStackTrace();
			result.addExceptNum(1);
		}

		for (int i = 0; i < threadTestCnt; i++) {
			try {
				final DocumentUpdateEntity<BaseDocument> updateEntity = list.get(i).get();
				if (updateEntity.getKey() != null) {
					result.addSuccessNum(1);
				} else {
					result.addFailNum(1);
				}
			} catch (InterruptedException | ExecutionException e) {
				result.addFailNum(1);
			}
		}

		return result;
	}

	private TestTaskResult testArangoSelect() {
		final TestTaskResult result = new TestTaskResult();
		final ArrayList<CompletableFuture<BaseDocument>> list = new ArrayList<>(threadTestCnt);
		final ArangoCollectionAsync entityCollection = database.collection(ENTITY_COLLECTION);
		try {
			for (int i = 0; i < threadTestCnt; i++) {
				final CompletableFuture<BaseDocument> f = entityCollection
						.getDocument(namePrefix + String.valueOf(i + 1 + nameIdOffset), BaseDocument.class);
				list.add(f);
			}
		} catch (final ArangoDBException e) {
			e.printStackTrace();
			result.addExceptNum(1);
		}

		for (int i = 0; i < threadTestCnt; i++) {
			try {
				final BaseDocument document = list.get(i).get();
				if (document.getKey() != null) {
					result.addSuccessNum(1);
				} else {
					result.addFailNum(1);
				}
			} catch (InterruptedException | ExecutionException e) {
				result.addFailNum(1);
			}
		}

		return result;
	}

	private TestTaskResult testArangoDelete() {
		final TestTaskResult result = new TestTaskResult();
		final ArrayList<CompletableFuture<DocumentDeleteEntity<Void>>> list = new ArrayList<>(threadTestCnt);
		final ArangoCollectionAsync entityCollection = database.collection(ENTITY_COLLECTION);
		try {
			for (int i = 0; i < threadTestCnt; i++) {
				final CompletableFuture<DocumentDeleteEntity<Void>> f = entityCollection
						.deleteDocument(namePrefix + String.valueOf(i + 1 + nameIdOffset));
				list.add(f);
			}
		} catch (final ArangoDBException e) {
			e.printStackTrace();
			result.addExceptNum(1);
		}

		for (int i = 0; i < threadTestCnt; i++) {
			try {
				final DocumentDeleteEntity<Void> deleteEntity = list.get(i).get();
				if (deleteEntity.getKey() != null) {
					result.addSuccessNum(1);
				} else {
					result.addFailNum(1);
				}
			} catch (InterruptedException | ExecutionException e) {
				result.addFailNum(1);
			}
		}

		return result;
	}

}
