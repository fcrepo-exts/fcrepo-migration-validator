/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.fcrepo.migration.validator.impl.Fedora3ValidationConfig;
import org.fcrepo.migration.validator.report.ReportType;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the report directory resolution and accessors on the validation config.
 *
 * @author dbernstein
 */
public class ValidationConfigTest {

    private static final Path RESULTS_DIR = Path.of("target", "test", "validation-config-test");

    private Fedora3ValidationConfig config;

    @Before
    public void setup() {
        config = new Fedora3ValidationConfig();
        config.setResultsDirectory(RESULTS_DIR);
        config.setOcflRepositoryRootDirectory(new File("target/test/ocfl-root"));
    }

    @Test
    public void testReportDirectories() {
        assertThat(config.getReportDirectory(ReportType.html)).isEqualTo(RESULTS_DIR.resolve("html"));
        assertThat(config.getReportDirectory(ReportType.csv)).isEqualTo(RESULTS_DIR.resolve("csv"));
        assertThat(config.getReportDirectory(ReportType.tsv)).isEqualTo(RESULTS_DIR.resolve("tsv"));
        assertThat(config.getJsonOutputDirectory()).isEqualTo(RESULTS_DIR.resolve("json"));
    }

    @Test
    public void testAccessors() {
        final var indexDir = new File("target/test/index");
        final var pidFile = new File("target/test/pids.txt");

        config.setThreadCount(4);
        config.setIndexDirectory(indexDir);
        config.setFedora3Hostname("fedora.info");
        config.setObjectsToValidate(pidFile);
        config.setEnableChecksums(true);
        config.setLimit(10);
        config.setResume(true);
        config.setFailureOnly(true);
        config.setDeleteInactive(true);
        config.setValidateHeadOnly(true);
        config.setCheckNumObjects(true);

        assertThat(config.getThreadCount()).isEqualTo(4);
        assertThat(config.getIndexDirectory()).isEqualTo(indexDir);
        assertThat(config.getFedora3Hostname()).isEqualTo("fedora.info");
        assertThat(config.getObjectsToValidate()).isEqualTo(pidFile);
        assertThat(config.enableChecksums()).isTrue();
        assertThat(config.getLimit()).isEqualTo(10);
        assertThat(config.isResume()).isTrue();
        assertThat(config.isFailureOnly()).isTrue();
        assertThat(config.isDeleteInactive()).isTrue();
        assertThat(config.validateHeadOnly()).isTrue();
        assertThat(config.checkNumObjects()).isTrue();
        assertThat(config.getOcflRepositoryRootDirectory()).isEqualTo(new File("target/test/ocfl-root"));
        assertThat(config.toString()).contains("threadCount");
    }
}
