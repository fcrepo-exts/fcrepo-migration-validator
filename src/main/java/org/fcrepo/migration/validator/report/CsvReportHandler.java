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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.fcrepo.migration.validator.api.ObjectReportSummary;
import org.fcrepo.migration.validator.api.ObjectValidationResults;
import org.fcrepo.migration.validator.api.ReportHandler;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResultsSummary;

/**
 * Report handler for csv output
 *
 * @author mikejritter
 */
public class CsvReportHandler implements ReportHandler {

    private final Path outputDir;
    private final ReportType reportType;

    /**
     * Constructor
     *
     * @param outputDir the directory to write csv outputs
     * @param reportType the report type to generate, only csv or tsv
     */
    public CsvReportHandler(final Path outputDir, final ReportType reportType) {
        if (reportType == ReportType.html) {
            throw new IllegalArgumentException("Invalid report type given, must be csv or tsv");
        }

        this.outputDir = outputDir;
        this.reportType = reportType;
        try {
            Files.createDirectories(outputDir);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void beginReport() {
        // no-op
    }

    @Override
    public String objectLevelReport(final ObjectValidationResults objectValidationResults) {
        final var csvFile = outputDir.resolve(objectValidationResults.getObjectId() + reportType.getExtension());
        return writeValidationResults(csvFile, objectValidationResults);
    }

    @Override
    public String validationSummary(final ValidationResultsSummary validationSummary) {
        final var mapper = new CsvMapper();
        final var formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        final var csvFile = outputDir.resolve("migration-validation-summary" +
                                              LocalDate.now().format(formatter) + reportType.getExtension());
        final var schema = mapper.schemaFor(ObjectReportSummary.class)
                                 .withHeader()
                                 .withColumnSeparator(reportType.getSeparator());

        try (var fileWriter = Files.newBufferedWriter(csvFile);
             var csvWriter = mapper.writer(schema).writeValues(fileWriter)) {
            csvWriter.writeAll(validationSummary.getObjectReports());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return csvFile.toString();
    }

    @Override
    public String repositoryLevelReport(final ObjectValidationResults objectValidationResults) {
        final var csvFile = outputDir.resolve("repository" + reportType.getExtension());
        return writeValidationResults(csvFile, objectValidationResults);
    }

    private String writeValidationResults(final Path file, final ObjectValidationResults objectValidationResults) {
        final var mapper = CsvMapper.builder()
                                    .addMixIn(ValidationResult.class, ValidationResultMixin.class)
                                    .build();
        final var schema = mapper.schemaFor(ValidationResult.class)
                                 .withHeader()
                                 .withColumnSeparator(reportType.getSeparator());

        try (var fileWriter = Files.newBufferedWriter(file);
             var csvWriter = mapper.writer(schema).writeValues(fileWriter)) {
            csvWriter.writeAll(objectValidationResults.getResults());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return file.toString();
    }

    @Override
    public void endReport() {
        // no-op
    }

    /**
     * Mixin for csv specific changes
     */
    @JsonPropertyOrder({"status", "validationLevel", "validationType", "details", "sourceObjectId", "sourceResourceId",
                        "targetObjectId", "targetResourceId"})
    public abstract static class ValidationResultMixin {

        @JsonIgnore
        abstract int getIndex();

    }
}
