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
import org.fcrepo.migration.validator.impl.Fedora3ValidationConfig;
import org.fcrepo.migration.validator.impl.Fedora3ValidationExecutionManager;
import org.fcrepo.migration.validator.report.ReportGeneratorImpl;
import org.fcrepo.migration.validator.report.ResultsReportHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

/**
 * @author awoods
 * @since 2020-12-14
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-context.xml")
public class ObjectValidationIT extends AbstractValidationIT {

    private AnnotationConfigApplicationContext context;
    private Fedora3ValidationExecutionManager executionManager;

    @Before
    public void setup() {
        context = new AnnotationConfigApplicationContext("org.fcrepo.migration.validator");
        getConfig(context);
        executionManager = context.getBean(Fedora3ValidationExecutionManager.class);
    }

    @After
    public void teardown() {
        context.close();
    }

    @Test
    public void test() {
        executionManager.doValidation();

        // run report generator with 'ResultsReportHandler'
        final ResultsReportHandler reportHandler = new ResultsReportHandler();
        final var generator = new ReportGeneratorImpl(getConfig(context).getJsonOuputDirectory(), reportHandler);
        generator.generate();

        // verify expected results
        Assert.assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());
    }

    private Fedora3ValidationConfig getConfig(final AnnotationConfigApplicationContext context) {
        final var config = context.getBean(Fedora3ValidationConfig.class);

        final F3SourceTypes f3SourceType = F3SourceTypes.AKUBRA;
        final File f3DatastreamsDir = new File("src/test/resources/test-object-validation/f3/datastreams");
        final File f3ObjectsDir = new File("src/test/resources/test-object-validation/f3/objects");
        final File f3ExportedDir = null;
        final String f3hostname = null;
        final int threadCount = 1;
        final File resultsDirectory = new File("target/test/results-object-validation");

        config.setSourceType(f3SourceType);
        config.setDatastreamsDirectory(f3DatastreamsDir);
        config.setObjectsDirectory(f3ObjectsDir);
        config.setExportedDirectory(f3ExportedDir);
        config.setFedora3Hostname(f3hostname);
        config.setThreadCount(threadCount);
        config.setResultsDirectory(resultsDirectory.toPath());

        return config;
    }


}