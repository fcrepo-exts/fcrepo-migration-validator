/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import org.fcrepo.migration.FedoraObjectProcessor;
import org.fcrepo.migration.handlers.ObjectAbstractionStreamingFedoraObjectHandler;
import org.fcrepo.migration.validator.api.ObjectValidationConfig;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.Validator;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.OBJECT_READABLE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validators perform the specific validation work on a Fedora 3 object and its corresponding OCFL object.
 *
 * @author dbernstein
 */
public class Fedora3ObjectValidator implements Validator<FedoraObjectProcessor> {

    private static final Logger LOGGER = getLogger(Fedora3ObjectValidator.class);


    private final OcflObjectSessionFactory factory;
    private final ObjectValidationConfig objectValidationConfig;

    public Fedora3ObjectValidator(final OcflObjectSessionFactory factory,
                                  final ObjectValidationConfig objectValidationConfig) {
        this.factory = factory;
        this.objectValidationConfig = objectValidationConfig;
    }

    @Override
    public List<ValidationResult> validate(final FedoraObjectProcessor object) {
        final var objectInfo = object.getObjectInfo();
        var fedoraId = objectInfo.getFedoraURI();
        if (fedoraId == null) {
            fedoraId = "info:fedora/" + objectInfo.getPid();
        }
        final var ocflSession = this.factory.newSession(fedoraId);
        final var handler = objectValidationConfig.isValidateHeadOnly()
                            ? new HeadOnlyValidationHandler(ocflSession, objectValidationConfig)
                            : new ValidatingObjectHandler(ocflSession, objectValidationConfig);

        try (object) {
            object.processObject(new ObjectAbstractionStreamingFedoraObjectHandler(handler));
            return handler.getValidationResults();
        } catch (Exception ex) {
            LOGGER.error("Source object {} could not be read due to: {}", objectInfo.getPid(), ex.getMessage(), ex);
            final var results = handler.getValidationResults();
            final var list = new ArrayList<>(results);
            list.add(new ValidationResult(results.size(), FAIL, OBJECT,
                    OBJECT_READABLE, fedoraId, ocflSession.ocflObjectId(), "Source object could not be read: " +
                    ex.getMessage()));

            return list;
        }
    }

}
