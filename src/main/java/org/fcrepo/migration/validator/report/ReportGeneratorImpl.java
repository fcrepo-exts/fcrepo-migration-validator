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

import org.apache.commons.io.FilenameUtils;
import org.fcrepo.migration.validator.api.ObjectValidationReport;
import org.fcrepo.migration.validator.api.ReportHandler;
import org.fcrepo.migration.validator.api.ValidationReportSummary;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.impl.FileSystemValidationResultReader;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * This concrete implementation orchestrates the generation of the validation report
 *
 * @author dbernstein
 * @author awoods
 * @since 2020-12-16
 */
public class ReportGeneratorImpl {

    private Path resultDir;
    private ReportHandler reportHandler;
    private ValidationReportSummary summary;

    /**
     * Constructor
     *
     * @param resultDir where validation result files are located
     * @param reportHandler that writes the report to disk
     */
    public ReportGeneratorImpl(final Path resultDir, final ReportHandler reportHandler) {
        this.resultDir = resultDir;
        this.reportHandler = reportHandler;
        this.summary = new ValidationReportSummary();
    }

    /**
     * This method starts the report generation
     */
    public String generate() {
        reportHandler.beginReport();
        final String reportFilename = processResults();
        reportHandler.endReport();

        return reportFilename;
    }

    private String processResults() {
        try {
            return doProcessResults();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String doProcessResults() throws IOException {
        final FileSystemValidationResultReader reader = new FileSystemValidationResultReader();

        // iterate through the validation result (JSON) files
        Files.walkFileTree(resultDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {

                // If file is a validation result (i.e. *.json)
                final String extension = FilenameUtils.getExtension(file.toFile().getName());
                if (extension.equalsIgnoreCase("json")) {
                    final ValidationResult validationResult = reader.read(file.toFile());
                    final String reportFilename =
                            reportHandler.objectLevelReport(new ObjectValidationReport(validationResult));

                    // Update summary with newly created object reports
                    summary.addObjectReport(reportFilename);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return reportHandler.validationSummary(summary);
    }
}
