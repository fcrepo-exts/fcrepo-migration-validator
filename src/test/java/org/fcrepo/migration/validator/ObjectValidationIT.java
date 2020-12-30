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

import org.fcrepo.migration.validator.impl.F3SourceTypes;
import org.fcrepo.migration.validator.impl.Fedora3ObjectConfiguration;
import org.fcrepo.migration.validator.impl.Fedora3ValidationConfig;
import org.fcrepo.migration.validator.impl.Fedora3ValidationExecutionManager;
import org.fcrepo.migration.validator.report.ReportGeneratorImpl;
import org.fcrepo.migration.validator.report.ResultsReportHandler;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

/**
 * @author awoods
 * @since 2020-12-14
 */
public class ObjectValidationIT extends AbstractValidationIT {

    final static File FIXTURES_BASE_DIR = new File("src/test/resources/test-object-validation");
    final static File RESULTS_DIR = new File("target/test/results-object-validation");

    @Test
    public void test() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir);

        // verify expected results
        Assert.assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());
    }

    @Test
    @Ignore("currently not passing")
    public void testNumberOfObjectsFailMoreOcfl() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-ocfl/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-ocfl/f3/objects");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir);

        // verify expected results (1 object in f3, 2 objects in OCFL)
        Assert.assertEquals("Should be one error!", 1, reportHandler.getErrors().size());
        // TODO: check for the specific ValidationResult.ValidationLevel
        // TODO: check for the specific ValidationResult.ValidationType
    }

    @Test
    @Ignore("currently not passing")
    public void testNumberOfObjectsFailMoreF3() {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-f3/f3/datastreams");
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, "bad-num-objects-more-f3/f3/objects");
        final ResultsReportHandler reportHandler = doValidation(f3DatastreamsDir, f3ObjectsDir);

        // verify expected results (2 objects in f3, 1 object in OCFL)
        Assert.assertEquals("Should be one error!", 1, reportHandler.getErrors().size());
        // TODO: check for the specific ValidationResult.ValidationLevel
        // TODO: check for the specific ValidationResult.ValidationType
    }

    private ResultsReportHandler doValidation(final File f3DatastreamsDir, final File f3ObjectsDir) {
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir);
        final var configuration = new Fedora3ObjectConfiguration(config);
        final var executionManager = new Fedora3ValidationExecutionManager(configuration);
        executionManager.doValidation();

        // run report generator with 'ResultsReportHandler'
        final ResultsReportHandler reportHandler = new ResultsReportHandler();
        final var generator = new ReportGeneratorImpl(config.getJsonOuputDirectory(), reportHandler);
        generator.generate();
        return reportHandler;
    }

    private Fedora3ValidationConfig getConfig(final File f3DatastreamsDir, final File f3ObjectsDir) {
        final var config = new Fedora3ValidationConfig();

        final F3SourceTypes f3SourceType = F3SourceTypes.AKUBRA;
        final File f3ExportedDir = null;
        final String f3hostname = null;
        final int threadCount = 1;

        config.setSourceType(f3SourceType);
        config.setDatastreamsDirectory(f3DatastreamsDir);
        config.setObjectsDirectory(f3ObjectsDir);
        config.setExportedDirectory(f3ExportedDir);
        config.setFedora3Hostname(f3hostname);
        config.setThreadCount(threadCount);
        config.setResultsDirectory(RESULTS_DIR.toPath());

        return config;
    }


}