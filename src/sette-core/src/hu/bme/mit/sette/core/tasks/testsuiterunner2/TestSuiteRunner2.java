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
package hu.bme.mit.sette.core.tasks.testsuiterunner2;

import static hu.bme.mit.sette.core.tasks.testsuiterunner2.TestSuiteRunner2Helper.decideResultType;
import static hu.bme.mit.sette.core.tasks.testsuiterunner2.TestSuiteRunner2Helper.loadTestClasses;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import hu.bme.mit.sette.core.exceptions.TestSuiteRunner2Exception;
import hu.bme.mit.sette.core.model.parserxml.FileCoverageElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetCoverageXml;
import hu.bme.mit.sette.core.model.parserxml.SnippetElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.parserxml.SnippetProjectElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetResultXml;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.AntExecutor;
import hu.bme.mit.sette.core.tasks.EvaluationTask;
import hu.bme.mit.sette.core.tasks.testsuiterunner.CoverageInfo;
import hu.bme.mit.sette.core.tasks.testsuiterunner.HtmlGenerator;
import hu.bme.mit.sette.core.tasks.testsuiterunner.JaCoCoClassLoader;
import hu.bme.mit.sette.core.tasks.testsuiterunner.LineStatus;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;
import hu.bme.mit.sette.core.validator.PathType;
import hu.bme.mit.sette.core.validator.PathValidator;
import hu.bme.mit.sette.core.validator.ValidationException;
import lombok.Getter;
import lombok.Setter;

// tailored for evosuite and extra snippets
public final class TestSuiteRunner2 extends EvaluationTask<Tool> {
    private static final String ANT_BUILD_TEST2_FILENAME;

    static {
        ANT_BUILD_TEST2_FILENAME = "build-test2.xml";
    }

    private String getAntBuildTest2Data() {
        List<String> lines = new ArrayList<>();
        lines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        lines.add("<!-- Build file for compiling tests -->");
        lines.add("<project default=\"compile-test2\">");
        lines.add("    <import file=\"build.xml\" />");
        lines.add("");
        lines.add("    <target name=\"compile-test2\">");
        lines.add("        <mkdir dir=\"build\" />");
        lines.add(
                "        <javac destdir=\"build\" includeantruntime=\"false\" source=\"${source}\" target=\"${target}\" debug=\"on\" nowarn=\"on\">");
        lines.add("            <compilerarg value=\"-Xlint:none\" />");
        lines.add("            <compilerarg value=\"-encoding\" />");
        lines.add("            <compilerarg value=\"UTF8\" />");
        lines.add("            <classpath>");

        if (getTool().getClass().getSimpleName().startsWith("EvoSuite")) {
            lines.add("                <pathelement path=\"evosuite.jar\" />");
        } else {
            lines.add("                <pathelement path=\"junit.jar\" />");
        }

        lines.add("                <fileset dir=\"snippet-lib\" erroronmissingdir=\"no\">");
        lines.add("                    <include name=\"**/*.jar\" />");
        lines.add("                </fileset>");
        lines.add("            </classpath>");
        lines.add("            <src path=\"snippet-src\" />");
        if (getTool().getClass().getSimpleName().startsWith("EvoSuite")) {
            lines.add("            <src path=\"test-original\" />");
        } else {
            lines.add("            <src path=\"test\" />");
        }
        lines.add("        </javac>");
        lines.add("    </target>");
        lines.add("</project>");
        lines.add("");

        return String.join("\n", lines);
    }

    @Getter
    @Setter
    private Pattern snippetSelector = null;

    private Path evosuiteJar = null;

    public TestSuiteRunner2(SnippetProject snippetProject, Path outputDir, Tool tool,
            String runnerProjectTag) {
        super(snippetProject, outputDir, tool, runnerProjectTag);
    }

    public final void analyze() throws Exception {
        if (!RunnerProjectUtils.getRunnerLogFile(getRunnerProjectSettings()).exists()) {
            throw new TestSuiteRunner2Exception(
                    "Run the tool on the runner project first (and then parse and generate tests)");
        }

        // create build file
        PathUtils.write(
                getRunnerProjectSettings().getBaseDir().toPath().resolve(ANT_BUILD_TEST2_FILENAME),
                getAntBuildTest2Data().getBytes());

        // force rebuild?
        // PathUtils.deleteIfExists(getRunnerProjectSettings().getBaseDir().toPath().resolve("build"));

        // copy evojar if needed
        // FIXME without reflection...
        if (getTool().getClass().getSimpleName().startsWith("EvoSuite")) {
            evosuiteJar = new File(getRunnerProjectSettings().getBaseDir(), "evosuite.jar")
                    .toPath();
            if (!PathUtils.exists(evosuiteJar)) {
                Field fld = getTool().getClass().getDeclaredField("toolJar");
                fld.setAccessible(true);

                Path toolJarSource = (Path) fld.get(getTool());

                PathUtils.copy(toolJarSource, evosuiteJar);
            }
        }

        // ant build
        AntExecutor.executeAnt(getRunnerProjectSettings().getBaseDir(),
                ANT_BUILD_TEST2_FILENAME);

        //
        Serializer serializer = new Persister(new AnnotationStrategy());

        // binary directories for the JaCoCoClassLoader
        File[] binaryDirectories = new File[2];
        binaryDirectories[0] = getSnippetProject().getBuildDir().toFile();
        binaryDirectories[1] = getRunnerProjectSettings().getBinaryDirectory();
        log.debug("Binary directories: {}", (Object) binaryDirectories);

        // foreach containers
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                // FIXME duplicated in RunnerProjectRunner -> replace loop with proper iterator
                if (snippetSelector != null
                        && !snippetSelector.matcher(snippet.getId()).matches()) {
                    String msg = String.format("Skipping %s (--snippet-selector)", snippet.getId());
                    log.info(msg);
                    continue;
                }

                handleSnippet(snippet, serializer, binaryDirectories);
            }
        }

        // NOTE check whether all inputs and info files are created
        // foreach containers
        for (SnippetContainer container : getSnippetProject().getSnippetContainers()) {
            // foreach snippets
            for (Snippet snippet : container.getSnippets().values()) {
                // FIXME duplicated in RunnerProjectRunner (and above too) -> replace loop with
                // proper iterator
                if (snippetSelector != null
                        && !snippetSelector.matcher(snippet.getId()).matches()) {
                    String msg = String.format("Skipping %s (--snippet-selector)", snippet.getId());
                    log.info(msg);
                    continue;
                }

                File resultXmlFile = RunnerProjectUtils
                        .getSnippetResultFile(getRunnerProjectSettings(), snippet);

                new PathValidator(resultXmlFile.toPath()).type(PathType.REGULAR_FILE).validate();
            }
        }

        // TODO remove debug
        System.err.println("=> ANALYZE ENDED");
    }

    private void handleSnippet(Snippet snippet, Serializer serializer, File[] binaryDirectories)
            throws Exception {
        File inputsXmlFile = RunnerProjectUtils.getSnippetInputsFile(getRunnerProjectSettings(),
                snippet);

        if (!inputsXmlFile.exists()) {
            throw new RuntimeException("Missing inputsXML: " + inputsXmlFile);
        }

        // it is now tricky, classloader hell
        SnippetInputsXml inputsXml;
        {
            // save current class loader
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

            // set snippet project class loader
            Thread.currentThread().setContextClassLoader(getSnippetProject().getClassLoader());

            // read data
            inputsXml = serializer.read(SnippetInputsXml.class, inputsXmlFile);

            // set back the original class loader
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // skip N/A, EX, T/M and already done
        if (inputsXml.getResultType() != ResultType.S) {
            Double reqCov;
            if (inputsXml.getResultType() == ResultType.C) {
                reqCov = snippet.getRequiredStatementCoverage();
            } else if (inputsXml.getResultType() == ResultType.NC) {
                throw new RuntimeException("SETTE error: result is NC before test-runner"
                        + snippet.getContainer().getJavaClass().getSimpleName() + "_"
                        + snippet.getMethod().getName());
            } else {
                reqCov = null;
            }

            // create results xml
            SnippetResultXml resultXml = SnippetResultXml.createForWithResult(inputsXml,
                    inputsXml.getResultType(), reqCov);
            resultXml.validate();

            // TODO needs more documentation
            File resultFile = RunnerProjectUtils.getSnippetResultFile(getRunnerProjectSettings(),
                    snippet);

            Serializer serializerWrite = new Persister(new AnnotationStrategy(),
                    new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));

            serializerWrite.write(resultXml, resultFile);

            return;
        }

        if (inputsXml.getGeneratedInputCount() == 0) {
            // throw new RuntimeException("No inputs: " + inputsXmlFile);
        }

        // NOTE remove try-catch
        try {
            // analyze
            SnippetCoverageXml coverageXml = analyzeOne(snippet, binaryDirectories);

            // create results xml
            SnippetResultXml resultXml = SnippetResultXml.createForWithResult(inputsXml,
                    coverageXml.getResultType(), coverageXml.getAchievedCoverage());
            resultXml.validate();

            // TODO needs more documentation
            File resultFile = RunnerProjectUtils.getSnippetResultFile(getRunnerProjectSettings(),
                    snippet);

            Serializer serializerWrite = new Persister(new AnnotationStrategy(),
                    new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));

            serializerWrite.write(resultXml, resultFile);
        } catch (ValidationException ex) {
            System.err.println(ex.getMessage());
            throw new RuntimeException("Validation failed");
        } catch (Throwable ex) {
            // now dump and go on
            System.err.println("========================================================");
            ex.printStackTrace();
            System.err.println("========================================================");
            System.err.println("========================================================");
            throw new RuntimeException(ex);
        }
    }

    private SnippetCoverageXml analyzeOne(Snippet snippet, File[] binaryDirectories)
            throws Throwable {
        //
        // Initialize
        //
        String snippetClassName = snippet.getContainer().getJavaClass().getName();
        String snippetMethodName = snippet.getMethod().getName();

        // example: Env1_StdIO_writesEofToStdin_writesEofToStdin_Test
        String testClassName = snippet.getContainer().getJavaClass().getName() + "_"
                + snippet.getMethod().getName();
        testClassName += "_" + snippet.getMethod().getName();
        testClassName += "_Test";

        log.debug("Snippet: {}#{}()", snippetClassName, snippetMethodName);
        log.debug("Test: {}", testClassName);

        // create JaCoCo runtime and instrumenter
        IRuntime runtime = new LoggerRuntime();
        Instrumenter instrumenter = new Instrumenter(runtime);

        // start runtime
        RuntimeData data = new RuntimeData();
        runtime.startup(data);

        // create class loader

        ClassLoader parentClassLoader = getSnippetProject().getClassLoader();
        if (evosuiteJar != null) {
            parentClassLoader = new URLClassLoader(new URL[] { evosuiteJar.toUri().toURL(),
                    getRunnerProjectSettings().getBinaryDirectory().toURI().toURL()
            });
        }
        JaCoCoClassLoader testClassLoader = new JaCoCoClassLoader(binaryDirectories, instrumenter,
                parentClassLoader);
        // load test class
        // snippet class and other dependencies will be loaded and instrumented
        // on the fly
        List<Class<?>> testClasses = loadTestClasses(testClassLoader, testClassName);

        if (testClasses.isEmpty()) {
            log.error("No test class was found for: " + snippet.getId());
        }

        // run junit
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener() {
            @Override
            public void testRunStarted(Description description) throws Exception {
                log.trace("testRunStarted: " + description.getDisplayName());
            }

            @Override
            public void testRunFinished(Result result) throws Exception {
                log.trace("testRunFinished: success: " + result.wasSuccessful());
            }

            @Override
            public void testStarted(Description description) throws Exception {
                log.trace("testStarted: " + description.getDisplayName());
            }

            @Override
            public void testFinished(Description description) throws Exception {
                log.trace("testFinished: " + description.getDisplayName());
            }

            @Override
            public void testFailure(Failure failure) throws Exception {
                log.info("testFailure: " + failure.getMessage());
                log.info("testFailure: " + failure.getTestHeader());
                log.info("testFailure: " + failure.getTrace());
                log.info("testFailure: " + failure.getDescription());

                failure.getException().printStackTrace();
                System.exit(999);
                throw new RuntimeException("JUnit has failed");
            }

            @Override
            public void testAssumptionFailure(Failure failure) {
                log.info("testAssumptionFailure: " + failure.getMessage());
                log.info("testAssumptionFailure: " + failure.getTestHeader());
                log.info("testAssumptionFailure: " + failure.getTrace());
                log.info("testAssumptionFailure: " + failure.getDescription());

                failure.getException().printStackTrace();
                System.exit(999);
                throw new RuntimeException("JUnit has failed");
            }

            @Override
            public void testIgnored(Description description) throws Exception {
                log.info("testIgnored: " + description.getDisplayName());
                System.exit(999);
                throw new RuntimeException("JUnit has failed");
            }
        });

        // save current class loader
        ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        Thread.currentThread().setContextClassLoader(testClassLoader);

        Result result = junit.run(testClasses.toArray(new Class<?>[0]));
        log.info("Success: " + result.wasSuccessful());

        // set back the original class loader
        Thread.currentThread().setContextClassLoader(originalClassLoader);

        //
        // Collect data
        //
        ExecutionDataStore executionData = new ExecutionDataStore();
        SessionInfoStore sessionInfos = new SessionInfoStore();
        data.collect(executionData, sessionInfos, false);
        runtime.shutdown();

        // get classes to analyse
        // store string to avoid the mess up between the different class loaders
        Set<String> javaClasses = new HashSet<>();
        javaClasses.add(snippetClassName);

        for (Constructor<?> inclConstructor : snippet.getIncludedConstructors()) {
            javaClasses.add(inclConstructor.getDeclaringClass().getName());
        }

        for (Method inclMethod : snippet.getIncludedMethods()) {
            javaClasses.add(inclMethod.getDeclaringClass().getName());
        }

        // TODO inner classes are not handled well enough

        // TODO anonymous classes can also have anonymous classes -> recursion

        Set<String> toAdd = new HashSet<>();
        for (String javaClass : javaClasses) {
            int i = 1;
            while (true) {
                // guess anonymous classes, like ClassName$1, ClassName$2 etc.
                try {
                    testClassLoader.loadClass(javaClass + "$" + i);
                    toAdd.add(javaClass + "$" + i);
                    i++;
                } catch (ClassNotFoundException ex) {
                    // bad guess, no more anonymous classes on this level
                    break;
                }
            }
        }
        javaClasses.addAll(toAdd);

        // analyse classes
        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

        for (String javaClassName : javaClasses) {
            log.trace("Analysing: {}", javaClassName);
            analyzer.analyzeClass(testClassLoader.readBytes(javaClassName), javaClassName);
        }

        // TODO remove debug
        // new File("D:/SETTE/!DUMP/" + getTool().getName()).mkdirs();
        // PrintStream out = new PrintStream("D:/SETTE/!DUMP/"
        // + getTool().getName() + "/" + testClassName + ".out");

        Map<String, Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>>> coverageInfo = new HashMap<>();

        for (IClassCoverage cc : coverageBuilder.getClasses()) {
            String file = cc.getPackageName() + '/' + cc.getSourceFileName();
            file = file.replace('\\', '/');

            if (!coverageInfo.containsKey(file)) {
                coverageInfo.put(file, Triple.of(new TreeSet<Integer>(), new TreeSet<Integer>(),
                        new TreeSet<Integer>()));
            }

            // out.printf("Coverage of class %s%n", cc.getName());
            //
            // printCounter(out, "instructions",
            // cc.getInstructionCounter());
            // printCounter(out, "branches", cc.getBranchCounter());
            // printCounter(out, "lines", cc.getLineCounter());
            // printCounter(out, "methods", cc.getMethodCounter());
            // printCounter(out, "complexity", cc.getComplexityCounter());

            for (int l = cc.getFirstLine(); l <= cc.getLastLine(); l++) {
                switch (LineStatus.fromJaCoCo(cc.getLine(l).getStatus())) {
                    case FULLY_COVERED:
                        coverageInfo.get(file).getLeft().add(l);
                        break;

                    case PARTLY_COVERED:
                        coverageInfo.get(file).getMiddle().add(l);
                        break;

                    case NOT_COVERED:
                        coverageInfo.get(file).getRight().add(l);
                        break;

                    default:
                        // empty
                        break;
                }
            }
        }

        SnippetCoverageXml coverageXml = createAndWriteCoverageXmlAndHtml(snippet,
                new CoverageInfo(coverageInfo));

        return coverageXml;
    }

    private SnippetCoverageXml createAndWriteCoverageXmlAndHtml(Snippet snippet,
            CoverageInfo coverageInfo) throws Exception {
        // decide result type
        Pair<ResultType, Double> resultTypeAndCoverage = decideResultType(snippet, coverageInfo);
        ResultType resultType = resultTypeAndCoverage.getLeft();
        double coverage = resultTypeAndCoverage.getRight();

        // FIXME hook to check snippets, but should be elsewhere
        if (getTool().getClass().getSimpleName().equals("SnippetInputCheckerTool")) {
            if (resultType != ResultType.C) {
                System.err.println("FAILURE for Checker, not C: " + snippet.getId());
                throw new RuntimeException();
            }
        }

        // create coverage XML
        SnippetCoverageXml coverageXml = new SnippetCoverageXml();
        coverageXml.setToolName(getTool().getName());
        coverageXml.setSnippetProjectElement(new SnippetProjectElement(
                getSnippetProject().getBaseDir().toFile().getCanonicalPath()));

        coverageXml.setSnippetElement(new SnippetElement(
                snippet.getContainer().getJavaClass().getName(), snippet.getMethod().getName()));

        coverageXml.setResultType(resultType);
        coverageXml.setAchievedCoverage(coverage);

        for (Entry<String, Triple<SortedSet<Integer>, SortedSet<Integer>, SortedSet<Integer>>> entry : coverageInfo.data
                .entrySet()) {
            SortedSet<Integer> full = entry.getValue().getLeft();
            SortedSet<Integer> partial = entry.getValue().getMiddle();
            SortedSet<Integer> not = entry.getValue().getRight();

            FileCoverageElement fce = new FileCoverageElement();
            fce.setName(entry.getKey());
            fce.setFullyCoveredLines(StringUtils.join(full, ' '));
            fce.setPartiallyCoveredLines(StringUtils.join(partial, ' '));
            fce.setNotCoveredLines(StringUtils.join(not, ' '));

            coverageXml.getCoverage().add(fce);
        }

        coverageXml.validate();

        // TODO needs more documentation
        File coverageFile = RunnerProjectUtils.getSnippetCoverageFile(getRunnerProjectSettings(),
                snippet);

        Serializer serializer = new Persister(new AnnotationStrategy(),
                new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));

        serializer.write(coverageXml, coverageFile);

        // generate html
        new HtmlGenerator(this).generate(snippet, coverageXml);

        return coverageXml;
    }
}
