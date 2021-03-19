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
import org.fcrepo.migration.validator.api.ObjectValidationResults;
import org.fcrepo.migration.validator.api.ObjectReportSummary;
import org.fcrepo.migration.validator.api.ReportHandler;
import org.fcrepo.migration.validator.api.ValidationResultsSummary;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.impl.FileSystemValidationResultReader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This concrete implementation orchestrates the generation of the validation report
 *
 * @author dbernstein
 * @author awoods
 * @since 2020-12-16
 */
public class ReportGeneratorImpl {

    private static final Logger LOGGER = getLogger(ReportGeneratorImpl.class);

    private Path resultDir;
    private ReportHandler reportHandler;
    private ValidationResultsSummary summary;

    /**
     * Constructor
     *
     * @param resultDir where validation result files are located
     * @param reportHandler that writes the report to disk
     */
    public ReportGeneratorImpl(final Path resultDir, final ReportHandler reportHandler) {
        this.resultDir = resultDir;
        this.reportHandler = reportHandler;
        this.summary = new ValidationResultsSummary();
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
        // iterate through the validation result (JSON) files
        Files.walkFileTree(resultDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {

                // If file is a validation result (i.e. result-*.json, ValidationResultUtils.resolvePathToJsonResult)
                // ..and the containing object has not already been loaded
                // ..then, load all result files as a set.
                final String objectId = file.getParent().toFile().getName();
                if (!summary.containsReport(objectId) && isValidationResultFile(file.toFile().getName())) {
                    final var reportSummary = loadValidationResults(file.getParent().toFile());

                    // Update summary with newly created object reports
                    summary.addObjectReport(objectId, reportSummary);
                } else {
                    LOGGER.debug("Not a validation result file: {}", file);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return reportHandler.validationSummary(summary);
    }

    /**
     * Validation result files have the following naming convention:
     *   result-*.json, ValidationResultUtils.resolvePathToJsonResult
     *
     * @param filename to be inspected
     * @return true if matches above naming convention
     */
    private boolean isValidationResultFile(final String filename) {
        return FilenameUtils.getExtension(filename).equalsIgnoreCase("json") && filename.startsWith("result-");
    }

    private ObjectReportSummary loadValidationResults(final File objectDir) {
        LOGGER.debug("Loading validation results from: {}", objectDir);
        final FilenameFilter filter = (dir, name) -> isValidationResultFile(name);

        final FileSystemValidationResultReader reader = new FileSystemValidationResultReader();
        final List<ValidationResult> resultsList = new ArrayList<>();
        for (final File f : Objects.requireNonNull(objectDir.listFiles(filter))) {
            resultsList.add(reader.read(f));
        }

        resultsList.sort(Comparator.comparingInt(ValidationResult::getIndex));
        final var validationResults = new ObjectValidationResults(resultsList);
        final var reportFilename = reportHandler.objectLevelReport(validationResults);
        return new ObjectReportSummary(validationResults.hasErrors(), validationResults.getObjectId(), reportFilename);
    }
}
