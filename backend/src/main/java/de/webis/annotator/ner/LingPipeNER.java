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

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.AbstractExternalizable;
import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;

import java.io.File;
import java.io.IOException;

public class LingPipeNER implements EntityAnnotator {
    private Chunker chunker;

    public LingPipeNER() {
        File modelFile = new File(getClass().getResource("/ne-en-news-muc6.AbstractCharLmRescoringChunker").getPath());
        try {
            chunker = (Chunker) AbstractExternalizable.readObject(modelFile);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle fileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());

        if (chunker != null) {
            Chunking chunking = chunker.chunk(query.getText());

            EntityAnnotation annotation = new EntityAnnotation();
            for (Chunk chunk : chunking.chunkSet()) {
                annotation.setBegin(chunk.start());
                annotation.setEnd(chunk.end());
                annotation.setMention(query.getText().substring(chunk.start(), chunk.end()));
                annotation.setScore(chunk.score());

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

    @Override
    public String getAnnotationTag() {
        return "lingpipe";
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new LingPipeNER();
        annotator.annotate(new Query("new york times square dance"));
    }
}
