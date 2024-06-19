/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_EXISTS_IN_TARGET;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.junit.Test;

/**
 * Tests for head only validations
 *
 * @author mikeritter
 */
public class HeadOnlyIT extends AbstractValidationIT {

    final File EMPTY_BASE_DIR = new File(FIXTURES_BASE_DIR, "empty");
    final File HEAD_ONLY_BASE_DIR = new File(FIXTURES_BASE_DIR, "head-only-it");
    final File VERSION_BASE_DIR = new File(FIXTURES_BASE_DIR, "version-validation-it");

    @Test
    public void validateHeadOnly() {
        // reuse the versioned f3 object
        final var f3ObjectsDir = new File(VERSION_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(HEAD_ONLY_BASE_DIR, "valid/f6/data/ocfl-root");
        final var config = getConfig(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);
        config.setValidateHeadOnly(true);

        final var reportHandler = doValidation(config);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // verify datastream metadata for HEAD version only
        final var validations = reportHandler.getPassed().stream()
                                             .filter(result -> result.getValidationType() == BINARY_METADATA)
                                             .map(ValidationResult::getDetails)
                                             .collect(Collectors.toList());
        assertThat(validations).isNotEmpty().allMatch(details -> details.contains("HEAD"));
    }

    @Test
    public void failHeadOnly() {
        // reuse the versioned object
        final var f3ObjectsDir = new File(VERSION_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(VERSION_BASE_DIR, "valid/f6/data/ocfl-root");
        final var config = getConfig(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);
        config.setValidateHeadOnly(true);

        final var reportHandler = doValidation(config);

        // only ds1 should have too many versions
        assertEquals("Should be some errors!", 1, reportHandler.getErrors().size());

        // verify datastream metadata for HEAD version only
        assertThat(reportHandler.getErrors())
            .map(ValidationResult::getValidationType)
            .contains(BINARY_VERSION_COUNT);
    }

    @Test
    public void failHeadOnlyDeletedObject() {
        final var f3ObjectsDir = new File(HEAD_ONLY_BASE_DIR, "deleted-object/f3/objects");
        final var f6OcflRootDir = new File(HEAD_ONLY_BASE_DIR, "valid/f6/data/ocfl-root");
        final var config = getConfig(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);
        config.setValidateHeadOnly(true);

        final var reportHandler = doValidation(config);
        Assertions.assertThat(reportHandler.getErrors()).hasSize(1);

        final var error = reportHandler.getErrors().get(0);
        Assertions.assertThat(error.getValidationType()).isEqualByComparingTo(SOURCE_OBJECT_EXISTS_IN_TARGET);
    }

    @Test
    public void failHeadOnlyDeletedDatastream() {
        final var f3ObjectsDir = new File(HEAD_ONLY_BASE_DIR, "deleted-datastream/f3/objects");
        final var f6OcflRootDir = new File(HEAD_ONLY_BASE_DIR, "valid/f6/data/ocfl-root");
        final var config = getConfig(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);
        config.setValidateHeadOnly(true);

        final var reportHandler = doValidation(config);
        Assertions.assertThat(reportHandler.getErrors()).hasSize(1);

        final var error = reportHandler.getErrors().get(0);
        Assertions.assertThat(error.getValidationType()).isEqualByComparingTo(SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET);
    }

    @Test
    public void testObjectNotMigrated() {
        // reuse the versioned object
        final var f3ObjectsDir = new File(HEAD_ONLY_BASE_DIR, "deleted-object/f3/objects");
        final var f6OcflRootDir = new File(EMPTY_BASE_DIR, "f6/data/ocfl-root");
        final var config = getConfig(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);
        config.setValidateHeadOnly(true);

        final var reportHandler = doValidation(config);
        Assertions.assertThat(reportHandler.getErrors()).isEmpty();
    }

    @Test
    public void failObjectNotMigrated() {
        // reuse the versioned object
        final var f3ObjectsDir = new File(VERSION_BASE_DIR, "valid/f3/objects");
        final var f6OcflRootDir = new File(EMPTY_BASE_DIR, "f6/data/ocfl-root");
        final var config = getConfig(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);
        config.setValidateHeadOnly(true);

        final var reportHandler = doValidation(config);
        Assertions.assertThat(reportHandler.getErrors()).hasSize(1);

        final var error = reportHandler.getErrors().get(0);
        Assertions.assertThat(error.getValidationType()).isEqualByComparingTo(SOURCE_OBJECT_EXISTS_IN_TARGET);
    }

    @Test
    public void failDatastreamNotFound() {
        final var f3ObjectsDir = new File(HEAD_ONLY_BASE_DIR, "extra-datastream/f3/objects");
        final var f6OcflRootDir = new File(HEAD_ONLY_BASE_DIR, "valid/f6/data/ocfl-root");
        final var config = getConfig(emptyDatastreamDir(), f3ObjectsDir, f6OcflRootDir);
        config.setValidateHeadOnly(true);

        final var reportHandler = doValidation(config);
        Assertions.assertThat(reportHandler.getErrors()).hasSize(1);

        final var error = reportHandler.getErrors().get(0);
        Assertions.assertThat(error.getValidationType()).isEqualByComparingTo(SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET);
    }

}
