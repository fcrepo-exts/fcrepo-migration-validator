/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.OBJECT_READABLE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.fcrepo.migration.validator.api.ObjectReportSummary;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResultsSummary;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Round trips validation results through the filesystem writer and reader, and covers the results summary.
 *
 * @author awoods
 */
public class ValidationResultIoTest {

    private Path workDir;

    @Before
    public void setup() throws IOException {
        workDir = Files.createTempDirectory("validation-result-io-test");
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(workDir.toFile());
    }

    @Test
    public void testWriteThenRead() {
        final var jsonRoot = workDir.resolve("json");
        final var writer = new FileSystemValidationResultWriter(jsonRoot, false);
        final var result = new ValidationResult(0, OK, OBJECT, OBJECT_READABLE, "object-1", "info:fedora/object-1",
                                                "all good");
        writer.write(List.of(result));

        final var written = ValidationResultUtils.resolvePathToJsonResult(result, id -> id);
        final var read = new FileSystemValidationResultReader().read(jsonRoot.resolve(written).toFile());

        assertThat(read.getStatus()).isEqualTo(OK);
        assertThat(read.getSourceObjectId()).isEqualTo("object-1");
        assertThat(read.getDetails()).isEqualTo("all good");
    }

    @Test
    public void testWriteFailureOnlySkipsPassingResults() {
        final var jsonRoot = workDir.resolve("failure-only");
        final var writer = new FileSystemValidationResultWriter(jsonRoot, true);
        final var passed = new ValidationResult(0, OK, OBJECT, OBJECT_READABLE, "object-1", "info:fedora/object-1",
                                                "all good");
        final var failed = new ValidationResult(1, FAIL, OBJECT, OBJECT_READABLE, "object-1", "info:fedora/object-1",
                                                "not good");
        writer.write(List.of(passed, failed));

        assertThat(jsonRoot.resolve(ValidationResultUtils.resolvePathToJsonResult(passed, id -> id))).doesNotExist();
        assertThat(jsonRoot.resolve(ValidationResultUtils.resolvePathToJsonResult(failed, id -> id))).exists();
    }

    @Test
    public void testReadFailsOnMissingFile() {
        final var reader = new FileSystemValidationResultReader();
        final var missing = workDir.resolve("no-such-result.json").toFile();

        assertThatThrownBy(() -> reader.read(missing)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testResultsSummary() {
        final var summary = new ValidationResultsSummary();
        final var objectReport = new ObjectReportSummary(true, "object-1", "object-1.html");
        final var repositoryReport = new ObjectReportSummary(false, "repository", "repository.html");

        summary.addObjectReport("object-1", objectReport);
        summary.addRepositoryReport(repositoryReport);

        assertThat(summary.containsReport("object-1")).isTrue();
        assertThat(summary.getObjectReports()).containsExactly(objectReport);
        assertThat(summary.getRepositoryReport()).isEqualTo(repositoryReport);
        assertThat(objectReport.getReportHref()).isEqualTo("object-1.html");
    }

    @Test
    public void testResultsSummaryRejectsDuplicateReport() {
        final var summary = new ValidationResultsSummary();
        summary.addObjectReport("object-1", new ObjectReportSummary(true, "object-1", "object-1.html"));

        assertThatThrownBy(() -> summary.addObjectReport("object-1",
                                                         new ObjectReportSummary(true, "object-1", "object-1.html")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
