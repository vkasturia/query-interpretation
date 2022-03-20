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

import com.microsoft.azure.cognitiveservices.search.entitysearch.implementation.BingEntitySearchAPIImpl;
import com.microsoft.azure.cognitiveservices.search.entitysearch.models.*;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.LoggedAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.PersistentIndex;
import okhttp3.Request;

import java.io.IOException;
import java.util.List;

public class BingEntitySearch implements LoggedAnnotator {
    private BingEntitySearchAPIImpl client;
    private PersistentIndex<String, EntityAnnotation> log;

    public BingEntitySearch() {
        client = new BingEntitySearchAPIImpl("https://entity-linking-framework.cognitiveservices.azure.com/bing/v7.0/entities/",
                builder -> builder.addNetworkInterceptor(chain -> {
                    Request request;
                    Request original = chain.request();
                    //Insert Bing Subscription Key
                    Request.Builder requestBuilder = original.newBuilder()
                            .addHeader("Ocp-Apim-Subscription-Key", "");

                    request = requestBuilder.build();

                    return chain.proceed(request);
                }));

        log = new PersistentIndex<>("data/persistent/logging/bing-entity-search");
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle fileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());
        if (log.contains(query.getText())) {
            List<EntityAnnotation> annotations = log.get(query.getText());

            for (EntityAnnotation annotation : annotations) {
                if (annotation != null)
                    try {
                        fileHandle.writeAnnotation(annotation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }

            fileHandle.flush();
            return fileHandle;
        }

        SearchResponse entityData;

        try {
            entityData = client.bingEntities().search(query.getText(),
                    new SearchOptionalParameter().withMarket("en-US"));
        } catch (ErrorResponseException e) {
            e.printStackTrace();
            return fileHandle;
        }

        if (entityData != null) {
            if (entityData.entities() != null)
                if (entityData.entities().value().size() > 0) {
                    List<Thing> entries = entityData.entities().value();
                    extractWikiLinks(entries, fileHandle);
                }

            if (entityData.places() != null)
                if (entityData.places().value().size() > 0) {
                    List<Thing> entries = entityData.places().value();
                    extractWikiLinks(entries, fileHandle);
                }

            fileHandle.flush();

            try {
                List<EntityAnnotation> annotations = fileHandle.loadAnnotations();

                if (annotations.isEmpty()) {
                    log.put(query.getText(), null);
                }

                for (EntityAnnotation annotation : annotations) {
                    log.put(query.getText(), annotation);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return fileHandle;
    }

    private void extractWikiLinks(List<Thing> entries, EntityAnnotationFileHandle annotationFileHandle) {
        EntityAnnotation annotation = new EntityAnnotation();
        for (Thing thing : entries) {
            if (thing.contractualRules() != null)
                for (ContractualRulesContractualRule rulesAttribution : thing.contractualRules()) {
                    if (rulesAttribution instanceof ContractualRulesLinkAttribution) {
                        if (((ContractualRulesLinkAttribution) rulesAttribution).text().equals("Wikipedia")) {
                            annotation.setUrl(((ContractualRulesLinkAttribution) rulesAttribution).url());
                            try {
                                annotationFileHandle.writeAnnotation(annotation);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        }
    }

    @Override
    public void close() {
        log.close();
    }

    @Override
    public String getAnnotationTag() {
        return "bing-entity-search";
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new BingEntitySearch();
        annotator.annotate(new Query("berlin"));
    }
}
