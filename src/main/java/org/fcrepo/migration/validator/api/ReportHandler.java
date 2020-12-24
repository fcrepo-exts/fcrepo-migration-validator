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

/**
 * An interface defining hooks for validation result processors implemented by report generators
 *
 * @author dbernstein
 */
public interface ReportHandler {

    /**
     * A hook indicating the start of a result processing run
     */
    void beginReport();

    /**
     * A hook hook for processing an object level validation report
     * @param objectValidationResults An individual object validation report
     * @return filename of object report
     */
    String objectLevelReport(ObjectValidationResults objectValidationResults);

    /**
     * A hook for processing a validation run's summary info.
     * @param validationSummary to be processed
     * @return filename of full report
     */
    String validationSummary(ValidationResultsSummary validationSummary);

    /**
     * A hook indicating the end of a result processing run
     */
    void endReport();
}
