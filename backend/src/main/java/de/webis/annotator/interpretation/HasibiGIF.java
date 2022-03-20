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

package de.webis.annotator.interpretation;

import de.webis.annotator.InterpretationAnnotator;
import de.webis.annotator.LoggedAnnotator;
import de.webis.annotator.entitylinking.DandelionEntityExtractor;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.InterpretationAnnotation;
import de.webis.datastructures.Query;
import de.webis.parser.CorpusStreamReader;
import de.webis.parser.WebisJsonStreamReader;

import java.io.*;
import java.util.*;

public class HasibiGIF implements InterpretationAnnotator {

    @Override
    public Set<InterpretationAnnotation> annotate(Query query, EntityAnnotationFileHandle entityAnnotations) {
        PrintWriter writer = null;
        ProcessBuilder processBuilder = new ProcessBuilder();

        try {
            writer = new PrintWriter(new FileWriter("data/annotations/interpretation/tmp.tsv"));
            writer.println("qid\tmention\tfreebase_id\tscore");
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<EntityAnnotation> annotations = null;

        try {
            annotations = entityAnnotations.loadAnnotations();
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (annotations != null && writer != null) {
            for (EntityAnnotation entityAnnotation : annotations) {
                writer.println(
                        query.getId() + "\t" + entityAnnotation.getMention() + "\t" + entityAnnotation.getUrl() +
                                "\t" + entityAnnotation.getScore());
            }

            writer.flush();
            writer.close();
        }

        processBuilder.command("third-party/EntityLinkingInQueries-ELQ/run.sh");
        Process process = null;

        try {
            process = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (process != null) {
            try {
                int exitVal = process.waitFor();

                if (exitVal == 0) {
                    return parseAnnotations(query, annotations);
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

        return new HashSet<>();
    }

    private Set<InterpretationAnnotation> parseAnnotations(Query query, List<EntityAnnotation> entities) throws IOException {
        Set<InterpretationAnnotation> annotations = new HashSet<>();

        BufferedReader reader = new BufferedReader(new FileReader("data/annotations/interpretation/tmp-GIF-th0.0.txt"));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] attribs = line.split("\t");

            InterpretationAnnotation annotation = new InterpretationAnnotation();
            annotation.setId(Integer.parseInt(attribs[1]));

            String queryText = query.getText();

            for (int i = 2; i < attribs.length; i++) {
                for (EntityAnnotation entityAnnotation : entities) {
                    if (entityAnnotation.getUrl().equals(attribs[i])) {
//                        queryText = queryText.substring(0, entityAnnotation.getBegin()) + entityAnnotation.getUrl() + queryText.substring(entityAnnotation.getEnd());
                        queryText = queryText.replace(entityAnnotation.getMention(), entityAnnotation.getUrl());
                        break;
                    }
                }
            }

            List<String> interpretation = new LinkedList<>(Arrays.asList(queryText.split(" ")));

            annotation.setInterpretation(interpretation);
            annotations.add(annotation);
        }

        reader.close();
        System.out.println(annotations);
        return annotations;
    }

    public static void main(String[] args) {
        CorpusStreamReader streamReader = new WebisJsonStreamReader();
        LoggedAnnotator entityAnnotator = new DandelionEntityExtractor();
        HasibiGIF hasibiGIF = new HasibiGIF();

        while (streamReader.hasNext()) {
            Query query = streamReader.next();
            EntityAnnotationFileHandle annotationFileHandle = entityAnnotator.annotate(query);

            hasibiGIF.annotate(query, annotationFileHandle);
        }

        entityAnnotator.close();
    }
}
