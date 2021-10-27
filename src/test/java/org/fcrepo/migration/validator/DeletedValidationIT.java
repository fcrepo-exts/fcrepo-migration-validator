/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_HEAD_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_DELETED;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_DELETED;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.fcrepo.migration.validator.api.ValidationResult;
import org.junit.Test;

/**
 * @author mikejritter
 */
public class DeletedValidationIT extends AbstractValidationIT {

    final File DELETED_BASE_DIR = new File(FIXTURES_BASE_DIR, "deleted-validation-it");

    @Test
    public void testValidateDeletedDatastream() {
        final var f3DatastreamsDir = new File(DELETED_BASE_DIR, "valid/f3/datastreams");
        final var f3ObjectsDir = new File(DELETED_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(DELETED_BASE_DIR, "valid/f6/data/ocfl-root");
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        final var reportHandler = doValidation(config);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // verify datastream metadata
        // only 1 datastream was deleted, so we expect 1 SOURCE_OBJECT_RESOURCE_DELETED
        assertThat(reportHandler.getPassed())
            .map(ValidationResult::getValidationType)
            .containsOnlyOnce(SOURCE_OBJECT_RESOURCE_DELETED);
    }

    @Test
    public void testValidateDeletedDatastreamError() {
        final var f3DatastreamsDir = new File(DELETED_BASE_DIR, "valid/f3/datastreams");
        final var f3ObjectsDir = new File(DELETED_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(DELETED_BASE_DIR, "ocfl-missing-version/f6/data/ocfl-root");
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        final var reportHandler = doValidation(config);

        // verify expected results
        // SOURCE_OBJECT_RESOURCE_DELETED -> no deleted version exists
        // BINARY_VERSION_COUNT -> we expected an extra version from OCFL because of the deleted resource
        // BINARY_HEAD_COUNT -> the datastream was never deleted, so it still exists in the HEAD for OCFL
        assertThat(reportHandler.getErrors())
            .isNotEmpty()
            .map(ValidationResult::getValidationType)
            .containsOnly(SOURCE_OBJECT_RESOURCE_DELETED, BINARY_VERSION_COUNT, BINARY_HEAD_COUNT);
    }

    @Test
    public void testValidateDeletedDatastreamFailure() {
        final var f3DatastreamsDir = new File(DELETED_BASE_DIR, "valid/f3/datastreams");
        final var f3ObjectsDir = new File(DELETED_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(DELETED_BASE_DIR, "ocfl-not-deleted/f6/data/ocfl-root");
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        final var reportHandler = doValidation(config);

        // verify expected results
        // unlike the previous test, we have an equal number of versions for our test file (props), however the ocfl
        // headers show that the file is not deleted
        // SOURCE_OBJECT_RESOURCE_DELETED -> no deleted version exists
        // BINARY_HEAD_COUNT -> the datastream was never deleted, so it still exists in the HEAD for OCFL
        assertThat(reportHandler.getErrors())
            .isNotEmpty()
            .map(ValidationResult::getValidationType)
            .containsOnly(SOURCE_OBJECT_RESOURCE_DELETED, BINARY_HEAD_COUNT);
    }

    // Deleted Objects

    @Test
    public void testValidateDeletedObject() {
        final var f3DatastreamsDir = new File(DELETED_BASE_DIR, "deleted-object/f3/datastreams");
        final var f3ObjectsDir = new File(DELETED_BASE_DIR, "deleted-object/f3/objects");
        final var f6OcflRootDir = new File(DELETED_BASE_DIR, "deleted-object/f6/data/ocfl-root");
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        final var reportHandler = doValidation(config);

        // verify expected results
        assertThat(reportHandler.getErrors()).isEmpty();
        assertThat(reportHandler.getPassed()).map(ValidationResult::getValidationType)
                                             .contains(SOURCE_OBJECT_DELETED, SOURCE_OBJECT_RESOURCE_DELETED);
    }

    @Test
    public void testF3ObjectNotDeleted() {
        final var f3DatastreamsDir = new File(DELETED_BASE_DIR, "f3-object-not-deleted/f3/datastreams");
        final var f3ObjectsDir = new File(DELETED_BASE_DIR, "f3-object-not-deleted/f3/objects");
        final var f6OcflRootDir = new File(DELETED_BASE_DIR, "deleted-object/f6/data/ocfl-root");
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        final var reportHandler = doValidation(config);

        // - METADATA state is not equal
        // - METADATA lastModifiedDate is not equal (extra deleted version)
        // - BINARY_VERSION_COUNT for all datastreams
        // - BINARY_HEAD_COUNT
        final var errors = reportHandler.getErrors();
        assertThat(errors).isNotEmpty()
                          .map(ValidationResult::getValidationType)
                          .contains(METADATA, BINARY_VERSION_COUNT, BINARY_HEAD_COUNT);

        // some METADATA should contain not found errors
        assertThat(errors).filteredOn(error -> error.getDetails().contains("not found"))
                          .isNotEmpty();
    }

    @Test
    public void testOcflObjectNotDeleted() {
        final var f3DatastreamsDir = new File(DELETED_BASE_DIR, "deleted-object/f3/datastreams");
        final var f3ObjectsDir = new File(DELETED_BASE_DIR, "deleted-object/f3/objects");
        final var f6OcflRootDir = new File(DELETED_BASE_DIR, "ocfl-not-deleted/f6/data/ocfl-root");
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        final var reportHandler = doValidation(config);

        // expected errors:
        // - SOURCE_OBJECT_DELETED
        // - SOURCE_OBJECT_RESOURCE_DELETED
        // - BINARY_VERSION_COUNT
        // - BINARY_HEAD_COUNT
        final var errors = reportHandler.getErrors();
        assertThat(errors).isNotEmpty()
                          .map(ValidationResult::getValidationType)
                          .contains(SOURCE_OBJECT_DELETED, SOURCE_OBJECT_RESOURCE_DELETED,
                                    BINARY_VERSION_COUNT, BINARY_HEAD_COUNT);
    }

}
