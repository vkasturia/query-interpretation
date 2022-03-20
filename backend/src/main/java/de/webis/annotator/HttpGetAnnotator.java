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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public abstract class HttpGetAnnotator extends HttpAnnotator {


    public HttpGetAnnotator(String scheme, String host, int port, String path) {
        super(scheme, host, port, path);
    }

    public HttpGetAnnotator(String scheme, String host, int port, String path, String logPath) {
        super(scheme, host, port, path, logPath);
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle annotationFileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());

        if (logger != null)
            if (logger.contains(query.getText())) {
                try {
                    extractAnnotation(query, logger.get(query.getText()), annotationFileHandle);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                annotationFileHandle.flush();

                return annotationFileHandle;
            }

        URL url = null;

        prepareRequest(query);
        uriBuilder.addParameters(defaultParams);
        try {
            url = uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
        }

        if (url != null) {
            try {
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

                    extractAnnotation(query, content.toString(), annotationFileHandle);

                    if (logger != null)
                        logger.put(query.getText(), content.toString());

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        annotationFileHandle.flush();
        uriBuilder.clearParameters();

        return annotationFileHandle;
    }


}
