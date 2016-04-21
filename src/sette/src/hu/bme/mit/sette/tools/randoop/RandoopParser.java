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
 * Copyright 2014-2016
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
package hu.bme.mit.sette.tools.randoop;

import static hu.bme.mit.sette.core.util.io.PathUtils.exists;
import static java.util.stream.Collectors.toList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.RunResultParserBase;
import hu.bme.mit.sette.core.util.io.PathUtils;

public class RandoopParser extends RunResultParserBase<RandoopTool> {
    private final static Pattern TEST_COUNT_LINE_PATTERN = Pattern
            .compile("^Writing (-?\\d+) junit tests$");

    public RandoopParser(SnippetProject snippetProject, Path outputDir, RandoopTool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    @Override
    protected void parseSnippet(Snippet snippet, SnippetOutFiles outFiles,
            SnippetInputsXml inputsXml) throws Exception {
        List<String> outputLines = outFiles.readOutputLines();
        List<String> errorLines = outFiles.readErrorOutputLines();
        Path lookUpDir = getRunnerProjectSettings().getBaseDir().resolve("test")
                .resolve(RunnerProjectUtils.getSnippetBaseFilename(snippet) + "_Test");

        // do not parse inputs
        inputsXml.setGeneratedInputs(null);

        if (outputLines.isEmpty()) {
            // FIXME extremely odd
            throw new RuntimeException("output file empty: " + outFiles.outputFile);
        }

        for (String infoLine : outFiles.readInfoLines()) {
            if (infoLine.contains("Exit value: 1")) {
                inputsXml.setResultType(ResultType.EX);
                break;
            }
        }

        if (inputsXml.getResultType() == null && !exists(lookUpDir) && !errorLines.isEmpty()) {
            String firstLine = errorLines.get(0);

            if (firstLine.startsWith("java.io.FileNotFoundException:")
                    && firstLine.endsWith("_Test/Test.java (No such file or directory)")) {
                // this means that no input was generated but the generation
                // was successful
                inputsXml.setGeneratedInputCount(0);

                if (snippet.getRequiredStatementCoverage() <= Double.MIN_VALUE
                        || snippet.getMethod().getParameterCount() == 0) {
                    // C only if the required statement coverage is 0% or
                    // the method takes no parameters
                    inputsXml.setResultType(ResultType.C);
                } else {
                    inputsXml.setResultType(ResultType.NC);
                }
            } else if (firstLine.startsWith("java.lang.Error: classForName")) {
                // exception, no output that not supported -> EX
                inputsXml.setResultType(ResultType.EX);
            } else if (firstLine.matches("[A-Za-z0-9: ]*") && !firstLine.contains("Error")
                    && !firstLine.contains("Exception")) {
                // normal text on stderr, skip
            } else {
                // TODO
                throw new RuntimeException("TODO parser problem:" + outFiles.errorOutputFile);
            }
        }

        if (inputsXml.getResultType() == null) {
            // always S for Randoop
            if (snippet.getRequiredStatementCoverage() == 0) {
                inputsXml.setResultType(ResultType.C);
            } else {
                inputsXml.setResultType(ResultType.S);
            }

            // get how many tests were generated
            int generatedInputCount = getGeneratedInputCountFromOutputLines(outputLines);

            if (generatedInputCount >= 0) {
                inputsXml.setGeneratedInputCount(generatedInputCount);
            } else {
                // NOTE randoop did not write out the result, we have to determine it :(
                if (!exists(lookUpDir)) {
                    inputsXml.setGeneratedInputCount(0);
                } else {
                    System.err.println("Determining test case count: " + lookUpDir);

                    List<Path> testFiles = Files.list(lookUpDir).collect(toList());

                    int cnt = 0;
                    for (Path file : testFiles) {
                        log.debug("Parsing with JavaParser: {}", file);
                        CompilationUnit cu = JavaParser.parse(file.toFile());
                        log.debug("Parsed with JavaParser: {}", file);
                        ClassOrInterfaceDeclaration cls = (ClassOrInterfaceDeclaration) cu
                                .getTypes().get(0);
                        cnt += cls.getMembers().stream()
                                .filter(bd -> bd instanceof MethodDeclaration).mapToInt(bd -> {
                                    MethodDeclaration md = (MethodDeclaration) bd;
                                    if (md.getName().startsWith("test")
                                            && md.getName().length() >= 5) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
                                }).sum();
                    }

                    System.err.println("Test case count: " + cnt);
                    inputsXml.setGeneratedInputCount(cnt);
                }
            }

            // create input placeholders
        }

        inputsXml.validate();
    }

    @Override
    protected void afterParse() {
        // fix compilation error in test suite files
        Path testDir = getRunnerProjectSettings().getTestDir();

        Iterator<Path> it = PathUtils.walk(testDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .sorted().collect(Collectors.toList()).iterator();

        while (it.hasNext()) {
            Path testFile = it.next();
            if (!testFile.getFileName().toString().endsWith("Test.java")) {
                continue;
            }

            List<String> lines = PathUtils.readAllLines(testFile);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).replace("public static Test suite() {",
                        "public static TestSuite suite() {");
                lines.set(i, line);
            }
            PathUtils.write(testFile, lines);
        }
    }

    /**
     * @return a number >= 0 if it is present in the output file, otherwise -1
     */
    private static int getGeneratedInputCountFromOutputLines(List<String> outputLines) {
        IntSummaryStatistics ints = outputLines.stream()
                .map(line -> TEST_COUNT_LINE_PATTERN.matcher(line.trim()))
                .filter(m -> m.matches())
                .mapToInt(m -> Integer.parseInt(m.group(1)))
                .summaryStatistics();

        if (ints.getMin() < 0) {
            throw new RuntimeException("RANDOOP: Number of tests is negative:" + outputLines);
        } else if (ints.getCount() > 0) {
            return (int) ints.getSum();
        } else {
            return -1;
        }
    }
}
