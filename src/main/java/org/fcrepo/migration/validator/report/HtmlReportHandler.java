/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.report;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.fcrepo.migration.validator.api.ObjectReportSummary;
import org.fcrepo.migration.validator.api.ObjectValidationResults;
import org.fcrepo.migration.validator.api.ReportHandler;
import org.fcrepo.migration.validator.api.ValidationResultsSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static freemarker.template.Configuration.VERSION_2_3_30;
import static freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER;

/**
 * This class is responsible for writing the HTML to disk for a given validation report
 *
 * @author awoods
 * @since 2020-12-16
 */
public class HtmlReportHandler implements ReportHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlReportHandler.class);

    private final File outputDir;
    private final Configuration config;
    private final long numProcessed;

    /**
     * Constructor
     * @param outputDir to which the HTML files are written
     * @param numProcessed the number of PIDs validated in the current run
     */
    public HtmlReportHandler(final Path outputDir, final long numProcessed) {
        this.outputDir = outputDir.toFile();
        this.numProcessed = numProcessed;

        // Setup FreeMarker template
        this.config = new Configuration(VERSION_2_3_30);
        this.outputDir.mkdirs();
        config.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "/templates");

        // These configuration items are set per the FreeMarker guidance:
        // https://freemarker.apache.org/docs/pgui_quickstart_createconfiguration.html
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(RETHROW_HANDLER);
        config.setLogTemplateExceptions(false);
        config.setWrapUncheckedExceptions(true);
        config.setFallbackOnNullLoopVariable(false);
    }

    @Override
    public void beginReport() {
        // Intentionally empty for now
    }

    /**
     * This method write the HTML report for a single object
     *
     * @param objectValidationResults An individual object validation report
     * @return filename of HTML object report
     */
    @Override
    public String objectLevelReport(final ObjectValidationResults objectValidationResults) {
        final var id = objectValidationResults.getEncodedObjectId();
        final var filename = id + ".html";
        final var success = objectValidationResults.getPassed();
        final var errors = objectValidationResults.getErrors();

        // Organize data for template
        final Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("success", success);
        data.put("successCount", success.size());
        data.put("errors", errors);
        data.put("errorCount", errors.size());

        final var file = new File(outputDir, filename);
        file.getParentFile().mkdirs();
        try (final var writer = new FileWriter(file)) {
            final Template template = config.getTemplate("object.ftl");
            template.process(data, writer);
        } catch (final IOException | TemplateException e) {
            LOGGER.error("Unable to write report {}", filename, e);
            throw new RuntimeException(e);
        }

        return filename;
    }

    @Override
    public String repositoryLevelReport(final ObjectValidationResults objectValidationResults) {
        final String filename = "repository.html";

        final var file = new File(outputDir, filename);
        try (final var writer = new FileWriter(file)) {
            final Template template = config.getTemplate("repository.ftl");
            final Map<String, Object> data = new HashMap<>();
            data.put("validations", objectValidationResults.getResults());
            template.process(data, writer);
        } catch (final IOException | TemplateException e) {
            LOGGER.error("Unable to write report {}", filename, e);
            throw new RuntimeException(e);
        }

        return filename;
    }

    /**
     * This method writes the HTML summary of the entire validation run
     *
     * @param validationSummary of this validation run
     * @return filename of full report
     */
    @Override
    public String validationSummary(final ValidationResultsSummary validationSummary) {
        final String reportFilename = "index.html";

        // Ensure we have a 'validationSummary'
        if (validationSummary == null) {
            throw new NullPointerException("Unable to process end of report without a 'validationSummary'");
        }

        // Organize data for template
        final Map<String, Object> data = new HashMap<>();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd, HH:mm:ss z");
        final var errors = validationSummary.getObjectReports().stream()
                                            .filter(ObjectReportSummary::hasErrors)
                                            .collect(Collectors.toList());

        data.put("date", format.format(new Date()));
        data.put("objectCount", validationSummary.getObjectReports().size());
        data.put("objects", validationSummary.getObjectReports());
        data.put("errors", errors);
        data.put("errorCount", errors.size());
        data.put("numProcessed", numProcessed);

        final var file = new File(outputDir, reportFilename);
        try (final var writer = new FileWriter(file)) {
            final Template template = config.getTemplate("summary.ftl");
            template.process(data, writer);
        } catch (final IOException | TemplateException e) {
            LOGGER.error("Unable to write report {}", reportFilename, e);
            throw new RuntimeException(e);
        }

        return reportFilename;
    }

    @Override
    public void endReport() {
        // Intentionally empty for now
    }
}
