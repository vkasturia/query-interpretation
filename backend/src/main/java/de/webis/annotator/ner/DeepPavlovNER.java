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

public class DeepPavlovNER extends HttpPostAnnotator {
    private ObjectMapper jsonMapper;

    public DeepPavlovNER() {
        super("http", "localhost", 5555, "model");
        jsonMapper = new ObjectMapper();
    }

    @Override
    protected void extractAnnotation(Query query, String response, EntityAnnotationFileHandle annotationFileHandle) throws IOException {
        JsonNode node = jsonMapper.readValue(response, JsonNode.class);

        JsonNode queryNode = node.get(0).get(0);
        JsonNode annotationNode = node.get(0).get(1);

        StringBuilder mentionBuilder = new StringBuilder();
        EntityAnnotation annotation = new EntityAnnotation();

        for (int i = 0; i < annotationNode.size(); i++) {
            String annotationTag = annotationNode.get(i).asText();

            if (annotationTag.equals("O")) {
                if (mentionBuilder.length() > 0) {
                    String mention = mentionBuilder.toString().trim();
                    annotation.setMention(mention);

                    annotation.setBegin(query.getText().indexOf(mention));
                    annotation.setEnd(annotation.getBegin() + mention.length());

                    mentionBuilder = new StringBuilder();
                    annotationFileHandle.writeAnnotation(annotation);
                }
            } else {
                if (annotationTag.startsWith("I")) {
                    mentionBuilder.append(" ").append(queryNode.get(i).asText());
                } else {
                    if (mentionBuilder.length() > 0) {
                        String mention = mentionBuilder.toString().trim();
                        annotation.setMention(mention);

                        annotation.setBegin(query.getText().indexOf(mention));
                        annotation.setEnd(annotation.getBegin() + mention.length());

                        mentionBuilder = new StringBuilder();
                        annotationFileHandle.writeAnnotation(annotation);
                    }

                    mentionBuilder.append(queryNode.get(i).asText());
                }

            }
        }

        if (mentionBuilder.length() > 0) {
            String mention = mentionBuilder.toString().trim();
            annotation.setMention(mention);

            annotation.setBegin(query.getText().indexOf(mention));
            annotation.setEnd(annotation.getBegin() + mention.length());

            annotationFileHandle.writeAnnotation(annotation);
        }
    }

    @Override
    protected void prepareRequest(Query query) {
        entityBuilder.setText("{\"x\": [\"" + query.getText() + "\"]}");
    }

    @Override
    public String getAnnotationTag() {
        return "deeppavlov-ner";
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new DeepPavlovNER();

        try {
            System.out.println(annotator.annotate(new Query("Michigan is a state in the United States")).loadAnnotations());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
