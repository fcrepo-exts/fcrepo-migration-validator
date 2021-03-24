/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.migration.validator.impl;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import edu.wisc.library.ocfl.api.model.DigestAlgorithm;

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
