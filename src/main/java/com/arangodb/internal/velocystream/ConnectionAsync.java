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

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;

import javax.net.ssl.SSLContext;

import com.arangodb.internal.HostHandler;
import com.arangodb.internal.velocystream.internal.Chunk;
import com.arangodb.internal.velocystream.internal.Connection;
import com.arangodb.internal.velocystream.internal.Message;
import com.arangodb.internal.velocystream.internal.MessageStore;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ConnectionAsync extends Connection {

	public static class Builder {

		private final HostHandler hostHandler;
		private final MessageStore messageStore;
		private Integer timeout;
		private Boolean useSsl;
		private SSLContext sslContext;

		public Builder(final HostHandler hostHandler, final MessageStore messageStore) {
			super();
			this.hostHandler = hostHandler;
			this.messageStore = messageStore;
		}

		public Builder timeout(final Integer timeout) {
			this.timeout = timeout;
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

		public ConnectionAsync build() {
			return new ConnectionAsync(hostHandler, timeout, useSsl, sslContext, messageStore);
		}
	}

	private ConnectionAsync(final HostHandler hostHandler, final Integer timeout, final Boolean useSsl,
		final SSLContext sslContext, final MessageStore messageStore) {
		super(hostHandler, timeout, useSsl, sslContext, messageStore);
	}

	public synchronized CompletableFuture<Message> write(final Message message, final Collection<Chunk> chunks) {
		final CompletableFuture<Message> future = new CompletableFuture<>();
		final FutureTask<Message> task = new FutureTask<>(new Callable<Message>() {
			@Override
			public Message call() throws Exception {
				try {
					future.complete(messageStore.get(message.getId()));
				} catch (final Exception e) {
					future.completeExceptionally(e);
				}
				return null;
			}
		});
		messageStore.storeMessage(message.getId(), task);
		super.writeIntern(message, chunks);
		return future;
	}

}
