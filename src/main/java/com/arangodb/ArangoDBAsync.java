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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;

import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.LogEntity;
import com.arangodb.entity.LogLevelEntity;
import com.arangodb.entity.UserEntity;
import com.arangodb.internal.ArangoDBConstants;
import com.arangodb.internal.ArangoExecutorAsync;
import com.arangodb.internal.CollectionCache;
import com.arangodb.internal.CollectionCache.DBAccess;
import com.arangodb.internal.DocumentCache;
import com.arangodb.internal.InternalArangoDB;
import com.arangodb.internal.velocypack.VPackConfigure;
import com.arangodb.internal.velocypack.VPackConfigureAsync;
import com.arangodb.internal.velocystream.Communication;
import com.arangodb.internal.velocystream.CommunicationAsync;
import com.arangodb.internal.velocystream.CommunicationSync;
import com.arangodb.internal.velocystream.ConnectionAsync;
import com.arangodb.internal.velocystream.ConnectionSync;
import com.arangodb.internal.velocystream.DefaultHostHandler;
import com.arangodb.internal.velocystream.Host;
import com.arangodb.internal.velocystream.HostHandler;
import com.arangodb.model.LogOptions;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;
import com.arangodb.velocypack.VPack;
import com.arangodb.velocypack.VPackAnnotationFieldFilter;
import com.arangodb.velocypack.VPackAnnotationFieldNaming;
import com.arangodb.velocypack.VPackDeserializer;
import com.arangodb.velocypack.VPackInstanceCreator;
import com.arangodb.velocypack.VPackJsonDeserializer;
import com.arangodb.velocypack.VPackJsonSerializer;
import com.arangodb.velocypack.VPackParser;
import com.arangodb.velocypack.VPackSerializer;
import com.arangodb.velocypack.ValueType;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoDBAsync extends InternalArangoDB<ArangoExecutorAsync, CompletableFuture<Response>, ConnectionAsync> {

	public static class Builder {

		private final List<Host> hosts;
		private Host host;
		private Integer timeout;
		private String user;
		private String password;
		private Boolean useSsl;
		private SSLContext sslContext;
		private Integer chunksize;
		private final VPack.Builder vpackBuilder;
		private final CollectionCache collectionCache;
		private final VPackParser vpackParser;

		public Builder() {
			super();
			vpackBuilder = new VPack.Builder();
			collectionCache = new CollectionCache();
			vpackParser = new VPackParser();
			VPackConfigure.configure(vpackBuilder, vpackParser, collectionCache);
			VPackConfigureAsync.configure(vpackBuilder);
			host = new Host(ArangoDBConstants.DEFAULT_HOST, ArangoDBConstants.DEFAULT_PORT);
			hosts = new ArrayList<>();
			loadProperties(ArangoDBAsync.class.getResourceAsStream(DEFAULT_PROPERTY_FILE));
		}

		public Builder loadProperties(final InputStream in) {
			if (in != null) {
				final Properties properties = new Properties();
				try {
					properties.load(in);
					loadHosts(properties, this.hosts);
					final String host = loadHost(properties, this.host.getHost());
					final int port = loadPort(properties, this.host.getPort());
					this.host = new Host(host, port);
					timeout = loadTimeout(properties, timeout);
					user = loadUser(properties, user);
					password = loadPassword(properties, password);
					useSsl = loadUseSsl(properties, useSsl);
					chunksize = loadChunkSize(properties, chunksize);
				} catch (final IOException e) {
					throw new ArangoDBException(e);
				}
			}
			return this;
		}

		/**
		 * @deprecated will be removed in version 4.2.0 use {@link #host(String, int)} instead
		 * 
		 * @param host
		 * @return
		 */
		@Deprecated
		public Builder host(final String host) {
			this.host = new Host(host, this.host.getPort());
			return this;
		}

		/**
		 * @deprecated will be removed in version 4.2.0 use {@link #host(String, int)} instead
		 * 
		 * @param port
		 * @return
		 */
		@Deprecated
		public Builder port(final Integer port) {
			host = new Host(host.getHost(), port);
			return this;
		}

		/**
		 * Adds a host to connect to. Multiple hosts can be added to provide fallbacks.
		 * 
		 * @param host
		 *            address of the host
		 * @param port
		 *            port of the host
		 * @return {@link ArangoDB.Builder}
		 */
		public Builder host(final String host, final int port) {
			hosts.add(new Host(host, port));
			return this;
		}

		public Builder timeout(final Integer timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder user(final String user) {
			this.user = user;
			return this;
		}

		public Builder password(final String password) {
			this.password = password;
			return this;
		}

		public Builder useSsl(final Boolean useSsl) {
			this.useSsl = useSsl;
			return this;
		}

		public Builder sslContext(final SSLContext sslContext) {
			this.sslContext = sslContext;
			return this;
		}

		public Builder chunksize(final Integer chunksize) {
			this.chunksize = chunksize;
			return this;
		}

		public <T> Builder registerSerializer(final Class<T> clazz, final VPackSerializer<T> serializer) {
			vpackBuilder.registerSerializer(clazz, serializer);
			return this;
		}

		/**
		 * Register a special serializer for a member class which can only be identified by its enclosing class.
		 * 
		 * @param clazz
		 *            type of the enclosing class
		 * @param serializer
		 *            serializer to register
		 * @return builder
		 */
		public <T> Builder registerEnclosingSerializer(final Class<T> clazz, final VPackSerializer<T> serializer) {
			vpackBuilder.registerEnclosingSerializer(clazz, serializer);
			return this;
		}

		public <T> Builder registerDeserializer(final Class<T> clazz, final VPackDeserializer<T> deserializer) {
			vpackBuilder.registerDeserializer(clazz, deserializer);
			return this;
		}

		public <T> Builder registerInstanceCreator(final Class<T> clazz, final VPackInstanceCreator<T> creator) {
			vpackBuilder.registerInstanceCreator(clazz, creator);
			return this;
		}

		public Builder registerJsonDeserializer(final ValueType type, final VPackJsonDeserializer deserializer) {
			vpackParser.registerDeserializer(type, deserializer);
			return this;
		}

		public Builder registerJsonDeserializer(
			final String attribute,
			final ValueType type,
			final VPackJsonDeserializer deserializer) {
			vpackParser.registerDeserializer(attribute, type, deserializer);
			return this;
		}

		public <T> Builder registerJsonSerializer(final Class<T> clazz, final VPackJsonSerializer<T> serializer) {
			vpackParser.registerSerializer(clazz, serializer);
			return this;
		}

		public <T> Builder registerJsonSerializer(
			final String attribute,
			final Class<T> clazz,
			final VPackJsonSerializer<T> serializer) {
			vpackParser.registerSerializer(attribute, clazz, serializer);
			return this;
		}

		public <T extends Annotation> Builder annotationFieldFilter(
			final Class<T> type,
			final VPackAnnotationFieldFilter<T> fieldFilter) {
			vpackBuilder.annotationFieldFilter(type, fieldFilter);
			return this;
		}

		public <T extends Annotation> Builder annotationFieldNaming(
			final Class<T> type,
			final VPackAnnotationFieldNaming<T> fieldNaming) {
			vpackBuilder.annotationFieldNaming(type, fieldNaming);
			return this;
		}

		public ArangoDBAsync build() {
			if (hosts.isEmpty()) {
				hosts.add(host);
			}
			final HostHandler hostHandler = new DefaultHostHandler(hosts);
			return new ArangoDBAsync(asyncBuilder(hostHandler), vpackBuilder.build(),
					vpackBuilder.serializeNullValues(true).build(), vpackParser, collectionCache,
					syncBuilder(hostHandler));
		}

		private CommunicationAsync.Builder asyncBuilder(final HostHandler hostHandler) {
			return new CommunicationAsync.Builder(hostHandler).timeout(timeout).user(user).password(password)
					.useSsl(useSsl).sslContext(sslContext).chunksize(chunksize);
		}

		private CommunicationSync.Builder syncBuilder(final HostHandler hostHandler) {
			return new CommunicationSync.Builder(hostHandler).timeout(timeout).user(user).password(password)
					.useSsl(useSsl).sslContext(sslContext).chunksize(chunksize);
		}

	}

	public ArangoDBAsync(final CommunicationAsync.Builder commBuilder, final VPack vpack, final VPack vpackNull,
		final VPackParser vpackParser, final CollectionCache collectionCache,
		final CommunicationSync.Builder syncbuilder) {
		super(new ArangoExecutorAsync(commBuilder.build(vpack, collectionCache), vpack, vpackNull, vpackParser,
				new DocumentCache(), collectionCache));
		final Communication<Response, ConnectionSync> cacheCom = syncbuilder.build(vpack, collectionCache);
		collectionCache.init(new DBAccess() {
			@Override
			public ArangoDatabase db(final String name) {
				return new ArangoDatabase(cacheCom, vpackNull, vpack, vpackParser, executor.documentCache(), null,
						name);
			}
		});
	}

	protected ArangoExecutorAsync executor() {
		return executor;
	}

	public void shutdown() {
		executor.communication().disconnect();
	}

	/**
	 * Returns a handler of the system database
	 * 
	 * @return database handler
	 */
	public ArangoDatabaseAsync db() {
		return db(ArangoDBConstants.SYSTEM);
	}

	/**
	 * Returns a handler of the database by the given name
	 * 
	 * @param name
	 *            Name of the database
	 * @return database handler
	 */
	public ArangoDatabaseAsync db(final String name) {
		return new ArangoDatabaseAsync(this, name);
	}

	/**
	 * Creates a new database
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#create-database">API
	 *      Documentation</a>
	 * @param name
	 *            Has to contain a valid database name
	 * @return true if the database was created successfully.
	 */
	public CompletableFuture<Boolean> createDatabase(final String name) {
		return executor.execute(createDatabaseRequest(name), createDatabaseResponseDeserializer());
	}

	/**
	 * Retrieves a list of all existing databases
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#list-of-databases">API
	 *      Documentation</a>
	 * @return a list of all existing databases
	 */
	public CompletableFuture<Collection<String>> getDatabases() {
		return executor.execute(getDatabasesRequest(db().name()), getDatabaseResponseDeserializer());
	}

	/**
	 * Retrieves a list of all databases the current user can access
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#list-of-accessible-databases">API
	 *      Documentation</a>
	 * @return a list of all databases the current user can access
	 */
	public CompletableFuture<Collection<String>> getAccessibleDatabases() {
		return executor.execute(getAccessibleDatabasesRequest(db().name()), getDatabaseResponseDeserializer());
	}

	/**
	 * List available database to the specified user
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/UserManagement/index.html#list-the-databases-available-to-a-user">API
	 *      Documentation</a>
	 * @param user
	 *            The name of the user for which you want to query the databases
	 * @return
	 * @throws ArangoDBException
	 */
	public CompletableFuture<Collection<String>> getAccessibleDatabasesFor(final String user) {
		return executor.execute(getAccessibleDatabasesForRequest(db().name(), user),
			getAccessibleDatabasesForResponseDeserializer());
	}

	/**
	 * Returns the server name and version number.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/MiscellaneousFunctions/index.html#return-server-version">API
	 *      Documentation</a>
	 * @return the server version, number
	 */
	public CompletableFuture<ArangoDBVersion> getVersion() {
		return executor.execute(getVersionRequest(), ArangoDBVersion.class);
	}

	/**
	 * Create a new user. This user will not have access to any database. You need permission to the _system database in
	 * order to execute this call.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#create-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param passwd
	 *            The user password
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> createUser(final String user, final String passwd) {
		return executor.execute(createUserRequest(db().name(), user, passwd, new UserCreateOptions()),
			UserEntity.class);
	}

	/**
	 * Create a new user. This user will not have access to any database. You need permission to the _system database in
	 * order to execute this call.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#create-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param passwd
	 *            The user password
	 * @param options
	 *            Additional properties of the user, can be null
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> createUser(
		final String user,
		final String passwd,
		final UserCreateOptions options) {
		return executor.execute(createUserRequest(db().name(), user, passwd, options), UserEntity.class);
	}

	/**
	 * Removes an existing user, identified by user. You need access to the _system database.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#remove-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return void
	 */
	public CompletableFuture<Void> deleteUser(final String user) {
		return executor.execute(deleteUserRequest(db().name(), user), Void.class);
	}

	/**
	 * Fetches data about the specified user. You can fetch information about yourself or you need permission to the
	 * _system database in order to execute this call.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#fetch-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> getUser(final String user) {
		return executor.execute(getUserRequest(db().name(), user), UserEntity.class);
	}

	/**
	 * Fetches data about all users. You can only execute this call if you have access to the _system database.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#list-available-users">API
	 *      Documentation</a>
	 * @return informations about all users
	 */
	public CompletableFuture<Collection<UserEntity>> getUsers() {
		return executor.execute(getUsersRequest(db().name()), getUsersResponseDeserializer());
	}

	/**
	 * Partially updates the data of an existing user. The name of an existing user must be specified in user. You can
	 * only change the password of your self. You need access to the _system database to change the active flag.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#update-user">API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param options
	 *            Properties of the user to be changed
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> updateUser(final String user, final UserUpdateOptions options) {
		return executor.execute(updateUserRequest(db().name(), user, options), UserEntity.class);
	}

	/**
	 * Replaces the data of an existing user. The name of an existing user must be specified in user. You can only
	 * change the password of your self. You need access to the _system database to change the active flag.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/UserManagement/index.html#replace-user">API
	 *      Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param options
	 *            Additional properties of the user, can be null
	 * @return information about the user
	 */
	public CompletableFuture<UserEntity> replaceUser(final String user, final UserUpdateOptions options) {
		return executor.execute(replaceUserRequest(db().name(), user, options), UserEntity.class);
	}

	/**
	 * Generic Execute. Use this method to execute custom FOXX services.
	 * 
	 * @param request
	 *            VelocyStream request
	 * @return VelocyStream response
	 * @throws ArangoDBException
	 */
	public CompletableFuture<Response> execute(final Request request) {
		return executor.execute(request, response -> response);
	}

	/**
	 * Returns fatal, error, warning or info log messages from the server's global log.
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AdministrationAndMonitoring/index.html#read-global-logs-from-the-server">API
	 *      Documentation</a>
	 * @param options
	 *            Additional options, can be null
	 * @return the log messages
	 */
	public CompletableFuture<LogEntity> getLogs(final LogOptions options) {
		return executor.execute(getLogsRequest(options), LogEntity.class);
	}

	/**
	 * Returns the server's current loglevel settings.
	 * 
	 * @return the server's current loglevel settings
	 * @throws ArangoDBException
	 */
	public CompletableFuture<LogLevelEntity> getLogLevel() {
		return executor.execute(getLogLevelRequest(), LogLevelEntity.class);
	}

	/**
	 * Modifies and returns the server's current loglevel settings.
	 * 
	 * @param entity
	 *            loglevel settings
	 * @return the server's current loglevel settings
	 * @throws ArangoDBException
	 */
	public CompletableFuture<LogLevelEntity> setLogLevel(final LogLevelEntity entity) {
		return executor.execute(setLogLevelRequest(entity), LogLevelEntity.class);
	}
}
