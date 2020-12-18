/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.migration.validator.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResultWriter;
import org.slf4j.Logger;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import static org.fcrepo.migration.validator.impl.ValidationResultUtils.resolvePathToJsonResult;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A file-system based result writer
 *
 * @author dbernstein
 */
public class FileSystemValidationResultWriter implements ValidationResultWriter {

    private static final Logger LOGGER = getLogger(FileSystemValidationResultWriter.class);

    private Path validationRoot;


    /**
     * Constructor
     *
     * @param validationRoot The root of validation report associated with the run
     */
    public FileSystemValidationResultWriter(final Path validationRoot) {
        this.validationRoot = validationRoot;
        this.validationRoot.toFile().mkdirs();
    }

    @Override
    public void write(final List<ValidationResult> results) {
        final var objectMapper = new ObjectMapper();
        for (final var result : results) {
            LOGGER.info("Writing of results here: {}", result);
            final var jsonFilePath = this.validationRoot.resolve(resolvePathToJsonResult(result));
            final var file = jsonFilePath.toFile();
            file.getParentFile().mkdirs();
            try (final var writer = new FileWriter(file)) {
                final var resultStr = objectMapper.writeValueAsString(result);
                writer.write(resultStr);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
