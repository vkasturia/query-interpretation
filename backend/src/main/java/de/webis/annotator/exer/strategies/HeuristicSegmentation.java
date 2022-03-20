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

package de.webis.annotator.exer.strategies;

import de.webis.datastructures.Query;
import de.webis.query.segmentation.application.QuerySegmentation;
import de.webis.query.segmentation.core.Segmentation;
import de.webis.query.segmentation.strategies.SegmentationStrategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HeuristicSegmentation implements ExerStrategy {
    private QuerySegmentation querySegmentation;
    private final double scoreThreshold;

    public HeuristicSegmentation(SegmentationStrategy segmentationStrategy, double scoreThreshold) {
        querySegmentation = new QuerySegmentation(segmentationStrategy);
        this.scoreThreshold = scoreThreshold;
    }

    @Override
    public Set<String> apply(Query query) {
        Set<String> segments = new HashSet<>();

        Object[] segmentations = querySegmentation.performSegmentationWithFilteration(
                new de.webis.query.segmentation.core.Query(query.getText()), scoreThreshold);

        Map<Segmentation, Integer> segmentationScores = querySegmentation.performSegmentationWithHeuristic(segmentations);

        for (Segmentation segmentation : segmentationScores.keySet()) {
            segments.addAll(segmentation.getSegments());
        }

        return segments;
    }
}
