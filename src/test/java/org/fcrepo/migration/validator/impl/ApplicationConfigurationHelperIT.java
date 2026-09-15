/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import io.ocfl.api.exception.OcflInputException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the source-type specific wiring and pid file handling in {@link ApplicationConfigurationHelper}.
 *
 * @author Dan Field
 */
public class ApplicationConfigurationHelperIT {

    private static final Path FIXTURES_BASE_DIR = Path.of("src", "test", "resources", "test-object-validation");
    private static final Path F3_OBJECTS_DIR = FIXTURES_BASE_DIR.resolve("valid/f3/objects");
    private static final Path F6_OCFL_ROOT_DIR = FIXTURES_BASE_DIR.resolve("valid/f6/data/ocfl-root");

    private Path workDir;

    @Before
    public void setup() throws IOException {
        workDir = Files.createTempDirectory("app-config-helper-it");
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(workDir.toFile());
    }

    @Test
    public void testExportedObjectSource() throws IOException {
        final var exportedDir = Files.createDirectories(workDir.resolve("exported"));
        final var config = baseConfig();
        config.setSourceType(F3SourceTypes.EXPORTED);
        config.setExportedDirectory(exportedDir.toFile());

        assertNotNull(new ApplicationConfigurationHelper(config).objectSource());
    }

    @Test
    public void testExportedObjectSourceRequiresExportedDirectory() {
        final var config = baseConfig();
        config.setSourceType(F3SourceTypes.EXPORTED);

        final var helper = new ApplicationConfigurationHelper(config);
        assertThrows(OcflInputException.class, helper::objectSource);
    }

    @Test
    public void testLegacyObjectSource() throws IOException {
        final var datastreamsDir = Files.createDirectories(workDir.resolve("legacy-datastreams"));
        final var config = baseConfig();
        config.setSourceType(F3SourceTypes.LEGACY);
        config.setDatastreamsDirectory(datastreamsDir.toFile());
        config.setObjectsDirectory(F3_OBJECTS_DIR.toFile());

        assertNotNull(new ApplicationConfigurationHelper(config).objectSource());
    }

    @Test
    public void testLegacyObjectSourceRequiresObjectsDirectoryToExist() throws IOException {
        final var datastreamsDir = Files.createDirectories(workDir.resolve("legacy-datastreams-missing-objects"));
        final var config = baseConfig();
        config.setSourceType(F3SourceTypes.LEGACY);
        config.setDatastreamsDirectory(datastreamsDir.toFile());
        config.setObjectsDirectory(FIXTURES_BASE_DIR.resolve("does-not-exist").toFile());

        final var helper = new ApplicationConfigurationHelper(config);
        assertThrows(OcflInputException.class, helper::objectSource);
    }

    @Test
    public void testReadObjectsToValidate() throws IOException {
        final var pidFile = Files.write(workDir.resolve("pids.txt"), List.of("object-1", "object-2"));

        final var config = baseConfig();
        config.setObjectsToValidate(pidFile.toFile());

        assertEquals(Set.of("object-1", "object-2"),
                     new ApplicationConfigurationHelper(config).readObjectsToValidate());
    }

    @Test
    public void testReadObjectsToValidateWithoutPidFile() {
        assertTrue(new ApplicationConfigurationHelper(baseConfig()).readObjectsToValidate().isEmpty());
    }

    @Test
    public void testReadObjectsToValidateFailsOnMissingPidFile() {
        final var config = baseConfig();
        config.setObjectsToValidate(workDir.resolve("no-such-pid-file.txt").toFile());

        final var helper = new ApplicationConfigurationHelper(config);
        final var exception = assertThrows(RuntimeException.class, helper::readObjectsToValidate);
        assertTrue("Expected the IOException to be wrapped", exception.getCause() instanceof IOException);
    }

    @Test
    public void testOcflComponents() {
        final var helper = new ApplicationConfigurationHelper(baseConfig());

        assertNotNull(helper.ocflRepository());
        assertNotNull(helper.ocflObjectSessionFactory());
        assertNotNull(helper.getObjectValidationConfig());
        assertNotNull(helper.resumeManager());
        assertNotNull(helper.validationResultWriter());
        assertEquals(1, helper.getThreadCount());
        assertEquals(0, helper.getLimit());
        assertFalse(helper.checkNumObjects());
    }

    private Fedora3ValidationConfig baseConfig() {
        final var config = new Fedora3ValidationConfig();
        config.setSourceType(F3SourceTypes.AKUBRA);
        config.setThreadCount(1);
        config.setResultsDirectory(workDir.resolve("results"));
        config.setOcflRepositoryRootDirectory(F6_OCFL_ROOT_DIR.toFile());
        config.setDigestAlgorithm(F6DigestAlgorithm.sha512);
        return config;
    }
}
