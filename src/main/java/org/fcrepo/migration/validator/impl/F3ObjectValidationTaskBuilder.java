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

import org.fcrepo.migration.FedoraObjectProcessor;
import org.fcrepo.migration.validator.api.AbstractValidationTaskBuilder;

/**
 * A builder for F3ObjectValidationTask instances.
 *
 * @author dbernstein
 */
public class F3ObjectValidationTaskBuilder extends AbstractValidationTaskBuilder<F3ObjectValidationTask> {

    private boolean enableChecksums;
    private F6DigestAlgorithm digestAlgorithm;
    private FedoraObjectProcessor processor;

    @Override
    public F3ObjectValidationTask build() {
        return new F3ObjectValidationTask(processor, objectSessionFactory, writer, enableChecksums, digestAlgorithm);
    }

    /**
     * @param enableChecksums
     * @param digestAlgorithm
     */
    public F3ObjectValidationTaskBuilder enableChecksums(final boolean enableChecksums,
                                                         final F6DigestAlgorithm digestAlgorithm) {
        this.enableChecksums = enableChecksums;
        this.digestAlgorithm = digestAlgorithm;
        return this;
    }

    /**
     * @param processor
     */
    public F3ObjectValidationTaskBuilder processor(final FedoraObjectProcessor processor) {
        this.processor = processor;
        return this;
    }
}
