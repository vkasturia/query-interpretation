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

package de.webis.annotator.exer;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.LoggedAnnotator;
import de.webis.annotator.exer.strategies.AllNGrams;
import de.webis.annotator.exer.strategies.ExerStrategy;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.PersistentStore;
import de.webis.utils.StreamSerializer;

import java.io.IOException;
import java.util.Set;

public class WebisExplicitEntityRetriever implements EntityAnnotator, LoggedAnnotator {
    private PersistentStore<String, Set<String>> index;

    private ExerStrategy strategy;

    public WebisExplicitEntityRetriever(ExerStrategy strategy) {
        this.strategy = strategy;

        index = new PersistentStore<>("data/persistent/wiki_table_webis_disambiguations_without_redirects_namespaces_concepts_final");
        index.setSerializer(StreamSerializer.class);
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle annotationFileHandle = new EntityAnnotationFileHandle(
                query.getText(), getAnnotationTag());

        EntityAnnotation entityAnnotation = new EntityAnnotation();
        Set<String> segments = strategy.apply(query);

        long start = System.currentTimeMillis();
        for (String segment : segments) {
            Set<String> annotations = index.get(segment);

            if (annotations != null) {
                entityAnnotation.setBegin(query.getText().indexOf(segment));
                entityAnnotation.setEnd(entityAnnotation.getBegin() + segment.length());
                entityAnnotation.setMention(segment);

                for (String annotation : annotations) {
                    entityAnnotation.setUrl(annotation);

                    try {
                        annotationFileHandle.writeAnnotation(entityAnnotation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        annotationFileHandle.setRuntime(System.currentTimeMillis() - start);
        annotationFileHandle.flush();
        return annotationFileHandle;
    }

    @Override
    public String getAnnotationTag() {
        return "webis-exer";
    }

    @Override
    public void close() {
        index.close();
    }

    public static void main(String[] args) {
        WebisExplicitEntityRetriever webisExplicitEntityRetriever = new WebisExplicitEntityRetriever(new AllNGrams());
        try {
            System.out.println(webisExplicitEntityRetriever.annotate(new Query("barack obama")).loadAnnotations());
        } catch (IOException e) {
            e.printStackTrace();
        }

        webisExplicitEntityRetriever.close();

//        webisExplicitEntityRetriever = new WebisExplicitEntityRetriever(new All1Grams());
//        webisExplicitEntityRetriever.annotate(new Query("new york times square dance"));
//
//        webisExplicitEntityRetriever.close();
    }
}
