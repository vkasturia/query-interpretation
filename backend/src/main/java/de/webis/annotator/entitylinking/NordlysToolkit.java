/*
* Copyright 2018-2022 Vaibhav Kasturia <vbh18kas@gmail.com>
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and associated documentation files (the "Software"), to deal in the Software without restriction, 
* including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
* and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
* subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial 
* portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
* LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE 
* OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package de.webis.annotator.entitylinking;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public abstract class NordlysToolkit implements EntityAnnotator {
    private static final File workingDir = new File("./third-party/nordlys");
    protected static final ObjectMapper jsonMapper = new ObjectMapper();

    public NordlysToolkit() {
        jsonMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        jsonMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        jsonMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    public EntityAnnotationFileHandle recognize(Query query) {
        System.out.println("Annotate \"" + query + "\" ...");
        StringBuilder builder = new StringBuilder();

        EntityAnnotationFileHandle annotationFileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());

        Process process;
        try {
            process = Runtime.getRuntime().exec(
                    getCmd(query.getText()),
                    null,
                    workingDir);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            process.waitFor();

            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        parseAnnotations(builder.toString(), annotationFileHandle);

        annotationFileHandle.flush();

        return annotationFileHandle;
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        return recognize(query);
    }

    protected abstract String[] getCmd(String query);

    protected abstract void parseAnnotations(String jsonString, EntityAnnotationFileHandle fileHandle);

    public static void main(String[] args) {
        NordlysToolkit linker = new NordlysEntityRetriever();

        linker.annotate(new Query("directed bela glen glenda bride monster plan 9 outer space"));
    }
}
