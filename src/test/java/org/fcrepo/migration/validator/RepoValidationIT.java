/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.REPOSITORY;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.REPOSITORY_RESOURCE_COUNT;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.fcrepo.migration.validator.impl.ApplicationConfigurationHelper;
import org.fcrepo.migration.validator.impl.Fedora3ValidationConfig;
import org.fcrepo.migration.validator.impl.Fedora3ValidationExecutionManager;
import org.fcrepo.migration.validator.report.ReportGeneratorImpl;
import org.fcrepo.migration.validator.report.ResultsReportHandler;
import org.junit.Test;

/**
 * @author awoods
 * @since 2020-12-14
 */
public class RepoValidationIT extends AbstractValidationIT {

    @Test
    public void test() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir, false);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // check that the repository validations were run
        assertThat(reportHandler.getPassed())
            .anyMatch(result -> result.getValidationType() == REPOSITORY_RESOURCE_COUNT)
            .anyMatch(result -> result.getValidationLevel() == REPOSITORY);
    }

    @Test
    public void testHeadOnly() {
        // pull from head only data
        final File headOnlyBase = new File(FIXTURES_BASE_DIR, "head-only-it");
        final File f3DatastreamsDir = new File(headOnlyBase, "valid/f3/datastreams");
        final File f3ObjectsDir = new File(headOnlyBase, "valid/f3/objects");
        final File f6OcflRootDir = new File(headOnlyBase, "valid/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir, true);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // check that the repository validations were run
        assertThat(reportHandler.getPassed())
            .anyMatch(result -> result.getValidationType() == REPOSITORY_RESOURCE_COUNT)
            .anyMatch(result -> result.getValidationLevel() == REPOSITORY);
    }

    @Test
    public void testHeadOnlyDeleted() {
        // pull from deleted validation
        final File deletedBase = new File(FIXTURES_BASE_DIR, "deleted-validation-it/deleted-object");
        final File f3ObjectsDir = new File(deletedBase, "f3/objects");
        final File f3DatastreamDir = new File(deletedBase, "f3/datastreams");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "empty/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamDir, f3ObjectsDir, f6OcflRootDir, true);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());

        // check that the repository validations were run
        assertThat(reportHandler.getPassed())
            .anyMatch(result -> result.getValidationType() == REPOSITORY_RESOURCE_COUNT)
            .anyMatch(result -> result.getValidationLevel() == REPOSITORY);
    }


    private ResultsReportHandler doValidation(final File f3DatastreamsDir,
                                              final File f3ObjectsDir,
                                              final File f6OcflRootDir,
                                              final boolean headOnly) {
        final Fedora3ValidationConfig config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);
        config.setCheckNumObjects(true);
        config.setValidateHeadOnly(headOnly);
        final var configuration = new ApplicationConfigurationHelper(config);
        final var executionManager = new Fedora3ValidationExecutionManager(configuration);
        executionManager.doValidation();

        // run report generator with 'ResultsReportHandler'
        final ResultsReportHandler reportHandler = new ResultsReportHandler();
        final var generator = new ReportGeneratorImpl(config.getJsonOutputDirectory(), reportHandler);
        generator.generate();
        return reportHandler;
    }
}