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
package org.fcrepo.migration.validator.report;

import org.apache.commons.io.FileUtils;
import org.fcrepo.migration.validator.api.ReportHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author awoods
 * @since 2020-12-16
 */
public class ReportGeneratorImplTest {

    private ReportGeneratorImpl generator;

    @Before
    public void setup() {
        final Path resultDir = Path.of("src/test/resources/results");
        final Path outputDir = Path.of("target/test/html");

        if (outputDir.toFile().exists()) {
            try {
                FileUtils.forceDelete(outputDir.toFile());
            } catch (IOException e) {
                Assert.fail("Error removing output dir: " + outputDir);
            }
        }
        Assert.assertTrue("Error making output dir: " + outputDir, outputDir.toFile().mkdirs());
        final ReportHandler reportHandler = new HtmlReportHandler(outputDir);
        this.generator = new ReportGeneratorImpl(resultDir, reportHandler);
    }

    @Test
    public void testGenerate() {
        final String reportFilename = generator.generate();
        Assert.assertNotNull(reportFilename);
        Assert.assertEquals("index.html", reportFilename);
    }

}