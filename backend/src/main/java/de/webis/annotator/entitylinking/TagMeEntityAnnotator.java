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
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.HttpGetAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TagMeEntityAnnotator extends HttpGetAnnotator {
    private static ObjectMapper jsonMapper;

    public TagMeEntityAnnotator() {
        super("https", "tagme.d4science.org", 443, "/tagme/tag");
        defaultParams = new ArrayList<>();
        defaultParams.add(new BasicNameValuePair("lang", "en"));
        defaultParams.add(new BasicNameValuePair("include_abstract", "false"));
        defaultParams.add(new BasicNameValuePair("include_categories", "false"));
        //Insert Token 
        defaultParams.add(new BasicNameValuePair("gcube-token", ""));

        jsonMapper = new ObjectMapper();
    }

    @Override
    protected void prepareRequest(Query query) {
        uriBuilder.addParameter("text", query.getText());
    }

    @Override
    public String getAnnotationTag() {
        return "tagme";
    }

    @Override
    protected void extractAnnotation(Query query, String response, EntityAnnotationFileHandle annotationFileHandle) throws IOException {
        JsonNode node = jsonMapper.readValue(response, JsonNode.class);
        Iterator<JsonNode> annotationIter = node.get("annotations").iterator();

        EntityAnnotation annotation = new EntityAnnotation();

        while (annotationIter.hasNext()) {
            JsonNode annotationNode = annotationIter.next();

            annotation.setBegin(annotationNode.get("start").asInt());
            annotation.setEnd(annotationNode.get("end").asInt());
            annotation.setMention(annotationNode.get("spot").asText());
            annotation.setScore(annotationNode.get("rho").asDouble());

            String url = "https://en.wikipedia.org/wiki/" +
                    URLEncoder.encode(annotationNode.get("title").asText(), "utf-8").replace("+", "_");

            annotation.setUrl(url);

            annotationFileHandle.writeAnnotation(annotation);
        }

        annotationFileHandle.setRuntime(node.get("time").asDouble());
    }

    public static void main(String[] args) throws IOException {
        EntityAnnotator entityAnnotator = new TagMeEntityAnnotator();
        EntityAnnotationFileHandle annotationFileHandle = entityAnnotator.annotate(new Query("new york times square dance"));
        List<EntityAnnotation> annotations = annotationFileHandle.loadAnnotations();
        System.out.println(annotations);
    }


}
