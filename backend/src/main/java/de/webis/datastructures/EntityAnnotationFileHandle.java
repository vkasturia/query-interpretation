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

package de.webis.datastructures;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class EntityAnnotationFileHandle {
    private final String filePath;

    private BufferedWriter annotationOut;

    private static ObjectMapper jsonMapper = new ObjectMapper();

    private double runtime;

    public EntityAnnotationFileHandle(String query, String annotationTag) {
        this.filePath = "./data/annotations/" + annotationTag + "/" + query.replaceAll("[\\s]+", "-") + ".ldjson";

        File file = new File(filePath);

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try {
            annotationOut = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        runtime = -1;
    }

    public void writeAnnotation(EntityAnnotation annotation) throws IOException {
        if (annotationOut != null) {
            annotationOut.write(jsonMapper.writeValueAsString(annotation) + "\n");
        }
    }

    public List<EntityAnnotation> loadAnnotations() throws IOException {
        List<EntityAnnotation> annotations = new ArrayList<>();


        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = reader.readLine()) != null) {
            annotations.add(jsonMapper.readValue(line, EntityAnnotation.class));
        }

        reader.close();

        return annotations;
    }

    public void prepareAnnotations(int maxAnnotations) {
        List<EntityAnnotation> annotations = null;
        try {
            annotations = loadAnnotations();
        } catch (IOException e) {
            e.printStackTrace();
        }

        annotations = sortAnnotations(annotations);

        Set<String> urls = new HashSet<>();

        try {
            annotationOut = new BufferedWriter(new FileWriter(filePath));
            int numAnnotations = 0;
            for (EntityAnnotation annotation : annotations) {
                if (!urls.contains(annotation.getUrl())) {
                    writeAnnotation(annotation);
                    numAnnotations++;

//                    if(annotation.getScore() < 0.75){
//                        break;
//                    }

                    urls.add(annotation.getUrl());
                }

                if (numAnnotations == maxAnnotations) {
                    break;
                }
            }

            annotationOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prepareAnnotations() {
        prepareAnnotations(-1);
    }

    public void flush() {
        try {
            annotationOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<EntityAnnotation> sortAnnotations(List<EntityAnnotation> annotations) {
        return annotations.stream()
                .sorted(Comparator.comparingDouble(EntityAnnotation::getScore).reversed())
                .collect(Collectors.toList());
    }

    public double getRuntime() {
        return runtime;
    }

    public void setRuntime(double runtime) {
        this.runtime = runtime;
    }
}
