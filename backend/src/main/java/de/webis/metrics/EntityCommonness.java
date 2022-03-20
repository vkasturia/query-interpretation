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

package de.webis.metrics;

import de.webis.parser.WikipediaXMLStreamReader;
import net.amygdalum.stringsearchalgorithms.search.MatchOption;
import net.amygdalum.stringsearchalgorithms.search.StringFinder;
import net.amygdalum.stringsearchalgorithms.search.StringMatch;
import net.amygdalum.stringsearchalgorithms.search.chars.Horspool;
import net.amygdalum.util.io.StringCharProvider;
import org.apache.commons.csv.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityCommonness extends Metric {

    private static final Set<String> excludedNamespaces = new HashSet<>(
            Arrays.asList(
                    "User", "Wikipedia", "WP", "Project", "mw", "File", "MediaWiki", "Template", "Help", "Category",
                    "Portal", "Draft", "Image", "wikt", "TimedText", "Module", "wiktionary", "Wikisource"
            ));

    private static final Pattern anchorPattern = Pattern.compile("\\[\\[.+?\\]\\]");

    public EntityCommonness() {
//        super("/Users/vkasturia/Documents/code/wstud-query-entity-linking/data/persistent/entity-commonness");
        super("./data/persistent/entity-commonness");
    }

    @Override
    protected void calculate() {
        File file = new File("./data/persistent/wiki-anchor-links.csv");
        if (!file.exists()) {
            collectAnchors(file);
        }

        Map<String, Long> mentionOccurences = new HashMap<>();
        Map<String, Long> mentionDestinationOccurences = new HashMap<>();

        try {
            CSVParser parser = new CSVParser(new BufferedReader(new FileReader(file)),
                    CSVFormat.DEFAULT
                            .withQuoteMode(QuoteMode.ALL)
                            .withAllowMissingColumnNames(true)
                            .withSkipHeaderRecord(true));

            for (CSVRecord record : parser) {
                if (mentionOccurences.size() % 10000 == 0) {
                    System.out.print("\rProcessed anchors: " + mentionOccurences.size());
                }

                if (mentionOccurences.size() % 100000 == 0) {
                    flush(mentionDestinationOccurences);
                }

                String mention = record.get(0).trim().toLowerCase();
                String entity = record.get(1).trim().toLowerCase();
                Long value = Long.parseLong(record.get(2));

                mentionOccurences.putIfAbsent(mention, 0L);
                mentionOccurences.put(mention,
                        mentionOccurences.get(mention) + value);

                String key = mention + "->" + entity;
                mentionDestinationOccurences.putIfAbsent(key, 0L);
                mentionDestinationOccurences.put(key, mentionDestinationOccurences.get(key) + value);
            }
            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
        }

        flush(mentionDestinationOccurences);
        storage.forEachKey(s -> {
            try {
                String anchorText = s.split("->")[0];

                if (mentionOccurences.containsKey(anchorText)) {
                    storage.put(s, storage.get(s) / Double.valueOf(mentionOccurences.get(anchorText)));
                } else {
                    System.err.println("[ERROR: ] + Skipping " + s);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("[ERROR ] + Split by \"->\" failed.");
            }

        });

        storage.flush();
    }

    private void flush(Map<String, Long> data) {
        for (Map.Entry<String, Long> anchorDes : data.entrySet()) {
            Double existingValue = storage.get(anchorDes.getKey());

            if (existingValue == null) {
                existingValue = 0.0;
            }

            storage.put(anchorDes.getKey(), Double.valueOf(anchorDes.getValue()) + existingValue);
        }

        storage.flush();
        data.clear();
    }

    private void collectAnchors(File outFile) {
        WikipediaXMLStreamReader wikiDumpReader = new WikipediaXMLStreamReader();
        int numDocuments = 0;
        CSVPrinter csvPrinter = null;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
            csvPrinter = new CSVPrinter(writer,
                    CSVFormat.DEFAULT
                            .withSkipHeaderRecord(true)
                            .withAllowMissingColumnNames(true).withQuoteMode(QuoteMode.ALL));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Map<String, String> mentionsArticle = new HashMap<>();
        Map<String, Long> mentionEntityOccurences = new HashMap<>();

        while (wikiDumpReader.hasNext()) {
            if (numDocuments % 100 == 0) {
                System.out.print("\rProcessed documents: " + numDocuments);
            }

            mentionsArticle.clear();
            mentionEntityOccurences.clear();

            String content = wikiDumpReader.nextRawContent();

            if (content != null) {
                Matcher matcher = anchorPattern.matcher(content);
                while (matcher.find()) {
                    String linkMarkdown = matcher.group();
                    String[] linkComp = linkMarkdown.substring(2, linkMarkdown.length() - 2).split("[|]");

                    if (linkComp.length == 0) {
                        continue;
                    }

                    if (linkComp[0].contains(":")) {
                        if (linkComp[0].startsWith(":")) {
                            linkComp[0] = linkComp[0].substring(1);
                        }

                        String[] entityComp = linkComp[0].split(":");
                        String namespace = entityComp.length > 0 ? entityComp[0] : "";
                        if (excludedNamespaces.contains(namespace)) {
                            continue;
                        }
                    }

                    String mention = linkComp[linkComp.length - 1];
                    String entity = linkComp[0];

                    if (!mentionsArticle.containsKey(mention)) {
                        mentionsArticle.put(mention, entity);
                    } else {
                        if (mentionsArticle.get(mention) != null)
                            if (!mentionsArticle.get(mention).toLowerCase().equals(entity.toLowerCase())) {
                                mentionsArticle.put(mention, null);
                            }
                    }

                    mentionEntityOccurences.putIfAbsent(mention + "->" + entity, 0L);
                    mentionEntityOccurences.put(mention + "->" + entity, mentionEntityOccurences.get(mention + "->" + entity) + 1L);
                }

                content = matcher.replaceAll(" ");
                StringCharProvider charProvider = new StringCharProvider(content, 0);

                for (Map.Entry<String, String> mentionEntry : mentionsArticle.entrySet()) {
                    String mention = mentionEntry.getKey()/*.split("->")[0]*/;

                    if (mentionEntry.getValue() != null && !mention.isEmpty()) {
                        Horspool stringSearch = new Horspool(mention);
                        StringFinder finder = stringSearch.createFinder(charProvider, MatchOption.NON_EMPTY);

                        String key = mentionEntry.getKey() + "->" + mentionEntry.getValue();
                        List<StringMatch> results = finder.findAll();
                        mentionEntityOccurences.put(key, mentionEntityOccurences.get(key) + results.size());
                    }
                }

                for (Map.Entry<String, Long> mentionOccurences : mentionEntityOccurences.entrySet()) {
                    String[] mentionComp = mentionOccurences.getKey().split("->");
                    try {
                        csvPrinter.printRecord(mentionComp[0], mentionComp[1], mentionOccurences.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ArrayIndexOutOfBoundsException e) {
                    }
                }


                numDocuments++;
            }
        }

        try {
            csvPrinter.flush();
            csvPrinter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Metric commonness = new EntityCommonness();

        System.out.println(commonness.get("scott sturgis", "converter"));

        commonness.close();
    }
}
