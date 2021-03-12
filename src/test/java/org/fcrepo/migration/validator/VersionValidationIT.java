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
package org.fcrepo.migration.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_HEAD_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author awoods
 * @since 2020-12-14
 */
public class VersionValidationIT extends AbstractValidationIT {

    final File VERSIONS_BASE_DIR = new File(FIXTURES_BASE_DIR, "version-validation-it");

    @Before
    public void setup() throws IOException {
        // Create empty datastream dirs
        FileUtils.forceMkdir(new File(VERSIONS_BASE_DIR, "valid/f3/datastreams"));
        FileUtils.forceMkdir(new File(VERSIONS_BASE_DIR, "invalid-metadata/f3/datastreams"));
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(RESULTS_DIR);
    }

    @Test
    public void test() {
        final var sourceObject = "1711.dl:FEG6WWJ664RHQ8X/DS1";
        final var f3DatastreamsDir = new File(VERSIONS_BASE_DIR, "valid/f3/datastreams");
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "valid/f6/data/ocfl-root");
        final var reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // verify two entries for created date
        final var createdDateMatches = reportHandler.getPassed().stream()
                     .filter(result -> result.getSourceObjectId().equals(sourceObject))
                     .filter(result -> result.getDetails().contains("binary creation dates match"))
                     .count();
        final var lastModifiedMatches = reportHandler.getPassed().stream()
            .filter(result -> result.getSourceObjectId().equals(sourceObject))
            .filter(result -> result.getDetails().contains("last modified dates match"))
            .count();
        assertEquals("Should be two created date validations for DS1", 2, createdDateMatches);
        assertEquals("Should be two last modified date validations for DS1", 2, lastModifiedMatches);
    }

    @Test
    public void testOcflMissingVersion() {
        final var f3DatastreamsDir = new File(VERSIONS_BASE_DIR, "valid/f3/datastreams");
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "ocfl-fewer-versions/f6/data/ocfl-root");
        final var reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        final var errors = reportHandler.getErrors();
        assertThat(errors).hasSize(3)
                          .map(ValidationResult::getValidationType)
                          .containsOnly(BINARY_VERSION_COUNT, BINARY_HEAD_COUNT,
                                        SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET);
    }

    @Test
    public void testF3MissingVersion() {
        final var f3DatastreamsDir = new File(VERSIONS_BASE_DIR, "valid/f3/datastreams");
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "ocfl-more-versions/f6/data/ocfl-root");
        final var reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        final var errors = reportHandler.getErrors();
        assertThat(errors).hasSize(1)
                          .map(ValidationResult::getValidationType)
                          .containsOnly(BINARY_VERSION_COUNT);
    }

    @Test
    public void testInvalidMetadata() {
        final var f3DatastreamsDir = new File(VERSIONS_BASE_DIR, "invalid-metadata/f3/datastreams");
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "invalid-metadata/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "valid/f6/data/ocfl-root");
        final var reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        final var errors = reportHandler.getErrors();
        assertThat(errors).hasSize(4)
                          .map(ValidationResult::getDetails)
                          .anyMatch(details -> details.contains("creation dates do no match"))
                          .anyMatch(details -> details.contains("last modified dates do no match"));
    }

}