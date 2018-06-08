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

import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Mark Vollmary
 *
 */
public abstract class BaseTest {

	protected static final String TEST_DB = "java_driver_test_db";
	protected static ArangoDBAsync arangoDB;
	protected static ArangoDatabaseAsync db;

	@BeforeClass
	public static void init() throws InterruptedException, ExecutionException {
		if (arangoDB == null) {
			arangoDB = new ArangoDBAsync.Builder().build();
		}
		try {
			arangoDB.db(TEST_DB).drop().get();
		} catch (final Exception e) {
		}
		arangoDB.createDatabase(TEST_DB).get();
		BaseTest.db = arangoDB.db(TEST_DB);
	}

	@AfterClass
	public static void shutdown() throws InterruptedException, ExecutionException {
		arangoDB.db(TEST_DB).drop().get();
		arangoDB.shutdown();
		arangoDB = null;
	}

	protected boolean requireVersion(final int major, final int minor) throws InterruptedException, ExecutionException {
		final String[] split = arangoDB.getVersion().get().getVersion().split("\\.");
		return Integer.valueOf(split[0]) >= major && Integer.valueOf(split[1]) >= minor;
	}
}
