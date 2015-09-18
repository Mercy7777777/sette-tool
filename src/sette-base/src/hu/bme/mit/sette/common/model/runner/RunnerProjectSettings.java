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
package hu.bme.mit.sette.common.model.runner;

import java.io.File;

import org.apache.commons.lang3.Validate;

import hu.bme.mit.sette.common.Tool;
import hu.bme.mit.sette.common.exceptions.ConfigurationException;
import hu.bme.mit.sette.common.model.snippet.SnippetProjectSettings;
import hu.bme.mit.sette.common.validator.FileType;
import hu.bme.mit.sette.common.validator.FileValidator;
import hu.bme.mit.sette.common.validator.GeneralValidator;
import hu.bme.mit.sette.common.validator.exceptions.ValidatorException;

/**
 * Stores settings for a runner project.
 *
 * @param <T>
 *            The type of the tool.
 */
public final class RunnerProjectSettings<T extends Tool> {
    /** Name of the directory containing the compiled files of the runner project. */
    public static final String BINARY_DIRNAME = "build";

    /** Name of the directory containing the generated files by the runner. */
    public static final String GENERATED_DIRNAME = "gen";

    /** Name of the directory containing the runner's output. */
    public static final String RUNNER_OUTPUT_DIRNAME = "runner-out";

    /** Name of the directory containing the tests. */
    public static final String TEST_DIRNAME = "test";

    /** The settings of the snippet project. */
    private final SnippetProjectSettings snippetProjectSettings;

    /** The tool. */
    private final T tool;

    /** The base directory of the runner project. */
    private final File baseDirectory;

    /** The tag for the runner project. */
    private final String tag;

    /**
     * Creates an instance of the object. The project will be located in the
     * <code>parentDirectory</code> in a subdirectory named as
     * <code>[snippet project name]___[tool name]___[tag]</code> (lowercase), e.g.:
     * 
     * <pre>
     * <code>
     * sette-snippets___random-tool___1st-run
     * sette-snippets___random-tool___2nd-run
     * sette-snippets___random-tool___3rd-run
     * sette-snippets___se-tool___1st-run
     * sette-snippets___se-tool___2nd-run
     * sette-snippets___se-tool___3rd-run
     * test-snippets___random-tool___1st-run
     * test-snippets___random-tool___2nd-run
     * test-snippets___se-tool___1st-run
     * </code>
     * </pre>
     *
     * @param snippetProjectSettings
     *            The settings of the snippet project.
     * @param parentDirectory
     *            the parent directory
     * @param tool
     *            The tool.
     * @param tag
     */
    public RunnerProjectSettings(SnippetProjectSettings snippetProjectSettings,
            File parentDirectory, T tool, String tag) {
        Validate.notNull(snippetProjectSettings, "Snippet project settings must not be null");
        Validate.notNull(parentDirectory, "The parent directory must not be null");
        Validate.notNull(tool, "The tool must not be null");
        Validate.notBlank(tag, "The tag must not be blank");
        Validate.isTrue(!tag.contains("___"), "The tag must contan the '___' substring");

        this.snippetProjectSettings = snippetProjectSettings;
        this.tool = tool;
        this.tag = tag;

        String projectName = String.format("%s___%s___%s", snippetProjectSettings.getProjectName(),
                tool.getName(), tag).toLowerCase();
        this.baseDirectory = new File(parentDirectory, projectName);
    }

    /**
     * Returns the settings of the snippet project.
     *
     * @return The settings of the snippet project.
     */
    public SnippetProjectSettings getSnippetProjectSettings() {
        return this.snippetProjectSettings;
    }

    /**
     * Returns the tool.
     *
     * @return The tool.
     */
    public T getTool() {
        return this.tool;
    }

    /**
     * Returns the tag of the runner project.
     * 
     * @return the tag of the runner project.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Returns the name of the runner project.
     *
     * @return The name of the runner project.
     */
    public String getProjectName() {
        return this.baseDirectory.getName();
    }

    /**
     * Returns the base directory.
     *
     * @return The base directory.
     */
    public File getBaseDirectory() {
        return this.baseDirectory;
    }

    /**
     * Returns the snippet source directory.
     *
     * @return The snippet source directory.
     */
    public File getSnippetSourceDirectory() {
        return new File(this.baseDirectory, snippetProjectSettings.getSnippetSourceDirectoryPath());
    }

    /**
     * Returns the snippet library directory.
     *
     * @return The snippet library directory.
     */
    public File getSnippetLibraryDirectory() {
        return new File(this.baseDirectory, snippetProjectSettings.getLibraryDirectoryPath());
    }

    /**
     * Returns the binary directory.
     *
     * @return The binary directory.
     */
    public File getBinaryDirectory() {
        return new File(this.baseDirectory, RunnerProjectSettings.BINARY_DIRNAME);
    }

    /**
     * Returns the generated directory.
     *
     * @return The generated directory.
     */
    public File getGeneratedDirectory() {
        return new File(this.baseDirectory, RunnerProjectSettings.GENERATED_DIRNAME);
    }

    /**
     * Returns the runner output directory.
     *
     * @return The runner directory.
     */
    public File getRunnerOutputDirectory() {
        return new File(this.baseDirectory, RunnerProjectSettings.RUNNER_OUTPUT_DIRNAME);
    }

    /**
     * Returns the directory containing the tests.
     *
     * @return The directory containing the tests.
     */
    public File getTestDirectory() {
        return new File(this.baseDirectory, RunnerProjectSettings.TEST_DIRNAME);
    }

    /**
     * Validates whether the runner project exists. This method does not check whether the
     * underlying snippet project exists.
     *
     * @throws ConfigurationException
     *             If the runner project does not exist or it has other file problems.
     */
    public void validateExists() throws ConfigurationException {
        try {
            GeneralValidator validator = new GeneralValidator(this);

            // base directory
            FileValidator v1 = new FileValidator(this.baseDirectory);
            v1.type(FileType.DIRECTORY).readable(true).executable(true);
            validator.addChildIfInvalid(v1);

            // snippet source directory
            FileValidator v2 = new FileValidator(this.getSnippetSourceDirectory());
            v2.type(FileType.DIRECTORY).readable(true).executable(true);
            validator.addChildIfInvalid(v2);

            // snippet library directory
            if (this.getSnippetLibraryDirectory().exists()) {
                FileValidator v3 = new FileValidator(this.getSnippetLibraryDirectory())
                        .type(FileType.DIRECTORY).readable(true).executable(true);
                validator.addChildIfInvalid(v3);
            }

            // generated directory
            if (this.getGeneratedDirectory().exists()) {
                FileValidator v4 = new FileValidator(this.getGeneratedDirectory())
                        .type(FileType.DIRECTORY).readable(true).executable(true);
                validator.addChildIfInvalid(v4);
            }

            // runner output directory
            if (this.getRunnerOutputDirectory().exists()) {
                FileValidator v5 = new FileValidator(this.getRunnerOutputDirectory())
                        .type(FileType.DIRECTORY).readable(true).executable(true);
                validator.addChildIfInvalid(v5);
            }

            // test directory
            if (this.getTestDirectory().exists()) {
                FileValidator v6 = new FileValidator(this.getTestDirectory())
                        .type(FileType.DIRECTORY).readable(true).executable(true);
                validator.addChildIfInvalid(v6);
            }

            validator.validate();
        } catch (ValidatorException ex) {
            throw new ConfigurationException(
                    "The runner project or a part of it does not exists or is not readable", ex);
        }
    }

    /**
     * Validates whether the runner project does not exist.
     *
     * @throws ConfigurationException
     *             If the runner project exists.
     */
    public void validateNotExists() throws ConfigurationException {
        try {
            // base directory
            FileValidator v = new FileValidator(this.baseDirectory);
            v.type(FileType.NONEXISTENT);
            v.validate();
        } catch (ValidatorException ex) {
            throw new ConfigurationException("The runner project already exists", ex);
        }
    }
}
