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

import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.report.ResultsReportHandler;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fcrepo.migration.validator.AbstractValidationIT.BinaryMetadataValidation.CREATION_DATE;
import static org.fcrepo.migration.validator.AbstractValidationIT.BinaryMetadataValidation.LAST_MODIFIED_DATE;
import static org.fcrepo.migration.validator.AbstractValidationIT.BinaryMetadataValidation.SIZE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_HEAD_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_SIZE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_EXISTS_IN_TARGET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author awoods
 * @author dbernstein
 * @since 2020-12-14
 */
public class ObjectValidationIT extends AbstractValidationIT {

    @Test
    public void test() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());
        final var resultsByType = reportHandler.getPassed().stream()
                                               .collect(Collectors.groupingBy(ValidationResult::getValidationType));

        // check that we have results for each of the f3 properties we look for
        final var metadataResults = resultsByType.get(METADATA);
        assertThat(metadataResults).map(ObjectMetadataValidation::fromResult)
                                   .containsAll(Arrays.asList(ObjectMetadataValidation.values()));

        // check datastream validations
        // we have 7 datastreams overall -- 4 files and 3 inline
        final var totalManaged = 4;
        final var totalDatastreams = 7;
        final var binaryMetadata = resultsByType.get(BINARY_METADATA).stream()
                                                .map(BinaryMetadataValidation::fromResult)
                                                .collect(Collectors.toList());
        assertThat(binaryMetadata).containsOnly(CREATION_DATE, LAST_MODIFIED_DATE, SIZE);
        assertThat(binaryMetadata).filteredOn(validation -> validation == SIZE).hasSize(totalManaged);
        assertThat(binaryMetadata).filteredOn(validation -> validation == CREATION_DATE).hasSize(totalDatastreams);
        assertThat(binaryMetadata).filteredOn(validation -> validation == LAST_MODIFIED_DATE).hasSize(totalDatastreams);

        // check that each managed datastream also has a BINARY_SIZE validation
        final var sizeValidations = resultsByType.get(BINARY_SIZE);
        assertThat(sizeValidations).hasSize(totalManaged);
    }

    @Test
    public void testBadMetadata() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "bad-metadata/f3/objects");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        final var errors = reportHandler.getErrors();
        assertThat(errors).hasSize(4)
                          .map(ObjectMetadataValidation::fromResult)
                          .contains(ObjectMetadataValidation.LABEL, ObjectMetadataValidation.OWNER,
                                    ObjectMetadataValidation.CREATED_DATE, ObjectMetadataValidation.LAST_MODIFIED_DATE);
    }

    @Test
    public void testNumberOfObjectsFailMoreOcfl() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-ocfl/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-ocfl/f3/objects");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-ocfl/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results (1 object in f3, 2 objects in OCFL)
        final var errors = reportHandler.getErrors();
        assertEquals("Should be one error!", 1, errors.size());

        final var validationResult = errors.get(0);
        assertNotNull(validationResult);
        assertEquals("Should be HEAD count error", BINARY_HEAD_COUNT, validationResult.getValidationType());
        assertEquals("Should be OBJECT validation level", OBJECT, validationResult.getValidationLevel());
    }

    @Test
    public void testNumberOfObjectsFailMoreF3() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-f3/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-f3/f3/objects");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-f3/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results (2 objects in f3, 1 object in OCFL)
        final var errors = reportHandler.getErrors();
        final var sourceObject = "1711.dl:UWPAbout";

        assertEquals("Should be two errors!", 2, errors.size());
        errors.stream()
              .filter(x -> x.getValidationType().equals(SOURCE_OBJECT_EXISTS_IN_TARGET))
              .findFirst().ifPresentOrElse(result -> {
            assertEquals("Should be validation level OBJECT", OBJECT, result.getValidationLevel());
            assertEquals("Source object should be " + sourceObject, sourceObject, result.getSourceObjectId());
            assertEquals("Should be validation type SOURCE_OBJECT_EXISTS_IN_TARGET",
                         SOURCE_OBJECT_EXISTS_IN_TARGET,
                         result.getValidationType());
        }, () -> fail("Unable to find error for SOURCE_OBJECT_EXISTS_IN_TARGET"));

        errors.stream()
              .filter(x -> x.getValidationType().equals(BINARY_HEAD_COUNT))
              .findFirst().ifPresentOrElse(result -> {
            assertEquals("Should be validation level OBJECT", OBJECT, result.getValidationLevel());
            assertEquals("Should be validation type BINARY_HEAD_COUNT", BINARY_HEAD_COUNT, result.getValidationType());
        }, () -> fail("Unable to find error for BINARY_HEAD_COUNT"));
    }

}