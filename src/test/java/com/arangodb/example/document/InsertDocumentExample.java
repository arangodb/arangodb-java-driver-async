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

package com.arangodb.example.document;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.example.ExampleBase;
import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.ValueType;

/**
 * @author Mark Vollmary
 *
 */
public class InsertDocumentExample extends ExampleBase {

	@Test
	public void insertBean() {
		final CompletableFuture<DocumentCreateEntity<TestEntity>> f = collection.insertDocument(new TestEntity("bar"));
		f.whenComplete((doc, ex) -> {
			assertThat(doc.getKey(), is(notNullValue()));
		});
	}

	@Test
	public void insertBaseDocument() {
		final BaseDocument value = new BaseDocument();
		value.addAttribute("foo", "bar");
		final CompletableFuture<DocumentCreateEntity<BaseDocument>> f = collection.insertDocument(value);
		f.whenComplete((doc, ex) -> {
			assertThat(doc.getKey(), is(notNullValue()));
		});
	}

	@Test
	public void insertVPack() {
		final VPackBuilder builder = new VPackBuilder();
		builder.add(ValueType.OBJECT).add("foo", "bar").close();
		final CompletableFuture<DocumentCreateEntity<VPackSlice>> f = collection.insertDocument(builder.slice());
		f.whenComplete((doc, ex) -> {
			assertThat(doc.getKey(), is(notNullValue()));
		});
	}

	@Test
	public void insertJson() {
		final CompletableFuture<DocumentCreateEntity<String>> f = collection.insertDocument("{\"foo\":\"bar\"}");
		f.whenComplete((doc, ex) -> {
			assertThat(doc.getKey(), is(notNullValue()));
		});
	}

}
