/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.ocfl.api.exception.OcflInputException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the source-type specific wiring and pid file handling in {@link ApplicationConfigurationHelper}.
 *
 * @author mikejritter
 */
public class ApplicationConfigurationHelperIT {

    private static final File FIXTURES_BASE_DIR = new File("src/test/resources/test-object-validation");
    private static final File F3_OBJECTS_DIR = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
    private static final File F6_OCFL_ROOT_DIR = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");

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

        assertThat(new ApplicationConfigurationHelper(config).objectSource()).isNotNull();
    }

    @Test
    public void testExportedObjectSourceRequiresExportedDirectory() {
        final var config = baseConfig();
        config.setSourceType(F3SourceTypes.EXPORTED);

        final var helper = new ApplicationConfigurationHelper(config);
        assertThatThrownBy(helper::objectSource).isInstanceOf(OcflInputException.class);
    }

    @Test
    public void testLegacyObjectSource() throws IOException {
        final var datastreamsDir = Files.createDirectories(workDir.resolve("legacy-datastreams"));
        final var config = baseConfig();
        config.setSourceType(F3SourceTypes.LEGACY);
        config.setDatastreamsDirectory(datastreamsDir.toFile());
        config.setObjectsDirectory(F3_OBJECTS_DIR);

        assertThat(new ApplicationConfigurationHelper(config).objectSource()).isNotNull();
    }

    @Test
    public void testLegacyObjectSourceRequiresObjectsDirectoryToExist() throws IOException {
        final var datastreamsDir = Files.createDirectories(workDir.resolve("legacy-datastreams-missing-objects"));
        final var config = baseConfig();
        config.setSourceType(F3SourceTypes.LEGACY);
        config.setDatastreamsDirectory(datastreamsDir.toFile());
        config.setObjectsDirectory(new File(FIXTURES_BASE_DIR, "does-not-exist"));

        final var helper = new ApplicationConfigurationHelper(config);
        assertThatThrownBy(helper::objectSource).isInstanceOf(OcflInputException.class);
    }

    @Test
    public void testReadObjectsToValidate() throws IOException {
        final var pidFile = workDir.resolve("pids.txt");
        Files.write(pidFile, java.util.List.of("object-1", "object-2"));

        final var config = baseConfig();
        config.setObjectsToValidate(pidFile.toFile());

        assertThat(new ApplicationConfigurationHelper(config).readObjectsToValidate())
            .containsExactlyInAnyOrder("object-1", "object-2");
    }

    @Test
    public void testReadObjectsToValidateWithoutPidFile() {
        assertThat(new ApplicationConfigurationHelper(baseConfig()).readObjectsToValidate()).isEmpty();
    }

    @Test
    public void testReadObjectsToValidateFailsOnMissingPidFile() {
        final var config = baseConfig();
        config.setObjectsToValidate(workDir.resolve("no-such-pid-file.txt").toFile());

        final var helper = new ApplicationConfigurationHelper(config);
        assertThatThrownBy(helper::readObjectsToValidate)
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void testOcflComponents() {
        final var helper = new ApplicationConfigurationHelper(baseConfig());

        assertThat(helper.ocflRepository()).isNotNull();
        assertThat(helper.ocflObjectSessionFactory()).isNotNull();
        assertThat(helper.getObjectValidationConfig()).isNotNull();
        assertThat(helper.resumeManager()).isNotNull();
        assertThat(helper.validationResultWriter()).isNotNull();
        assertThat(helper.getThreadCount()).isEqualTo(1);
        assertThat(helper.getLimit()).isZero();
        assertThat(helper.checkNumObjects()).isFalse();
    }

    private Fedora3ValidationConfig baseConfig() {
        final var config = new Fedora3ValidationConfig();
        config.setSourceType(F3SourceTypes.AKUBRA);
        config.setThreadCount(1);
        config.setResultsDirectory(workDir.resolve("results"));
        config.setOcflRepositoryRootDirectory(F6_OCFL_ROOT_DIR);
        config.setDigestAlgorithm(F6DigestAlgorithm.sha512);
        return config;
    }
}
