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

package de.webis.datastructures.persistent;

import de.webis.datastructures.Query;
import de.webis.utils.StreamSerializer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LuceneIndex {
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private final int hitsPerPage;

    public LuceneIndex(String dir) {
        analyzer = new StandardAnalyzer();
        hitsPerPage = 150;

        try {
            IndexReader reader = DirectoryReader.open(
                    FSDirectory.open(Paths.get(dir))
            );

            searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Set<String>> get(Query query) {
        QueryParser parser = new QueryParser("segment", analyzer);
        Map<String, Set<String>> searchResults = new LinkedHashMap<>();
        try {
            org.apache.lucene.search.Query luceneQuery = parser.parse(query.getText());
            TopDocs results = searcher.search(luceneQuery, hitsPerPage);
            ScoreDoc[] hits = results.scoreDocs;

            for (ScoreDoc scoreDoc : hits) {
                Document doc = searcher.doc(scoreDoc.doc);
                String segment = doc.get("segment");


                HashSet<String> entities = (HashSet<String>)
                        StreamSerializer.deserialize(doc.getBinaryValue("entities").bytes);
                searchResults.put(segment, entities);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        return mapToSegment(query, searchResults);
    }

    private Map<String, Set<String>> mapToSegment(Query query, Map<String, Set<String>> luceneEntities) {
        Map<String, Set<String>> segmentEntityMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : luceneEntities.entrySet()) {
            segmentEntityMap.put(query.getText(), entry.getValue());
        }

        return segmentEntityMap;
    }
}
