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
package hu.bme.mit.sette.core.random;

import org.apache.commons.lang3.StringEscapeUtils;

import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

// FIXME fixes string escapes in test cases (EvoSuite)
// usage: compilationUnit.accept(new JavaParserFixStringVisitor(), null)
public class JavaParserFixStringVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(CharLiteralExpr n, Void arg) {
        handle(n);
    }

    @Override
    public void visit(StringLiteralExpr n, Void arg) {
        handle(n);
    }

    // FIXME
    // private static final CharSequenceTranslator MAPPING = new LookupTranslator(
    // new String[][] { { "\\\\", "\\" },
    // // { "\\\"", "\"" },
    // // { "\\'", "'" },
    // // { "\\", "" }
    // });

    private static void handle(StringLiteralExpr n) {
        String v = StringEscapeUtils.escapeJava(n.getValue());
        // v = MAPPING.translate(v); // do not revert back \\u0342 etc...
        n.setValue(v);
    }
}
