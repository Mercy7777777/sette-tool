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
package hu.bme.mit.sette.samplesnippets._2_complex.dependencies;

import hu.bme.mit.sette.common.snippets.SnippetInputContainer;
import samplesnippets.CoordinateStructure;

public final class TS2_Structure_Inputs {
    private TS2_Structure_Inputs() {
        throw new UnsupportedOperationException("Static class");
    }
    
    public static SnippetInputContainer guessParams() {
        SnippetInputContainer inputs = new SnippetInputContainer(2);

        inputs.addByParameters(1, 1);
        inputs.addByParameters(-1, 1);
        inputs.addByParameters(1, -1);
        inputs.addByParameters(-1, -1);
        inputs.addByParameters(0, 0);

        return inputs;
    }

    public static SnippetInputContainer guess() {
        SnippetInputContainer inputs = new SnippetInputContainer(1);

        inputs.addByParameters((Object) null);

        CoordinateStructure c;

        c = new CoordinateStructure();
        c.x = 1;
        c.y = 1;
        inputs.addByParameters(c);

        c = new CoordinateStructure();
        c.x = 1;
        c.y = -1;
        inputs.addByParameters(c);

        c = new CoordinateStructure();
        c.x = -1;
        c.y = 1;
        inputs.addByParameters(c);

        c = new CoordinateStructure();
        c.x = -1;
        c.y = -1;
        inputs.addByParameters(c);

        inputs.addByParameters(new CoordinateStructure());

        return inputs;
    }
}
