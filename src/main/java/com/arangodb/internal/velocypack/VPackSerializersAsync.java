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
import java.util.Date;

import com.arangodb.velocypack.VPackSerializer;
import com.arangodb.velocypack.internal.VPackSerializers;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class VPackSerializersAsync {

	public static VPackSerializer<Instant> INSTANT = (builder, attribute, value, context) -> {
		VPackSerializers.DATE.serialize(builder, attribute, Date.from(value), context);
	};
	public static VPackSerializer<LocalDate> LOCAL_DATE = (builder, attribute, value, context) -> {
		INSTANT.serialize(builder, attribute, value.atStartOfDay(ZoneId.systemDefault()).toInstant(), context);
	};
	public static VPackSerializer<LocalDateTime> LOCAL_DATE_TIME = (builder, attribute, value, context) -> {
		INSTANT.serialize(builder, attribute, value.atZone(ZoneId.systemDefault()).toInstant(), context);
	};

}
