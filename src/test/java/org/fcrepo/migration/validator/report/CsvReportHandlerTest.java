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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
 * Covers the success and failure paths of the delimited report writer.
 *
 * @author Dan Field
 */
public class CsvReportHandlerTest {

    private Path workDir;

    @Before
    public void setup() throws IOException {
        workDir = Files.createTempDirectory("csv-report-handler-test");
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(workDir.toFile());
    }

    @Test
    public void testWritesAllReportsAsCsv() {
        assertWritesAllReports(ReportType.csv);
    }

    @Test
    public void testWritesAllReportsAsTsv() {
        assertWritesAllReports(ReportType.tsv);
    }

    @Test
    public void testHtmlReportTypeIsRejected() {
        final var outputDir = workDir.resolve("html-rejected");

        assertThrows(IllegalArgumentException.class, () -> new CsvReportHandler(outputDir, ReportType.html));
    }

    @Test
    public void testUncreatableOutputDirectoryIsRejected() throws IOException {
        // an existing regular file cannot also be a directory
        final var outputDir = Files.createFile(workDir.resolve("not-a-directory")).resolve("output");

        final var exception = assertThrows(RuntimeException.class,
                                           () -> new CsvReportHandler(outputDir, ReportType.csv));
        assertTrue("Expected the IOException to be wrapped", exception.getCause() instanceof IOException);
    }

    @Test
    public void testObjectLevelReportFailsWhenUnwritable() throws IOException {
        final var outputDir = workDir.resolve("object-unwritable");
        final var handler = new CsvReportHandler(outputDir, ReportType.csv);
        final var results = objectResults();

        Files.createDirectories(outputDir.resolve(results.getEncodedObjectId() + ".csv"));

        final var exception = assertThrows(RuntimeException.class, () -> handler.objectLevelReport(results));
        assertTrue("Expected the IOException to be wrapped", exception.getCause() instanceof IOException);
    }

    @Test
    public void testValidationSummaryFailsWhenUnwritable() throws IOException {
        final var outputDir = workDir.resolve("summary-unwritable");
        final var handler = new CsvReportHandler(outputDir, ReportType.csv);
        final var summary = new ValidationResultsSummary();

        final var date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Files.createDirectories(outputDir.resolve("migration-validation-summary" + date + ".csv"));

        final var exception = assertThrows(RuntimeException.class, () -> handler.validationSummary(summary));
        assertTrue("Expected the IOException to be wrapped", exception.getCause() instanceof IOException);
    }

    private void assertWritesAllReports(final ReportType reportType) {
        final var outputDir = workDir.resolve(reportType.name());
        final var handler = new CsvReportHandler(outputDir, reportType);
        handler.beginReport();

        final var objectReport = Path.of(handler.objectLevelReport(objectResults()));
        final var repositoryReport = Path.of(handler.repositoryLevelReport(repositoryResults()));

        final var summary = new ValidationResultsSummary();
        summary.addObjectReport("object-1", new ObjectReportSummary(true, "object-1", objectReport.toString()));
        final var summaryReport = Path.of(handler.validationSummary(summary));
        handler.endReport();

        assertTrue("Expected an object report", Files.exists(objectReport));
        assertTrue("Expected a repository report", Files.exists(repositoryReport));
        assertTrue("Expected a summary report", Files.exists(summaryReport));
        assertEquals("repository" + reportType.getExtension(), repositoryReport.getFileName().toString());
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
