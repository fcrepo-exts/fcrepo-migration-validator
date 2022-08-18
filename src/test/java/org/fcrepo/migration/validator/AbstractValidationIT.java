/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.impl.ApplicationConfigurationHelper;
import org.fcrepo.migration.validator.impl.F3SourceTypes;
import org.fcrepo.migration.validator.impl.Fedora3ValidationConfig;
import org.fcrepo.migration.validator.impl.Fedora3ValidationExecutionManager;
import org.fcrepo.migration.validator.impl.ValidatingObjectHandler;
import org.fcrepo.migration.validator.report.ReportGeneratorImpl;
import org.fcrepo.migration.validator.report.ResultsReportHandler;
import org.junit.After;

/**
 * @author awoods
 * @since 2020-12-14
 */
public abstract class AbstractValidationIT {

    final static File FIXTURES_BASE_DIR = new File("src/test/resources/test-object-validation");
    final static File RESULTS_DIR = new File("target/test/results-object-validation");

    @After
    public void teardown() {
        FileUtils.deleteQuietly(RESULTS_DIR);
    }

    ResultsReportHandler doValidation(final File f3DatastreamsDir, final File f3ObjectsDir, final File f6OcflRootDir) {
        return doValidation(getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir));
    }

    ResultsReportHandler doValidation(final Fedora3ValidationConfig config) {
        final var configuration = new ApplicationConfigurationHelper(config);
        final var executionManager = new Fedora3ValidationExecutionManager(configuration);
        executionManager.doValidation();

        // run report generator with 'ResultsReportHandler'
        final ResultsReportHandler reportHandler = new ResultsReportHandler();
        final var generator = new ReportGeneratorImpl(config.getJsonOutputDirectory(), reportHandler);
        generator.generate();
        return reportHandler;
    }

    Fedora3ValidationConfig getConfig(final File f3DatastreamsDir, final File f3ObjectsDir, final File f6OcflRootDir) {
        final var config = new Fedora3ValidationConfig();

        final F3SourceTypes f3SourceType = F3SourceTypes.AKUBRA;
        final File f3ExportedDir = null;
        final String f3hostname = null;
        final int threadCount = 1;

        config.setSourceType(f3SourceType);
        config.setDatastreamsDirectory(f3DatastreamsDir);
        config.setOcflRepositoryRootDirectory(f6OcflRootDir);
        config.setObjectsDirectory(f3ObjectsDir);
        config.setExportedDirectory(f3ExportedDir);
        config.setFedora3Hostname(f3hostname);
        config.setThreadCount(threadCount);
        config.setResultsDirectory(RESULTS_DIR.toPath());

        return config;
    }

    /**
     * Quick enum to help check the type of validations run. So instead of running result.getDetails.contains(...),
     * create an enum type based on the details for (hopefully) cleaner assertions.
     */
    public enum BinaryMetadataValidation {
        CREATION_DATE, LAST_MODIFIED_DATE, SIZE;

        public static BinaryMetadataValidation fromResult(final ValidationResult result) {
            if (result.getValidationType() != ValidationResult.ValidationType.BINARY_METADATA) {
                throw new IllegalArgumentException("Enum type is only for BINARY_METADATA!");
            }

            final var details = result.getDetails();
            if (details.contains("last modified date")) {
                return LAST_MODIFIED_DATE;
            } else if (details.contains("creation date")) {
                return CREATION_DATE;
            } else if (details.contains("size")) {
                return SIZE;
            }

            throw new IllegalArgumentException("Unknown details type!");
        }
    }

    /**
     * Same as BinaryMetadataValidation but for Object metadata. Could probably be combined but keeping them distinct
     * for now.
     */
    public enum ObjectMetadataValidation {
        LABEL, OWNER, STATE, CREATED_DATE, LAST_MODIFIED_DATE;

        public static ObjectMetadataValidation fromResult(final ValidationResult result) {
            if (result.getValidationType() != ValidationResult.ValidationType.METADATA) {
                throw new IllegalArgumentException("Enum type is only for METADATA!");
            }

            final var details = result.getDetails();
            if (details.contains(ValidatingObjectHandler.F3_LABEL)) {
                return LABEL;
            } else if (details.contains(ValidatingObjectHandler.F3_OWNER_ID)) {
                return OWNER;
            } else if (details.contains(ValidatingObjectHandler.F3_STATE)) {
                return STATE;
            } else if (details.contains(ValidatingObjectHandler.F3_CREATED_DATE)) {
                return CREATED_DATE;
            } else if (details.contains(ValidatingObjectHandler.F3_LAST_MODIFIED_DATE)) {
                return LAST_MODIFIED_DATE;
            }

            throw new IllegalArgumentException("Unknown details type!");
        }
    }

    /**
     * For tests which have inline datastreams, create an empty directory
     * @return the empty directory
     */
    public File emptyDatastreamDir() {
        final Path parent = RESULTS_DIR.toPath().getParent();
        try {
            final var dir = Files.createDirectories(parent.resolve("empty-datastreams"));
            dir.toFile().deleteOnExit();
            return dir.toFile();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create tmp directory for testing");
        }
    }
}