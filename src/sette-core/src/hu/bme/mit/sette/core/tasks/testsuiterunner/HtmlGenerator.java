package hu.bme.mit.sette.core.tasks.testsuiterunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import hu.bme.mit.sette.core.model.parserxml.FileCoverageElement;
import hu.bme.mit.sette.core.model.parserxml.SnippetCoverageXml;
import hu.bme.mit.sette.core.model.runner.RunnerProjectUtils;
import hu.bme.mit.sette.core.model.snippet.Snippet;
import hu.bme.mit.sette.core.tasks.EvaluationTask;
import hu.bme.mit.sette.core.tool.Tool;
import hu.bme.mit.sette.core.util.io.PathUtils;

public final class HtmlGenerator {
    private final EvaluationTask<Tool> testSuiteRunner;

    /**
     * @param testSuiteRunner
     */
    public HtmlGenerator(EvaluationTask<Tool> testSuiteRunner) {
        this.testSuiteRunner = testSuiteRunner;
    }

    public void generate(Snippet snippet, SnippetCoverageXml coverageXml) throws IOException {
        File htmlFile = RunnerProjectUtils.getSnippetHtmlFile(
                this.testSuiteRunner.getRunnerProjectSettings(),
                snippet);

        String htmlTitle = this.testSuiteRunner.getTool().getName() + " - "
                + snippet.getContainer().getJavaClass().getName() + '.'
                + snippet.getMethod().getName() + "()";
        StringBuilder htmlData = new StringBuilder();
        htmlData.append("<!DOCTYPE html>\n");
        htmlData.append("<html lang=\"hu\">\n");
        htmlData.append("<head>\n");
        htmlData.append("       <meta charset=\"utf-8\" />\n");
        htmlData.append("       <title>" + htmlTitle + "</title>\n");
        htmlData.append("       <style type=\"text/css\">\n");
        htmlData.append("               .code { font-family: 'Consolas', monospace; }\n");
        htmlData.append(
                "               .code .line { border-bottom: 1px dotted #aaa; white-space: pre; }\n");
        htmlData.append("               .code .green { background-color: #CCFFCC; }\n");
        htmlData.append("               .code .yellow { background-color: #FFFF99; }\n");
        htmlData.append("               .code .red { background-color: #FFCCCC; }\n");
        htmlData.append("               .code .line .number {\n");
        htmlData.append("                       display: inline-block;\n");
        htmlData.append("                       width:50px;\n");
        htmlData.append("                       text-align:right;\n");
        htmlData.append("                       margin-right:5px;\n");
        htmlData.append("               }\n");
        htmlData.append("       </style>\n");
        htmlData.append("</head>\n");
        htmlData.append("\n");
        htmlData.append("<body>\n");
        htmlData.append("       <h1>" + htmlTitle + "</h1>\n");

        for (FileCoverageElement fce : coverageXml.getCoverage()) {
            htmlData.append("       <h2>" + fce.getName() + "</h2>\n");
            htmlData.append("       \n");

            File src = new File(this.testSuiteRunner.getSnippetProject().getSourceDir().toFile(),
                    fce.getName());
            List<String> srcLines = PathUtils.readAllLines(src.toPath());

            int[] full = TestSuiteRunnerHelper.linesToArray(fce.getFullyCoveredLines());
            int[] partial = TestSuiteRunnerHelper.linesToArray(fce.getPartiallyCoveredLines());
            int[] not = TestSuiteRunnerHelper.linesToArray(fce.getNotCoveredLines());

            htmlData.append("       <div class=\"code\">\n");
            int i = 1;
            for (String srcLine : srcLines) {
                String divClass = getLineDivClass(i, full, partial, not);
                htmlData.append("               <div class=\"" + divClass
                        + "\"><div class=\"number\">" + i + "</div> " + srcLine + "</div>\n");
                i++;
            }
            htmlData.append("       </div>\n\n");
        }

        htmlData.append("</body>\n");
        htmlData.append("</html>\n");

        PathUtils.write(htmlFile.toPath(), htmlData.toString().getBytes());
    }

    private static String getLineDivClass(int lineNumber, int[] full, int[] partial, int[] not) {
        if (Arrays.binarySearch(full, lineNumber) >= 0) {
            return "line green";
        } else if (Arrays.binarySearch(partial, lineNumber) >= 0) {
            return "line yellow";
        } else if (Arrays.binarySearch(not, lineNumber) >= 0) {
            return "line red";
        } else {
            return "line";
        }
    }
}
