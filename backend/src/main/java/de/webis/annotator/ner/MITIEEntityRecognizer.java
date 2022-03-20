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
import edu.mit.ll.mitie.*;

import java.io.IOException;

public class MITIEEntityRecognizer implements EntityAnnotator {
    private NamedEntityExtractor namedEntityExtractor;

    public MITIEEntityRecognizer() {
        System.loadLibrary("javamitie");
        namedEntityExtractor = new NamedEntityExtractor(getClass().getResource("/ner_model.dat").getPath());
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle fileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());

        StringVector tokens = global.tokenize(query.getText());
        EntityMentionVector entities = namedEntityExtractor.extractEntities(tokens);

        EntityAnnotation entityAnnotation = new EntityAnnotation();
        StringBuilder mentionBuilder = new StringBuilder();
        for (int i = 0; i < entities.size(); i++) {
            EntityMention mention = entities.get(i);

            for (int j = mention.getStart(); j < mention.getEnd(); j++) {
                mentionBuilder.append(tokens.get(j)).append(" ");
            }

            entityAnnotation.setMention(mentionBuilder.toString().trim());

            entityAnnotation.setBegin(query.getText().indexOf(entityAnnotation.getMention()));
            entityAnnotation.setEnd(entityAnnotation.getBegin() + entityAnnotation.getMention().length());

            entityAnnotation.setScore(mention.getScore());

            try {
                fileHandle.writeAnnotation(entityAnnotation);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mentionBuilder.delete(0, mentionBuilder.length());
        }

        fileHandle.flush();
        return fileHandle;
    }

    @Override
    public String getAnnotationTag() {
        return "mit-ie";
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new MITIEEntityRecognizer();

        annotator.annotate(new Query("Barack Obama"));
    }
}
