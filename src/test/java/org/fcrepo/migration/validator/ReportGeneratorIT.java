/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.ocfl.api.OcflRepository;
import org.fcrepo.migration.validator.impl.ApplicationConfigurationHelper;
import org.fcrepo.migration.validator.impl.Fedora3ValidationExecutionManager;
import org.fcrepo.migration.validator.report.CsvReportHandler;
import org.fcrepo.migration.validator.report.HtmlReportHandler;
import org.fcrepo.migration.validator.report.ReportGeneratorImpl;
import org.fcrepo.migration.validator.report.ReportType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author mikejritter
 */
@RunWith(Parameterized.class)
public class ReportGeneratorIT extends AbstractValidationIT {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { ReportType.html }, { ReportType.csv }
        });
    }

    /**
     * Regex to ignore non-object reports
     */
    private static final Predicate<String> IGNORE = Pattern.compile("index|repository|migration-validation-summary")
               .asPredicate()
               .negate();

    private final ReportType reportType;

    public ReportGeneratorIT(final ReportType reportType) {
        this.reportType = reportType;
    }

    @Test
    public void testAllPass() throws Exception {
        final var f3ObjectsDir = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
        final var f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final var f6OcflRootDir = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");

        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);
        final var reportDir = config.getReportDirectory(reportType);

        final var configHelper = new ApplicationConfigurationHelper(config);
        final var executionManager = new Fedora3ValidationExecutionManager(configHelper);
        executionManager.doValidation();

        final var numProcessed = executionManager.getNumProcessed();
        final var handler = reportType == ReportType.html ? new HtmlReportHandler(reportDir, numProcessed)
                                                          : new CsvReportHandler(reportDir, reportType);
        final var generator = new ReportGeneratorImpl(config.getJsonOutputDirectory(), handler);
        generator.generate();

        final var ocflRepository = configHelper.ocflRepository();
        final var objectIds = getExpectedObjectReports(ocflRepository, reportType);

        // get object reports and validate
        final var objectReports = getObjectReports(reportDir, IGNORE);
        assertThat(objectReports)
            .hasSize(objectIds.size())
            .allMatch(objectIds::contains);
    }

    @Test
    public void testOnlyWriteFailureAllPass() throws IOException {
        final var f3ObjectsDir = new File(FIXTURES_BASE_DIR, "valid/f3/objects");
        final var f3DatastreamsDir = new File(FIXTURES_BASE_DIR, "valid/f3/datastreams");
        final var f6OcflRootDir = new File(FIXTURES_BASE_DIR, "valid/f6/data/ocfl-root");

        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);
        config.setFailureOnly(true);
        final var reportDir = config.getReportDirectory(reportType);

        final var configHelper = new ApplicationConfigurationHelper(config);
        final var executionManager = new Fedora3ValidationExecutionManager(configHelper);
        executionManager.doValidation();

        final var numProcessed = executionManager.getNumProcessed();
        final var handler = reportType == ReportType.html ? new HtmlReportHandler(reportDir, numProcessed)
                                                          : new CsvReportHandler(reportDir, reportType);
        final var generator = new ReportGeneratorImpl(config.getJsonOutputDirectory(), handler);
        generator.generate();

        // filter out non-object reports
        final var objectReports = getObjectReports(reportDir, IGNORE);
        assertThat(objectReports).isEmpty();
    }

    public List<String> getExpectedObjectReports(final OcflRepository repository, final ReportType reportType) {
        return repository.listObjectIds()
                         .map(objectId -> objectId.substring("info:fedora/".length()))
                         .map(objectId -> URLEncoder.encode(objectId, Charset.defaultCharset()))
                         .map(objectId -> objectId + reportType.getExtension())
                         .collect(Collectors.toList());
    }

    public List<String> getObjectReports(final Path reportDir, final Predicate<String> filter) throws IOException {
        try (var reports = Files.list(reportDir)) {
            return reports.map(Path::getFileName)
                .map(Path::toString)
                .filter(filter)
                .collect(Collectors.toList());
        }
    }
}
