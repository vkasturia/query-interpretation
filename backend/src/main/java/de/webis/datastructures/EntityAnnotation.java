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

import org.apache.commons.io.FilenameUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class EntityAnnotation implements Annotation {
    private int begin;
    private int end;
    private String mention;

    private List<String> url;

    private double score;

    public EntityAnnotation() {
        url = new ArrayList<>();
    }

    public EntityAnnotation(int begin, int end, String mention, String url) {
        this.begin = begin;
        this.end = end;
        this.mention = mention;
        this.url = new ArrayList<>();
        this.url.add(url);

    }

    public EntityAnnotation(int begin, int end, String mention, String url, double score) {
        this.begin = begin;
        this.end = end;
        this.mention = mention;
        this.url = new ArrayList<>();
        this.url.add(url);
        this.score = score;
    }

    @Override
    public String toString() {
        return mention + " -> " + url + " | " + score;
    }

    @Override
    public int hashCode() {
        if (!hasUrl()) {
            return mention.hashCode();
        }

        try {
            URL urlObj = new URL(url.get(0));
            String encodedBaseName = URLDecoder.decode(
                    FilenameUtils.getBaseName(urlObj.getPath()),
                    "utf-8");

            return encodedBaseName.hashCode();
        } catch (MalformedURLException | UnsupportedEncodingException | IllegalArgumentException e) {
            System.out.println(url.get(0));
            return url.get(0).hashCode();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof EntityAnnotation) {
//            System.out.println(this.url + " == " + ((EntityAnnotation) other).url + " = " + (this.hashCode() == other.hashCode()));
            return this.hashCode() == other.hashCode();
//            if (!hasUrl()){
//                return mention.equals(((EntityAnnotation) other).mention);
//            }
//
//            if (!((EntityAnnotation) other).hasUrl()){
//                return ((EntityAnnotation) other).mention.equals(mention);
//            }
//
//            String thisString;
//            String otherString;
//
//            try {
//                URL thisUrl = new URL(url.get(0));
//
//                thisString = FilenameUtils.getBaseName(thisUrl.getPath());
//            } catch (MalformedURLException e) {
//                thisString = url.get(0);
//            }
//
//            try {
//                URL otherUrl = new URL(((EntityAnnotation) other).url.get(0));
//
//                otherString = FilenameUtils.getBaseName(otherUrl.getPath());
//            } catch (MalformedURLException e) {
//                otherString = ((EntityAnnotation) other).url.get(0);
//            }
//
//            return otherString.equals(thisString);
        }

        return false;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public String getMention() {
        return mention;
    }

    public String getUrl() {
        if (!hasUrl()) {
            return null;
        }

        return url.get(0);
    }

    @Override
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setMention(String mention) {
        this.mention = mention;
    }

    public void setUrl(String url) {
        if (this.url.isEmpty()) {
            this.url.add(url);
        } else {
            this.url.set(0, url);
        }
    }

    public boolean hasUrl() {
        if (this.url.isEmpty()) {
            return false;
        }

        return url.get(0) != null;
    }

    public static void main(String[] args) {
        EntityAnnotation entityEncoded = new EntityAnnotation();
        entityEncoded.setUrl("https://en.wikipedia.org/wiki/Fritz_M%C3%B6ller");


        EntityAnnotation entityDecoded = new EntityAnnotation();
        entityDecoded.setUrl("https://en.wikipedia.org/wiki/Fritz_Möller");

        System.out.println(entityEncoded.equals(entityDecoded));
    }
}
