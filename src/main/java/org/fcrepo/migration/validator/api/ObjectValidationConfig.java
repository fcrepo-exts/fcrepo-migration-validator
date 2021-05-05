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
package org.fcrepo.migration.validator.api;

import org.fcrepo.migration.validator.impl.F6DigestAlgorithm;

/**
 * Hold some configuration options for validation objects
 *
 * @author mikejritter
 */
public class ObjectValidationConfig {

    private final boolean checksum;
    private final boolean deleteInactive;
    private final boolean validateHeadOnly;
    private final F6DigestAlgorithm digestAlgorithm;

    public ObjectValidationConfig(final boolean checksum,
                                  final boolean deleteInactive,
                                  final boolean validateHeadOnly,
                                  final F6DigestAlgorithm digestAlgorithm) {
        this.checksum = checksum;
        this.deleteInactive = deleteInactive;
        this.validateHeadOnly = validateHeadOnly;
        this.digestAlgorithm = digestAlgorithm;
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

    public F6DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }
}
