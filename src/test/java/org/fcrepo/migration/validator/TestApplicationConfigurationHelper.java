/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import org.fcrepo.migration.validator.api.ValidationResultWriter;
import org.fcrepo.migration.validator.impl.ApplicationConfigurationHelper;
import org.fcrepo.migration.validator.impl.Fedora3ValidationConfig;

/**
 * Class which extends ApplicationConfigurationHelper in order to supply certain test cases
 *
 * @author mikejritter
 */
public class TestApplicationConfigurationHelper extends ApplicationConfigurationHelper {
    public TestApplicationConfigurationHelper(final Fedora3ValidationConfig config) {
        super(config);
    }

    @Override
    public ValidationResultWriter validationResultWriter() {
        return new ExceptingValidationResultWriter();
    }
}
