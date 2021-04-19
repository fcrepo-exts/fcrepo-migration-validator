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
package org.fcrepo.migration.validator;

import org.fcrepo.migration.validator.api.ReportHandler;
import org.fcrepo.migration.validator.impl.F3SourceTypes;
import org.fcrepo.migration.validator.impl.ApplicationConfigurationHelper;
import org.fcrepo.migration.validator.impl.F6DigestAlgorithm;
import org.fcrepo.migration.validator.impl.Fedora3ValidationConfig;
import org.fcrepo.migration.validator.impl.Fedora3ValidationExecutionManager;
import org.fcrepo.migration.validator.report.CsvReportHandler;
import org.fcrepo.migration.validator.report.HtmlReportHandler;
import org.fcrepo.migration.validator.report.ReportGeneratorImpl;
import org.fcrepo.migration.validator.report.ReportType;
import org.slf4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

import static org.slf4j.LoggerFactory.getLogger;
import static picocli.CommandLine.Help.Visibility.ALWAYS;

//TODO pull in version and git revision from generated property file

/**
 * The command line tool entry point and parameter definitions
 * @author dbernstein
 */
@CommandLine.Command(name = "fcrepo-migration-validator", mixinStandardHelpOptions = true, sortOptions = false,
        version = "0.1.0-SNAPSHOT")
public class Driver implements Callable<Integer> {

    private static final Logger LOGGER = getLogger(Driver.class);

    @CommandLine.Option(names = {"--source-type", "-s"}, required = true, order = 1,
            description = "Fedora 3 source type. Choices: akubra | legacy | exported")
    private F3SourceTypes f3SourceType;

    @CommandLine.Option(names = {"--datastreams-dir", "-d"}, order = 2,
            description = "Directory containing Fedora 3 datastreams " +
                    "(used with --source-type 'akubra' or 'legacy')")
    private File f3DatastreamsDir;

    @CommandLine.Option(names = {"--objects-dir", "-o"}, order = 3,
            description = "Directory containing Fedora 3 objects (used with --source-type 'akubra' or 'legacy')")
    private File f3ObjectsDir;

    @CommandLine.Option(names = {"--exported-dir", "-e"}, order = 4,
            description = "Directory containing Fedora 3 export (used with --source-type 'exported')")
    private File f3ExportedDir;

    @CommandLine.Option(names = {"--f3hostname", "-f"},
            defaultValue = "fedora.info", showDefaultValue = ALWAYS, order = 5,
            description = "Hostname of Fedora 3, used for replacing placeholder in 'E' and 'R' datastream URLs")
    private String f3hostname;

    @CommandLine.Option(names = {"--index-dir", "-i"}, order = 6,
            description = "Directory where cached index of datastreams (will reuse index if already exists)")
    private File indexDir;

    @CommandLine.Option(names = {"--results-dir", "-r"}, defaultValue = "output", order = 7,
            description = "Directory where validation results are placed")
    private File resultsDirectory;

    @CommandLine.Option(names = {"--ocfl-root-dir", "-c"}, order = 8,
            description = "The root directory of the Fedora OCFL.")
    private File ocflRootDirectory;

    @CommandLine.Option(names = {"--threads", "-t"}, order = 9,
            description = "The number of threads for parallel processing. Default 5", defaultValue = "5")
    private int threadCount;

    @CommandLine.Option(names = {"--pid-file", "-p"}, order = 10,
                        description = "PID file listing which Fedora 3 objects to validate")
    private File objectsToValidate;

    @CommandLine.Option(names = {"--report-type", "-R"}, order = 11, defaultValue = "html",
                        description = "Type of report to generate: ${COMPLETION-CANDIDATES}")
    private ReportType reportType;

    @CommandLine.Option(names = {"--checksum", "-C"}, order = 15,
                        description = "Enable checksum validations of datastreams")
    private boolean checksum;

    @CommandLine.Option(names = {"--algorithm", "-a"}, order = 16, defaultValue = "sha512", showDefaultValue = ALWAYS,
                        description = "The digest algorithm to use during checksum validation: " +
                                      "${COMPLETION-CANDIDATES}")
    private F6DigestAlgorithm algorithm;

    @CommandLine.Option(names = {"--head-only", "-H"}, order = 18,
                        description = "Validate only the most recent version of a datastream")
    private boolean validateHeadOnly;

    @CommandLine.Option(names = {"--check-num-objects", "-n"}, order = 17,
                        description = "Enable validation comparing the number of objects in the Fedora 3 and Fedora " +
                                      "OCFL repositories. This validation is always disabled if a PID File is used.")
    private boolean checkNumberOfObjects;

    @CommandLine.Option(names = {"--delete-inactive", "-I"}, order = 18,
                        description = "Validate objects in the Inactive state as deleted.")
    private boolean deleteInactive;

    @CommandLine.Option(names = {"--debug"}, order = 30, description = "Enables debug logging")
    private boolean debug;

    @Override
    public Integer call() {
        final var config = new Fedora3ValidationConfig();
        config.setSourceType(f3SourceType);
        config.setEnableChecksums(checksum);
        config.setCheckNumObjects(checkNumberOfObjects);
        config.setValidateHeadOnly(validateHeadOnly);
        config.setDigestAlgorithm(algorithm);
        config.setDatastreamsDirectory(f3DatastreamsDir);
        config.setObjectsDirectory(f3ObjectsDir);
        config.setExportedDirectory(f3ExportedDir);
        config.setFedora3Hostname(f3hostname);
        config.setThreadCount(threadCount);
        config.setResultsDirectory(resultsDirectory.toPath());
        config.setOcflRepositoryRootDirectory(ocflRootDirectory);
        config.setObjectsToValidate(objectsToValidate);
        config.setDeleteInactive(deleteInactive);
        LOGGER.info("Configuration created: {}", config);

        LOGGER.info("Preparing to execute validation run...");
        final var executionManager = new Fedora3ValidationExecutionManager(new ApplicationConfigurationHelper(config));
        executionManager.doValidation();

        final ReportHandler reportHandler;
        if (reportType == ReportType.html) {
            reportHandler = new HtmlReportHandler(config.getReportDirectory(reportType));
        } else {
            reportHandler = new CsvReportHandler(config.getReportDirectory(reportType), reportType);
        }
        final var generator = new ReportGeneratorImpl(config.getJsonOutputDirectory(), reportHandler);
        final var summaryFile = generator.generate();
        LOGGER.info("Validation report written to: {}", summaryFile);

        return 0;
    }

    /**
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        final Driver driver = new Driver();
        final CommandLine cmd = new CommandLine(driver);
        cmd.registerConverter(F3SourceTypes.class, F3SourceTypes::toType);
        cmd.setExecutionExceptionHandler(new ValidatorExceptionHandler(driver));

        cmd.execute(args);
    }

    private static class ValidatorExceptionHandler implements CommandLine.IExecutionExceptionHandler {

        private final Driver driver;

        ValidatorExceptionHandler(final Driver driver) {
            this.driver = driver;
        }

        @Override
        public int handleExecutionException(
                final Exception ex,
                final CommandLine commandLine,
                final CommandLine.ParseResult parseResult) {
            commandLine.getErr().println(ex.getMessage());
            if (driver.debug) {
                ex.printStackTrace(commandLine.getErr());
            }
            commandLine.usage(commandLine.getErr());
            return commandLine.getCommandSpec().exitCodeOnExecutionException();
        }
    }

}