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

import org.apache.commons.io.FileUtils;
import org.fcrepo.migration.validator.impl.ApplicationConfigurationHelper;
import org.fcrepo.migration.validator.impl.F3SourceTypes;
import org.fcrepo.migration.validator.impl.Fedora3ValidationConfig;
import org.fcrepo.migration.validator.impl.Fedora3ValidationExecutionManager;
import org.fcrepo.migration.validator.report.ReportGeneratorImpl;
import org.fcrepo.migration.validator.report.ResultsReportHandler;
import org.junit.After;
import org.junit.Test;

import java.io.File;

import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_HEAD_COUNT;
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

    final static File FIXTURES_BASE_DIR = new File("src/test/resources/test-object-validation");
    final static File RESULTS_DIR = new File("target/test/results-object-validation");

    @After
    public void teardown() {
        FileUtils.deleteQuietly(RESULTS_DIR);
    }

    @Test
    public void test() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());
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

    private ResultsReportHandler doValidation(final File f3DatastreamsDir, final File f3ObjectsDir,
                                              final File f6OcflRootDir) {
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);
        final var configuration = new ApplicationConfigurationHelper(config);
        final var executionManager = new Fedora3ValidationExecutionManager(configuration);
        executionManager.doValidation();

        // run report generator with 'ResultsReportHandler'
        final ResultsReportHandler reportHandler = new ResultsReportHandler();
        final var generator = new ReportGeneratorImpl(config.getJsonOuputDirectory(), reportHandler);
        generator.generate();
        return reportHandler;
    }

    private Fedora3ValidationConfig getConfig(final File f3DatastreamsDir, final File f3ObjectsDir,
                                              final File f6OcflRootDir) {
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


}