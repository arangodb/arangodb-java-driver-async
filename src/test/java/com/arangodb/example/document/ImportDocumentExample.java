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

package com.arangodb.example.document;

import com.arangodb.entity.DocumentImportEntity;
import com.arangodb.example.ExampleBase;
import com.arangodb.model.DocumentImportOptions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Mark Vollmary
 */
public class ImportDocumentExample extends ExampleBase {

    private static final Logger log = LoggerFactory.getLogger(ImportDocumentExample.class);

    @Test
    public void importDocument() {
        List<CompletableFuture<DocumentImportEntity>> completableFutures =
                IntStream.range(0, 100)
                        .mapToObj(i -> IntStream.range(0, 500)
                                .mapToObj(it -> new TestEntity(UUID.randomUUID().toString())).collect(Collectors.toList())
                        )
                        .map(p -> collection.importDocuments(p, new DocumentImportOptions()))
                        .collect(Collectors.toList());

        completableFutures.forEach(cf -> {
            try {
                log.info(String.valueOf(cf.get().getCreated()));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

    }

}
