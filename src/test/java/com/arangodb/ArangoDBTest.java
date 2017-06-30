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

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.LogEntity;
import com.arangodb.entity.LogLevel;
import com.arangodb.entity.LogLevelEntity;
import com.arangodb.entity.UserEntity;
import com.arangodb.model.LogOptions;
import com.arangodb.model.LogOptions.SortOrder;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.RequestType;
import com.arangodb.velocystream.Response;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoDBTest {

	private static final String ROOT = "root";
	private static final String USER = "mit dem mund";
	private static final String PW = "machts der hund";

	@Test
	public void getVersion() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<ArangoDBVersion> f = arangoDB.getVersion();
		assertThat(f, is(notNullValue()));
		f.whenComplete((version, ex) -> {
			assertThat(version, is(notNullValue()));
			assertThat(version.getServer(), is(notNullValue()));
			assertThat(version.getVersion(), is(notNullValue()));
		});
		f.get();
	}

	@Test
	public void createDatabase() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<Boolean> f = arangoDB.createDatabase(BaseTest.TEST_DB);
		assertThat(f, is(notNullValue()));
		f.whenComplete((result, ex) -> {
			assertThat(result, is(true));
		});
		f.get();
		try {
			arangoDB.db(BaseTest.TEST_DB).drop().get();
		} catch (final ArangoDBException e) {
		}
	}

	@Test
	public void deleteDatabase() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final Boolean resultCreate = arangoDB.createDatabase(BaseTest.TEST_DB).get();
		assertThat(resultCreate, is(true));
		final CompletableFuture<Boolean> f = arangoDB.db(BaseTest.TEST_DB).drop();
		assertThat(f, is(notNullValue()));
		f.whenComplete((resultDelete, ex) -> {
			assertThat(resultDelete, is(true));
		});
		f.get();
	}

	@Test
	public void getDatabases() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		try {
			Collection<String> dbs = arangoDB.getDatabases().get();
			assertThat(dbs, is(notNullValue()));
			assertThat(dbs.size(), is(greaterThan(0)));
			final int dbCount = dbs.size();
			assertThat(dbs.iterator().next(), is("_system"));
			arangoDB.createDatabase(BaseTest.TEST_DB).get();
			dbs = arangoDB.getDatabases().get();
			assertThat(dbs.size(), is(greaterThan(dbCount)));
			assertThat(dbs, hasItem("_system"));
			assertThat(dbs, hasItem(BaseTest.TEST_DB));
		} finally {
			arangoDB.db(BaseTest.TEST_DB).drop().get();
		}
	}

	@Test
	public void getAccessibleDatabases() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<Collection<String>> f = arangoDB.getAccessibleDatabases();
		assertThat(f, is(notNullValue()));
		f.whenComplete((dbs, ex) -> {
			assertThat(dbs, is(notNullValue()));
			assertThat(dbs.size(), greaterThan(0));
			assertThat(dbs, hasItem("_system"));
		});
		f.get();
	}

	@Test
	public void getAccessibleDatabasesFor() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<Collection<String>> f = arangoDB.getAccessibleDatabasesFor("root");
		assertThat(f, is(notNullValue()));
		f.whenComplete((dbs, ex) -> {
			assertThat(dbs, is(notNullValue()));
			assertThat(dbs, is(notNullValue()));
			assertThat(dbs.size(), greaterThan(0));
			assertThat(dbs, hasItem("_system"));
		});
		f.get();
	}

	@Test
	public void createUser() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		try {
			final CompletableFuture<UserEntity> f = arangoDB.createUser(USER, PW, null);
			assertThat(f, is(notNullValue()));
			f.whenComplete((result, ex) -> {
				assertThat(result, is(notNullValue()));
				assertThat(result.getUser(), is(USER));
				assertThat(result.getChangePassword(), is(false));
			});
			f.get();
		} finally {
			arangoDB.deleteUser(USER).get();
		}
	}

	@Test
	public void deleteUser() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		arangoDB.createUser(USER, PW, null).get();
		arangoDB.deleteUser(USER).get();
	}

	@Test
	public void getUserRoot() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<UserEntity> f = arangoDB.getUser(ROOT);
		assertThat(f, is(notNullValue()));
		f.whenComplete((user, ex) -> {
			assertThat(user, is(notNullValue()));
			assertThat(user.getUser(), is(ROOT));
		});
		f.get();
	}

	@Test
	public void getUser() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		try {
			arangoDB.createUser(USER, PW, null).get();
			final CompletableFuture<UserEntity> f = arangoDB.getUser(USER);
			assertThat(f, is(notNullValue()));
			f.whenComplete((user, ex) -> {
				assertThat(user.getUser(), is(USER));
			});
			f.get();
		} finally {
			arangoDB.deleteUser(USER).get();
		}

	}

	@Test
	public void getUsersOnlyRoot() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<Collection<UserEntity>> f = arangoDB.getUsers();
		assertThat(f, is(notNullValue()));
		f.whenComplete((users, ex) -> {
			assertThat(users, is(notNullValue()));
			assertThat(users.size(), greaterThan(0));
		});
		f.get();
	}

	@Test
	public void getUsers() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		try {
			arangoDB.createUser(USER, PW, null).get();
			final CompletableFuture<Collection<UserEntity>> f = arangoDB.getUsers();
			assertThat(f, is(notNullValue()));
			f.whenComplete((users, ex) -> {
				assertThat(users, is(notNullValue()));
				assertThat(users.size(), is(2));
				for (final UserEntity user : users) {
					assertThat(user.getUser(), anyOf(is(ROOT), is(USER)));
				}
			});
			f.get();
		} finally {
			arangoDB.deleteUser(USER).get();
		}
	}

	@Test
	public void updateUserNoOptions() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		try {
			arangoDB.createUser(USER, PW, null).get();
			arangoDB.updateUser(USER, null).get();
		} finally {
			arangoDB.deleteUser(USER).get();
		}
	}

	@Test
	public void updateUser() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		try {
			final Map<String, Object> extra = new HashMap<>();
			extra.put("hund", false);
			arangoDB.createUser(USER, PW, new UserCreateOptions().extra(extra)).get();
			extra.put("hund", true);
			extra.put("mund", true);
			{
				final CompletableFuture<UserEntity> f = arangoDB.updateUser(USER, new UserUpdateOptions().extra(extra));
				assertThat(f, is(notNullValue()));
				f.whenComplete((user, ex) -> {
					assertThat(user, is(notNullValue()));
					assertThat(user.getExtra().size(), is(2));
					assertThat(user.getExtra().get("hund"), is(notNullValue()));
					assertThat(Boolean.valueOf(String.valueOf(user.getExtra().get("hund"))), is(true));
				});
				f.get();
			}
			final CompletableFuture<UserEntity> f = arangoDB.getUser(USER);
			assertThat(f, is(notNullValue()));
			f.whenComplete((user2, ex) -> {
				assertThat(user2.getExtra().size(), is(2));
				assertThat(user2.getExtra().get("hund"), is(notNullValue()));
				assertThat(Boolean.valueOf(String.valueOf(user2.getExtra().get("hund"))), is(true));
			});
			f.get();
		} finally {
			arangoDB.deleteUser(USER).get();
		}
	}

	@Test
	public void replaceUser() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		try {
			final Map<String, Object> extra = new HashMap<>();
			extra.put("hund", false);
			arangoDB.createUser(USER, PW, new UserCreateOptions().extra(extra)).get();
			extra.remove("hund");
			extra.put("mund", true);
			{
				final CompletableFuture<UserEntity> f = arangoDB.replaceUser(USER,
					new UserUpdateOptions().extra(extra));
				assertThat(f, is(notNullValue()));
				f.whenComplete((user, ex) -> {
					assertThat(user, is(notNullValue()));
					assertThat(user.getExtra().size(), is(1));
					assertThat(user.getExtra().get("mund"), is(notNullValue()));
					assertThat(Boolean.valueOf(String.valueOf(user.getExtra().get("mund"))), is(true));
				});
				f.get();
			}
			{
				final CompletableFuture<UserEntity> f = arangoDB.getUser(USER);
				assertThat(f, is(notNullValue()));
				f.whenComplete((user2, ex) -> {
					assertThat(user2.getExtra().size(), is(1));
					assertThat(user2.getExtra().get("mund"), is(notNullValue()));
					assertThat(Boolean.valueOf(String.valueOf(user2.getExtra().get("mund"))), is(true));
				});
				f.get();
			}
		} finally {
			arangoDB.deleteUser(USER).get();
		}
	}

	@Test
	public void authenticationFailPassword() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().password("no").build();
		try {
			arangoDB.getVersion().get();
			fail();
		} catch (final ArangoDBException e) {

		}
	}

	@Test
	public void authenticationFailUser() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().user("no").build();
		try {
			arangoDB.getVersion().get();
			fail();
		} catch (final ArangoDBException e) {

		}
	}

	@Test
	public void execute() throws VPackException, InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<Response> f = arangoDB
				.execute(new Request("_system", RequestType.GET, "/_api/version"));
		assertThat(f, is(notNullValue()));
		f.whenComplete((response, ex) -> {
			assertThat(response.getBody(), is(notNullValue()));
			assertThat(response.getBody().get("version").isString(), is(true));
		});
		f.get();
	}

	@Test
	public void getLogs() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<LogEntity> f = arangoDB.getLogs(null);
		assertThat(f, is(notNullValue()));
		f.whenComplete((logs, ex) -> {
			assertThat(logs, is(notNullValue()));
			assertThat(logs.getTotalAmount(), greaterThan(0L));
			assertThat((long) logs.getLid().size(), is(logs.getTotalAmount()));
			assertThat((long) logs.getLevel().size(), is(logs.getTotalAmount()));
			assertThat((long) logs.getTimestamp().size(), is(logs.getTotalAmount()));
			assertThat((long) logs.getText().size(), is(logs.getTotalAmount()));
		});
		f.get();
	}

	@Test
	public void getLogsUpto() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final LogEntity logs = arangoDB.getLogs(null).get();
		final CompletableFuture<LogEntity> f = arangoDB.getLogs(new LogOptions().upto(LogLevel.WARNING));
		assertThat(f, is(notNullValue()));
		f.whenComplete((logsUpto, ex) -> {
			assertThat(logsUpto, is(notNullValue()));
			assertThat(logs.getTotalAmount() >= logsUpto.getTotalAmount(), is(true));
			assertThat(logsUpto.getLevel(), not(contains(LogLevel.INFO)));
		});
		f.get();
	}

	@Test
	public void getLogsLevel() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final LogEntity logs = arangoDB.getLogs(null).get();
		final CompletableFuture<LogEntity> f = arangoDB.getLogs(new LogOptions().level(LogLevel.INFO));
		assertThat(f, is(notNullValue()));
		f.whenComplete((logsInfo, ex) -> {
			assertThat(logsInfo, is(notNullValue()));
			assertThat(logs.getTotalAmount() >= logsInfo.getTotalAmount(), is(true));
			assertThat(logsInfo.getLevel(), everyItem(is(LogLevel.INFO)));
		});
		f.get();
	}

	@Test
	public void getLogsStart() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final LogEntity logs = arangoDB.getLogs(null).get();
		assertThat(logs.getLid(), not(empty()));
		final CompletableFuture<LogEntity> f = arangoDB.getLogs(new LogOptions().start(logs.getLid().get(0) + 1));
		assertThat(f, is(notNullValue()));
		f.whenComplete((logsStart, ex) -> {
			assertThat(logsStart, is(notNullValue()));
			assertThat(logsStart.getLid(), not(contains(logs.getLid().get(0))));
		});
		f.get();
	}

	@Test
	public void getLogsSize() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final LogEntity logs = arangoDB.getLogs(null).get();
		assertThat(logs.getLid().size(), greaterThan(0));
		final CompletableFuture<LogEntity> f = arangoDB.getLogs(new LogOptions().size(logs.getLid().size() - 1));
		assertThat(f, is(notNullValue()));
		f.whenComplete((logsSize, ex) -> {
			assertThat(logsSize, is(notNullValue()));
			assertThat(logsSize.getLid().size(), is(logs.getLid().size() - 1));
		});
		f.get();
	}

	@Test
	public void getLogsOffset() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final LogEntity logs = arangoDB.getLogs(null).get();
		assertThat(logs.getTotalAmount(), greaterThan(0L));
		final CompletableFuture<LogEntity> f = arangoDB
				.getLogs(new LogOptions().offset((int) (logs.getTotalAmount() - 1)));
		assertThat(f, is(notNullValue()));
		f.whenComplete((logsOffset, ex) -> {
			assertThat(logsOffset, is(notNullValue()));
			assertThat(logsOffset.getLid().size(), is(1));
		});
		f.get();
	}

	@Test
	public void getLogsSearch() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final LogEntity logs = arangoDB.getLogs(null).get();
		final CompletableFuture<LogEntity> f = arangoDB.getLogs(new LogOptions().search(BaseTest.TEST_DB));
		assertThat(f, is(notNullValue()));
		f.whenComplete((logsSearch, ex) -> {
			assertThat(logsSearch, is(notNullValue()));
			assertThat(logs.getTotalAmount(), greaterThan(logsSearch.getTotalAmount()));
		});
		f.get();
	}

	@Test
	public void getLogsSortAsc() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<LogEntity> f = arangoDB.getLogs(new LogOptions().sort(SortOrder.asc));
		assertThat(f, is(notNullValue()));
		f.whenComplete((logs, ex) -> {
			assertThat(logs, is(notNullValue()));
			long lastId = -1;
			for (final Long id : logs.getLid()) {
				assertThat(id, greaterThan(lastId));
				lastId = id;
			}
		});
		f.get();
	}

	@Test
	public void getLogsSortDesc() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<LogEntity> f = arangoDB.getLogs(new LogOptions().sort(SortOrder.desc));
		assertThat(f, is(notNullValue()));
		f.whenComplete((logs, ex) -> {
			assertThat(logs, is(notNullValue()));
			long lastId = Long.MAX_VALUE;
			for (final Long id : logs.getLid()) {
				assertThat(lastId, greaterThan(id));
				lastId = id;
			}
		});
		f.get();
	}

	@Test
	public void getLogLevel() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final CompletableFuture<LogLevelEntity> f = arangoDB.getLogLevel();
		assertThat(f, is(notNullValue()));
		f.whenComplete((logLevel, ex) -> {
			assertThat(logLevel, is(notNullValue()));
			assertThat(logLevel.getAgency(), is(LogLevelEntity.LogLevel.INFO));
		});
		f.get();
	}

	@Test
	public void setLogLevel() throws InterruptedException, ExecutionException {
		final ArangoDBAsync arangoDB = new ArangoDBAsync.Builder().build();
		final LogLevelEntity entity = new LogLevelEntity();
		try {
			entity.setAgency(LogLevelEntity.LogLevel.ERROR);
			final CompletableFuture<LogLevelEntity> f = arangoDB.setLogLevel(entity);
			assertThat(f, is(notNullValue()));
			f.whenComplete((logLevel, ex) -> {
				assertThat(logLevel, is(notNullValue()));
				assertThat(logLevel.getAgency(), is(LogLevelEntity.LogLevel.ERROR));
			});
			f.get();
		} finally {
			entity.setAgency(LogLevelEntity.LogLevel.INFO);
			arangoDB.setLogLevel(entity).get();
		}
	}
}
