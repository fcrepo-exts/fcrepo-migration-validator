package org.fcrepo.migration.validator.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
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

    public enum ColumnSeparator {
        comma(','), tab('\t'), pipe('|');

        private final char separator;

        ColumnSeparator(final char separator) {
            this.separator = separator;
        }
    }

    private final Path outputDir;
    private final ColumnSeparator separator;

    /**
     * Constructor
     *
     * @param outputDir the directory to write csv outputs
     * @param separator the column separator use
     */
    public CsvReportHandler(final Path outputDir, final ColumnSeparator separator) {
        this.outputDir = outputDir;
        this.separator = separator;

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
        final var mapper = new CsvMapper();
        final var schema = mapper.schemaFor(ValidationResult.class)
                                 .withHeader()
                                 .withColumnSeparator(separator.separator);

        final var csvWriter = mapper.writer(schema);
        final var csvFile = outputDir.resolve(objectValidationResults.getObjectId() + ".csv");
        try (final var fileWriter = Files.newBufferedWriter(csvFile)) {
            csvWriter.writeValues(fileWriter).writeAll(objectValidationResults.getResults());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return csvFile.toString();
    }

    @Override
    public String validationSummary(final ValidationResultsSummary validationSummary) {
        final var formatter = DateTimeFormatter.ISO_DATE_TIME;
        return outputDir.resolve("fedora-validation-summary-" + LocalDateTime.now().format(formatter) + ".csv").toString();
    }

    @Override
    public void endReport() {
        // no-op
    }
}
