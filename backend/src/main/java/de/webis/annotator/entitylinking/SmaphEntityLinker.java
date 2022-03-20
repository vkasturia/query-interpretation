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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.PersistentStore;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SmaphEntityLinker implements EntityAnnotator {
    private static URIBuilder uriBuilder;
    private static List<NameValuePair> defaultParams;
    private static ObjectMapper jsonMapper;

    private static PersistentStore<String, String> logger;
    private BufferedWriter runtimeLog;

    public SmaphEntityLinker() {
        uriBuilder = new URIBuilder();
        uriBuilder
                .setScheme("http")
                .setHost("localhost")
                .setPort(8080)
                .setPath("/smaph/annotate");

        defaultParams = new ArrayList<>();
        //Insert ID and Key
        defaultParams.add(new BasicNameValuePair("google-cse-id", ""));
        defaultParams.add(new BasicNameValuePair("google-api-key", ""));

        jsonMapper = new ObjectMapper();

        logger = new PersistentStore<>("./data/persistent/logging/smaph3");
        try {
            runtimeLog = new BufferedWriter(new FileWriter("./data/persistent/logging/smaph3/runtimeLog.txt", true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        System.out.println("Annotate \"" + query + "\" ...");
        EntityAnnotationFileHandle annotationFileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());

        if (logger.contains(query.getText())) {
            try {
                extractAnnotations(query, logger.get(query.getText()), annotationFileHandle);
            } catch (IOException e) {
                e.printStackTrace();
            }

            annotationFileHandle.flush();
            return annotationFileHandle;
        }

        uriBuilder.addParameters(defaultParams);
        uriBuilder.addParameter("q", query.getText());

        URL url = null;

        try {
            url = uriBuilder.build().toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
        }

        if (url != null) {
            try {
                long start = System.currentTimeMillis();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                connection.setDoOutput(true);


                int status = connection.getResponseCode();

                if (status == 200) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    logger.put(query.getText(), content.toString());

                    extractAnnotations(query, content.toString(), annotationFileHandle);
                    runtimeLog.write(String.valueOf(System.currentTimeMillis() - start));
                    runtimeLog.newLine();
                } else {
                    System.err.println(connection.getResponseCode() + ": " + connection.getResponseMessage());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        uriBuilder.clearParameters();
        annotationFileHandle.flush();

        return annotationFileHandle;
    }

    private void extractAnnotations(Query query, String json, EntityAnnotationFileHandle annotationFileHandle) throws IOException {
        JsonNode node = jsonMapper.readValue(json, JsonNode.class);
        Iterator<JsonNode> annotationIter = node.get("annotations").iterator();

        EntityAnnotation annotation = new EntityAnnotation();

        while (annotationIter.hasNext()) {
            JsonNode annoNode = annotationIter.next();
            annotation.setBegin(annoNode.get("begin").asInt());
            annotation.setEnd(annoNode.get("end").asInt());
            annotation.setMention(query.getText().substring(annotation.getBegin(), annotation.getEnd()));
            annotation.setUrl(URLDecoder.decode(annoNode.get("url").asText(), "utf-8").replace(" ", "_"));
            annotation.setScore(annoNode.get("score").asDouble());

            annotationFileHandle.writeAnnotation(annotation);
        }
    }

    public void close() {
        try {
            runtimeLog.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.close();
    }

    @Override
    public String getAnnotationTag() {
        return "smaph-3-greedy";
    }

    public static void main(String[] args) {
        SmaphEntityLinker entityLinker = new SmaphEntityLinker();
        try {
            System.out.println(entityLinker.annotate(new Query("wikipedia")).loadAnnotations());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
