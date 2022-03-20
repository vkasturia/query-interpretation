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

package de.webis.parser;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

public class WikipediaXMLStreamReader {

    private List<File> wikiDumpFiles;
    private XMLStreamReader eventReader;

    private StringBuilder currentContent;


    public WikipediaXMLStreamReader() {
        wikiDumpFiles = new LinkedList<>();


        collectFiles(new File("/media/storage1/corpora/corpus-wikipedia/pages-articles-20190701"));
    }

    private void collectFiles(File dir) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.getName().matches("^enwiki-20190701-pages-articles(.)*")) {
                    wikiDumpFiles.add(file);
                }
            }
        }
    }

    private void nextFile() {
        if (!wikiDumpFiles.isEmpty()) {
            try {
                System.out.println("Process file " + wikiDumpFiles.get(0).toString());
                eventReader = XMLInputFactory.newInstance().createXMLStreamReader(new FileReader(wikiDumpFiles.get(0)));
            } catch (XMLStreamException | FileNotFoundException e) {
                e.printStackTrace();
            }

            wikiDumpFiles.remove(0);

        } else {
            eventReader = null;
        }
    }

    public String nextRawContent() {
        if (eventReader == null) {
            nextFile();
        }

        try {
            while (eventReader.hasNext()) {
                eventReader.next();

                if (eventReader.isStartElement()) {
                    if (eventReader.getLocalName().equals("text")) {
                        return eventReader.getElementText();
                    }
                }

                if (eventReader.getEventType() == XMLStreamConstants.END_DOCUMENT) {
                    nextFile();

                    if (eventReader == null) {
                        return null;
                    }
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean hasNext() {
        if (eventReader != null) {
            try {
                return !wikiDumpFiles.isEmpty() || eventReader.hasNext();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        } else {
            return !wikiDumpFiles.isEmpty();
        }

        return false;
    }

    public static void main(String[] args) {
        WikipediaXMLStreamReader reader = new WikipediaXMLStreamReader();

        while (reader.hasNext()) {
            System.out.println(reader.nextRawContent());
            System.out.println("***************************************");
        }
    }
}
