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

package com.arangodb.internal.velocypack;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.arangodb.velocypack.VPackDeserializer;
import com.arangodb.velocypack.internal.VPackDeserializers;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class VPackDeserializersAsync {

	public static VPackDeserializer<Instant> INSTANT = (parent, vpack, context) -> {
		return VPackDeserializers.DATE.deserialize(parent, vpack, context).toInstant();
	};
	public static VPackDeserializer<LocalDate> LOCAL_DATE = (parent, vpack, context) -> {
		return INSTANT.deserialize(parent, vpack, context).atZone(ZoneId.systemDefault()).toLocalDate();
	};
	public static VPackDeserializer<LocalDateTime> LOCAL_DATE_TIME = (parent, vpack, context) -> {
		return INSTANT.deserialize(parent, vpack, context).atZone(ZoneId.systemDefault()).toLocalDateTime();
	};

}
