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

import com.google.common.collect.ObjectArrays;
import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import opennlp.tools.entitylinker.EntityLinker;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OpenNLPEntityLinker implements EntityAnnotator {
    private SentenceDetectorME sentenceDetector;
    private Tokenizer tokenizer;
    private NameFinderME personFinder;
    private NameFinderME locationFinder;
    private NameFinderME organizationFinder;

    private EntityLinker entityLinker;

    public OpenNLPEntityLinker() {
        InputStream smStream = getClass().getResourceAsStream("/opennlp/en-sent.bin");
        InputStream tokenStream = getClass().getResourceAsStream("/opennlp/en-token.bin");

        InputStream nerPersonStream = getClass().getResourceAsStream("/opennlp/en-ner-person.bin");
        InputStream nerLocationStream = getClass().getResourceAsStream("/opennlp/en-ner-location.bin");
        InputStream nerOrganizationStream = getClass().getResourceAsStream("/opennlp/en-ner-organization.bin");

        SentenceModel sentenceModel = null;
        TokenizerModel tokenizerModel = null;
        TokenNameFinderModel personFinderModel = null;
        TokenNameFinderModel locationFinderModel = null;
        TokenNameFinderModel organizationFinderModel = null;

        try {
            sentenceModel = new SentenceModel(smStream);
            tokenizerModel = new TokenizerModel(tokenStream);

            personFinderModel = new TokenNameFinderModel(nerPersonStream);
            locationFinderModel = new TokenNameFinderModel(nerLocationStream);
            organizationFinderModel = new TokenNameFinderModel(nerOrganizationStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (sentenceModel != null)
            sentenceDetector = new SentenceDetectorME(sentenceModel);

        if (tokenizerModel != null)
            tokenizer = new TokenizerME(tokenizerModel);


        if (personFinderModel != null)
            personFinder = new NameFinderME(personFinderModel);


        if (locationFinderModel != null)
            locationFinder = new NameFinderME(locationFinderModel);

        if (organizationFinderModel != null)
            organizationFinder = new NameFinderME(organizationFinderModel);

//        try {
//            entityLinker = EntityLinkerFactory.getLinker(new EntityLinkerProperties(getClass().getResourceAsStream("/opennlp/entitylinker.properties")));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle fileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());

        Span[] sentences = sentenceDetector.sentPosDetect(query.getText());
        List<Span[]> tokensBySentence = new ArrayList<>();
        List<Span[]> namesBySentence = new ArrayList<>();

        for (Span sentenceSpan : sentences) {
            Span[] tokens = tokenizer.tokenizePos(
                    String.valueOf(sentenceSpan.getCoveredText(query.getText())));

            tokensBySentence.add(tokens);

            Span[] persons = personFinder.find(
                    tokenizer.tokenize(String.valueOf(sentenceSpan.getCoveredText(query.getText()))));

            Span[] locations = locationFinder.find(
                    tokenizer.tokenize(String.valueOf(sentenceSpan.getCoveredText(query.getText()))));

            Span[] organizations = organizationFinder.find(
                    tokenizer.tokenize(String.valueOf(sentenceSpan.getCoveredText(query.getText()))));

            Span[] names = ObjectArrays.concat(persons, locations, Span.class);
            names = ObjectArrays.concat(names, organizations, Span.class);

            namesBySentence.add(names);
        }

        EntityAnnotation annotation = new EntityAnnotation();
        for (int i = 0; i < namesBySentence.size(); i++) {
            for (Span name : namesBySentence.get(i)) {
                StringBuilder mentionBuilder = new StringBuilder();

                for (int j = name.getStart(); j < name.getEnd(); j++) {
                    mentionBuilder.append(tokensBySentence.get(i)[j].getCoveredText(sentences[i].getCoveredText(query.getText())))
                            .append(" ");
                }

                annotation.setMention(mentionBuilder.toString().trim());
                annotation.setBegin(query.getText().indexOf(annotation.getMention()));
                annotation.setEnd(annotation.getBegin() + annotation.getMention().length());
                annotation.setScore(name.getProb());

                try {
                    fileHandle.writeAnnotation(annotation);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mentionBuilder.delete(0, mentionBuilder.length());
            }
        }
        fileHandle.flush();

        return fileHandle;
    }

    @Override
    public String getAnnotationTag() {
        return "opennlp";
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new OpenNLPEntityLinker();
        annotator.annotate(new Query("new york times square dance"));
    }
}
