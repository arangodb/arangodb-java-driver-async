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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.arangodb.ArangoDBAsync;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabaseAsync;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.LogEntity;
import com.arangodb.entity.LogLevelEntity;
import com.arangodb.entity.Permissions;
import com.arangodb.entity.ServerRole;
import com.arangodb.entity.UserEntity;
import com.arangodb.internal.ArangoExecutor.ResponseDeserializer;
import com.arangodb.internal.net.CommunicationProtocol;
import com.arangodb.internal.net.HostResolver;
import com.arangodb.internal.net.HostResolver.EndpointResolver;
import com.arangodb.internal.util.ArangoSerializationFactory;
import com.arangodb.internal.util.ArangoSerializationFactory.Serializer;
import com.arangodb.internal.velocystream.ConnectionAsync;
import com.arangodb.internal.velocystream.VstCommunication;
import com.arangodb.internal.velocystream.VstCommunicationAsync;
import com.arangodb.internal.velocystream.VstCommunicationSync;
import com.arangodb.internal.velocystream.VstProtocol;
import com.arangodb.internal.velocystream.internal.ConnectionSync;
import com.arangodb.model.LogOptions;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.RequestType;
import com.arangodb.velocystream.Response;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoDBAsyncImpl extends
		InternalArangoDB<ArangoExecutorAsync, CompletableFuture<Response>, ConnectionAsync> implements ArangoDBAsync {

	private final CommunicationProtocol cp;

	public ArangoDBAsyncImpl(final VstCommunicationAsync.Builder commBuilder, final ArangoSerializationFactory util,
		final VstCommunicationSync.Builder syncbuilder, final HostResolver hostResolver) {
		super(new ArangoExecutorAsync(commBuilder.build(util.get(Serializer.INTERNAL)), util, new DocumentCache()),
				util);
		final VstCommunication<Response, ConnectionSync> cacheCom = syncbuilder.build(util.get(Serializer.INTERNAL));
		cp = new VstProtocol(cacheCom);
		hostResolver.init(new EndpointResolver() {
			@Override
			public Collection<String> resolve(final boolean closeConnections) throws ArangoDBException {
				try {
					return executor.execute(
						new Request(ArangoDBConstants.SYSTEM, RequestType.GET, ArangoDBConstants.PATH_ENDPOINTS),
						new ResponseDeserializer<Collection<String>>() {
							@Override
							public Collection<String> deserialize(final Response response) throws VPackException {
								final VPackSlice field = response.getBody().get(ArangoDBConstants.ENDPOINTS);
								Collection<String> endpoints;
								if (field.isNone()) {
									endpoints = Collections.<String> emptyList();
								} else {
									final Collection<Map<String, String>> tmp = util().deserialize(field,
										Collection.class);
									endpoints = new ArrayList<>();
									for (final Map<String, String> map : tmp) {
										for (final String value : map.values()) {
											endpoints.add(value);
										}
									}
								}
								return endpoints;
							}
						}, null).get();
				} catch (InterruptedException | ExecutionException e) {
					throw new ArangoDBException(e);
					// TODO
					// if (e.getResponseCode() == 403) {
					// response = Collections.<String> emptyList();
					// } else {
					// throw e;
					// }
				} finally {
					if (closeConnections) {
						ArangoDBAsyncImpl.this.shutdown();
					}
				}
			}
		});
	}

	@Override
	protected ArangoExecutorAsync executor() {
		return executor;
	}

	@Override
	public void shutdown() throws ArangoDBException {
		try {
			executor.disconnect();
			cp.close();
		} catch (final IOException e) {
			throw new ArangoDBException(e);
		}
	}

	@Override
	public ArangoDatabaseAsync db() {
		return db(ArangoDBConstants.SYSTEM);
	}

	@Override
	public ArangoDatabaseAsync db(final String name) {
		return new ArangoDatabaseAsyncImpl(this, name);
	}

	@Override
	public CompletableFuture<Boolean> createDatabase(final String name) {
		return executor.execute(createDatabaseRequest(name), createDatabaseResponseDeserializer());
	}

	@Override
	public CompletableFuture<Collection<String>> getDatabases() {
		return executor.execute(getDatabasesRequest(db().name()), getDatabaseResponseDeserializer());
	}

	@Override
	public CompletableFuture<Collection<String>> getAccessibleDatabases() {
		return db().getAccessibleDatabases();
	}

	@Override
	public CompletableFuture<Collection<String>> getAccessibleDatabasesFor(final String user) {
		return executor.execute(getAccessibleDatabasesForRequest(db().name(), user),
			getAccessibleDatabasesForResponseDeserializer());
	}

	@Override
	public CompletableFuture<ArangoDBVersion> getVersion() {
		return db().getVersion();
	}

	@Override
	public CompletableFuture<ServerRole> getRole() {
		return executor.execute(getRoleRequest(), getRoleResponseDeserializer());
	}

	@Override
	public CompletableFuture<UserEntity> createUser(final String user, final String passwd) {
		return executor.execute(createUserRequest(db().name(), user, passwd, new UserCreateOptions()),
			UserEntity.class);
	}

	@Override
	public CompletableFuture<UserEntity> createUser(
		final String user,
		final String passwd,
		final UserCreateOptions options) {
		return executor.execute(createUserRequest(db().name(), user, passwd, options), UserEntity.class);
	}

	@Override
	public CompletableFuture<Void> deleteUser(final String user) {
		return executor.execute(deleteUserRequest(db().name(), user), Void.class);
	}

	@Override
	public CompletableFuture<UserEntity> getUser(final String user) {
		return executor.execute(getUserRequest(db().name(), user), UserEntity.class);
	}

	@Override
	public CompletableFuture<Collection<UserEntity>> getUsers() {
		return executor.execute(getUsersRequest(db().name()), getUsersResponseDeserializer());
	}

	@Override
	public CompletableFuture<UserEntity> updateUser(final String user, final UserUpdateOptions options) {
		return executor.execute(updateUserRequest(db().name(), user, options), UserEntity.class);
	}

	@Override
	public CompletableFuture<UserEntity> replaceUser(final String user, final UserUpdateOptions options) {
		return executor.execute(replaceUserRequest(db().name(), user, options), UserEntity.class);
	}

	@Override
	public CompletableFuture<Void> grantDefaultDatabaseAccess(final String user, final Permissions permissions)
			throws ArangoDBException {
		return executor.execute(updateUserDefaultDatabaseAccessRequest(user, permissions), Void.class);
	}

	@Override
	public CompletableFuture<Void> grantDefaultCollectionAccess(final String user, final Permissions permissions)
			throws ArangoDBException {
		return executor.execute(updateUserDefaultCollectionAccessRequest(user, permissions), Void.class);
	}

	@Override
	public CompletableFuture<Response> execute(final Request request) {
		return executor.execute(request, response -> response);
	}

	@Override
	public CompletableFuture<LogEntity> getLogs(final LogOptions options) {
		return executor.execute(getLogsRequest(options), LogEntity.class);
	}

	@Override
	public CompletableFuture<LogLevelEntity> getLogLevel() {
		return executor.execute(getLogLevelRequest(), LogLevelEntity.class);
	}

	@Override
	public CompletableFuture<LogLevelEntity> setLogLevel(final LogLevelEntity entity) {
		return executor.execute(setLogLevelRequest(entity), LogLevelEntity.class);
	}
}
