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

package de.webis.datastructures;

import de.webis.evaluation.Evaluator;

import java.util.*;
import java.util.regex.Pattern;

public class InterpretationAnnotation implements Annotation {
    private int id;
    private List<String> interpretation;
    private double relevance;

    private List<String> containedEntities;
    private Set<String> contextWords;

    private static Pattern WIKI_URL_PATTERN = Pattern.compile("^http(s)?://en.wikipedia.org/wiki/(.)*");

    public InterpretationAnnotation() {
        containedEntities = new ArrayList<>();
        contextWords = new HashSet<>();
    }

    public InterpretationAnnotation(List<String> interpretation) {
        containedEntities = new ArrayList<>();
        contextWords = new HashSet<>();
        setInterpretation(interpretation);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<String> getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(List<String> interpretation) {
        this.interpretation = interpretation;

        for (String part : interpretation) {
            if (WIKI_URL_PATTERN.matcher(part).matches()) {
                containedEntities.add(part);
            } else {
                contextWords.addAll(Arrays.asList(part.split("\\s")));
            }
        }
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }

    public List<String> getContainedEntities() {
        return containedEntities;
    }

    public Set<String> getContextWords() {
        return contextWords;
//        Set<String> uniqueParts = new HashSet<>(interpretation);
//        Set<String> uniqueEntities = new HashSet<>(containedEntities);
//
//        uniqueParts.removeAll(uniqueEntities);
//
//        return new HashSet<String>(
//                Arrays.asList(
//                        String.join(" ", uniqueParts).split(" "))
//        );
    }

    @Override
    public double getScore() {
        return getRelevance();
    }

    @Override
    public int hashCode() {

        if (Evaluator.MATCHTYPE == MatchType.PM) {
            return String.join("", interpretation)
                    .replaceAll("http(s)?://en.wikipedia.org/wiki/", "")
                    .replaceAll("[\\s]+", "").hashCode();
        } else {
            return String.join("|", interpretation)
                    .replaceAll("http(s)?://en.wikipedia.org/wiki/", "")
                    .trim().hashCode();
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InterpretationAnnotation)) {
            return false;
        }

        return this.hashCode() == obj.hashCode();
    }

    @Override
    public String toString() {
        return String.join(" ", interpretation) + " | " + String.valueOf(relevance);
    }
}
