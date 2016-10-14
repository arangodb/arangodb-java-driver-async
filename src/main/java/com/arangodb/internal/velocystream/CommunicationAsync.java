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
import com.arangodb.velocypack.VPack;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocypack.exception.VPackParserException;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class CommunicationAsync extends Communication<CompletableFuture<Response>, ConnectionAsync> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommunicationAsync.class);

	public static class Builder {
		private String host;
		private Integer port;
		private Integer timeout;
		private String user;
		private String password;
		private Boolean useSsl;
		private SSLContext sslContext;
		private Integer chunksize;

		public Builder() {
			super();
		}

		public Builder host(final String host) {
			this.host = host;
			return this;
		}

		public Builder port(final Integer port) {
			this.port = port;
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

		public Communication<CompletableFuture<Response>, ConnectionAsync> build(
			final VPack vpack,
			final CollectionCache collectionCache) {
			return new CommunicationAsync(host, port, timeout, user, password, useSsl, sslContext, vpack,
					collectionCache, chunksize);
		}
	}

	private CommunicationAsync(final String host, final Integer port, final Integer timeout, final String user,
		final String password, final Boolean useSsl, final SSLContext sslContext, final VPack vpack,
		final CollectionCache collectionCache, final Integer chunksize) {
		super(host, port, timeout, user, password, useSsl, sslContext, vpack, collectionCache, chunksize,
				new ConnectionAsync.Builder(new MessageStore()).host(host).port(port).timeout(timeout).useSsl(useSsl)
						.sslContext(sslContext).build());
	}

	@Override
	public CompletableFuture<Response> execute(final Request request) {
		connect(connection);
		final CompletableFuture<Response> rfuture = new CompletableFuture<>();
		try {
			final Message message = createMessage(request);
			send(message).whenComplete((m, ex) -> {
				if (m != null) {
					try {
						collectionCache.setDb(request.getDatabase());
						final Response response = createResponse(m);
						if (response.getResponseCode() >= 300) {
							if (response.getBody() != null) {
								final ErrorEntity errorEntity = vpack.deserialize(response.getBody(),
									ErrorEntity.class);
								final String errorMessage = String.format("Response: %s, Error: %s - %s",
									errorEntity.getCode(), errorEntity.getErrorNum(), errorEntity.getErrorMessage());
								rfuture.completeExceptionally(new ArangoDBException(errorMessage));
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

	private CompletableFuture<Message> send(final Message message) throws IOException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Send Message (id=%s, head=%s, body=%s)", message.getId(), message.getHead(),
				message.getBody() != null ? message.getBody() : "{}"));
		}
		return connection.write(message, buildChunks(message));
	}

	@Override
	protected void authenticate() {
		Response response = null;
		try {
			response = execute(
				new AuthenticationRequest(user, password != null ? password : "", ArangoDBConstants.ENCRYPTION_PLAIN))
						.get();
		} catch (final InterruptedException e) {
			throw new ArangoDBException(e);
		} catch (final ExecutionException e) {
			throw new ArangoDBException(e);
		}
		checkError(response);
	}

}
