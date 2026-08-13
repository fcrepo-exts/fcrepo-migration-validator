/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.report;

import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.REPOSITORY;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.OBJECT_READABLE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.REPOSITORY_RESOURCE_COUNT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.fcrepo.migration.validator.api.ObjectReportSummary;
import org.fcrepo.migration.validator.api.ObjectValidationResults;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResultsSummary;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the success and failure paths of the HTML report writer.
 *
 * @author Dan Field
 */
public class HtmlReportHandlerTest {

    private Path outputDir;

    @Before
    public void setup() throws IOException {
        outputDir = Files.createTempDirectory("html-report-handler-test");
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(outputDir.toFile());
    }

    @Test
    public void testWritesAllReports() {
        final var handler = new HtmlReportHandler(outputDir, 2);
        handler.beginReport();

        final var objectReport = handler.objectLevelReport(objectResults());
        final var repositoryReport = handler.repositoryLevelReport(repositoryResults());

        final var summary = new ValidationResultsSummary();
        summary.addObjectReport("object-1", new ObjectReportSummary(true, "object-1", objectReport));
        summary.addRepositoryReport(new ObjectReportSummary(false, "repository", repositoryReport));

        final var summaryReport = handler.validationSummary(summary);
        handler.endReport();

        assertEquals("index.html", summaryReport);
        assertTrue("Expected an object report", Files.exists(outputDir.resolve(objectReport)));
        assertTrue("Expected a repository report", Files.exists(outputDir.resolve(repositoryReport)));
        assertTrue("Expected a summary report", Files.exists(outputDir.resolve(summaryReport)));
    }

    @Test
    public void testObjectLevelReportFailsWhenUnwritable() throws IOException {
        final var handler = new HtmlReportHandler(outputDir, 1);
        final var results = objectResults();

        // a directory where the report file should go makes the FileWriter fail
        Files.createDirectories(outputDir.resolve(results.getEncodedObjectId() + ".html"));

        assertThrows(RuntimeException.class, () -> handler.objectLevelReport(results));
    }

    @Test
    public void testRepositoryLevelReportFailsWhenUnwritable() throws IOException {
        final var handler = new HtmlReportHandler(outputDir, 1);
        final var results = repositoryResults();
        Files.createDirectories(outputDir.resolve("repository.html"));

        assertThrows(RuntimeException.class, () -> handler.repositoryLevelReport(results));
    }

    @Test
    public void testValidationSummaryFailsWhenUnwritable() throws IOException {
        final var handler = new HtmlReportHandler(outputDir, 1);
        final var summary = new ValidationResultsSummary();
        Files.createDirectories(outputDir.resolve("index.html"));

        assertThrows(RuntimeException.class, () -> handler.validationSummary(summary));
    }

    @Test
    public void testValidationSummaryRequiresSummary() {
        final var handler = new HtmlReportHandler(outputDir, 1);

        assertThrows(NullPointerException.class, () -> handler.validationSummary(null));
    }

    private ObjectValidationResults objectResults() {
        return new ObjectValidationResults(List.of(
            new ValidationResult(0, OK, OBJECT, OBJECT_READABLE, "object-1", "info:fedora/object-1", "all good"),
            new ValidationResult(1, FAIL, OBJECT, OBJECT_READABLE, "object-1", "info:fedora/object-1", "not good")));
    }

    private ObjectValidationResults repositoryResults() {
        return new ObjectValidationResults(List.of(
            new ValidationResult(0, OK, REPOSITORY, REPOSITORY_RESOURCE_COUNT, "counts match")));
    }
}
