/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.report.ResultsReportHandler;
import org.junit.Test;

/**
 * @author mikejritter
 */
public class ResumeManagerIT extends AbstractValidationIT {

    private final String F6_OCFL = "resume-it/f6/data/ocfl-root";
    private final String F3_OBJECTS = "resume-it/f3/objects";
    private final String F3_DATASTREAMS = "resume-it/f3/datastreams";

    @Test
    public void test() throws IOException {
        final File f3DatastreamsDir = new File(FIXTURES_BASE_DIR, F3_DATASTREAMS);
        final File f3ObjectsDir = new File(FIXTURES_BASE_DIR, F3_OBJECTS);
        final File f6OcflRootDir = new File(FIXTURES_BASE_DIR, F6_OCFL);
        final var config = getConfig(f3DatastreamsDir, f3ObjectsDir, f6OcflRootDir);
        config.setLimit(1);
        config.setResume(true);
        final ResultsReportHandler reportHandler = doValidation(config);

        // verify expected results
        assertEquals("Should be no errors!", 0, reportHandler.getErrors().size());
        final var objectResults = reportHandler.getPassed().stream()
                                               .map(ValidationResult::getSourceObjectId)
                                               .collect(Collectors.toSet());

        assertThat(objectResults).hasSize(1);

        // check resume.txt
        final var resume = RESULTS_DIR.toPath().resolve("resume.txt");
        try (Stream<String> lines = Files.lines(resume)) {
            assertThat(lines).hasSize(1);
        }

        // second run for picking up the other pid
        final ResultsReportHandler reportHandler2 = doValidation(config);
        final var moreResults = reportHandler2.getPassed().stream()
                                              .map(ValidationResult::getSourceObjectId)
                                              .collect(Collectors.toSet());

        assertThat(moreResults).hasSize(2);
    }

}
