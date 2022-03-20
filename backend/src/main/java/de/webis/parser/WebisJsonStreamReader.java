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

package de.webis.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.InterpretationAnnotation;
import de.webis.datastructures.Query;
import de.webis.datastructures.WebisCorpusQuery;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WebisJsonStreamReader extends CorpusStreamReader {
    private ObjectMapper jsonMapper;
    private Iterator<JsonNode> queryIter;

    public WebisJsonStreamReader() {
        jsonMapper = new ObjectMapper();
        try {
            JsonNode node = jsonMapper.readValue(
                    new File("./data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus-test.json"),
                    JsonNode.class);

            queryIter = node.get("queries").iterator();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WebisJsonStreamReader(CorpusType corpusType) {
        jsonMapper = new ObjectMapper();
        try {
            File corpusFile;

            if (corpusType == CorpusType.TRAIN) {
                corpusFile = new File("./data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus-train.json");
            } else {
                corpusFile = new File("./data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus-test.json");
            }

            JsonNode node = jsonMapper.readValue(
                    corpusFile,
                    JsonNode.class);

            queryIter = node.get("queries").iterator();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Query next() {
        WebisCorpusQuery query = new WebisCorpusQuery();

        JsonNode currentQuery = queryIter.next();

        query.setId(currentQuery.get("id").asText());
        query.setText(currentQuery.get("query").asText());
        query.setDifficulty(currentQuery.get("difficulty").asInt());

        Set<String> categories = new HashSet<>();

        Iterator<JsonNode> elementIter = currentQuery.get("categories").iterator();

        while (elementIter.hasNext()) {
            categories.add(elementIter.next().asText());
        }

        query.setCategories(categories);

        elementIter = currentQuery.get("explicit_entities").iterator();

        while (elementIter.hasNext()) {
            JsonNode element = elementIter.next();
            EntityAnnotation annotation = new EntityAnnotation();

            annotation.setMention(element.get("mention").asText());
            annotation.setBegin(query.getText().indexOf(annotation.getMention()));
            annotation.setEnd(annotation.getBegin() + annotation.getMention().length());

            Iterator<JsonNode> entityIter = element.get("entity").iterator();
            annotation.setUrl(entityIter.next().asText());
            annotation.setScore(element.get("relevance").asDouble());

            if (annotation.getUrl().matches("^http(s)?://en.wikipedia.org/wiki/(.)*")) {
                query.addExplicitEntity(annotation);
            }
        }

        elementIter = currentQuery.get("implicit_entities").iterator();

        while (elementIter.hasNext()) {
            JsonNode element = elementIter.next();
            EntityAnnotation annotation = new EntityAnnotation();

            annotation.setMention(element.get("mention").asText());
            annotation.setBegin(query.getText().indexOf(annotation.getMention()));
            annotation.setEnd(annotation.getBegin() + annotation.getMention().length());

            Iterator<JsonNode> entityIter = element.get("entity").iterator();
            annotation.setUrl(entityIter.next().asText());
            annotation.setScore(element.get("relevance").asDouble());

            if (annotation.getUrl().matches("^http(s)?://en.wikipedia.org/wiki/(.)*")) {
                query.addImplicitEntity(annotation);
            }
        }

        elementIter = currentQuery.get("interpretations").iterator();

        while (elementIter.hasNext()) {
            JsonNode element = elementIter.next();
            InterpretationAnnotation annotation = new InterpretationAnnotation();
            List<String> interpretation = new LinkedList<>();

            annotation.setId(element.get("id").asInt());
            element.get("interpretation").iterator().forEachRemaining(
                    jsonNode -> interpretation.add(jsonNode.asText()));

            annotation.setInterpretation(interpretation);
            annotation.setRelevance(element.get("relevance").asDouble());
            if (annotation.getRelevance() > 1) {
                query.addInterpretation(annotation);
            }
        }


        return query;
    }

    @Override
    public boolean hasNext() {
        return queryIter.hasNext();
    }

    public static void main(String[] args) {
        CorpusStreamReader streamReader = new WebisJsonStreamReader();

        System.out.println(streamReader.next());
    }
}