/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;

import org.fcrepo.migration.validator.impl.Fedora3ValidationConfig;
import org.fcrepo.migration.validator.report.ReportType;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the report directory resolution and accessors on the validation config.
 *
 * @author Dan Field
 */
public class ValidationConfigTest {

    private static final Path RESULTS_DIR = Path.of("target", "test", "validation-config-test");
    private static final Path OCFL_ROOT_DIR = Path.of("target", "test", "ocfl-root");

    private Fedora3ValidationConfig config;

    @Before
    public void setup() {
        config = new Fedora3ValidationConfig();
        config.setResultsDirectory(RESULTS_DIR);
        config.setOcflRepositoryRootDirectory(OCFL_ROOT_DIR.toFile());
    }

    @Test
    public void testReportDirectories() {
        assertEquals(RESULTS_DIR.resolve("html"), config.getReportDirectory(ReportType.html));
        assertEquals(RESULTS_DIR.resolve("csv"), config.getReportDirectory(ReportType.csv));
        assertEquals(RESULTS_DIR.resolve("tsv"), config.getReportDirectory(ReportType.tsv));
        assertEquals(RESULTS_DIR.resolve("json"), config.getJsonOutputDirectory());
    }

    @Test
    public void testAccessors() {
        final var indexDir = Path.of("target", "test", "index");
        final var pidFile = Path.of("target", "test", "pids.txt");

        config.setThreadCount(4);
        config.setIndexDirectory(indexDir.toFile());
        config.setFedora3Hostname("fedora.info");
        config.setObjectsToValidate(pidFile.toFile());
        config.setEnableChecksums(true);
        config.setLimit(10);
        config.setResume(true);
        config.setFailureOnly(true);
        config.setDeleteInactive(true);
        config.setValidateHeadOnly(true);
        config.setCheckNumObjects(true);

        assertEquals(4, config.getThreadCount());
        assertEquals(indexDir.toFile(), config.getIndexDirectory());
        assertEquals("fedora.info", config.getFedora3Hostname());
        assertEquals(pidFile.toFile(), config.getObjectsToValidate());
        assertTrue(config.enableChecksums());
        assertEquals(10, config.getLimit());
        assertTrue(config.isResume());
        assertTrue(config.isFailureOnly());
        assertTrue(config.isDeleteInactive());
        assertTrue(config.validateHeadOnly());
        assertTrue(config.checkNumObjects());
        assertEquals(OCFL_ROOT_DIR.toFile(), config.getOcflRepositoryRootDirectory());
        assertTrue("Expected the config to describe itself", config.toString().contains("threadCount"));
    }
}
