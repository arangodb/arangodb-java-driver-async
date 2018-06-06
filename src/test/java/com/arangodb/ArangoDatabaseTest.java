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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import com.arangodb.entity.AqlExecutionExplainEntity;
import com.arangodb.entity.AqlExecutionExplainEntity.ExecutionPlan;
import com.arangodb.entity.AqlFunctionEntity;
import com.arangodb.entity.AqlParseEntity;
import com.arangodb.entity.AqlParseEntity.AstNode;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionPropertiesEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.DatabaseEntity;
import com.arangodb.entity.GraphEntity;
import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.PathEntity;
import com.arangodb.entity.Permissions;
import com.arangodb.entity.QueryCachePropertiesEntity;
import com.arangodb.entity.QueryCachePropertiesEntity.CacheMode;
import com.arangodb.entity.QueryEntity;
import com.arangodb.entity.QueryTrackingPropertiesEntity;
import com.arangodb.entity.ServerRole;
import com.arangodb.entity.TraversalEntity;
import com.arangodb.model.AqlFunctionDeleteOptions;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.CollectionsReadOptions;
import com.arangodb.model.TransactionOptions;
import com.arangodb.model.TraversalOptions;
import com.arangodb.model.TraversalOptions.Direction;
import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.exception.VPackException;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoDatabaseTest extends BaseTest {

	private static final String COLLECTION_NAME = "db_test";
	private static final String GRAPH_NAME = "graph_test";

	@Test
	public void create() throws InterruptedException, ExecutionException {
		try {
			final Boolean result = arangoDB.db(BaseTest.TEST_DB + "_1").create().get();
			assertThat(result, is(true));
		} finally {
			try {
				arangoDB.db(BaseTest.TEST_DB + "_1").drop();
			} catch (final ArangoDBException e) {
			}
		}
	}

	@Test
	public void getVersion() throws InterruptedException, ExecutionException {
		final CompletableFuture<ArangoDBVersion> f = db.getVersion();
		assertThat(f, is(notNullValue()));
		f.whenComplete((version, ex) -> {
			assertThat(version, is(notNullValue()));
			assertThat(version.getServer(), is(notNullValue()));
			assertThat(version.getVersion(), is(notNullValue()));
		});
		f.get();
	}

	@Test
	public void exists() throws InterruptedException, ExecutionException {
		assertThat(db.exists().get(), is(true));
		assertThat(arangoDB.db("no").exists().get(), is(false));
	}

	@Test
	public void getAccessibleDatabases() throws InterruptedException, ExecutionException {
		final CompletableFuture<Collection<String>> f = db.getAccessibleDatabases();
		assertThat(f, is(notNullValue()));
		f.whenComplete((dbs, ex) -> {
			assertThat(dbs, is(notNullValue()));
			assertThat(dbs.size(), greaterThan(0));
			assertThat(dbs, hasItem("_system"));
		});
		f.get();
	}

	@Test
	public void createCollection() throws InterruptedException, ExecutionException {
		try {
			final CompletableFuture<CollectionEntity> f = db.createCollection(COLLECTION_NAME, null);
			assertThat(f, is(notNullValue()));
			f.whenComplete((result, ex) -> {
				assertThat(result, is(notNullValue()));
				assertThat(result.getId(), is(notNullValue()));
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void createCollectionWithReplicationFactor() throws InterruptedException, ExecutionException {
		if (arangoDB.getRole().get() == ServerRole.SINGLE) {
			return;
		}
		try {
			final CollectionEntity result = db
					.createCollection(COLLECTION_NAME, new CollectionCreateOptions().replicationFactor(2)).get();
			assertThat(result, is(notNullValue()));
			assertThat(result.getId(), is(notNullValue()));
			assertThat(db.collection(COLLECTION_NAME).getProperties().get().getReplicationFactor(), is(2));
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void createCollectionWithNumberOfShards() throws InterruptedException, ExecutionException {
		if (arangoDB.getRole().get() == ServerRole.SINGLE) {
			return;
		}
		try {
			final CollectionEntity result = db
					.createCollection(COLLECTION_NAME, new CollectionCreateOptions().numberOfShards(2)).get();
			assertThat(result, is(notNullValue()));
			assertThat(result.getId(), is(notNullValue()));
			assertThat(db.collection(COLLECTION_NAME).getProperties().get().getNumberOfShards(), is(2));
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void createCollectionWithNumberOfShardsAndShardKey() throws InterruptedException, ExecutionException {
		if (arangoDB.getRole().get() == ServerRole.SINGLE) {
			return;
		}
		try {
			final CollectionEntity result = db
					.createCollection(COLLECTION_NAME, new CollectionCreateOptions().numberOfShards(2).shardKeys("a"))
					.get();
			assertThat(result, is(notNullValue()));
			assertThat(result.getId(), is(notNullValue()));
			final CollectionPropertiesEntity properties = db.collection(COLLECTION_NAME).getProperties().get();
			assertThat(properties.getNumberOfShards(), is(2));
			assertThat(properties.getShardKeys().size(), is(1));
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void createCollectionWithNumberOfShardsAndShardKeys() throws InterruptedException, ExecutionException {
		if (arangoDB.getRole().get() == ServerRole.SINGLE) {
			return;
		}
		try {
			final CollectionEntity result = db.createCollection(COLLECTION_NAME,
				new CollectionCreateOptions().numberOfShards(2).shardKeys("a", "b")).get();
			assertThat(result, is(notNullValue()));
			assertThat(result.getId(), is(notNullValue()));
			final CollectionPropertiesEntity properties = db.collection(COLLECTION_NAME).getProperties().get();
			assertThat(properties.getNumberOfShards(), is(2));
			assertThat(properties.getShardKeys().size(), is(2));
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void deleteCollection() throws InterruptedException, ExecutionException {
		db.createCollection(COLLECTION_NAME, null).get();
		db.collection(COLLECTION_NAME).drop().get();
		try {
			db.collection(COLLECTION_NAME).getInfo().get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void deleteSystemCollection() throws InterruptedException, ExecutionException {
		if (arangoDB.getRole().get() != ServerRole.SINGLE) {
			return;
		}
		final String name = "_system_test";
		db.createCollection(name, new CollectionCreateOptions().isSystem(true)).get();
		db.collection(name).drop(true).get();
		try {
			db.collection(name).getInfo().get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void deleteSystemCollectionFail() throws InterruptedException, ExecutionException {
		if (arangoDB.getRole().get() != ServerRole.SINGLE) {
			return;
		}
		final String name = "_system_test";
		db.createCollection(name, new CollectionCreateOptions().isSystem(true)).get();
		try {
			db.collection(name).drop().get();
			fail();
		} catch (final Exception e) {
		}
		db.collection(name).drop(true).get();
		try {
			db.collection(name).getInfo().get();
			fail();
		} catch (final Exception e) {
		}
	}

	@Test
	public void getIndex() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			final Collection<String> fields = new ArrayList<>();
			fields.add("a");
			final IndexEntity createResult = db.collection(COLLECTION_NAME).ensureHashIndex(fields, null).get();
			final CompletableFuture<IndexEntity> f = db.getIndex(createResult.getId());
			assertThat(f, is(notNullValue()));
			f.whenComplete((readResult, ex) -> {
				assertThat(readResult.getId(), is(createResult.getId()));
				assertThat(readResult.getType(), is(createResult.getType()));
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void deleteIndex() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			final Collection<String> fields = new ArrayList<>();
			fields.add("a");
			final IndexEntity createResult = db.collection(COLLECTION_NAME).ensureHashIndex(fields, null).get();
			final CompletableFuture<String> f = db.deleteIndex(createResult.getId());
			assertThat(f, is(notNullValue()));
			f.whenComplete((id, ex) -> {
				assertThat(id, is(createResult.getId()));
				try {
					db.getIndex(id);
					fail();
				} catch (final ArangoDBException e) {
				}
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void getCollections() throws InterruptedException, ExecutionException {
		try {
			final Collection<CollectionEntity> systemCollections = db.getCollections(null).get();
			db.createCollection(COLLECTION_NAME + "1", null).get();
			db.createCollection(COLLECTION_NAME + "2", null).get();
			final CompletableFuture<Collection<CollectionEntity>> f = db.getCollections(null);
			assertThat(f, is(notNullValue()));
			f.whenComplete((collections, ex) -> {
				assertThat(collections.size(), is(2 + systemCollections.size()));
				assertThat(collections, is(notNullValue()));
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME + "1").drop().get();
			db.collection(COLLECTION_NAME + "2").drop().get();
		}
	}

	@Test
	public void getCollectionsExcludeSystem() throws InterruptedException, ExecutionException {
		try {
			final CollectionsReadOptions options = new CollectionsReadOptions().excludeSystem(true);
			final Collection<CollectionEntity> systemCollections = db.getCollections(options).get();
			assertThat(systemCollections.size(), is(0));
			db.createCollection(COLLECTION_NAME + "1", null).get();
			db.createCollection(COLLECTION_NAME + "2", null).get();
			final CompletableFuture<Collection<CollectionEntity>> f = db.getCollections(options);
			assertThat(f, is(notNullValue()));
			f.whenComplete((collections, ex) -> {
				assertThat(collections.size(), is(2));
				assertThat(collections, is(notNullValue()));
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME + "1").drop().get();
			db.collection(COLLECTION_NAME + "2").drop().get();
		}
	}

	@Test
	public void grantAccess() throws InterruptedException, ExecutionException {
		try {
			arangoDB.createUser("user1", "1234", null).get();
			db.grantAccess("user1").get();
		} finally {
			arangoDB.deleteUser("user1").get();
		}
	}

	@Test
	public void grantAccessRW() throws InterruptedException, ExecutionException {
		try {
			arangoDB.createUser("user1", "1234", null).get();
			db.grantAccess("user1", Permissions.RW).get();
		} finally {
			arangoDB.deleteUser("user1").get();
		}
	}

	@Test
	public void grantAccessRO() throws InterruptedException, ExecutionException {
		try {
			arangoDB.createUser("user1", "1234", null).get();
			db.grantAccess("user1", Permissions.RO).get();
		} finally {
			arangoDB.deleteUser("user1").get();
		}
	}

	@Test
	public void grantAccessNONE() throws InterruptedException, ExecutionException {
		try {
			arangoDB.createUser("user1", "1234", null).get();
			db.grantAccess("user1", Permissions.NONE).get();
		} finally {
			arangoDB.deleteUser("user1").get();
		}
	}

	@Test(expected = ExecutionException.class)
	public void grantAccessUserNotFound() throws InterruptedException, ExecutionException {
		db.grantAccess("user1", Permissions.RW).get();
	}

	@Test
	public void revokeAccess() throws InterruptedException, ExecutionException {
		try {
			arangoDB.createUser("user1", "1234", null).get();
			db.revokeAccess("user1").get();
		} finally {
			arangoDB.deleteUser("user1").get();
		}
	}

	@Test(expected = ExecutionException.class)
	public void revokeAccessUserNotFound() throws InterruptedException, ExecutionException {
		db.revokeAccess("user1").get();
	}

	@Test
	public void resetAccess() throws InterruptedException, ExecutionException {
		try {
			arangoDB.createUser("user1", "1234", null).get();
			db.resetAccess("user1").get();
		} finally {
			arangoDB.deleteUser("user1").get();
		}
	}

	@Test(expected = ExecutionException.class)
	public void resetAccessUserNotFound() throws InterruptedException, ExecutionException {
		db.resetAccess("user1").get();
	}

	@Test
	public void grantDefaultCollectionAccess() throws InterruptedException, ExecutionException {
		try {
			arangoDB.createUser("user1", "1234").get();
			db.grantDefaultCollectionAccess("user1", Permissions.RW).get();
		} finally {
			arangoDB.deleteUser("user1").get();
		}
	}

	@Test
	public void getPermissions() throws ArangoDBException, InterruptedException, ExecutionException {
		assertThat(Permissions.RW, is(db.getPermissions("root").get()));
	}

	@Test
	public void query() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}
			final CompletableFuture<ArangoCursorAsync<String>> f = db.query("for i in db_test return i._id", null, null,
				String.class);
			assertThat(f, is(notNullValue()));
			f.whenComplete((cursor, ex) -> {
				assertThat(cursor, is(notNullValue()));
				for (int i = 0; i < 10; i++, cursor.next()) {
					assertThat(cursor.hasNext(), is(i != 10));
				}
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void queryForEach() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}
			final CompletableFuture<ArangoCursorAsync<String>> f = db.query("for i in db_test return i._id", null, null,
				String.class);
			assertThat(f, is(notNullValue()));
			f.whenComplete((cursor, ex) -> {
				assertThat(cursor, is(notNullValue()));
				final AtomicInteger i = new AtomicInteger(0);
				cursor.forEachRemaining(e -> {
					i.incrementAndGet();
				});
				assertThat(i.get(), is(10));
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void queryStream() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}
			final CompletableFuture<ArangoCursorAsync<String>> f = db.query("for i in db_test return i._id", null, null,
				String.class);
			assertThat(f, is(notNullValue()));
			f.whenComplete((cursor, ex) -> {
				assertThat(cursor, is(notNullValue()));
				final AtomicInteger i = new AtomicInteger(0);
				cursor.forEachRemaining(e -> {
					i.incrementAndGet();
				});
				assertThat(i.get(), is(10));
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void queryWithCount() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}

			final CompletableFuture<ArangoCursorAsync<String>> f = db.query("for i in db_test Limit 6 return i._id",
				null, new AqlQueryOptions().count(true), String.class);
			assertThat(f, is(notNullValue()));
			f.whenComplete((cursor, ex) -> {
				assertThat(cursor, is(notNullValue()));
				for (int i = 0; i < 6; i++, cursor.next()) {
					assertThat(cursor.hasNext(), is(i != 6));
				}
				assertThat(cursor.getCount(), is(6));
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void queryWithLimitAndFullCount() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}

			final CompletableFuture<ArangoCursorAsync<String>> f = db.query("for i in db_test Limit 5 return i._id",
				null, new AqlQueryOptions().fullCount(true), String.class);
			assertThat(f, is(notNullValue()));
			f.whenComplete((cursor, ex) -> {
				assertThat(cursor, is(notNullValue()));
				for (int i = 0; i < 5; i++, cursor.next()) {
					assertThat(cursor.hasNext(), is(i != 5));
				}
				assertThat(cursor.getStats(), is(notNullValue()));
				assertThat(cursor.getStats().getFullCount(), is(10L));
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void queryWithBatchSize() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}
			final ArangoCursorAsync<String> cursor = db.query("for i in db_test return i._id", null,
				new AqlQueryOptions().batchSize(5).count(true), String.class).get();
			assertThat(cursor, is(notNullValue()));
			for (int i = 0; i < 10; i++, cursor.next()) {
				assertThat(cursor.hasNext(), is(i != 10));
			}
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void queryStreamWithBatchSize() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}
			final ArangoCursorAsync<String> cursor = db.query("for i in db_test return i._id", null,
				new AqlQueryOptions().batchSize(5).count(true), String.class).get();
			assertThat(cursor, is(notNullValue()));
			final AtomicInteger i = new AtomicInteger(0);
			cursor.streamRemaining().forEach(e -> {
				i.incrementAndGet();
			});
			assertThat(i.get(), is(10));
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	/**
	 * ignored. takes to long
	 * 
	 * @throws ExecutionException
	 */
	@Test
	@Ignore
	public void queryWithTTL() throws InterruptedException, ExecutionException {
		// set TTL to 1 seconds and get the second batch after 2 seconds!
		final int ttl = 1;
		final int wait = 2;
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}
			final ArangoCursorAsync<String> cursor = db.query("for i in db_test return i._id", null,
				new AqlQueryOptions().batchSize(5).ttl(ttl), String.class).get();
			assertThat(cursor, is(notNullValue()));
			for (int i = 0; i < 10; i++, cursor.next()) {
				assertThat(cursor.hasNext(), is(i != 10));
				if (i == 1) {
					Thread.sleep(wait * 1000);
				}
			}
			fail("this should fail");
		} catch (final ArangoDBException ex) {
			assertThat(ex.getMessage(), is("Response: 404, Error: 1600 - cursor not found"));
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void changeQueryCache() throws InterruptedException, ExecutionException {
		try {
			QueryCachePropertiesEntity properties = db.getQueryCacheProperties().get();
			assertThat(properties, is(notNullValue()));
			assertThat(properties.getMode(), is(CacheMode.off));
			assertThat(properties.getMaxResults(), greaterThan(0L));

			properties.setMode(CacheMode.on);
			properties = db.setQueryCacheProperties(properties).get();
			assertThat(properties, is(notNullValue()));
			assertThat(properties.getMode(), is(CacheMode.on));

			properties = db.getQueryCacheProperties().get();
			assertThat(properties.getMode(), is(CacheMode.on));
		} finally {
			final QueryCachePropertiesEntity properties = new QueryCachePropertiesEntity();
			properties.setMode(CacheMode.off);
			db.setQueryCacheProperties(properties).get();
		}
	}

	@Test
	public void queryWithCache() throws InterruptedException, ArangoDBException, ExecutionException {
		if (arangoDB.getRole().get() != ServerRole.SINGLE) {
			return;
		}
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}

			final QueryCachePropertiesEntity properties = new QueryCachePropertiesEntity();
			properties.setMode(CacheMode.on);
			db.setQueryCacheProperties(properties).get();

			final ArangoCursorAsync<String> cursor = db
					.query("FOR t IN db_test FILTER t.age >= 10 SORT t.age RETURN t._id", null,
						new AqlQueryOptions().cache(true), String.class)
					.get();

			assertThat(cursor, is(notNullValue()));
			assertThat(cursor.isCached(), is(false));

			final ArangoCursorAsync<String> cachedCursor = db
					.query("FOR t IN db_test FILTER t.age >= 10 SORT t.age RETURN t._id", null,
						new AqlQueryOptions().cache(true), String.class)
					.get();

			assertThat(cachedCursor, is(notNullValue()));
			assertThat(cachedCursor.isCached(), is(true));

		} finally {
			db.collection(COLLECTION_NAME).drop().get();
			final QueryCachePropertiesEntity properties = new QueryCachePropertiesEntity();
			properties.setMode(CacheMode.off);
			db.setQueryCacheProperties(properties).get();
		}
	}

	@Test
	public void queryCursor() throws ArangoDBException, InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			final int numbDocs = 10;
			for (int i = 0; i < numbDocs; i++) {
				db.collection(COLLECTION_NAME).insertDocument(new BaseDocument(), null).get();
			}

			final int batchSize = 5;
			final ArangoCursorAsync<String> cursor = db.query("for i in db_test return i._id", null,
				new AqlQueryOptions().batchSize(batchSize).count(true), String.class).get();
			assertThat(cursor, is(notNullValue()));
			assertThat(cursor.getCount(), is(numbDocs));

			final ArangoCursorAsync<String> cursor2 = db.cursor(cursor.getId(), String.class).get();
			assertThat(cursor2, is(notNullValue()));
			assertThat(cursor2.getCount(), is(numbDocs));
			assertThat(cursor2.hasNext(), is(true));

			for (int i = 0; i < batchSize; i++, cursor.next()) {
				assertThat(cursor.hasNext(), is(i != batchSize));
			}
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void changeQueryTrackingProperties() throws InterruptedException, ExecutionException {
		try {
			QueryTrackingPropertiesEntity properties = db.getQueryTrackingProperties().get();
			assertThat(properties, is(notNullValue()));
			assertThat(properties.getEnabled(), is(true));
			assertThat(properties.getTrackSlowQueries(), is(true));
			assertThat(properties.getMaxQueryStringLength(), greaterThan(0L));
			assertThat(properties.getMaxSlowQueries(), greaterThan(0L));
			assertThat(properties.getSlowQueryThreshold(), greaterThan(0L));
			properties.setEnabled(false);
			properties = db.setQueryTrackingProperties(properties).get();
			assertThat(properties, is(notNullValue()));
			assertThat(properties.getEnabled(), is(false));
			properties = db.getQueryTrackingProperties().get();
			assertThat(properties.getEnabled(), is(false));
		} finally {
			final QueryTrackingPropertiesEntity properties = new QueryTrackingPropertiesEntity();
			properties.setEnabled(true);
			db.setQueryTrackingProperties(properties).get();
		}
	}

	@Test
	public void queryWithBindVars() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME, null).get();
			for (int i = 0; i < 10; i++) {
				final BaseDocument baseDocument = new BaseDocument();
				baseDocument.addAttribute("age", 20 + i);
				db.collection(COLLECTION_NAME).insertDocument(baseDocument, null).get();
			}
			final Map<String, Object> bindVars = new HashMap<>();
			bindVars.put("@coll", COLLECTION_NAME);
			bindVars.put("age", 25);
			final CompletableFuture<ArangoCursorAsync<String>> f = db.query(
				"FOR t IN @@coll FILTER t.age >= @age SORT t.age RETURN t._id", bindVars, null, String.class);
			assertThat(f, is(notNullValue()));
			f.whenComplete((cursor, ex) -> {
				assertThat(cursor, is(notNullValue()));
				for (int i = 0; i < 5; i++, cursor.next()) {
					assertThat(cursor.hasNext(), is(i != 5));
				}
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void queryWithWarning() throws InterruptedException, ExecutionException {
		final CompletableFuture<ArangoCursorAsync<String>> f = arangoDB.db().query("return 1/0", null, null,
			String.class);
		assertThat(f, is(notNullValue()));
		f.whenComplete((cursor, ex) -> {
			assertThat(cursor, is(notNullValue()));
			assertThat(cursor.getWarnings(), is(notNullValue()));
			assertThat(cursor.getWarnings(), is(not(empty())));
		});
		f.get();
	}

	@Test
	public void queryClose() throws IOException, ArangoDBException, InterruptedException, ExecutionException {
		final ArangoCursorAsync<String> cursor = arangoDB.db()
				.query("for i in 1..2 return i", null, new AqlQueryOptions().batchSize(1), String.class).get();
		cursor.close();
		int count = 0;
		try {
			for (; cursor.hasNext(); cursor.next(), count++) {
			}
			fail();
		} catch (final ArangoDBException e) {
			assertThat(count, is(1));
		}

	}

	@Test
	public void explainQuery() throws InterruptedException, ExecutionException {
		final CompletableFuture<AqlExecutionExplainEntity> f = arangoDB.db().explainQuery("for i in _apps return i",
			null, null);
		assertThat(f, is(notNullValue()));
		f.whenComplete((explain, ex) -> {
			assertThat(explain, is(notNullValue()));
			assertThat(explain.getPlan(), is(notNullValue()));
			assertThat(explain.getPlans(), is(nullValue()));
			final ExecutionPlan plan = explain.getPlan();
			assertThat(plan.getCollections().size(), is(1));
			assertThat(plan.getCollections().iterator().next().getName(), is("_apps"));
			assertThat(plan.getCollections().iterator().next().getType(), is("read"));
			assertThat(plan.getEstimatedCost(), greaterThan(0));
			assertThat(plan.getEstimatedNrItems(), greaterThan(0));
			assertThat(plan.getVariables().size(), is(1));
			assertThat(plan.getVariables().iterator().next().getName(), is("i"));
			assertThat(plan.getNodes().size(), is(greaterThan(0)));
		});
		f.get();
	}

	@Test
	public void parseQuery() throws InterruptedException, ExecutionException {
		final CompletableFuture<AqlParseEntity> f = arangoDB.db().parseQuery("for i in _apps return i");
		assertThat(f, is(notNullValue()));
		f.whenComplete((parse, ex) -> {
			assertThat(parse, is(notNullValue()));
			assertThat(parse.getBindVars(), is(empty()));
			assertThat(parse.getCollections().size(), is(1));
			assertThat(parse.getCollections().iterator().next(), is("_apps"));
			assertThat(parse.getAst().size(), is(1));
			final AstNode root = parse.getAst().iterator().next();
			assertThat(root.getType(), is("root"));
			assertThat(root.getName(), is(nullValue()));
			assertThat(root.getSubNodes(), is(notNullValue()));
			assertThat(root.getSubNodes().size(), is(2));
			final Iterator<AstNode> iterator = root.getSubNodes().iterator();
			final AstNode for_ = iterator.next();
			assertThat(for_.getType(), is("for"));
			assertThat(for_.getSubNodes(), is(notNullValue()));
			assertThat(for_.getSubNodes().size(), is(2));
			final Iterator<AstNode> iterator2 = for_.getSubNodes().iterator();
			final AstNode first = iterator2.next();
			assertThat(first.getType(), is("variable"));
			assertThat(first.getName(), is("i"));
			final AstNode second = iterator2.next();
			assertThat(second.getType(), is("collection"));
			assertThat(second.getName(), is("_apps"));
			final AstNode return_ = iterator.next();
			assertThat(return_.getType(), is("return"));
			assertThat(return_.getSubNodes(), is(notNullValue()));
			assertThat(return_.getSubNodes().size(), is(1));
			assertThat(return_.getSubNodes().iterator().next().getType(), is("reference"));
			assertThat(return_.getSubNodes().iterator().next().getName(), is("i"));
		});
		f.get();
	}

	@Test
	@Ignore
	public void getAndClearSlowQueries() throws InterruptedException, ExecutionException {
		final QueryTrackingPropertiesEntity properties = db.getQueryTrackingProperties().get();
		final Long slowQueryThreshold = properties.getSlowQueryThreshold();
		try {
			properties.setSlowQueryThreshold(1L);
			db.setQueryTrackingProperties(properties);

			db.query("return sleep(1.1)", null, null, Void.class);
			final Collection<QueryEntity> slowQueries = db.getSlowQueries().get();
			assertThat(slowQueries, is(notNullValue()));
			assertThat(slowQueries.size(), is(1));
			final QueryEntity queryEntity = slowQueries.iterator().next();
			assertThat(queryEntity.getQuery(), is("return sleep(1.1)"));

			db.clearSlowQueries().get();
			assertThat(db.getSlowQueries().get().size(), is(0));
		} finally {
			properties.setSlowQueryThreshold(slowQueryThreshold);
			db.setQueryTrackingProperties(properties);
		}
	}

	@Test
	public void createGetDeleteAqlFunction() throws InterruptedException, ExecutionException {
		final Collection<AqlFunctionEntity> aqlFunctionsInitial = db.getAqlFunctions(null).get();
		assertThat(aqlFunctionsInitial, is(empty()));
		try {
			db.createAqlFunction("myfunctions::temperature::celsiustofahrenheit",
				"function (celsius) { return celsius * 1.8 + 32; }", null).get();

			final Collection<AqlFunctionEntity> aqlFunctions = db.getAqlFunctions(null).get();
			assertThat(aqlFunctions.size(), is(greaterThan(aqlFunctionsInitial.size())));
		} finally {
			db.deleteAqlFunction("myfunctions::temperature::celsiustofahrenheit", null).get();

			final Collection<AqlFunctionEntity> aqlFunctions = db.getAqlFunctions(null).get();
			assertThat(aqlFunctions.size(), is(aqlFunctionsInitial.size()));
		}
	}

	@Test
	public void createGetDeleteAqlFunctionWithNamespace() throws InterruptedException, ExecutionException {
		final Collection<AqlFunctionEntity> aqlFunctionsInitial = db.getAqlFunctions(null).get();
		assertThat(aqlFunctionsInitial, is(empty()));
		try {
			db.createAqlFunction("myfunctions::temperature::celsiustofahrenheit1",
				"function (celsius) { return celsius * 1.8 + 32; }", null).get();
			db.createAqlFunction("myfunctions::temperature::celsiustofahrenheit2",
				"function (celsius) { return celsius * 1.8 + 32; }", null).get();

		} finally {
			db.deleteAqlFunction("myfunctions::temperature", new AqlFunctionDeleteOptions().group(true)).get();

			final Collection<AqlFunctionEntity> aqlFunctions = db.getAqlFunctions(null).get();
			assertThat(aqlFunctions.size(), is(aqlFunctionsInitial.size()));
		}
	}

	@Test
	public void createGraph() throws InterruptedException, ExecutionException {
		try {
			final CompletableFuture<GraphEntity> f = db.createGraph(GRAPH_NAME, null, null);
			assertThat(f, is(notNullValue()));
			f.whenComplete((result, ex) -> {
				assertThat(result, is(notNullValue()));
				assertThat(result.getName(), is(GRAPH_NAME));
			});
			f.get();
		} finally {
			db.graph(GRAPH_NAME).drop().get();
		}
	}

	@Test
	public void getGraphs() throws InterruptedException, ExecutionException {
		try {
			db.createGraph(GRAPH_NAME, null, null).get();
			final CompletableFuture<Collection<GraphEntity>> f = db.getGraphs();
			assertThat(f, is(notNullValue()));
			f.whenComplete((graphs, ex) -> {
				assertThat(graphs, is(notNullValue()));
				assertThat(graphs.size(), is(1));
			});
			f.get();
		} finally {
			db.graph(GRAPH_NAME).drop().get();
		}
	}

	@Test
	public void transactionString() throws InterruptedException, ExecutionException {
		final TransactionOptions options = new TransactionOptions().params("test");
		final CompletableFuture<String> f = db.transaction("function (params) {return params;}", String.class, options);
		assertThat(f, is(notNullValue()));
		f.whenComplete((result, ex) -> {
			assertThat(result, is("test"));
		});
		f.get();
	}

	@Test
	public void transactionNumber() throws InterruptedException, ExecutionException {
		final TransactionOptions options = new TransactionOptions().params(5);
		final CompletableFuture<Integer> f = db.transaction("function (params) {return params;}", Integer.class,
			options);
		assertThat(f, is(notNullValue()));
		f.whenComplete((result, ex) -> {
			assertThat(result, is(5));
		});
		f.get();
	}

	@Test
	public void transactionVPack() throws VPackException, InterruptedException, ExecutionException {
		final TransactionOptions options = new TransactionOptions().params(new VPackBuilder().add("test").slice());
		final CompletableFuture<VPackSlice> f = db.transaction("function (params) {return params;}", VPackSlice.class,
			options);
		assertThat(f, is(notNullValue()));
		f.whenComplete((result, ex) -> {
			assertThat(result.isString(), is(true));
			assertThat(result.getAsString(), is("test"));
		});
		f.get();
	}

	@Test
	public void transactionEmpty() throws InterruptedException, ExecutionException {
		db.transaction("function () {}", null, null).get();
	}

	@Test
	public void transactionallowImplicit() throws InterruptedException, ExecutionException {
		try {
			db.createCollection("someCollection", null).get();
			db.createCollection("someOtherCollection", null).get();
			final String action = "function (params) {" + "var db = require('internal').db;"
					+ "return {'a':db.someCollection.all().toArray()[0], 'b':db.someOtherCollection.all().toArray()[0]};"
					+ "}";
			final TransactionOptions options = new TransactionOptions().readCollections("someCollection");
			db.transaction(action, VPackSlice.class, options).get();
			try {
				options.allowImplicit(false);
				db.transaction(action, VPackSlice.class, options).get();
				fail();
			} catch (final Exception e) {
			}
		} finally {
			db.collection("someCollection").drop().get();
			db.collection("someOtherCollection").drop().get();
		}
	}

	protected static class TransactionTestEntity {
		private String value;

		public TransactionTestEntity() {
			super();
		}
	}

	@Test
	public void transactionPojoReturn() {
		final String action = "function() { return {'value':'hello world'}; }";
		final CompletableFuture<TransactionTestEntity> f = db.transaction(action, TransactionTestEntity.class,
			new TransactionOptions());
		assertThat(f, is(notNullValue()));
		f.whenComplete((res, ex) -> {
			assertThat(res, is(notNullValue()));
			assertThat(res.value, is("hello world"));
		});
	}

	@Test
	public void getInfo() throws InterruptedException, ExecutionException {
		final CompletableFuture<DatabaseEntity> f = db.getInfo();
		assertThat(f, is(notNullValue()));
		f.whenComplete((info, ex) -> {
			assertThat(info, is(notNullValue()));
			assertThat(info.getId(), is(notNullValue()));
			assertThat(info.getName(), is(TEST_DB));
			assertThat(info.getPath(), is(notNullValue()));
			assertThat(info.getIsSystem(), is(false));
		});
		f.get();
	}

	@Test
	public void executeTraversal() throws InterruptedException, ExecutionException {
		try {
			db.createCollection("person", null).get();
			db.createCollection("knows", new CollectionCreateOptions().type(CollectionType.EDGES)).get();
			for (final String e : new String[] { "Alice", "Bob", "Charlie", "Dave", "Eve" }) {
				final BaseDocument doc = new BaseDocument();
				doc.setKey(e);
				db.collection("person").insertDocument(doc, null).get();
			}
			for (final String[] e : new String[][] { new String[] { "Alice", "Bob" }, new String[] { "Bob", "Charlie" },
					new String[] { "Bob", "Dave" }, new String[] { "Eve", "Alice" }, new String[] { "Eve", "Bob" } }) {
				final BaseEdgeDocument edge = new BaseEdgeDocument();
				edge.setKey(e[0] + "_knows_" + e[1]);
				edge.setFrom("person/" + e[0]);
				edge.setTo("person/" + e[1]);
				db.collection("knows").insertDocument(edge, null).get();
			}
			final TraversalOptions options = new TraversalOptions().edgeCollection("knows").startVertex("person/Alice")
					.direction(Direction.outbound);
			final CompletableFuture<TraversalEntity<BaseDocument, BaseEdgeDocument>> f = db
					.executeTraversal(BaseDocument.class, BaseEdgeDocument.class, options);
			assertThat(f, is(notNullValue()));
			f.whenComplete((traversal, ex) -> {
				assertThat(traversal, is(notNullValue()));

				final Collection<BaseDocument> vertices = traversal.getVertices();
				assertThat(vertices, is(notNullValue()));
				assertThat(vertices.size(), is(4));

				final Iterator<BaseDocument> verticesIterator = vertices.iterator();
				final Collection<String> v = Arrays.asList(new String[] { "Alice", "Bob", "Charlie", "Dave" });
				for (; verticesIterator.hasNext();) {
					assertThat(v.contains(verticesIterator.next().getKey()), is(true));
				}

				final Collection<PathEntity<BaseDocument, BaseEdgeDocument>> paths = traversal.getPaths();
				assertThat(paths, is(notNullValue()));
				assertThat(paths.size(), is(4));

				assertThat(paths.iterator().hasNext(), is(true));
				final PathEntity<BaseDocument, BaseEdgeDocument> first = paths.iterator().next();
				assertThat(first, is(notNullValue()));
				assertThat(first.getEdges().size(), is(0));
				assertThat(first.getVertices().size(), is(1));
				assertThat(first.getVertices().iterator().next().getKey(), is("Alice"));
			});
			f.get();
		} finally {
			db.collection("person").drop().get();
			db.collection("knows").drop().get();
		}
	}

	@Test
	public void getDocument() throws InterruptedException, ExecutionException {
		try {
			db.createCollection(COLLECTION_NAME).get();
			final BaseDocument value = new BaseDocument();
			value.setKey("123");
			db.collection(COLLECTION_NAME).insertDocument(value).get();
			final CompletableFuture<BaseDocument> f = db.getDocument(COLLECTION_NAME + "/123", BaseDocument.class);
			assertThat(f, is(notNullValue()));
			f.whenComplete((document, ex) -> {
				assertThat(document, is(notNullValue()));
				assertThat(document.getKey(), is("123"));
			});
			f.get();
		} finally {
			db.collection(COLLECTION_NAME).drop().get();
		}
	}

	@Test
	public void shouldIncludeExceptionMessage() throws InterruptedException, ExecutionException {
		final String version = db.getVersion().get().getVersion();
		if (version.startsWith("3.1") || version.startsWith("3.0")) {
			final String exceptionMessage = "My error context";
			final String action = "function (params) {" + "throw '" + exceptionMessage + "';" + "}";
			try {
				db.transaction(action, VPackSlice.class, null).join();
				fail();
			} catch (final CompletionException e) {
				final Throwable cause = e.getCause();
				if (cause instanceof ArangoDBException) {
					assertTrue(((ArangoDBException) cause).getException().equals(exceptionMessage));
				} else {
					fail("Root cause expected to be an ArangoDBException");
				}
			}
		}
	}

	@Test(expected = ArangoDBException.class)
	public void getDocumentWrongId() throws InterruptedException, ExecutionException {
		db.getDocument("123", BaseDocument.class).get();
	}

	@Test
	public void reloadRouting() throws InterruptedException, ExecutionException {
		db.reloadRouting().get();
	}
}
