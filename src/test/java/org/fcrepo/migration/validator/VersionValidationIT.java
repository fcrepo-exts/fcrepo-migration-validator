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
import static org.fcrepo.migration.validator.AbstractValidationIT.BinaryMetadataValidation.CREATION_DATE;
import static org.fcrepo.migration.validator.AbstractValidationIT.BinaryMetadataValidation.LAST_MODIFIED_DATE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_HEAD_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_DELETED;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.stream.Collectors;

import org.fcrepo.migration.validator.api.ValidationResult;
import org.junit.Test;

/**
 * @author awoods
 * @since 2020-12-14
 */
public class VersionValidationIT extends AbstractValidationIT {

    final File VERSIONS_BASE_DIR = new File(FIXTURES_BASE_DIR, "version-validation-it");

    @Test
    public void test() {
        final var f3DatastreamsDir = new File(VERSIONS_BASE_DIR, "valid/f3/datastreams");
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "valid/f6/data/ocfl-root");
        final var reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // verify datastream metadata
        // only 1 inline datastream with two versions, so we expect 2 results on all but size which should have none
        final var validations = reportHandler.getPassed().stream()
                     .filter(result -> result.getValidationType() == BINARY_METADATA)
                     .map(BinaryMetadataValidation::fromResult)
                     .collect(Collectors.toList());
        assertThat(validations).containsOnly(CREATION_DATE, LAST_MODIFIED_DATE);
        assertThat(validations).filteredOn(validation -> validation == CREATION_DATE).hasSize(2);
        assertThat(validations).filteredOn(validation -> validation == LAST_MODIFIED_DATE).hasSize(2);
    }

    @Test
    public void testOcflMissingVersion() {
        final var f3DatastreamsDir = new File(VERSIONS_BASE_DIR, "valid/f3/datastreams");
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "ocfl-fewer-versions/f6/data/ocfl-root");
        final var reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        final var errors = reportHandler.getErrors();
        assertThat(errors).hasSize(2)
                          .map(ValidationResult::getValidationType)
                          .containsOnly(BINARY_VERSION_COUNT,
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
        // 2 creation dates match; 2 last modified dates match; 0 size matches
        final var errors = reportHandler.getErrors().stream()
                                        .filter(result -> result.getValidationType() == BINARY_METADATA)
                                        .map(BinaryMetadataValidation::fromResult)
                                        .collect(Collectors.toList());
        assertThat(errors).containsOnly(CREATION_DATE, LAST_MODIFIED_DATE);
        assertThat(errors).filteredOn(validation -> validation == CREATION_DATE).hasSize(2);
        assertThat(errors).filteredOn(validation -> validation == LAST_MODIFIED_DATE).hasSize(2);
    }

    @Test
    public void testValidateHeadOnly() {
        final var f3DatastreamsDir = new File(VERSIONS_BASE_DIR, "valid/f3/datastreams");
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "valid/f6/data/ocfl-root");
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);
        config.setValidateHeadOnly(true);

        final var reportHandler = doValidation(config);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // verify datastream metadata
        // only 1 inline datastream with two versions, so we expect 2 results on all but size which should have none
        final var validations = reportHandler.getPassed().stream()
                                             .filter(result -> result.getValidationType() == BINARY_METADATA)
                                             .map(ValidationResult::getDetails)
                                             .collect(Collectors.toList());
        assertThat(validations).allMatch(details -> details.contains("HEAD"));
    }

    @Test
    public void testValidateDeletedDatastream() {
        final var f3DatastreamsDir = new File(VERSIONS_BASE_DIR, "deleted-datastream/f3/datastreams");
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "deleted-datastream/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "deleted-datastream/f6/data/ocfl-root");
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);
        config.setValidateHeadOnly(true);

        final var reportHandler = doValidation(config);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // verify datastream metadata
        // only 1 datastream was deleted, so we expect 1 SOURCE_OBJECT_RESOURCE_DELETED
        assertThat(reportHandler.getPassed())
            .map(ValidationResult::getValidationType)
            .containsOnlyOnce(SOURCE_OBJECT_RESOURCE_DELETED);
    }

}