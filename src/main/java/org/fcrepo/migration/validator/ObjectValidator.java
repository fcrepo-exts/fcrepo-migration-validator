package org.fcrepo.migration.validator;/*
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

import edu.wisc.library.ocfl.core.db.OcflObjectDetails;
import org.fcrepo.migration.FedoraObjectProcessor;

import java.util.List;

/**
 * Validators perform the specific validation work on a Fedora 3 object and its corresponding OCFL object.
 * @author dbernstein
 */
public interface ObjectValidator extends Validator {

    /**
     * Performs the validation on a single Fedora 3 object against its related OCFL object.
     * It produces one or more results.
     *
     * @param f3ObjectProcessor The source object
     * @param ocflObjectDetails The destination object
     * @return A list of one or more validation result objects.
     */
    public List<ValidationResult> validate(FedoraObjectProcessor f3ObjectProcessor,
                                           OcflObjectDetails ocflObjectDetails);

}
