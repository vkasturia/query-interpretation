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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.AmazonComprehendClientBuilder;
import com.amazonaws.services.comprehend.model.DetectEntitiesRequest;
import com.amazonaws.services.comprehend.model.DetectEntitiesResult;
import com.amazonaws.services.comprehend.model.Entity;
import de.webis.annotator.EntityAnnotator;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.Query;

import java.io.IOException;

public class AmazonComprohendEntityDetector implements EntityAnnotator {
    private static AmazonComprehend comprehendClient;

    public AmazonComprohendEntityDetector() {
        //Insert Access Key ID
        System.setProperty("aws.accessKeyId", "");
        //Insert Secret Key
        System.setProperty("aws.secretKey", "");
        AWSCredentialsProvider awsCredentials = DefaultAWSCredentialsProviderChain.getInstance();

        comprehendClient = AmazonComprehendClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion("eu-central-1")
                .build();

//        Set<String> artifactoryLoggers = new HashSet<>(Arrays.asList("org.apache.http", "groovyx.net.http", "com.amazonaws"));
//        for(String log:artifactoryLoggers) {
//            ch.qos.logback.classic.Logger artLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(log);
//            artLogger.setLevel(ch.qos.logback.classic.Level.INFO);
//            artLogger.setAdditive(false);
//        }
    }

    @Override
    public EntityAnnotationFileHandle annotate(Query query) {
        EntityAnnotationFileHandle fileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());

        DetectEntitiesRequest detectEntitiesRequest = new DetectEntitiesRequest().withText(query.getText()).withLanguageCode("en");
        DetectEntitiesResult detectEntitiesResult = comprehendClient.detectEntities(detectEntitiesRequest);

        EntityAnnotation annotation = new EntityAnnotation();
        for (Entity entity : detectEntitiesResult.getEntities()) {
            annotation.setBegin(entity.getBeginOffset());
            annotation.setEnd(entity.getEndOffset());
            annotation.setMention(entity.getText());
            annotation.setScore(entity.getScore());

            try {
                fileHandle.writeAnnotation(annotation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fileHandle.flush();

        return fileHandle;
    }

    @Override
    public String getAnnotationTag() {
        return "amazon-comprehend";
    }

    public static void main(String[] args) {
        EntityAnnotator annotator = new AmazonComprohendEntityDetector();
        annotator.annotate(new Query("new york times square dance"));
    }
}
