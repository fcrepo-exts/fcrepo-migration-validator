/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fcrepo.migration.validator.AbstractValidationIT.BinaryMetadataValidation.CREATION_DATE;
import static org.fcrepo.migration.validator.AbstractValidationIT.BinaryMetadataValidation.LAST_MODIFIED_DATE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
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
        final var datastreamId = "DS1";
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "valid/f6/data/ocfl-root");
        final var reportHandler = doValidation(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // verify datastream metadata
        // DS1 has 2 versions (not including RELS-INT), so we expect 2 results on all but size which should have none
        final var validations = reportHandler.getPassed().stream()
                     .filter(result -> result.getValidationType() == BINARY_METADATA)
                     .filter(result -> result.getTargetResourceId().contains(datastreamId))
                     .map(BinaryMetadataValidation::fromResult)
                     .collect(Collectors.toList());
        assertThat(validations).containsOnly(CREATION_DATE, LAST_MODIFIED_DATE);
        assertThat(validations).filteredOn(validation -> validation == CREATION_DATE).hasSize(2);
        assertThat(validations).filteredOn(validation -> validation == LAST_MODIFIED_DATE).hasSize(2);

        // verify binary version count has an additional version from the RELS-INT
        final var binaryVersionCount = reportHandler.getPassed().stream()
            .filter(result -> result.getValidationType() == BINARY_VERSION_COUNT)
            .filter(result -> result.getTargetResourceId().contains(datastreamId))
            .findFirst().orElseThrow(() -> new RuntimeException("Unable to find BINARY_VERSION_COUNT for DS1"));

        final var relsCount = "RELS-INT=1";
        final var sourceCount = "source=2";
        final var targetCount = "target=3";
        assertThat(binaryVersionCount.getDetails()).contains(sourceCount, relsCount, targetCount);
    }

    @Test
    public void testOcflMissingVersion() {
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "ocfl-fewer-versions/f6/data/ocfl-root");
        final var reportHandler = doValidation(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        final var errors = reportHandler.getErrors();
        assertThat(errors).hasSize(2)
                          .map(ValidationResult::getValidationType)
                          .containsOnly(BINARY_VERSION_COUNT,
                                        SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET);
    }

    @Test
    public void testF3MissingVersion() {
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "ocfl-more-versions/f6/data/ocfl-root");
        final var reportHandler = doValidation(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        final var errors = reportHandler.getErrors();
        assertThat(errors).hasSize(2)
                          .map(ValidationResult::getValidationType)
                          .containsOnly(BINARY_VERSION_COUNT, BINARY_METADATA);
    }

    @Test
    public void testInvalidMetadata() {
        final var f3ObjectsDir = new File(VERSIONS_BASE_DIR, "invalid-metadata/f3/objects");
        final var f6OcflRootDir = new File(VERSIONS_BASE_DIR, "valid/f6/data/ocfl-root");
        final var reportHandler = doValidation(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);

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

}