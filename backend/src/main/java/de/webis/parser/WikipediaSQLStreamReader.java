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

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikipediaSQLStreamReader {
    private BufferedReader sqlFileReader;

    private String currentStatement, nextStatement;

    private static final Pattern INSERT_PATTERN = Pattern.compile("INSERT\\s+INTO\\s+(.)*?");
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+VALUES\\s+");
    //    private static final Pattern VALUE_SEP_PATTERN = Pattern.compile("(?<=\\)),(?=\\()");
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("(?<qouted>['].*?((?<!\\\\)(?:\\\\{2})*)['])|(?<literal>[^,();]+)");

    private static final Pattern QOUTE_PATTERN = Pattern.compile("(^['\"])|(['\"]$)");

    public WikipediaSQLStreamReader(String sqlFile) {
        try {
            sqlFileReader = new BufferedReader(new FileReader(sqlFile));

            String line;
            while ((line = sqlFileReader.readLine()) != null) {
                if (INSERT_PATTERN.matcher(line).matches()) {
                    nextStatement = line;
                    break;
                }
            }
//            nextStatement = sqlFileReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasNext() {
        try {
            String line;
            while ((line = sqlFileReader.readLine()) != null) {
                if (INSERT_PATTERN.matcher(line).matches()) {
                    nextStatement = line;
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }

        return false;
    }

    public List<List<String>> next() {
        List<List<String>> data = new LinkedList<>();

        if (nextStatement == null) {
            try {
                throw new IOException("Unexpected end!");
            } catch (IOException e) {
                e.printStackTrace();
                return data;
            }
        }

        currentStatement = nextStatement;
        String[] components = SPLIT_PATTERN.split(currentStatement);
        if (components.length == 2) {
            String valueString = components[1];

            for (String valueSet : getValueSets(valueString)) {
                List<String> values = new LinkedList<>();

                Matcher valueMatcher = VALUE_PATTERN.matcher(valueSet);

                while (valueMatcher.find()) {
                    String value = StringEscapeUtils.unescapeJava(valueMatcher.group(0));

                    values.add(QOUTE_PATTERN.matcher(value).replaceAll(""));
                }

                data.add(values);
            }
        }


        return data;
    }

    public void close() {
        try {
            sqlFileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> getValueSets(String valueString) {
        List<String> valueSets = new LinkedList<>();
        StringBuilder builder = new StringBuilder();
        int balance = 0;
        boolean inside = false;
        boolean qouted = false;
        boolean escaped = false;

        for (char character : valueString.toCharArray()) {
            builder.append(character);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (!qouted) {
                if (character == '(') {
                    balance++;
                    inside = true;
                } else if (character == ')') {
                    balance--;
                }
            }

            if (character == '\'') {
                qouted = !qouted;
            } else if (character == '\\') {
                escaped = true;
            }

            if (balance == 0) {
                if (inside) {
                    valueSets.add(builder.toString());
                    inside = false;
                }

                builder.delete(0, builder.length());
            }
        }

        return valueSets;
    }

    public static void main(String[] args) {
        WikipediaSQLStreamReader dbStreamReader =
                new WikipediaSQLStreamReader("/media/storage1/corpora/corpus-wikipedia/enwiki-20200220-page_props.sql");

        int statementCounter = 0;
        while (dbStreamReader.hasNext()) {
            List<List<String>> data = dbStreamReader.next();

            System.out.print("\rStatements processed: " + statementCounter);

            statementCounter++;
        }

        System.out.print("\rStatements processed: " + statementCounter);
    }
}
