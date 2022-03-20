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


package de.webis.annotator.ner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.HttpPostAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

public class NLPArchitectNER extends HttpPostAnnotator {
    private URI serviceURL;
    private ObjectMapper jsonMapper;

    public NLPArchitectNER() {
        super("http", "localhost", 8123, "/inference");

        jsonMapper = new ObjectMapper();
    }

    @Override
    public String getAnnotationTag() {
        return "intel-nlp-architect";
    }

    @Override
    protected void extractAnnotation(Query query, String response, EntityAnnotationFileHandle annotationFileHandle) throws IOException {
        String[] responseLines = response.split("\n");

        for (String line : responseLines) {
            JsonNode node = jsonMapper.readValue(line, JsonNode.class);
            node = node.get(0).get("doc");

            Iterator<JsonNode> spanIterator = node.get("spans").iterator();

            EntityAnnotation annotation = new EntityAnnotation();
            while (spanIterator.hasNext()) {
                JsonNode span = spanIterator.next();
                annotation.setBegin(span.get("start").asInt());
                annotation.setEnd(span.get("end").asInt());

                try {
                    annotation.setMention(query.getText().substring(annotation.getBegin(), Math.min(annotation.getEnd(), query.getText().length())));
                    annotationFileHandle.writeAnnotation(annotation);
                } catch (StringIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    protected void prepareRequest(Query query) {
        entityBuilder.setText("{\"model_name\": \"ner\", \"docs\": [{\"id\": 1, \"doc\": \"" + query.getText() + "\"}]}");
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new NLPArchitectNER();
        annotator.annotate(new Query("new york times square dance"));
    }
}
