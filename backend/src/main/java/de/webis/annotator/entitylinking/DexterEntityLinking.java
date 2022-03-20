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
import de.webis.utils.WikiPageIDResolver;

import java.io.IOException;

public class DexterEntityLinking extends HttpGetAnnotator {
    private ObjectMapper jsonMapper;
    private WikiPageIDResolver wikiPageIDResolver;

    public DexterEntityLinking() {
        super("http", "localhost", 8080, "/dexter-webapp/api/rest/annotate");

        jsonMapper = new ObjectMapper();
        wikiPageIDResolver = new WikiPageIDResolver();
    }

    @Override
    protected void prepareRequest(Query query) {
        uriBuilder.addParameter("text", query.getText());
    }

    @Override
    protected void extractAnnotation(Query query, String response, EntityAnnotationFileHandle annotationFileHandle) {
        try {
            EntityAnnotation annotation = new EntityAnnotation();

            JsonNode node = jsonMapper.readValue(response, JsonNode.class);

            for (JsonNode jsonNode : node.get("spots")) {
                node = jsonNode;

                annotation.setBegin(node.get("start").asInt());
                annotation.setEnd(node.get("end").asInt());
                annotation.setMention(node.get("mention").asText());
                String url = wikiPageIDResolver.resolvePageID(node.get("entity").asText());
                annotation.setUrl(url);
                annotation.setScore(node.get("score").asDouble());

                if (annotation.getUrl() != null)
                    annotationFileHandle.writeAnnotation(annotation);
                else {
                    System.out.println(annotation);
                    System.out.println(node.get("entity").asText());
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAnnotationTag() {
        return "dexter";
    }

    public static void main(String[] args) {
        DexterEntityLinking annotator = new DexterEntityLinking();
        annotator.annotate(new Query("new york times square dance"));
        annotator.close();
    }
}
