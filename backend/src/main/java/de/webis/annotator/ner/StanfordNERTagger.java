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

package de.webis.annotator.ner;

import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.Triple;

import java.io.IOException;
import java.util.List;

public class StanfordNERTagger implements EntityAnnotator {
    private static AbstractSequenceClassifier<CoreLabel> classifier;

    public StanfordNERTagger() {
        try {
            classifier = CRFClassifier.getClassifier(
                    getClass()
                            .getClassLoader()
                            .getResource("classifiers/english.all.3class.distsim.crf.ser.gz")
                            .getFile()
            );
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle fileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());
        List<Triple<String, Integer, Integer>> annotations = classifier.classifyToCharacterOffsets(query.getText());

        EntityAnnotation entityAnnotation = new EntityAnnotation();
        for (Triple<String, Integer, Integer> annotation : annotations) {
            entityAnnotation.setBegin(annotation.second);
            entityAnnotation.setEnd(annotation.third);
            entityAnnotation.setMention(query.getText().substring(entityAnnotation.getBegin(), entityAnnotation.getEnd()));

            try {
                fileHandle.writeAnnotation(entityAnnotation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fileHandle.flush();

        return fileHandle;
    }

    @Override
    public String getAnnotationTag() {
        return "stanford-ner";
    }

    public static void main(String[] args) {
        StanfordNERTagger tagger = new StanfordNERTagger();
        tagger.annotate(new Query("new york times square dance"));
    }
}
