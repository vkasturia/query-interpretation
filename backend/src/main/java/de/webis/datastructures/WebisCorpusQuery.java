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

import java.util.ArrayList;
import java.util.List;

public class WebisCorpusQuery extends Query {

    private List<EntityAnnotation> explicitEntities;
    private List<EntityAnnotation> implicitEntities;

    private List<InterpretationAnnotation> interpretations;

    public WebisCorpusQuery() {
        explicitEntities = new ArrayList<>();
        implicitEntities = new ArrayList<>();

        interpretations = new ArrayList<>();
    }

    public List<EntityAnnotation> getExplicitEntities() {
        return explicitEntities;
    }

    public void setExplicitEntities(List<EntityAnnotation> explicitEntities) {
        this.explicitEntities = explicitEntities;
    }

    public List<EntityAnnotation> getImplicitEntities() {
        return implicitEntities;
    }

    public void setImplicitEntities(List<EntityAnnotation> implicitEntities) {
        this.implicitEntities = implicitEntities;
    }

    public List<InterpretationAnnotation> getInterpretations() {
        return interpretations;
    }

    public void setInterpretations(List<InterpretationAnnotation> interpretations) {
        this.interpretations = interpretations;
    }

    public void addExplicitEntity(EntityAnnotation annotation) {
        explicitEntities.add(annotation);
    }

    public void addImplicitEntity(EntityAnnotation annotation) {
        implicitEntities.add(annotation);
    }

    public void addInterpretation(InterpretationAnnotation annotation) {
        interpretations.add(annotation);
    }

    @Override
    public void addAnnotation(EntityAnnotation annotation) {
        throw new UnsupportedOperationException();
    }
}
