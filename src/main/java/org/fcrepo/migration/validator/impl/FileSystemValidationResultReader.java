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
import org.fcrepo.migration.validator.api.ValidationResultReader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class deserializes ValidationResult JSON files into ValidationResult objects
 *
 * @author awoods
 * @since 2020-12-20
 */
public class FileSystemValidationResultReader implements ValidationResultReader {

    private static final Logger LOGGER = getLogger(FileSystemValidationResultReader.class);

    /**
     * This method reads the validation result from disk
     *
     * @param validationResultFile to read
     */
    @Override
    public ValidationResult read(final File validationResultFile) {
        final var objectMapper = new ObjectMapper();

        LOGGER.info("Reading result from here: {}", validationResultFile);
        try (final var reader = new FileReader(validationResultFile)) {
            return objectMapper.readValue(reader, ValidationResult.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
