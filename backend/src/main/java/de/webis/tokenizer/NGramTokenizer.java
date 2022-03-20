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

package de.webis.tokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NGramTokenizer {
    private Analyzer analyzer;

    public NGramTokenizer() {
        Map<String, String> shingleFilterOptions = new HashMap<>();

        shingleFilterOptions.put("maxShingleSize", "8");

        try {
            analyzer = CustomAnalyzer.builder()
                    .withTokenizer(ClassicTokenizerFactory.class)
                    .addTokenFilter("standard")
                    .addTokenFilter(ShingleFilterFactory.class, shingleFilterOptions)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NGramTokenizer(int maxNGramLength, int minNGramLength) {
        Map<String, String> shingleFilterOptions = new HashMap<>();

        shingleFilterOptions.put("maxShingleSize", String.valueOf(Math.max(maxNGramLength, 2)));
        shingleFilterOptions.put("minShingleSize", String.valueOf(Math.max(minNGramLength, 2)));

        if (minNGramLength > 1) {
            shingleFilterOptions.put("outputUnigrams", String.valueOf(false));
        }

        try {
            analyzer = CustomAnalyzer.builder()
                    .withTokenizer(ClassicTokenizerFactory.class)
                    .addTokenFilter("standard")
                    .addTokenFilter(ShingleFilterFactory.class, shingleFilterOptions)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TokenStream getSegments(String text) {
        TokenStream tokenStream = analyzer.tokenStream(null, text);

        try {
            tokenStream.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tokenStream;
    }
}
