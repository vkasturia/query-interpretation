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

import com.textrazor.AnalysisException;
import com.textrazor.TextRazor;
import com.textrazor.annotations.Entity;
import com.textrazor.annotations.Response;
import de.webis.annotator.LoggedAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.PersistentIndex;

import java.io.IOException;
import java.util.List;

public class TextRazorEntityLinker implements LoggedAnnotator {
    //Insert API Key
    private static String API_KEY = "";

    private TextRazor client;
    private static PersistentIndex<String, EntityAnnotation> LOGGER;

    public TextRazorEntityLinker() {
        client = new TextRazor(API_KEY);
        client.addExtractor("words");
        client.addExtractor("entities");
//        client.setAllowOverlap(true);
        client.setLanguageOverride("eng");

        LOGGER = new PersistentIndex<>("./data/persistent/logging/textrazor");
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle fileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());


        if (LOGGER.contains(query.getText())) {
            List<EntityAnnotation> annotations = LOGGER.get(query.getText());

            for (EntityAnnotation annotation : annotations) {
                if (annotation != null) {
                    try {
                        fileHandle.writeAnnotation(annotation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            fileHandle.flush();

            return fileHandle;
        }

        Response response;

        try {
            response = client.analyze(query.getText()).getResponse();
        } catch (IOException | AnalysisException e) {
            e.printStackTrace();
            response = null;
        }

        if (response == null) {
            return fileHandle;
        }

        EntityAnnotation annotation = new EntityAnnotation();
        List<Entity> entities = response.getEntities();

        if (entities != null) {
            for (Entity entity : entities) {

                annotation.setBegin(entity.getStartingPos());
                annotation.setEnd(entity.getEndingPos());
                annotation.setMention(entity.getMatchedText());
                annotation.setScore(entity.getRelevanceScore());

                String wikiLink = entity.getWikiLink();

                if (!wikiLink.isEmpty()) {
                    annotation.setUrl(entity.getWikiLink());

                    try {
                        fileHandle.writeAnnotation(annotation);
                        LOGGER.put(query.getText(), annotation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (entities.isEmpty()) {
                LOGGER.put(query.getText(), null);
            }
        } else {
            LOGGER.put(query.getText(), null);
        }

        fileHandle.flush();


        return fileHandle;
    }

    @Override
    public String getAnnotationTag() {
        return "textrazor";
    }

    @Override
    public void close() {
        LOGGER.close();
    }

    public static void main(String[] args) {
        TextRazorEntityLinker annotator = new TextRazorEntityLinker();

        annotator.annotate(new Query("new york times square dance"));
        annotator.close();
    }
}
