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

import org.slf4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

import static org.slf4j.LoggerFactory.getLogger;

//TODO pull in version and git revision from generated property file

/**
 * The command line tool entry point and parameter definitions
 * @author dbernstein
 */
@CommandLine.Command(name = "fcrepo-migration-validator", mixinStandardHelpOptions = true, sortOptions = false,
        version = "0.1.0-SNAPSHOT")
public class Driver implements Callable<Integer> {

    private static final Logger LOGGER = getLogger(Driver.class);

    private enum F3SourceTypes {
        AKUBRA, LEGACY, EXPORTED;

        static F3SourceTypes toType(final String v) {
            return valueOf(v.toUpperCase());
        }
    }

    @CommandLine.Option(names = {"--source-type", "-t"}, required = true, order = 1,
            description = "Fedora 3 source type. Choices: akubra | legacy | exported")
    private F3SourceTypes f3SourceType;

    @CommandLine.Option(names = {"--datastreams-dir", "-d"}, order = 2,
            description = "Directory containing Fedora 3 datastreams (used with --source-type 'akubra' or 'legacy')")
    private File f3DatastreamsDir;

    @CommandLine.Option(names = {"--objects-dir", "-o"}, order = 3,
            description = "Directory containing Fedora 3 objects (used with --source-type 'akubra' or 'legacy')")
    private File f3ObjectsDir;

    @CommandLine.Option(names = {"--debug"}, order = 30, description = "Enables debug logging")
    private boolean debug;

    @Override
    public Integer call() throws Exception {

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