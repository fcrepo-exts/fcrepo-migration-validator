/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import java.io.File;
import java.nio.file.Path;

import edu.wisc.library.ocfl.api.OcflRepository;
import org.fcrepo.migration.validator.impl.F6DigestAlgorithm;

/**
 * Hold some configuration options for validation objects
 *
 * @author mikejritter
 */
public class ObjectValidationConfig {

    private final Path ocflRoot;
    private final boolean checksum;
    private final boolean deleteInactive;
    private final boolean validateHeadOnly;
    private final OcflRepository ocflRepository;
    private final F6DigestAlgorithm digestAlgorithm;

    public ObjectValidationConfig(final File ocflRoot,
                                  final boolean checksum,
                                  final boolean deleteInactive,
                                  final boolean validateHeadOnly,
                                  final OcflRepository ocflRepository,
                                  final F6DigestAlgorithm digestAlgorithm) {
        this.ocflRoot = ocflRoot.toPath();
        this.checksum = checksum;
        this.deleteInactive = deleteInactive;
        this.validateHeadOnly = validateHeadOnly;
        this.ocflRepository = ocflRepository;
        this.digestAlgorithm = digestAlgorithm;
    }

    public Path getOcflRoot() {
        return ocflRoot;
    }

    public boolean isChecksum() {
        return checksum;
    }

    public boolean deleteInactive() {
        return deleteInactive;
    }

    public boolean isValidateHeadOnly() {
        return validateHeadOnly;
    }

    public OcflRepository getOcflRepository() {
        return ocflRepository;
    }

    public F6DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }
}
