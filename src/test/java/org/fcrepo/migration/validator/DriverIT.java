/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
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
 * @author dbernstein
 */
public class DriverIT {

    private static final File FIXTURES_BASE_DIR = new File("src/test/resources/test-object-validation");
    private static final Path RESULTS_DIR = Path.of("target/test/results-driver-it");

    private static final File F3_DATASTREAMS_DIR = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
    private static final File F3_OBJECTS_DIR = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
    private static final File F6_OCFL_ROOT_DIR = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");

    @After
    public void teardown() {
        FileUtils.deleteQuietly(RESULTS_DIR.toFile());
    }

    @Test
    public void testHtmlReport() {
        Driver.main(args("--report-type", "html", "--check-num-objects", "--checksum", "--debug"));

        assertThat(RESULTS_DIR.resolve("html").resolve("index.html")).exists();
        assertThat(RESULTS_DIR.resolve("json")).exists();
    }

    @Test
    public void testCsvReport() {
        Driver.main(args("--report-type", "csv"));

        final var csvDir = RESULTS_DIR.resolve("csv");
        assertThat(csvDir).exists();
        assertThat(summaryReports(csvDir, ".csv")).isPositive();
    }

    @Test
    public void testTsvReport() {
        Driver.main(args("--report-type", "tsv"));

        final var tsvDir = RESULTS_DIR.resolve("tsv");
        assertThat(tsvDir).exists();
        assertThat(summaryReports(tsvDir, ".tsv")).isPositive();
    }

    @Test
    public void testHeadOnlyWithLimitAndResume() {
        Driver.main(args("--report-type", "html", "--head-only", "--inactive-as-deleted", "--failure-only",
                         "--limit", "1", "--resume"));

        assertThat(RESULTS_DIR.resolve("html").resolve("index.html")).exists();
        assertThat(RESULTS_DIR.resolve("resume.txt")).exists();
    }

    /**
     * A missing objects directory fails inside {@code call()}, which routes through the driver's
     * {@code IExecutionExceptionHandler}. No report should be produced.
     */
    @Test
    public void testExecutionExceptionIsHandled() {
        final var exitCode = execute("--source-type", "akubra",
                                     "--datastreams-dir", F3_DATASTREAMS_DIR.getAbsolutePath(),
                                     "--objects-dir", new File(FIXTURES_BASE_DIR, "does-not-exist").getAbsolutePath(),
                                     "--ocfl-root-dir", F6_OCFL_ROOT_DIR.getAbsolutePath(),
                                     "--results-dir", RESULTS_DIR.toString());

        assertThat(exitCode).isNotZero();
        assertThat(RESULTS_DIR.resolve("html").resolve("index.html")).doesNotExist();
    }

    /**
     * Same as above, but with debug enabled so the handler also dumps the stack trace.
     */
    @Test
    public void testExecutionExceptionIsHandledWithDebug() {
        final var exitCode = execute("--source-type", "akubra",
                                     "--datastreams-dir", F3_DATASTREAMS_DIR.getAbsolutePath(),
                                     "--objects-dir", new File(FIXTURES_BASE_DIR, "does-not-exist").getAbsolutePath(),
                                     "--ocfl-root-dir", F6_OCFL_ROOT_DIR.getAbsolutePath(),
                                     "--results-dir", RESULTS_DIR.toString(),
                                     "--debug");

        assertThat(exitCode).isNotZero();
    }

    @Test
    public void testInvalidSourceTypeIsRejected() {
        final var exitCode = execute("--source-type", "not-a-source-type",
                                     "--ocfl-root-dir", F6_OCFL_ROOT_DIR.getAbsolutePath(),
                                     "--results-dir", RESULTS_DIR.toString());

        assertThat(exitCode).isNotZero();
    }

    private int execute(final String... args) {
        return Driver.run(args);
    }

    /**
     * Builds a base argument list against the 'valid' akubra fixtures, appending any test specific arguments.
     */
    private String[] args(final String... additional) {
        final var base = new String[] {
            "--source-type", "akubra",
            "--datastreams-dir", F3_DATASTREAMS_DIR.getAbsolutePath(),
            "--objects-dir", F3_OBJECTS_DIR.getAbsolutePath(),
            "--ocfl-root-dir", F6_OCFL_ROOT_DIR.getAbsolutePath(),
            "--results-dir", RESULTS_DIR.toString(),
            "--threads", "1"
        };

        return Stream.concat(Stream.of(base), Stream.of(additional)).toArray(String[]::new);
    }

    private long summaryReports(final Path directory, final String extension) {
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().endsWith(extension)).count();
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to list " + directory, e);
        }
    }
}
