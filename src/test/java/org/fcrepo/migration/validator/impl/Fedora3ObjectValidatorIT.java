/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.OBJECT_READABLE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.fcrepo.migration.FedoraObjectProcessor;
import org.fcrepo.migration.ObjectInfo;
import org.fcrepo.migration.StreamingFedoraObjectHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that an object which cannot be read is reported as a failed OBJECT_READABLE validation rather than
 * aborting the run.
 *
 * @author dbernstein
 */
public class Fedora3ObjectValidatorIT {

    private static final File FIXTURES_BASE_DIR = new File("src/test/resources/test-object-validation");
    private static final File F6_OCFL_ROOT_DIR = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");

    private Path workDir;
    private ApplicationConfigurationHelper helper;

    @Before
    public void setup() throws IOException {
        workDir = Files.createTempDirectory("f3-object-validator-it");

        final var config = new Fedora3ValidationConfig();
        config.setSourceType(F3SourceTypes.AKUBRA);
        config.setThreadCount(1);
        config.setResultsDirectory(workDir.resolve("results"));
        config.setOcflRepositoryRootDirectory(F6_OCFL_ROOT_DIR);
        config.setDigestAlgorithm(F6DigestAlgorithm.sha512);
        helper = new ApplicationConfigurationHelper(config);
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(workDir.toFile());
    }

    @Test
    public void testUnreadableObjectIsReported() {
        final var validator = new Fedora3ObjectValidator(helper.ocflObjectSessionFactory(),
                                                         helper.getObjectValidationConfig());

        final var results = validator.validate(new UnreadableObjectProcessor("info:fedora/unreadable"));

        assertThat(results).hasSize(1);
        final var result = results.get(0);
        assertThat(result.getStatus()).isEqualTo(FAIL);
        assertThat(result.getValidationLevel()).isEqualTo(OBJECT);
        assertThat(result.getValidationType()).isEqualTo(OBJECT_READABLE);
        assertThat(result.getDetails()).contains("Source object could not be read");
    }

    /**
     * An object with no fedora URI falls back to a pid derived id, and still reports as unreadable.
     */
    @Test
    public void testUnreadableObjectWithoutFedoraUri() {
        final var validator = new Fedora3ObjectValidator(helper.ocflObjectSessionFactory(),
                                                         helper.getObjectValidationConfig());

        final var results = validator.validate(new UnreadableObjectProcessor(null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSourceObjectId()).isEqualTo("info:fedora/unreadable-pid");
    }

    /**
     * A processor whose FOXML cannot be parsed.
     */
    private static class UnreadableObjectProcessor implements FedoraObjectProcessor {

        private final String fedoraUri;

        UnreadableObjectProcessor(final String fedoraUri) {
            this.fedoraUri = fedoraUri;
        }

        @Override
        public ObjectInfo getObjectInfo() {
            return new ObjectInfo() {
                @Override
                public String getPid() {
                    return "unreadable-pid";
                }

                @Override
                public String getFedoraURI() {
                    return fedoraUri;
                }

                @Override
                public Path getFoxmlPath() {
                    return Path.of("does-not-exist.xml");
                }
            };
        }

        @Override
        public void processObject(final StreamingFedoraObjectHandler handler) throws XMLStreamException {
            throw new XMLStreamException("Unable to parse FOXML");
        }

        @Override
        public void close() {
            // nothing to close
        }
    }
}
