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
package org.fcrepo.migration.validator.impl;

import org.fcrepo.migration.ObjectSource;
import org.fcrepo.migration.foxml.AkubraFSIDResolver;
import org.fcrepo.migration.foxml.ArchiveExportedFoxmlDirectoryObjectSource;
import org.fcrepo.migration.foxml.InternalIDResolver;
import org.fcrepo.migration.foxml.LegacyFSIDResolver;
import org.fcrepo.migration.foxml.NativeFoxmlDirectoryObjectSource;
import org.fcrepo.migration.validator.api.ValidationResultWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;

import static edu.wisc.library.ocfl.api.util.Enforce.expressionTrue;
import static edu.wisc.library.ocfl.api.util.Enforce.notNull;

/**
 * A spring configuration for Fedora 3 related beans
 *
 * @author dbernstein
 */
@Configuration
@Lazy
public class Fedora3ObjectConfiguration {

    @Bean
    public Fedora3ValidationConfig config() {
        return new Fedora3ValidationConfig();
    }

    @Bean
    @Lazy
    public ValidationResultWriter validationResultWriter(final Fedora3ValidationConfig config) {
        return new FileSystemValidationResultWriter(config.getResultsDirectory());
    }

    @Bean
    @Lazy
    public ObjectSource objectSource(final Fedora3ValidationConfig config) throws IOException {
        final ObjectSource objectSource;
        final var f3ExportedDir = config.getExportedDirectory();
        final var f3DatastreamsDir = config.getDatastreamsDirectory();
        final var f3ObjectsDir = config.getObjectsDirectory();
        final var indexDir = config.getIndexDirectory();
        final var f3hostname = config.getFedora3Hostname();
        // Which F3 source are we using? - verify associated options
        final InternalIDResolver idResolver;
        switch (config.getSourceType()) {
            case EXPORTED:
                notNull(config.getExportedDirectory(), "f3ExportDir must be used with 'exported' source!");

                objectSource = new ArchiveExportedFoxmlDirectoryObjectSource(f3ExportedDir, f3hostname);
                break;
            case AKUBRA:
                notNull(f3DatastreamsDir, "f3DatastreamsDir must be used with 'akubra' or 'legacy' source!");
                notNull(f3ObjectsDir, "f3ObjectsDir must be used with 'akubra' or 'legacy' source!");
                expressionTrue(f3ObjectsDir.exists(), f3ObjectsDir, "f3ObjectsDir must exist! " +
                        f3ObjectsDir.getAbsolutePath());

                idResolver = new AkubraFSIDResolver(indexDir, f3DatastreamsDir);
                objectSource = new NativeFoxmlDirectoryObjectSource(f3ObjectsDir, idResolver, f3hostname);
                break;
            case LEGACY:
                notNull(f3DatastreamsDir, "f3DatastreamsDir must be used with 'akubra' or 'legacy' source!");
                notNull(f3ObjectsDir, "f3ObjectsDir must be used with 'akubra' or 'legacy' source!");
                expressionTrue(f3ObjectsDir.exists(), f3ObjectsDir, "f3ObjectsDir must exist! " +
                        f3ObjectsDir.getAbsolutePath());

                idResolver = new LegacyFSIDResolver(indexDir, f3DatastreamsDir);
                objectSource = new NativeFoxmlDirectoryObjectSource(f3ObjectsDir, idResolver, f3hostname);
                break;
            default:
                throw new RuntimeException("Should never happen");
        }

        return objectSource;
    }

}
