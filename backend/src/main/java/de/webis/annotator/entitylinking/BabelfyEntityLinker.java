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
import java.util.Iterator;

public class BabelfyEntityLinker extends HttpGetAnnotator {
    private ObjectMapper jsonMapper;

    public BabelfyEntityLinker() {
        super("https", "babelfy.io", 443, "/v1/disambiguate", "data/persistent/logging/babelfy");
        defaultParams.add(new BasicNameValuePair("lang", "EN"));
        //Insert Babelfy Key 
        defaultParams.add(new BasicNameValuePair("key", ""));
        jsonMapper = new ObjectMapper();
    }

    @Override
    protected void prepareRequest(Query query) {
        uriBuilder.addParameter("text", query.getText());
    }

    @Override
    protected void extractAnnotation(Query query, String response, EntityAnnotationFileHandle annotationFileHandle) throws IOException {
        JsonNode node = jsonMapper.readValue(response, JsonNode.class);

        Iterator<JsonNode> resultIterator = node.elements();

        EntityAnnotation annotation = new EntityAnnotation();
        while (resultIterator.hasNext()) {
            node = resultIterator.next();
            if (node.asText().equals("Your key is not valid or the daily requests limit has been reached. Please visit http://babelfy.org.")) {
                throw new IOException("Key is invalid or daily limit exceeded!");
            }

            if (node.has("DBpediaURL"))
                if (!node.get("DBpediaURL").asText().isEmpty()) {
                    annotation.setBegin(node.get("charFragment").get("start").asInt());
                    annotation.setEnd(node.get("charFragment").get("end").asInt() + 1);
                    annotation.setMention(query.getText().substring(annotation.getBegin(), annotation.getEnd()));
                    annotation.setUrl(node.get("DBpediaURL").asText()
                            .replaceAll("http://dbpedia.org/resource/", "https://en.wikipedia.org/wiki/"));
                    annotation.setScore(node.get("score").asDouble());

                    annotationFileHandle.writeAnnotation(annotation);
                }
        }

    }

    @Override
    public String getAnnotationTag() {
        return "babelfy";
    }

    public static void main(String[] args) {
        BabelfyEntityLinker annotator = new BabelfyEntityLinker();

        annotator.annotate(new Query("new york times square dance"));
        annotator.close();
    }
}
