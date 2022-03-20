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

package de.webis.annotator;

import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

public abstract class HttpPostAnnotator extends HttpAnnotator {
    protected EntityBuilder entityBuilder;

    public HttpPostAnnotator(String scheme, String host, int port, String path) {
        super(scheme, host, port, path);
    }

    public HttpPostAnnotator(String scheme, String host, int port, String path, String logPath) {
        super(scheme, host, port, path, logPath);
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle entityAnnotationFileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());
        HttpClient client = HttpClients.createDefault();
        uriBuilder.addParameters(defaultParams);

        HttpPost request = null;
        try {
            request = new HttpPost(uriBuilder.build());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (request != null) {
            request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
            request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());

            entityBuilder = EntityBuilder.create();
            entityBuilder.setContentType(ContentType.APPLICATION_JSON);
            prepareRequest(query);

            request.setEntity(entityBuilder.build());

            HttpResponse response;
            try {
                response = client.execute(request);

                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                    StringBuilder responseString = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        responseString.append(line).append("\n");
                    }

                    reader.close();
                    extractAnnotation(query, responseString.toString(), entityAnnotationFileHandle);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        entityAnnotationFileHandle.flush();
        uriBuilder.clearParameters();

        return entityAnnotationFileHandle;
    }
}
