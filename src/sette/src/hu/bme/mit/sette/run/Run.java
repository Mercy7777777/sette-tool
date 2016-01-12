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
package hu.bme.mit.sette.run;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import hu.bme.mit.sette.GeneratorUI;
import hu.bme.mit.sette.ParserUI;
import hu.bme.mit.sette.RunnerUI;
import hu.bme.mit.sette.core.SetteException;
import hu.bme.mit.sette.core.configuration.SetteConfiguration;
import hu.bme.mit.sette.core.model.parserxml.SnippetInputsXml;
import hu.bme.mit.sette.core.model.runner.ResultType;
import hu.bme.mit.sette.core.model.runner.RunnerProjectSettings;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.model.snippet.SnippetContainer;
import hu.bme.mit.sette.core.model.snippet.SnippetProject;
import hu.bme.mit.sette.core.tasks.CsvBatchGenerator;
import hu.bme.mit.sette.core.tasks.CsvGenerator;
import hu.bme.mit.sette.core.tasks.TestSuiteGenerator;
import hu.bme.mit.sette.core.tasks.TestSuiteRunner;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.tool.ToolRegister;
import hu.bme.mit.sette.core.validator.ValidationException;
import hu.bme.mit.sette.snippetbrowser.SnippetBrowser;

public final class Run {
    private static final Logger LOG = LoggerFactory.getLogger(Run.class);
    private static final String SETTE_CONFIG_FILENAME = "sette.config.json";

    public static File SNIPPET_DIR;
    public static String SNIPPET_PROJECT;
    public static File OUTPUT_DIR;
    public static int RUNNER_TIMEOUT_IN_MS;
    public static boolean SKIP_BACKUP = false;
    public static boolean CREATE_BACKUP = false;

    private static final String[] TASKS = new String[] { "exit", "generator", "runner", "parser",
            "test-generator", "test-runner", "snippet-browser", "export-csv", "export-csv-batch" };

    public static void main(String[] args) throws Exception { // FIXME throws exception
        LOG.debug("main() called");
        Thread.currentThread().setName("MAIN");

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable ex) {
                if (ex instanceof ThreadDeath) {
                    System.err.println("Thread death: " + Thread.currentThread().getName());
                } else {
                    System.out.println("== SETTE FAILURE ==");
                    System.out.println("== UNCAUGHT EXCEPTION ==");
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        });

        //
        // Parse properties and init tools
        //
        Path configFile = Paths.get(SETTE_CONFIG_FILENAME);
        String configJson = Joiner.on('\n').join(Files.readAllLines(configFile));
        SetteConfiguration config = SetteConfiguration.parse(configJson);

        // save settings
        // FIXME select snippet project
        // if (config.getSnippetProjects().size() > 1) {
        // System.out.println("Available snippet projects: " + config.getSnippetProjects());
        // throw new RuntimeException("For now please only specify one");
        // }

        // SNIPPET_DIR = new File(basedir, "sette-snippets");
        // SNIPPET_PROJECT = config.getSnippetProjects().first();
        // OUTPUT_DIR = new File(basedir, config.getOutputDir());
        // RUNNER_TIMEOUT_IN_MS = config.getRunnerTimeoutInMs();

        // create tools
        try {
            // String catgVersion = readToolVersion(new File(BASEDIR, catgVersionFile));
            // if (catgVersion != null) {
            // new CatgTool(new File(BASEDIR, catgPath), catgVersion).register();
            // }
            //
            // String jPetVersion = readToolVersion(new File(BASEDIR, jPETVersionFile));
            // if (jPetVersion != null) {
            // new JPetTool(new File(BASEDIR, jPETPath), new File(BASEDIR, jPETDefaultBuildXml),
            // jPetVersion).register();
            // }
            //
            // String spfVersion = readToolVersion(new File(BASEDIR, spfVersionFile));
            // if (spfVersion != null) {
            // new SpfTool(new File(BASEDIR, spfPath), new File(BASEDIR, spfDefaultBuildXml),
            // spfVersion).register();
            // }
            //
            // String evoSuiteVersion = readToolVersion(new File(BASEDIR, evoSuiteVersionFile));
            // if (evoSuiteVersion != null) {
            // new EvoSuiteTool(new File(BASEDIR, evoSuitePath),
            // new File(BASEDIR, evoSuiteDefaultBuildXml), evoSuiteVersion).register();
            // }
            //
            // String randoopVersion = readToolVersion(new File(BASEDIR, randoopVersionFile));
            // if (randoopVersion != null) {
            // new RandoopTool(new File(BASEDIR, randoopPath),
            // new File(BASEDIR, randoopDefaultBuildXml), randoopVersion).register();
            // }

            // TODO stuff
            stuff(args);
        } catch (Exception ex) {
            System.out.println("== SETTE FAILURE ==");
            System.err.println(ExceptionUtils.getStackTrace(ex));

            System.err.println("==========");
            ValidationException vex;
            if (ex instanceof ValidationException) {
                vex = (ValidationException) ex;
            } else {
                vex = (ValidationException) ex.getCause();
            }

            vex.printStackTrace();

            // System.exit(0);

            ex.printStackTrace();
            System.err.println("==========");
            ex.printStackTrace();

            if (ex instanceof ValidationException) {
                System.err.println("Details:");
                System.err.println(((ValidationException) ex).getMessage());
            } else if (ex.getCause() instanceof ValidationException) {
                System.err.println("Details:");
                System.err.println(((ValidationException) ex.getCause()).getMessage());
            }
            System.exit(2);
        }

        // finish notification to all channels
        LOG.info("SETTE Run.main() finished");
        System.out.println("SETTE Run.main() finished");
        System.err.println("SETTE Run.main() finished");
    }

    private static int parseRunnerTimeout(String runnerTimeout) {
        try {
            int timeout = Integer.parseInt(runnerTimeout.trim());
            if (timeout <= 0) {
                throw new Exception();
            } else {
                return timeout * 1000;
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "The runner-timeout parameter must be a valid, positive number");
        }
    }

    private static String readToolVersion(File versionFile) {
        try {
            String version = new String(Files.readAllBytes(versionFile.toPath())).trim();
            return version.isEmpty() ? null : version;
        } catch (IOException ex) {
            // TODO handle error
            System.err.println("Cannot read tool version from: " + versionFile);
            return null;
        }
    }

    public static void stuff(String[] args) throws Exception {
        // Get in/out streams
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintStream out = System.out;

        // Parse arguments
        /*
         * Examples:
         * 
         * ./sette.sh --task generator --tool CATG
         * 
         * ./sette.sh --task generator --tool CATG --runner-project-tag "1st-run" --runner-timeout
         * 30 --skip-backup
         * 
         * ./sette.sh --help
         * 
         */
        // create the command line parser
        CommandLineParser parser = new DefaultParser();

        // create the Options
        // NOTE consider using something better, e.g. JCommander
        Options options = new Options();

        String separatedToolNames = Joiner.on(", ").join(ToolRegister.toMap().keySet());

        Option helpOption = Option.builder("h").longOpt("help").desc("Prints this help message")
                .build();

        Option taskOption = Option.builder().longOpt("task").hasArg().argName("TASK")
                .desc(String.format("Task to execute (%s)", String.join(", ", TASKS))).build();

        Option toolOption = Option.builder().longOpt("tool").hasArg().argName("TOOL")
                .desc(String.format("Tool to use (%s)", separatedToolNames)).build();

        Option runnerProjectTagOption = Option.builder().longOpt("runner-project-tag").hasArg()
                .argName("TAG").desc("The tag of the desired runner project").build();

        Option skipBackupOption = Option.builder().longOpt("skip-backup")
                .desc("Skip backup without asking when generating a runner project that already exists")
                .build();

        Option createBackupOption = Option.builder().longOpt("create-backup")
                .desc("Create backup without asking when generating a runner project that already exists")
                .build();

        Option runnerTimeoutOption = Option.builder().longOpt("runner-timeout").hasArg()
                .argName("SEC")
                .desc("Timeout for execution of a tool on one snippet (in seconds) - "
                        + "if missing then the setting in sette.properties will be used")
                .build();

        Option snippetProjectOption = Option.builder().longOpt("snippet-project").hasArg()
                .argName("PROJECT_NAME").desc("Name of the snippet project use - "
                        + "if missing then the setting in sette.properties will be used")
                .build();

        options.addOption(helpOption).addOption(taskOption).addOption(toolOption)
                .addOption(runnerProjectTagOption).addOption(skipBackupOption)
                .addOption(createBackupOption).addOption(runnerTimeoutOption)
                .addOption(snippetProjectOption);

        String task, toolName, runnerProjectTag;

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args, false);

            if (line.hasOption("h")) {
                new HelpFormatter().printHelp("sette", options, true);
                System.exit(1);
            }

            task = line.getOptionValue("task");

            toolName = line.getOptionValue("tool");
            runnerProjectTag = line.getOptionValue("runner-project-tag");
            SKIP_BACKUP = line.hasOption("skip-backup");
            CREATE_BACKUP = line.hasOption("create-backup");

            if (SKIP_BACKUP && CREATE_BACKUP) {
                System.out.println("Cannot both skip ad create a backup");
                System.exit(1);
                return;
            }

            if (line.hasOption("runner-timeout")) {
                RUNNER_TIMEOUT_IN_MS = parseRunnerTimeout(line.getOptionValue("runner-timeout"));
            }

            if (line.hasOption("snippet-project")) {
                SNIPPET_PROJECT = line.getOptionValue("snippet-project");
                Validate.notBlank(SNIPPET_PROJECT, "The snippet-project must not be blank");
            }
        } catch (ParseException ex) {
            System.out.println("Cannot parse arguments: " + ex.getMessage());
            new HelpFormatter().printHelp("sette", options, true);
            System.exit(1);
            return;
        }

        // print settings
        // FIXME
        // out.println("Base directory: " + BASEDIR);
        // out.println("Snippet directory: " + SNIPPET_DIR);
        // out.println("Snippet project name: " + SNIPPET_PROJECT);
        // out.println("Output directory: " + OUTPUT_DIR);

        // if (ToolRegister.get(CatgTool.class) != null) {
        // out.println("CATG directory: " + ToolRegister.get(CatgTool.class).getDir());
        // }
        // if (ToolRegister.get(JPetTool.class) != null) {
        // out.println("jPET executable: " + ToolRegister.get(JPetTool.class).getPetExecutable());
        // }
        // if (ToolRegister.get(SpfTool.class) != null) {
        // out.println("SPF JAR: " + ToolRegister.get(SpfTool.class).getToolJAR());
        // }
        // if (ToolRegister.get(EvoSuiteTool.class) != null) {
        // out.println("EvoSuite JAR: " + ToolRegister.get(EvoSuiteTool.class).getToolJAR());
        // }
        // if (ToolRegister.get(RandoopTool.class) != null) {
        // out.println("Randoop JAR: " + ToolRegister.get(RandoopTool.class).getToolJAR());
        // }

        out.println("Tools:");
        for (Tool tool : ToolRegister.toMap().values()) {
            out.println(String.format("  %s (Version: %s, Supported Java version: %s)",
                    tool.getName(), tool.getVersion(), tool.getSupportedJavaVersion()));
        }

        // get task
        if (task == null) {
            task = Run.readTask(in, out);
        }

        if (task == null || "exit".equals(task)) {
            return;
        }

        SnippetProject snippetProject = Run.createSnippetProject();
        // NOTE shortcut to batch csv
        if ("export-csv-batch".equals(task)) {
            new CsvBatchGenerator(snippetProject, OUTPUT_DIR, toolName, runnerProjectTag)
                    .generateAll();
        } else {
            Tool tool;
            if (toolName == null) {
                tool = Run.readTool(in, out);
            } else {
                tool = ToolRegister.get(toolName);

                if (tool == null) {
                    // NOTE enhance
                    System.err.println("Invalid tool: " + toolName);
                    System.exit(1);
                    return;
                }
            }

            while (StringUtils.isBlank(runnerProjectTag)) {
                out.print("Enter a runner project tag: ");
                out.flush();
                runnerProjectTag = in.readLine();

                if (runnerProjectTag == null) {
                    out.println("Exiting...");
                    System.exit(1);
                    return;
                }
            }

            runnerProjectTag = runnerProjectTag.trim();

            switch (task) {
                case "generator":
                    new GeneratorUI(snippetProject, tool, runnerProjectTag).run(in, out);
                    break;

                case "runner":
                    new RunnerUI(snippetProject, tool, runnerProjectTag, RUNNER_TIMEOUT_IN_MS)
                            .run(in, out);
                    break;

                case "parser":
                    new ParserUI(snippetProject, tool, runnerProjectTag).run(in, out);
                    break;

                case "test-generator":
                    // NOTE now the generator skips the test suite generation and only generates the
                    // ant
                    // build file
                    // if (tool.getOutputType() == ToolOutputType.INPUT_VALUES) {
                    new TestSuiteGenerator(snippetProject, OUTPUT_DIR, tool, runnerProjectTag)
                            .generate();
                    // } else {
                    // out.println("This tool has already generated a test suite");
                    // }
                    break;

                case "test-runner":
                    new TestSuiteRunner(snippetProject, OUTPUT_DIR, tool, runnerProjectTag)
                            .analyze();
                    break;

                case "snippet-browser":
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                SnippetBrowser frame = new SnippetBrowser(snippetProject);
                                frame.setVisible(true);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    break;

                case "export-csv":
                    new CsvGenerator(snippetProject, OUTPUT_DIR, tool, runnerProjectTag).generate();
                    // NOTE old code
                    // out.print("Target file: ");
                    // String file = in.readLine();
                    // exportCsvOld(snippetProject, new File(file), runnerProjectTag);
                    break;

                default:
                    throw new UnsupportedOperationException(
                            "Task has not been implemented yet: " + task);
            }
        }
    }

    private static SnippetProject createSnippetProject() throws SetteException, IOException {
        return SnippetProject.parse(SNIPPET_DIR.toPath().resolve(SNIPPET_PROJECT));
    }

    private static String readTask(BufferedReader in, PrintStream out) throws IOException {
        String task = null;

        while (task == null) {
            out.println("Available tasks:");
            for (int i = 0; i < Run.TASKS.length; i++) {
                out.println(String.format("  [%d] %s", i, Run.TASKS[i]));
            }

            out.print("Select task: ");

            String line = in.readLine();

            if (line == null) {
                out.println("EOF detected, exiting");
                return null;
            } else if (StringUtils.isBlank(line)) {
                out.println("Exiting");
                return null;
            }

            task = Run.parseTask(line);
            if (task == null) {
                out.println("Invalid task: " + line.trim());
            }
        }

        out.println("Selected task: " + task);
        return task;
    }

    private static String parseTask(String task) {
        task = task.trim();
        int idx = ArrayUtils.indexOf(Run.TASKS, task.toLowerCase());

        if (idx >= 0) {
            return Run.TASKS[idx];
        } else {
            try {
                return Run.TASKS[Integer.parseInt(task)];
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static Tool readTool(BufferedReader in, PrintStream out) throws IOException {
        // select tool
        Tool[] tools = ToolRegister.toMap().values().toArray(new Tool[0]);
        Tool tool = null;
        while (tool == null) {
            out.println("Available tools:");
            for (int i = 0; i < tools.length; i++) {
                out.println(String.format("  [%d] %s", i + 1, tools[i].getName()));
            }

            out.print("Select tool: ");

            String line = in.readLine();

            if (line == null) {
                out.println("EOF detected, exiting");
                return null;
            } else if (StringUtils.isBlank(line)) {
                out.println("Exiting");
                return null;
            }

            line = line.trim();
            int idx = -1;

            for (int i = 0; i < tools.length; i++) {
                if (tools[i].getName().equalsIgnoreCase(line)) {
                    idx = i;
                    break;
                }
            }

            if (idx >= 0) {
                tool = tools[idx];
            } else {
                try {
                    tool = tools[Integer.parseInt(line) - 1];
                } catch (Exception ex) {
                    tool = null;
                }
            }

            if (tool == null) {
                out.println("Invalid tool: " + line.trim());
            }
        }

        out.println("Selected tool: " + tool.getName());

        return tool;
    }

    private Run() {
        throw new UnsupportedOperationException("Static class");
    }

    @SuppressWarnings("unused")
    private static void exportCsvOld(SnippetProject snippetProject, File file,
            String runnerProjectTag) throws Exception {
        // TODO enhance this method
        List<Tool> tools = ToolRegister.toMap().values().stream().sorted()
                .collect(Collectors.toList());

        List<String> columns = new ArrayList<>();
        columns.add("Category");
        columns.add("Goal");
        columns.add("Container");
        columns.add("Required Java version");
        columns.add("Snippet");
        columns.add("Required coverage");

        Map<Tool, RunnerProjectSettings<Tool>> rpss = new HashMap<>();
        ResultType[] resultTypes = ResultType.values();

        for (Tool tool : tools) {
            rpss.put(tool, new RunnerProjectSettings<>(snippetProject, OUTPUT_DIR,
                    tool, runnerProjectTag));

            for (ResultType resultType : resultTypes) {
                columns.add(resultType.toString() + " - " + tool.getName());
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String column : columns) {
            sb.append(column).append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("\n");

        for (SnippetContainer container : snippetProject.getSnippetContainers()) {
            for (Snippet snippet : container.getSnippets().values()) {
                sb.append(container.getCategory()).append(",");
                sb.append(container.getGoal()).append(",");
                sb.append(container.getJavaClass().getName()).append(",");
                sb.append(container.getRequiredJavaVersion()).append(",");
                sb.append(snippet.getMethod().getName()).append(",");
                sb.append(snippet.getRequiredStatementCoverage() + "%");

                for (Tool tool : tools) {
                    RunnerProjectSettings<Tool> set = rpss.get(tool);
                    File inputs = RunnerProjectUtils.getSnippetInputsFile(set, snippet);
                    File result = RunnerProjectUtils.getSnippetResultFile(set, snippet);

                    if (result.exists()) {
                        System.out.println(tool.getName());
                        System.out.println(snippet.getMethod());
                        // TODO error handling
                        throw new RuntimeException("RESULT EXISTS");
                    }

                    ResultType rt;

                    SnippetInputsXml snippetInputsXml;
                    if (!inputs.exists()) {
                        // TODO input should exist, revise this section
                        // out.println(tool.getFullName());
                        // out.println(snippet.getMethod());
                        // throw new RuntimeException("INPUT NOT EXISTS");
                        rt = ResultType.NA;
                    } else {
                        Serializer serializer = new Persister(new AnnotationStrategy());

                        snippetInputsXml = serializer.read(SnippetInputsXml.class, inputs);
                        snippetInputsXml.validate();
                        rt = snippetInputsXml.getResultType();
                    }

                    int pos = ArrayUtils.indexOf(resultTypes, rt);

                    for (int i = 0; i < pos; i++) {
                        sb.append(",");
                    }
                    sb.append(",1");
                    for (int i = pos + 1; i < resultTypes.length; i++) {
                        sb.append(",");
                    }
                }

                sb.append("\n");
            }
        }

        try {
            Files.write(file.toPath(), sb.toString().getBytes());
        } catch (IOException ex) {
            System.err.println("Operation failed");
            ex.printStackTrace();
        }

        // out.println(sb.toString());

        // StringBuilder sb = new StringBuilder(testCaseToolInputs.size() *
        // 100);
        //
        // if (testCaseToolInputs.size() <= 0)
        // return sb.append("No data");
        //
        // List<Tool> tools = new ArrayList<>(testCases.get(0)
        // .generatedToolInputs().keySet());
        // Collections.sort(tools);
        //
        // sb.append(";;");
        // for (Tool tool : tools) {
        // sb.append(';').append(tool.getName()).append(";;;;;");
        // }
        //
        // sb.append('\n');
        //
        // sb.append("Package;Class;Test case");
        //
        // for (int i = 0; i < tools.size(); i++) {
        // sb.append(";N/A;EX;T/M;NC;C;Note");
        // }
        //
        // sb.append('\n');
        //
        // Collections.sort(testCases);
        //
        // for (TestCase tc : testCases) {
        // sb.append(tc.getPkg()).append(';');
        // sb.append(tc.getCls()).append(';');
        // sb.append(tc.getName());
        //
        // for (Tool tool : tools) {
        // TestCaseToolInput tcti = tc.generatedToolInputs().get(tool);
        //
        // switch (tcti.getResult()) {
        // case NA:
        // // sb.append(";1;0;0;0;0;");
        // sb.append(";X;;;;;");
        // break;
        // case EX:
        // // sb.append(";0;1;0;0;0;");
        // sb.append(";;X;;;;");
        // break;
        // case TM:
        // // sb.append(";0;0;1;0;0;");
        // sb.append(";;;X;;;");
        // break;
        // case NC:
        // // sb.append(";0;0;0;1;0;");
        // sb.append(";;;;X;;");
        // break;
        // case C:
        // // sb.append(";0;0;0;0;1;");
        // sb.append(";;;;;X;");
        // break;
        // case UNKNOWN:
        // default:
        // sb.append(";UNKNOWN;UNKNOWN;UNKNOWN;UNKNOWN;UNKNOWN;");
        // break;
        //
        // }
        //
        // sb.append(tcti.getNote());
        // }
        //
        // sb.append('\n');
        // }
        //
        // return sb;

    }
}
