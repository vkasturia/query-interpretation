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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.HttpGetAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.List;

public class DandelionEntityExtractor extends HttpGetAnnotator {
    private static ObjectMapper jsonMapper;

    public DandelionEntityExtractor() {
        super("https", "api.dandelion.eu", 443, "/datatxt/nex/v1/", "./data/persistent/logging/dandelion");

        defaultParams.add(new BasicNameValuePair("lang", "en"));
        //Insert Token
        defaultParams.add(new BasicNameValuePair("token", ""));

        jsonMapper = new ObjectMapper();
    }

    @Override
    protected void prepareRequest(Query query) {
        uriBuilder.addParameter("text", query.getText());
    }

    @Override
    protected void extractAnnotation(Query query, String resonse, EntityAnnotationFileHandle fileHandle) throws IOException {
        JsonNode node;
        EntityAnnotation annotation = new EntityAnnotation();

        node = jsonMapper.readValue(resonse, JsonNode.class);

        if (node != null) {
            if (node.has("results")) {

                for (JsonNode jsonNode : node.get("results")) {
                    node = jsonNode;

                    toAnnotation(fileHandle, node, annotation);
                }
            } else {
                toAnnotation(fileHandle, node, annotation);
            }
        }
    }

    private void toAnnotation(EntityAnnotationFileHandle fileHandle, JsonNode node, EntityAnnotation annotation) throws IOException {
        for (JsonNode jsonNode : node.get("annotations")) {
            node = jsonNode;

            annotation.setBegin(node.get("start").asInt());
            annotation.setEnd(node.get("end").asInt());
            annotation.setMention(node.get("spot").asText());
            annotation.setUrl(node.get("uri").asText());
            annotation.setScore(node.get("confidence").asDouble());


            fileHandle.writeAnnotation(annotation);
        }
    }

    @Override
    public String getAnnotationTag() {
        return "dandelion";
    }

    public static void main(String[] args) throws IOException {
        DandelionEntityExtractor annotator = new DandelionEntityExtractor();
        EntityAnnotationFileHandle annotationFileHandle = annotator.annotate(new Query("new york times square dance"));
        List<EntityAnnotation> annotations = annotationFileHandle.loadAnnotations();
        System.out.println(annotations);
        annotator.close();
    }
}
