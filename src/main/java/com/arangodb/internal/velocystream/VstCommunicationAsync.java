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

package com.arangodb.internal.velocystream;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoDBException;
import com.arangodb.entity.ErrorEntity;
import com.arangodb.internal.ArangoDBConstants;
import com.arangodb.internal.CollectionCache;
import com.arangodb.internal.Host;
import com.arangodb.internal.net.ConnectionPool;
import com.arangodb.internal.net.DelHostHandler;
import com.arangodb.internal.net.HostHandler;
import com.arangodb.internal.velocystream.internal.AuthenticationRequest;
import com.arangodb.internal.velocystream.internal.Message;
import com.arangodb.internal.velocystream.internal.MessageStore;
import com.arangodb.util.ArangoSerialization;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocypack.exception.VPackParserException;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;

/**
 * @author Mark Vollmary
 *
 */
public class VstCommunicationAsync extends VstCommunication<CompletableFuture<Response>, ConnectionAsync> {

	private static final Logger LOGGER = LoggerFactory.getLogger(VstCommunicationAsync.class);
	private final CollectionCache collectionCache;

	public static class Builder {

		private final HostHandler hostHandler;
		private Integer timeout;
		private Long connectionTtl;
		private String user;
		private String password;
		private Boolean useSsl;
		private SSLContext sslContext;
		private Integer chunksize;
		private Integer maxConnections;

		public Builder(final HostHandler hostHandler) {
			super();
			this.hostHandler = hostHandler;
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

		public Builder maxConnections(final Integer maxConnections) {
			this.maxConnections = maxConnections;
			return this;
		}

		public Builder connectionTtl(final Long connectionTtl) {
			this.connectionTtl = connectionTtl;
			return this;
		}

		public VstCommunicationAsync build(final ArangoSerialization util, final CollectionCache collectionCache) {
			return new VstCommunicationAsync(hostHandler, timeout, user, password, useSsl, sslContext, util,
					collectionCache, chunksize, maxConnections, connectionTtl);
		}
	}

	private VstCommunicationAsync(final HostHandler hostHandler, final Integer timeout, final String user,
		final String password, final Boolean useSsl, final SSLContext sslContext, final ArangoSerialization util,
		final CollectionCache collectionCache, final Integer chunksize, final Integer maxConnections,
		final Long connectionTtl) {
		super(timeout, user, password, useSsl, sslContext, util, chunksize, new ConnectionPool<ConnectionAsync>(
				maxConnections != null ? Math.max(1, maxConnections) : ArangoDBConstants.MAX_CONNECTIONS_VST_DEFAULT) {
			private final ConnectionAsync.Builder builder = new ConnectionAsync.Builder(new MessageStore())
					.timeout(timeout).ttl(connectionTtl).useSsl(useSsl).sslContext(sslContext);

			@Override
			public ConnectionAsync createConnection(final Host host) {
				return builder.hostHandler(new DelHostHandler(hostHandler, host)).build();
			}
		});
		this.collectionCache = collectionCache;
	}

	@Override
	protected CompletableFuture<Response> execute(final Request request, final ConnectionAsync connection) {
		connect(connection);
		final CompletableFuture<Response> rfuture = new CompletableFuture<>();
		try {
			final Message message = createMessage(request);
			send(message, connection).whenComplete((m, ex) -> {
				if (m != null) {
					try {
						collectionCache.setDb(request.getDatabase());
						final Response response = createResponse(m);
						if (response.getResponseCode() >= 300) {
							if (response.getBody() != null) {
								final ErrorEntity errorEntity = util.deserialize(response.getBody(), ErrorEntity.class);
								rfuture.completeExceptionally(new ArangoDBException(errorEntity));
							} else {
								rfuture.completeExceptionally(new ArangoDBException(
										String.format("Response Code: %s", response.getResponseCode())));
							}
						} else {
							rfuture.complete(response);
						}
					} catch (final VPackParserException e) {
						LOGGER.error(e.getMessage(), e);
						rfuture.completeExceptionally(e);
					}
				} else if (ex != null) {
					LOGGER.error(ex.getMessage(), ex);
					rfuture.completeExceptionally(ex);
				} else {
					rfuture.cancel(true);
				}
			});
		} catch (final IOException | VPackException e) {
			LOGGER.error(e.getMessage(), e);
			rfuture.completeExceptionally(e);
		}
		return rfuture;
	}

	private CompletableFuture<Message> send(final Message message, final ConnectionAsync connection)
			throws IOException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Send Message (id=%s, head=%s, body=%s)", message.getId(), message.getHead(),
				message.getBody() != null ? message.getBody() : "{}"));
		}
		return connection.write(message, buildChunks(message));
	}

	@Override
	protected void authenticate(final ConnectionAsync connection) {
		Response response = null;
		try {
			response = execute(
				new AuthenticationRequest(user, password != null ? password : "", ArangoDBConstants.ENCRYPTION_PLAIN),
				connection).get();
		} catch (final InterruptedException e) {
			throw new ArangoDBException(e);
		} catch (final ExecutionException e) {
			throw new ArangoDBException(e);
		}
		checkError(response);
	}

}
