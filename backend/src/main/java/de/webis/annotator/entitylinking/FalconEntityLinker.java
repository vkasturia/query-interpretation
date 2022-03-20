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
import de.webis.annotator.HttpPostAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class FalconEntityLinker extends HttpPostAnnotator {
    private ObjectMapper jsonMapper;

    public FalconEntityLinker() {
        super("https", "labs.tib.eu", 443, "/falcon/api");

        defaultParams.add(new BasicNameValuePair("mode", "short"));
        jsonMapper = new ObjectMapper();
    }

    @Override
    public String getAnnotationTag() {
        return "falcon";
    }

    @Override
    protected void extractAnnotation(Query query, String response, EntityAnnotationFileHandle annotationFileHandle) throws IOException {
        String[] responseLines = response.split("\n");

        EntityAnnotation annotation = new EntityAnnotation();
        for (String line : responseLines) {
            JsonNode entityNode = jsonMapper.readValue(line, JsonNode.class).get("entities");

            for (JsonNode node : entityNode) {
                URI uri = null;
                try {
                    uri = new URI(node.get(0).asText());
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }


                if (uri != null) {
                    annotation.setBegin(0);
                    annotation.setEnd(query.getText().length() - 1);
                    annotation.setMention(query.getText());
                    annotation.setUrl(uri.getPath().replaceFirst("/resource/", "http://en.wikipedia.org/wiki/"));
                    annotation.setScore(node.get(1).asDouble());
                    annotationFileHandle.writeAnnotation(annotation);
                }
            }
        }
    }

    @Override
    protected void prepareRequest(Query query) {
        entityBuilder.setText("{\"text\":\"" + query.getText() + "\"}");
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new FalconEntityLinker();
        annotator.annotate(new Query("new york times square dance"));
    }


}
