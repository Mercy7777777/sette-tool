/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution based test input 
 * generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei <micskeiz@mit.bme.hu>
 *
 * Copyright 2014-2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the 
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the License for the specific language governing permissions and 
 * limitations under the License.
 */
// NOTE revise this file
package hu.bme.mit.sette.common.model.parserxml;

import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Element;

/**
 * Represents a snippet element.
 */
public final class SnippetElement implements XmlElement {
    /** The name of the snippet container. */
    @Element(name = "container", data = true)
    private String containerName;

    /** The name of the code snippet. */
    @Element(name = "name", data = true)
    private String name;

    /**
     * Instantiates a new snippet element.
     */
    public SnippetElement() {
    }

    /**
     * Instantiates a new snippet element.
     *
     * @param containerName
     *            the name of the snippet container
     * @param name
     *            the name of the code snippet
     */
    public SnippetElement(String containerName, String name) {
        this.containerName = containerName;
        this.name = name;
    }

    /**
     * Gets the name of the snippet container.
     *
     * @return the name of the snippet container
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * Sets the name of the snippet container.
     *
     * @param containerName
     *            the new name of the snippet container
     */
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    /**
     * Gets the name of the code snippet.
     *
     * @return the name of the code snippet
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the code snippet.
     *
     * @param name
     *            the new name of the code snippet
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void validate() throws ValidatorException {
        GeneralValidator v = new GeneralValidator(this);

        if (StringUtils.isBlank(containerName)) {
            v.addException("The container name must not be blank");
        }
        if (StringUtils.isBlank(name)) {
            v.addException("The name must not be blank");
        }

        v.validate();
    }
}
