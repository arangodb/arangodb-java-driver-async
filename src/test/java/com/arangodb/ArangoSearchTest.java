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

package com.arangodb;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.arangodb.entity.ViewEntity;
import com.arangodb.entity.ViewType;
import com.arangodb.entity.arangosearch.ArangoSearchPropertiesEntity;
import com.arangodb.entity.arangosearch.CollectionLink;
import com.arangodb.entity.arangosearch.ConsolidationPolicy;
import com.arangodb.entity.arangosearch.ConsolidationType;
import com.arangodb.entity.arangosearch.FieldLink;
import com.arangodb.entity.arangosearch.StoreValuesType;
import com.arangodb.model.arangosearch.ArangoSearchCreateOptions;
import com.arangodb.model.arangosearch.ArangoSearchPropertiesOptions;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoSearchTest extends BaseTest {

	private static final String VIEW_NAME = "view_test";

	@BeforeClass
	public static void setup() throws InterruptedException, ExecutionException {
		if (!requireVersion(arangoDB, 3, 4)) {
			return;
		}
		db.createArangoSearch(VIEW_NAME, new ArangoSearchCreateOptions()).get();
	}

	@Test
	public void exists() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		assertThat(db.arangoSearch(VIEW_NAME).exists().get(), is(true));
	}

	@Test
	public void getInfo() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		final ViewEntity info = db.arangoSearch(VIEW_NAME).getInfo().get();
		assertThat(info, is(not(nullValue())));
		assertThat(info.getId(), is(not(nullValue())));
		assertThat(info.getName(), is(VIEW_NAME));
		assertThat(info.getType(), is(ViewType.ARANGO_SEARCH));
	}

	@Test
	public void drop() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		final String name = VIEW_NAME + "_droptest";
		db.createArangoSearch(name, new ArangoSearchCreateOptions()).get();
		final ArangoViewAsync view = db.arangoSearch(name);
		view.drop().get();
		assertThat(view.exists().get(), is(false));
	}

	@Test
	public void rename() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		final String name = VIEW_NAME + "_renametest";
		final String newName = name + "_new";
		db.createArangoSearch(name, new ArangoSearchCreateOptions()).get();
		db.arangoSearch(name).rename(newName).get();
		assertThat(db.arangoSearch(name).exists().get(), is(false));
		assertThat(db.arangoSearch(newName).exists().get(), is(true));
	}

	@Test
	public void create() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		final String name = VIEW_NAME + "_createtest";
		final ViewEntity info = db.arangoSearch(name).create().get();
		assertThat(info, is(not(nullValue())));
		assertThat(info.getId(), is(not(nullValue())));
		assertThat(info.getName(), is(name));
		assertThat(info.getType(), is(ViewType.ARANGO_SEARCH));
		assertThat(db.arangoSearch(name).exists().get(), is(true));
	}

	@Test
	public void createWithOptions() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		final String name = VIEW_NAME + "_createtest_withotpions";
		final ViewEntity info = db.arangoSearch(name).create(new ArangoSearchCreateOptions()).get();
		assertThat(info, is(not(nullValue())));
		assertThat(info.getId(), is(not(nullValue())));
		assertThat(info.getName(), is(name));
		assertThat(info.getType(), is(ViewType.ARANGO_SEARCH));
		assertThat(db.arangoSearch(name).exists().get(), is(true));
	}

	@Test
	public void getProperties() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		final String name = VIEW_NAME + "_getpropertiestest";
		final ArangoSearchAsync view = db.arangoSearch(name);
		view.create(new ArangoSearchCreateOptions()).get();
		final ArangoSearchPropertiesEntity properties = view.getProperties().get();
		assertThat(properties, is(not(nullValue())));
		assertThat(properties.getId(), is(not(nullValue())));
		assertThat(properties.getName(), is(name));
		assertThat(properties.getType(), is(ViewType.ARANGO_SEARCH));
		assertThat(properties.getConsolidationIntervalMsec(), is(not(nullValue())));
		assertThat(properties.getCleanupIntervalStep(), is(not(nullValue())));
		final ConsolidationPolicy consolidate = properties.getConsolidationPolicy();
		assertThat(consolidate, is(is(not(nullValue()))));
		final Collection<CollectionLink> links = properties.getLinks();
		assertThat(links.isEmpty(), is(true));
	}

	@Test
	public void updateProperties() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		db.createCollection("view_update_prop_test_collection");
		final String name = VIEW_NAME + "_updatepropertiestest";
		final ArangoSearchAsync view = db.arangoSearch(name);
		view.create(new ArangoSearchCreateOptions()).get();
		final ArangoSearchPropertiesOptions options = new ArangoSearchPropertiesOptions();
		options.cleanupIntervalStep(15L);
		options.consolidationIntervalMsec(65000L);
		options.consolidationPolicy(ConsolidationPolicy.of(ConsolidationType.BYTES_ACCUM).threshold(1.));
		options.link(
			CollectionLink.on("view_update_prop_test_collection").fields(FieldLink.on("value").analyzers("identity")
					.trackListPositions(true).includeAllFields(true).storeValues(StoreValuesType.ID)));
		final ArangoSearchPropertiesEntity properties = view.updateProperties(options).get();
		assertThat(properties, is(not(nullValue())));
		assertThat(properties.getCleanupIntervalStep(), is(15L));
		assertThat(properties.getConsolidationIntervalMsec(), is(65000L));
		final ConsolidationPolicy consolidate = properties.getConsolidationPolicy();
		assertThat(consolidate, is(not(nullValue())));
		assertThat(consolidate.getType(), is(ConsolidationType.BYTES_ACCUM));
		assertThat(consolidate.getThreshold(), is(1.));
		assertThat(properties.getLinks().size(), is(1));
		final CollectionLink link = properties.getLinks().iterator().next();
		assertThat(link.getName(), is("view_update_prop_test_collection"));
		assertThat(link.getFields().size(), is(1));
		final FieldLink next = link.getFields().iterator().next();
		assertThat(next.getName(), is("value"));
		assertThat(next.getIncludeAllFields(), is(true));
		assertThat(next.getTrackListPositions(), is(true));
		assertThat(next.getStoreValues(), is(StoreValuesType.ID));
	}

	@Test
	public void replaceProperties() throws InterruptedException, ExecutionException {
		if (!requireVersion(3, 4)) {
			return;
		}
		db.createCollection("view_replace_prop_test_collection").get();
		final String name = VIEW_NAME + "_replacepropertiestest";
		final ArangoSearchAsync view = db.arangoSearch(name);
		view.create(new ArangoSearchCreateOptions()).get();
		final ArangoSearchPropertiesOptions options = new ArangoSearchPropertiesOptions();
		options.link(
			CollectionLink.on("view_replace_prop_test_collection").fields(FieldLink.on("value").analyzers("identity")));
		final ArangoSearchPropertiesEntity properties = view.replaceProperties(options).get();
		assertThat(properties, is(not(nullValue())));
		assertThat(properties.getLinks().size(), is(1));
		final CollectionLink link = properties.getLinks().iterator().next();
		assertThat(link.getName(), is("view_replace_prop_test_collection"));
		assertThat(link.getFields().size(), is(1));
		assertThat(link.getFields().iterator().next().getName(), is("value"));
	}

}
