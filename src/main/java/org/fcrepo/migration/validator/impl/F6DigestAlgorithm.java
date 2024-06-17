/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import io.ocfl.api.model.DigestAlgorithm;

/**
 * Supported digest algorithms for ocfl
 *
 * @author mikejritter
 */
public enum F6DigestAlgorithm {
    sha256(DigestAlgorithm.sha256.getJavaStandardName(), Hashing.sha256()),
    sha512(DigestAlgorithm.sha512.getJavaStandardName(), Hashing.sha512());

    private final String name;
    private final HashFunction hashFunction;

    F6DigestAlgorithm(final String name, final HashFunction hashFunction) {
        this.name = name;
        this.hashFunction = hashFunction;
    }

    public String getName() {
        return name;
    }

    public String getOcflUrn() {
        return "urn:" + name;
    }

    public Hasher hasher() {
        return hashFunction.newHasher();
    }

}
