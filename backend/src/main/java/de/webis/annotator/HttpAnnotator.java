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
import de.webis.datastructures.persistent.PersistentStore;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class HttpAnnotator implements LoggedAnnotator {
    protected URIBuilder uriBuilder;

    protected List<NameValuePair> defaultParams;

    protected PersistentStore<String, String> logger;


    public HttpAnnotator(String scheme, String host, int port, String path) {
        uriBuilder = new URIBuilder();

        uriBuilder
                .setScheme(scheme)
                .setHost(host)
                .setPort(port)
                .setPath(path);

        defaultParams = new ArrayList<>();

//        disableExtLogging();
    }

    public HttpAnnotator(String scheme, String host, int port, String path, String logPath) {
        uriBuilder = new URIBuilder();

        uriBuilder
                .setScheme(scheme)
                .setHost(host)
                .setPort(port)
                .setPath(path);

        defaultParams = new ArrayList<>();
        logger = new PersistentStore<>(logPath);

//        disableExtLogging();
    }

    @Override
    public void close() {
        if (logger != null) {
            logger.close();
        }
    }

    protected abstract void extractAnnotation(Query query, String response, EntityAnnotationFileHandle annotationFileHandle)
            throws IOException;

//    private void disableExtLogging(){
//        Set<String> artifactoryLoggers = new HashSet<>(Arrays.asList("org.apache.http", "groovyx.net.http"));
//        for(String log:artifactoryLoggers) {
//            ch.qos.logback.classic.Logger artLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(log);
//            artLogger.setLevel(ch.qos.logback.classic.Level.INFO);
//            artLogger.setAdditive(false);
//        }
//    }

    protected abstract void prepareRequest(Query query);
}
