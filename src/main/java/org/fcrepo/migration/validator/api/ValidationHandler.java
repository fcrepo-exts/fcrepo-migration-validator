/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import static java.lang.String.format;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_CHECKSUM;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_SIZE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.METADATA;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;
import edu.wisc.library.ocfl.api.model.OcflObjectVersion;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.migration.DatastreamVersion;
import org.fcrepo.migration.FedoraObjectVersionHandler;
import org.fcrepo.migration.ObjectInfo;
import org.fcrepo.migration.ObjectProperty;
import org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel;
import org.fcrepo.migration.validator.impl.F3ControlGroup;
import org.fcrepo.migration.validator.impl.F6DigestAlgorithm;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migration validator which performs a few basic types of validations built in:
 * - F3 object property against ocfl headers or stored model
 * - F3 datastream size against ocfl headers
 * - F3 datastream size against ocfl object on disk
 * - F3 datastream created date against ocfl headers
 * - F3 datastream last modified date against ocfl headers
 * - F3 checksum against ocfl object on disk
 *
 * @author mikejritter
 */
public interface ValidationHandler extends FedoraObjectVersionHandler {
    Logger LOGGER = LoggerFactory.getLogger(ValidationHandler.class);

    DateTimeFormatter ISO_8601 = DateTimeFormatter.ISO_INSTANT;

    String F3_LABEL = "info:fedora/fedora-system:def/model#label";
    String F3_STATE = "info:fedora/fedora-system:def/model#state";
    String F3_CREATED_DATE = "info:fedora/fedora-system:def/model#createdDate";
    String F3_LAST_MODIFIED_DATE = "info:fedora/fedora-system:def/view#lastModifiedDate";
    String F3_OWNER_ID = "info:fedora/fedora-system:def/model#ownerId";

    String RELS_INT = "RELS-INT";
    String DOWNLOAD_NAME_PROP = "info:fedora/fedora-system:def/model#downloadFilename";
    String RELS_DELETED_ENTRY = "FCREPO_MIGRATION_VALIDATOR_DELETED_ENTRY";

    /**
     * Fedora 3 ObjectProperties which migrated to OCFL
     */
    Map<String, PropertyResolver> OCFL_PROPERTY_RESOLVERS = Map.of(
        F3_LABEL, headers -> Optional.empty(),
        F3_STATE, headers -> Optional.empty(),
        F3_OWNER_ID, headers -> Optional.empty(),
        F3_CREATED_DATE, (DateTimeResolver) headers -> Optional.of(headers.getCreatedDate().toString()),
        F3_LAST_MODIFIED_DATE, (DateTimeResolver) headers -> Optional.of(headers.getLastModifiedDate().toString())
    );

    /**
     * Interface to resolve a Fedora 3 ObjectProperty in Fedora 6
     */
    interface PropertyResolver {
        Optional<String> resolve(final ResourceHeaders headers);

        default boolean equals(final String source, final String target) {
            return source.equals(target);
        }

        default Optional<String> fromModel(final Model model, final String ocflId, final String property) {
            return Optional.ofNullable(model.getProperty(model.createResource(ocflId), model.createProperty(property)))
                           .map(Statement::getObject)
                           .filter(RDFNode::isLiteral)
                           .map(node -> node.asLiteral().getLexicalForm());
        }
    }

    /**
     * DateTime resolver for mapping times to an Instant in order to account for differences in string formatting
     */
    interface DateTimeResolver extends PropertyResolver {
        @Override
        default boolean equals(final String source, final String target) {
            // For DateTime comparisons we convert back to Instants as Strings might have differences in formatting
            final var sourceDT = Instant.from(ISO_8601.parse(source));
            final var targetDT = Instant.from(ISO_8601.parse(target));
            return sourceDT.equals(targetDT);
        }
    }

    /**
     * Builder for creating ValidationResults in various places
     */
    class ValidationResultBuilder {
        private final String sourceObjectId;
        private final String targetObjectId;
        private final String sourceResource;
        private final String targetResource;
        private final ValidationLevel validationLevel;
        private final AtomicInteger index;

        public ValidationResultBuilder(final String sourceObjectId, final String targetObjectId,
                                       final String sourceResource, final String targetResource,
                                       final ValidationLevel validationLevel, final AtomicInteger index) {
            this.sourceObjectId = sourceObjectId;
            this.targetObjectId = targetObjectId;
            this.sourceResource = sourceResource;
            this.targetResource = targetResource;
            this.validationLevel = validationLevel;
            this.index = index;
        }

        public ValidationResult ok(final ValidationResult.ValidationType type, final String details) {
            return new ValidationResult(index.getAndIncrement(), OK, validationLevel, type, sourceObjectId,
                                        targetObjectId, sourceResource, targetResource, details);
        }

        public ValidationResult fail(final ValidationResult.ValidationType type, final String details) {
            LOGGER.info("[{}] {} validation failed: {}", sourceObjectId, type, details);
            return new ValidationResult(index.getAndIncrement(), FAIL, validationLevel, type, sourceObjectId,
                                        targetObjectId, sourceResource, targetResource, details);
        }
    }

    /**
     * Read an RDF from a DatastreamVersion
     *
     * @param dv the datastream version
     * @return the RDF model
     */
    default Model parseRdf(final DatastreamVersion dv) {
        final var model = ModelFactory.createDefaultModel();
        try (var is = dv.getContent()) {
            RDFDataMgr.read(model, is, Lang.RDFXML);
            return model;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse RDF XML", e);
        }
    }

    /**
     * Read a RELS-INT entry in order to extract the RDF models it has
     *
     * @param relsIntModel the RELS-INT model
     * @return a Map of each RDF Model the RELS-INT contains
     */
    default Map<String, Model> splitRelsInt(final Model relsIntModel) {
        final var infoFedora = "info:fedora/";
        final Map<String, Model> splitModels = new HashMap<>();
        for (final var it = relsIntModel.listStatements(); it.hasNext();) {
            final var statement = it.next();
            final var uri = statement.getSubject().getURI();
            final var id = uri.startsWith(infoFedora) ? uri.substring(infoFedora.length()) : uri;
            final var model = splitModels.computeIfAbsent(id, k -> ModelFactory.createDefaultModel());
            model.add(statement);
        }
        return splitModels;
    }

    /**
     * Validate an ObjectProperty from Fedora 3 to what exists in Fedora 6
     *
     * @param ocflId the id of the ocfl object
     * @param objectInfo the Fedora3 ObjectInto
     * @param op the Fedora3 ObjectProperty
     * @param headers the OCFL ResourceHeaders
     * @param model the model read from ocfl (if it exists)
     * @param builder the ValidationResultBuilder
     * @return the ValidationResults
     */
    default Optional<ValidationResult> validateObjectProperty(final String ocflId,
                                                              final ObjectInfo objectInfo,
                                                              final ObjectProperty op,
                                                              final ResourceHeaders headers,
                                                              final Model model,
                                                              final ValidationResultBuilder builder) {
        Optional<ValidationResult> result = Optional.empty();
        final var pid = objectInfo.getPid();
        final var property = op.getName();
        final var sourceValue = op.getValue();

        LOGGER.debug("PID = {}, object property: name = {}, value = {}", pid, property, sourceValue);
        final var resolver = OCFL_PROPERTY_RESOLVERS.get(property);
        if (resolver != null) {
            final var success = "pid: %s -> properties match: f3 prop name=%s, source=%s, target=%s";
            final var error = "pid: %s -> properties do not match: f3 prop name=%s, source=%s, target=%s";
            final var notFound = "pid: %s -> property not found in OCFL: f3 prop name=%s, source=%s";

            // try to get the ocfl property from the headers first, otherwise fallback to reading the n-triples
            result = resolver.resolve(headers)
                        .or(() -> resolver.fromModel(model, ocflId, property))
                        .map(targetVal -> resolver.equals(sourceValue, targetVal) ?
                                          builder.ok(METADATA, format(success, pid, property, sourceValue, targetVal)) :
                                          builder.fail(METADATA, format(error, pid, property, sourceValue, targetVal)))
                        .or(() -> Optional.of(builder.fail(METADATA, format(notFound, pid, property, sourceValue))));
        }

        return result;
    }

    /**
     * Validate the binary size of a DatastreamVersion from Fedora 3 to what is in the Fedora 6 headers
     *
     * @param dsVersion the DatastreamVersion
     * @param headers the Fedora 6 ResourceHeaders
     * @param version a string representation of the object version
     * @param builder the ValidationResultBuilder
     * @return the ValidationResults
     */
    default Optional<ValidationResult> validateSizeMeta(final DatastreamVersion dsVersion,
                                                        final ResourceHeaders headers,
                                                        final String version,
                                                        final ValidationResultBuilder builder) {
        Optional<ValidationResult> result = Optional.empty();
        final var error = "%s binary size does not match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary size matches: %s";

        final var dsInfo = dsVersion.getDatastreamInfo();
        final var controlGroup = F3ControlGroup.fromString(dsInfo.getControlGroup());
        if (controlGroup == F3ControlGroup.MANAGED) {
            final var sourceSize = dsVersion.getSize();
            final var targetSize = headers.getContentSize();
            if (sourceSize == targetSize) {
                result = Optional.of(builder.ok(BINARY_METADATA, format(success, version, sourceSize)));
            } else {
                result = Optional.of(builder.fail(BINARY_METADATA, format(error, version, sourceSize, targetSize)));
            }
        }

        return result;
    }

    /**
     * Validate the size of a DatastreamVersion to the size of the object on disk in the Fedora 6 OCFL repository
     *
     * @param dsVersion the DatastreamVersion
     * @param ocflRoot the ocfl-root directory
     * @param headers the Fedora 6 ResourceHeaders
     * @param ocflObjectVersion the OcflObjectVersion of the object
     * @param version a string representation of the object version
     * @param builder the ValidationResultBuilder
     * @return the ValidationResults
     */
    default Optional<ValidationResult> validateSizeOnDisk(final DatastreamVersion dsVersion,
                                                          final Path ocflRoot,
                                                          final ResourceHeaders headers,
                                                          final OcflObjectVersion ocflObjectVersion,
                                                          final String version,
                                                          final ValidationResultBuilder builder) {
        final var error = "%s binary size does not match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary size matches: %s";
        final var notFound = "%s %s file could not be found to check size!";

        final var dsInfo = dsVersion.getDatastreamInfo();
        final var controlGroup = F3ControlGroup.fromString(dsInfo.getControlGroup());

        if (controlGroup == F3ControlGroup.MANAGED) {
            final var sourceFile = dsVersion.getFile();
            return sourceFile.map(file -> {
                final var ocflRelativePath = ocflObjectVersion.getFile(headers.getContentPath())
                                                              .getStorageRelativePath();
                final var targetPath = ocflRoot.resolve(ocflRelativePath);
                if (Files.notExists(targetPath)) {
                    return builder.fail(BINARY_SIZE, format(notFound, version, "target"));
                }

                final var sourceBytes = file.length();
                final var targetBytes = targetPath.toFile().length();
                if (sourceBytes == targetBytes) {
                    return builder.ok(BINARY_SIZE, format(success, version, sourceBytes));
                }
                return builder.fail(BINARY_SIZE, format(error, version, sourceBytes, targetBytes));
            }).or(() -> Optional.of(builder.fail(BINARY_SIZE, format(notFound, version, "source"))));
        }

        return Optional.empty();
    }

    /**
     * Validate the created date of a DatastreamVersion from Fedora 3 to what is in the Fedora 6 headers
     *
     * @param sourceCreated the string representation of the Fedora 3 object creation date
     * @param headers the Fedora 6 ResourceHeaders
     * @param version a string representation of the object version
     * @param builder the ValidationResultBuilder
     * @return the result of validation
     */
    default Optional<ValidationResult> validateCreatedDate(final String sourceCreated,
                                                           final ResourceHeaders headers,
                                                           final String version,
                                                           final ValidationResultBuilder builder) {
        final Optional<ValidationResult> result;
        final var error = "%s binary creation dates do no match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary creation dates match: %s";

        final var sourceInstant = Instant.from(ISO_8601.parse(sourceCreated));
        final var targetCreated = headers.getCreatedDate();
        if (sourceInstant.equals(targetCreated)) {
            result = Optional.of(builder.ok(BINARY_METADATA, format(success, version, sourceCreated)));
        } else {
            result = Optional.of(builder.fail(BINARY_METADATA, format(error, version, sourceCreated, targetCreated)));
        }

        return result;
    }

    /**
     * Validate the last modified date of a DatastreamVersion from Fedora 3 to what is in the Fedora 6 headers
     *
     * @param dsVersion the DatastreamVersion of the Fedora 3 object
     * @param headers the ResourceHeaders of the Fedora 6 object
     * @param version a string representation of the object version
     * @param builder the ValidationResultBuilder
     * @return the ValidationResults
     */
    default Optional<ValidationResult> validateLastModified(final DatastreamVersion dsVersion,
                                                            final ResourceHeaders headers,
                                                            final String version,
                                                            final ValidationResultBuilder builder) {
        final Optional<ValidationResult> result;
        final var error = "%s binary last modified dates do no match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary last modified dates match: %s";

        // the last modified date in ocfl is derived from when a datastream was created
        // so we check equality of the two values
        final var sourceValue = Instant.from(ISO_8601.parse(dsVersion.getCreated()));
        final var targetValue = headers.getLastModifiedDate();

        if (sourceValue.equals(targetValue)) {
            result = Optional.of(builder.ok(BINARY_METADATA, format(success, version, sourceValue)));
        } else {
            result = Optional.of(builder.fail(BINARY_METADATA, format(error, version, sourceValue, targetValue)));
        }

        return result;
    }

    /**
     * Validate the checksum of a datastream. If the Fedora 3 object is not managed, no validation is run and an empty
     * Optional is returned.
     *
     * This can fail in multiple ways:
     * 1 - The F3 datastream can not be read
     * 2 - The F6 headers do not contain a checksum (only checking sha512 atm)
     * 3 - The two calculated checksums do not match
     *
     * @param dsVersion the DatastreamVersion of the Fedora 3 object
     * @param headers the ResourceHeaders of the Fedora 6 object
     * @param digestAlgorithm the digest algorithm to use
     * @param version a string representation of the object version
     * @param builder the ValidationResultBuilder
     * @return the ValidationResult
     */
    default Optional<ValidationResult> validateChecksum(final DatastreamVersion dsVersion,
                                                        final ResourceHeaders headers,
                                                        final F6DigestAlgorithm digestAlgorithm,
                                                        final String version,
                                                        final ValidationResultBuilder builder) {
        Optional<ValidationResult> result = Optional.empty();
        final var success = "%s binary checksums match: %s";
        final var error = "%s binary checksums do no match: sourceValue=%s, targetValue=%s";
        final var notFound = "%s binary checksum not found in Fedora 6 headers";
        final var exception = "%s binary checksum was unable to be calculated: exception=%s";

        final HashCode sourceHash;
        final var controlGroup = F3ControlGroup.fromString(dsVersion.getDatastreamInfo().getControlGroup());
        if (controlGroup == F3ControlGroup.MANAGED) {
            try {
                // compute the checksum of the datastream
                final var hasher = digestAlgorithm.hasher();
                ByteStreams.copy(dsVersion.getContent(), Funnels.asOutputStream(hasher));
                sourceHash = hasher.hash();
            } catch (IOException e) {
                return Optional.of(builder.fail(BINARY_CHECKSUM, format(exception, version, e)));
            }

            // retrieve the digest from the ocfl headers
            // note that digests are stored as urn:algorithm:hash
            final var ocflDigest = headers.getDigests().stream()
                                          .map(URI::toString)
                                          .filter(uri -> uri.startsWith(digestAlgorithm.getOcflUrn()))
                                          .map(uri -> uri.substring(uri.lastIndexOf(":") + 1))
                                          .findFirst();

            final var sourceValue = sourceHash.toString();
            result = ocflDigest.map(targetValue -> {
                if (Objects.equals(sourceValue, targetValue)) {
                    return builder.ok(BINARY_CHECKSUM, format(success, version, sourceValue));
                } else {
                    return builder.fail(BINARY_CHECKSUM, format(error, version, sourceValue, targetValue));
                }
            }).or(() -> Optional.of(builder.fail(BINARY_CHECKSUM, format(notFound, version))));
        }

        return result;
    }

    /**
     * @return the ValidationResults for an object
     */
    List<ValidationResult> getValidationResults();

}
