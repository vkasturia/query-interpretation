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
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;

import java.io.IOException;

public class NordlysEntityRetriever extends NordlysToolkit {
    public NordlysEntityRetriever() {
        super();
    }

    @Override
    protected String[] getCmd(String query) {
        return new String[]{"/home/kipu5728/Programs/anaconda3/envs/nordlys/bin/python",
                "-m",
                "nordlys.services.er",
                "-q",
                query,
                "-c",
                "../../config/nordlys/nordlys-config-er.json"};
    }

    @Override
    protected void parseAnnotations(String jsonString, EntityAnnotationFileHandle annotationFileHandle) {
        try {
            JsonNode node = jsonMapper.readValue(jsonString, JsonNode.class);

            for (JsonNode jsonNode : node.get("results")) {
                EntityAnnotation annotation = new EntityAnnotation();

                String url = jsonNode.get("entity").asText();
                url = url.replaceAll("[<>]", "").replace("dbpedia:", "https://en.wikipedia.org/wiki/");

                annotation.setUrl(url);
                annotation.setBegin(0);
                annotation.setEnd(0);
                annotation.setMention("");
                annotation.setScore(jsonNode.get("score").asDouble());

                annotationFileHandle.writeAnnotation(annotation);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAnnotationTag() {
        return "nordlys-er";
    }
}
