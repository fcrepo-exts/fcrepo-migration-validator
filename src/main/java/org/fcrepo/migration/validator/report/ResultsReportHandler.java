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
package org.fcrepo.migration.validator.report;

import org.fcrepo.migration.validator.api.ObjectValidationReport;
import org.fcrepo.migration.validator.api.ReportHandler;
import org.fcrepo.migration.validator.api.ValidationReportSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles/processes validation results by collecting detected errors
 * Although there may be production uses, this class is currently designed to facilitate testing.
 *
 * @author awoods
 * @since 2020-12-20
 */
public class ResultsReportHandler implements ReportHandler {

    private List<String> errors = new ArrayList<>();

    /**
     * A hook indicating the start of a result processing run
     */
    @Override
    public void beginReport() {

    }

    /**
     * A hook hook for processing an object level validation report
     *
     * @param objectValidationReport An individual object validation report
     * @return filename of object report
     */
    @Override
    public String objectLevelReport(final ObjectValidationReport objectValidationReport) {
        if (objectValidationReport.hasErrors()) {
            errors.add(objectValidationReport.getDetails());
        }

        // No HTML report filename
        return null;
    }

    /**
     * A hook for processing a validation run's summary info.
     *
     * @param validationSummary to be processed
     * @return filename of full report
     */
    @Override
    public String validationSummary(final ValidationReportSummary validationSummary) {
        return null;
    }

    /**
     * A hook indicating the end of a result processing run
     */
    @Override
    public void endReport() {

    }

    public List<String> getErrors() {
        return errors;
    }
}
