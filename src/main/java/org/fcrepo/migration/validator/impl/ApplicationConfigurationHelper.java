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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.storage.layout.config.HashedNTupleLayoutConfig;
import edu.wisc.library.ocfl.core.path.mapper.LogicalPathMappers;
import edu.wisc.library.ocfl.core.storage.filesystem.FileSystemOcflStorage;
import org.apache.commons.lang3.SystemUtils;
import org.fcrepo.migration.ObjectSource;
import org.fcrepo.migration.foxml.AkubraFSIDResolver;
import org.fcrepo.migration.foxml.ArchiveExportedFoxmlDirectoryObjectSource;
import org.fcrepo.migration.foxml.InternalIDResolver;
import org.fcrepo.migration.foxml.LegacyFSIDResolver;
import org.fcrepo.migration.foxml.NativeFoxmlDirectoryObjectSource;
import org.fcrepo.migration.validator.api.ObjectValidationConfig;
import org.fcrepo.migration.validator.api.ValidationResultWriter;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.cache.CaffeineCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static edu.wisc.library.ocfl.api.util.Enforce.expressionTrue;
import static edu.wisc.library.ocfl.api.util.Enforce.notNull;

/**
 * A helper class for configuring and creating application components.
 *
 * @author dbernstein
 */
public class ApplicationConfigurationHelper {

    private final Fedora3ValidationConfig config;
    private final Path workDirectory;
    private final Supplier<MutableOcflRepository> repositorySupplier;

    public ApplicationConfigurationHelper(final Fedora3ValidationConfig config) {
        this.config = config;
        try {
            this.workDirectory = Files.createTempDirectory("ocfl-work");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.repositorySupplier = Suppliers.memoize(() -> repository(config, workDirectory));
    }

    public ValidationResultWriter validationResultWriter() {
        return new FileSystemValidationResultWriter(config.getJsonOutputDirectory());
    }

    public ObjectSource objectSource() {
        try {
            return doObjectSource();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectSource doObjectSource () throws IOException {
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

    private MutableOcflRepository repository(final Fedora3ValidationConfig config, final Path workDir) {
        final var storage = FileSystemOcflStorage.builder()
                .repositoryRoot(config.getOcflRepositoryRootDirectory().toPath())
                .build();
        final var logicalPathMapper = SystemUtils.IS_OS_WINDOWS ?
                LogicalPathMappers.percentEncodingWindowsMapper() : LogicalPathMappers.percentEncodingLinuxMapper();

        return new OcflRepositoryBuilder().storage(storage)
                .defaultLayoutConfig(new HashedNTupleLayoutConfig())
                .logicalPathMapper(logicalPathMapper)
                .workDir(workDir)
                .buildMutable();
    }

    /**
     * Retrieves the OcflRepository
     *
     * @return the OcflRepository
     */
    public OcflRepository ocflRepository() {
        return repositorySupplier.get();
    }

    /**
     * Creates and return an OcflObjectSessionFactory.
     *
     * @return a session factory
     */
    public OcflObjectSessionFactory ocflObjectSessionFactory() {
        final var objectMapper = new ObjectMapper().configure(WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        final var headersCache = Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();

        final var rootIdCache = Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();
        // https://jira.lyrasis.org/browse/FCREPO-3632:
        return new DefaultOcflObjectSessionFactory(repositorySupplier.get(),
                workDirectory,
                objectMapper,
                new CaffeineCache<>(headersCache),
                new CaffeineCache<>(rootIdCache),
                CommitType.UNVERSIONED,
                "Authored by Fedora 6",
                "fedoraAdmin",
                "info:fedora/fedoraAdmin");
    }

    /**
     * Read the file containing the object ids if available and return it as a set
     *
     * @throws RuntimeException if the file cannot be read
     * @return a Set of objectIds
     */
    public Set<String> readObjectsToValidate() {
        final var file = config.getObjectsToValidate();
        final var objectIds = new HashSet<String>();
        if (file != null) {
            try (final var lines = Files.lines(file.toPath())) {
                lines.forEach(objectIds::add);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return objectIds;
    }

    public int getThreadCount() {
        return config.getThreadCount();
    }

    public ObjectValidationConfig getObjectValidationConfig() {
        return new ObjectValidationConfig(config.getOcflRepositoryRootDirectory(),
                                          config.enableChecksums(),
                                          config.isDeleteInactive(),
                                          config.validateHeadOnly(),
                                          repositorySupplier.get(),
                                          config.getDigestAlgorithm());
    }

    public Boolean checkNumObjects() {
        return config.checkNumObjects();
    }
}
