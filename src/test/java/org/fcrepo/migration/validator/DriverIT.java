/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

/**
 * Drives the command line entry point end to end so that argument binding, report selection, and the picocli
 * exception handling are all exercised.
 *
 * @author Dan Field
 */
public class DriverIT {

    private static final Path FIXTURES_BASE_DIR = Path.of("src", "test", "resources", "test-object-validation");
    private static final Path RESULTS_DIR = Path.of("target", "test", "results-driver-it");

    private static final Path F3_DATASTREAMS_DIR = FIXTURES_BASE_DIR.resolve("valid/f3/datastreams");
    private static final Path F3_OBJECTS_DIR = FIXTURES_BASE_DIR.resolve("valid/f3/objects");
    private static final Path F6_OCFL_ROOT_DIR = FIXTURES_BASE_DIR.resolve("valid/f6/data/ocfl-root");
    private static final Path MISSING_DIR = FIXTURES_BASE_DIR.resolve("does-not-exist");

    @After
    public void teardown() {
        FileUtils.deleteQuietly(RESULTS_DIR.toFile());
    }

    @Test
    public void testHtmlReport() {
        Driver.main(args("--report-type", "html", "--check-num-objects", "--checksum", "--debug"));

        assertTrue("Expected an html summary report", Files.exists(RESULTS_DIR.resolve("html/index.html")));
        assertTrue("Expected json results", Files.exists(RESULTS_DIR.resolve("json")));
    }

    @Test
    public void testCsvReport() {
        Driver.main(args("--report-type", "csv"));

        final var csvDir = RESULTS_DIR.resolve("csv");
        assertTrue("Expected a csv report directory", Files.exists(csvDir));
        assertTrue("Expected at least one csv report", countReports(csvDir, ".csv") > 0);
    }

    @Test
    public void testTsvReport() {
        Driver.main(args("--report-type", "tsv"));

        final var tsvDir = RESULTS_DIR.resolve("tsv");
        assertTrue("Expected a tsv report directory", Files.exists(tsvDir));
        assertTrue("Expected at least one tsv report", countReports(tsvDir, ".tsv") > 0);
    }

    @Test
    public void testHeadOnlyWithLimitAndResume() {
        Driver.main(args("--report-type", "html", "--head-only", "--inactive-as-deleted", "--failure-only",
                         "--limit", "1", "--resume"));

        assertTrue("Expected an html summary report", Files.exists(RESULTS_DIR.resolve("html/index.html")));
        assertTrue("Expected a resume file", Files.exists(RESULTS_DIR.resolve("resume.txt")));
    }

    /**
     * A missing objects directory fails inside call(), which routes through the driver's
     * IExecutionExceptionHandler. No report should be produced.
     */
    @Test
    public void testExecutionExceptionIsHandled() {
        final var exitCode = Driver.run(new String[] {
            "--source-type", "akubra",
            "--datastreams-dir", absolute(F3_DATASTREAMS_DIR),
            "--objects-dir", absolute(MISSING_DIR),
            "--ocfl-root-dir", absolute(F6_OCFL_ROOT_DIR),
            "--results-dir", RESULTS_DIR.toString()
        });

        assertNotEquals("Expected a non-zero exit code", 0, exitCode);
        assertFalse("No report should be written", Files.exists(RESULTS_DIR.resolve("html/index.html")));
    }

    /**
     * Same as above, but with debug enabled so the handler also dumps the stack trace.
     */
    @Test
    public void testExecutionExceptionIsHandledWithDebug() {
        final var exitCode = Driver.run(new String[] {
            "--source-type", "akubra",
            "--datastreams-dir", absolute(F3_DATASTREAMS_DIR),
            "--objects-dir", absolute(MISSING_DIR),
            "--ocfl-root-dir", absolute(F6_OCFL_ROOT_DIR),
            "--results-dir", RESULTS_DIR.toString(),
            "--debug"
        });

        assertNotEquals("Expected a non-zero exit code", 0, exitCode);
    }

    @Test
    public void testInvalidSourceTypeIsRejected() {
        final var exitCode = Driver.run(new String[] {
            "--source-type", "not-a-source-type",
            "--ocfl-root-dir", absolute(F6_OCFL_ROOT_DIR),
            "--results-dir", RESULTS_DIR.toString()
        });

        assertNotEquals("Expected a non-zero exit code", 0, exitCode);
    }

    /**
     * Builds a base argument list against the 'valid' akubra fixtures, appending any test specific arguments.
     */
    private String[] args(final String... additional) {
        final var base = new String[] {
            "--source-type", "akubra",
            "--datastreams-dir", absolute(F3_DATASTREAMS_DIR),
            "--objects-dir", absolute(F3_OBJECTS_DIR),
            "--ocfl-root-dir", absolute(F6_OCFL_ROOT_DIR),
            "--results-dir", RESULTS_DIR.toString(),
            "--threads", "1"
        };

        return Stream.concat(Stream.of(base), Stream.of(additional)).toArray(String[]::new);
    }

    private String absolute(final Path path) {
        return path.toAbsolutePath().toString();
    }

    private long countReports(final Path directory, final String extension) {
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().endsWith(extension)).count();
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to list " + directory, e);
        }
    }
}
