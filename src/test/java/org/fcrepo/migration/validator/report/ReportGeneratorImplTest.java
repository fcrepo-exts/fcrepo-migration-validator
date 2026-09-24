/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.report;

import org.apache.commons.io.FileUtils;
import org.fcrepo.migration.validator.api.ReportHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author awoods
 * @since 2020-12-16
 */
public class ReportGeneratorImplTest {

    private ReportGeneratorImpl generator;

    @BeforeEach
    public void setup() {
        final Path resultDir = Path.of("src/test/resources/results");
        final Path outputDir = Path.of("target/test/html");

        if (outputDir.toFile().exists()) {
            try {
                FileUtils.forceDelete(outputDir.toFile());
            } catch (IOException e) {
                Assertions.fail("Error removing output dir: " + outputDir);
            }
        }
        Assertions.assertTrue(outputDir.toFile().mkdirs(), "Error making output dir: " + outputDir);
        final ReportHandler reportHandler = new HtmlReportHandler(outputDir, 1);
        this.generator = new ReportGeneratorImpl(resultDir, reportHandler);
    }

    @Test
    public void testGenerate() {
        final String reportFilename = generator.generate();
        Assertions.assertNotNull(reportFilename);
        Assertions.assertEquals("index.html", reportFilename);
    }

}