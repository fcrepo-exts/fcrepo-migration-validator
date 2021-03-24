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
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_CHECKSUM;
import static org.fcrepo.migration.validator.impl.F6DigestAlgorithm.sha256;
import static org.fcrepo.migration.validator.impl.F6DigestAlgorithm.sha512;

import java.io.File;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.impl.ApplicationConfigurationHelper;
import org.fcrepo.migration.validator.impl.F6DigestAlgorithm;
import org.fcrepo.migration.validator.impl.Fedora3ValidationExecutionManager;
import org.fcrepo.migration.validator.report.ReportGeneratorImpl;
import org.fcrepo.migration.validator.report.ResultsReportHandler;
import org.junit.Test;

/**
 * Checksum validation for datastreams
 *
 * @author awoods
 * @since 2020-12-14
 */
public class DatastreamValidationIT extends AbstractValidationIT {

    @Test
    public void test() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler =
            doChecksumValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir, sha512);

        final var errors = reportHandler.getErrors();
        assertThat(errors).isEmpty();
        final var passed = reportHandler.getPassed();
        // expect 4 checksum validations
        assertThat(passed).map(ValidationResult::getValidationType)
                          .filteredOn(type -> type == BINARY_CHECKSUM)
                          .hasSize(4);
    }

    @Test
    public void testChecksumMismatch() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "invalid-f6-digests/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler =
            doChecksumValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir, sha512);

        // 2 files were modified to have invalid checksums, so expect failures on both REF + THUMB
        final Predicate<String> badFiles = Pattern.compile("THUMB|REF").asPredicate();
        final var errors = reportHandler.getErrors();
        assertThat(errors).isNotEmpty()
                          .filteredOn(result -> result.getValidationType() == BINARY_CHECKSUM)
                          .hasSize(2)
                          .allMatch(result -> badFiles.test(result.getTargetResourceId()));
    }

    @Test
    public void testChecksumNotFound() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler =
            doChecksumValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir, sha256);

        // expect 4 checksum failures, we did not pass in the correct digest algorithm
        final var errors = reportHandler.getErrors();
        assertThat(errors).filteredOn(result -> result.getValidationType() == BINARY_CHECKSUM)
                          .map(ValidationResult::getDetails)
                          .hasSize(4)
                          .allMatch(result -> result.contains("binary checksum not found"));

        final var passed = reportHandler.getPassed();
        assertThat(passed).map(ValidationResult::getValidationType)
                          .filteredOn(type -> type == BINARY_CHECKSUM)
                          .isEmpty();
    }

    /**
     * Custom doValidation so we can enable checksums with a digest algorithm
     *
     * @param f3DatastreamsDir
     * @param f3ObjectsDir
     * @param f6OcflRootDir
     * @return
     */
    private ResultsReportHandler doChecksumValidation(final File f3DatastreamsDir,
                                                      final File f3ObjectsDir,
                                                      final File f6OcflRootDir,
                                                      final F6DigestAlgorithm digestAlgorithm) {
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);
        config.setEnableChecksums(true);
        config.setDigestAlgorithm(digestAlgorithm);
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