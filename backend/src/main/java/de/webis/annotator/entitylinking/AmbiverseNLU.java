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
import de.webis.annotator.HttpPostAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class AmbiverseNLU extends HttpPostAnnotator {
    private int queryIncrement;

    private static ObjectMapper jsonMapper;

    public AmbiverseNLU() {
        super("http", "localhost", 8080, "/entitylinking/analyze");

        queryIncrement = 0;
        jsonMapper = new ObjectMapper();
    }

    @Override
    public String getAnnotationTag() {
        return "ambiverse-nlu";
    }

    @Override
    protected void extractAnnotation(Query query, String response, EntityAnnotationFileHandle annotationFileHandle) throws IOException {
        String[] responseLines = response.split("\n");

        for (String line : responseLines) {
            JsonNode results = jsonMapper.readValue(line, JsonNode.class);

            for (JsonNode matchNode : results.get("matches")) {
                String url;
                double score = 0.0;

                if (matchNode.get("entity").has("id")) {
                    url = matchNode.get("entity").get("id").asText();

                    if (results.has("entities")) {
                        for (JsonNode entityNode : results.get("entities")) {
                            if (entityNode.get("id").asText().equals(url)) {
                                url = entityNode.get("url").asText();

                                score = entityNode.get("salience").asDouble();
                            }
                        }
                    }

                    EntityAnnotation entityAnnotation = new EntityAnnotation(
                            matchNode.get("charOffset").asInt(),
                            matchNode.get("charOffset").asInt() + matchNode.get("charLength").asInt(),
                            matchNode.get("text").asText(),
                            URLDecoder.decode(url, StandardCharsets.UTF_8.name()).replaceAll("[ ]", "_"),
                            score);

                    annotationFileHandle.writeAnnotation(entityAnnotation);
                }
            }
        }

        queryIncrement++;
    }

    @Override
    protected void prepareRequest(Query query) {
        entityBuilder.setText("{\"docId\": \"" + queryIncrement + "\", \"language\": \"en\", \"text\": \"" + query.getText() + "\", \"extractConcepts\": \"false\"}");
    }
}
