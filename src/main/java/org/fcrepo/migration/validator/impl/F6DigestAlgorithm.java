package org.fcrepo.migration.validator.impl;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import edu.wisc.library.ocfl.api.model.DigestAlgorithm;

/**
 * Supported digest algorithms for ocfl
 */
public enum F6DigestAlgorithm {
    sha256(DigestAlgorithm.sha256.getOcflName(), Hashing.sha256()),
    sha512(DigestAlgorithm.sha512.getOcflName(), Hashing.sha512());

    private final String ocflName;
    private final HashFunction hashFunction;

    F6DigestAlgorithm(String ocflName, HashFunction hashFunction) {
        this.ocflName = ocflName;
        this.hashFunction = hashFunction;
    }

    public String getOcflName() {
        return ocflName;
    }

    public String getOcflUrn() {
        return "urn:" + ocflName;
    }

    public Hasher hasher() {
        return hashFunction.newHasher();
    }

}
