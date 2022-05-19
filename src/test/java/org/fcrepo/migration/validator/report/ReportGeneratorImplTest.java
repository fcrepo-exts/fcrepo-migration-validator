/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
        final ReportHandler reportHandler = new HtmlReportHandler(outputDir, 1);
        this.generator = new ReportGeneratorImpl(resultDir, reportHandler);
    }

    @Test
    public void testGenerate() {
        final String reportFilename = generator.generate();
        Assert.assertNotNull(reportFilename);
        Assert.assertEquals("index.html", reportFilename);
    }

}