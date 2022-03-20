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

package de.webis.annotator.segmentation;

import de.webis.datastructures.Query;
import de.webis.query.segmentation.application.QuerySegmentation;
import de.webis.query.segmentation.core.Segmentation;
import de.webis.query.segmentation.strategies.SegmentationStrategy;
import de.webis.query.segmentation.strategies.StrategyWikiBased;
import de.webis.query.segmentation.strategies.StrategyWtBaseline;

import java.util.Map;

public class WebisQuerySegmentation {
    private SegmentationStrategy segmentationStrategy;
    private SegmentationStrategy fallbackStrategy;
    private final double scoreDiffThreshold;

    public WebisQuerySegmentation() {
        segmentationStrategy = new StrategyWtBaseline();
        fallbackStrategy = new StrategyWikiBased();
        scoreDiffThreshold = 0.7;
    }

    public WebisQuerySegmentation(SegmentationStrategy segmentationStrategy) {
        this.segmentationStrategy = segmentationStrategy;
        fallbackStrategy = new StrategyWikiBased();
        scoreDiffThreshold = 0.7;
    }

    public WebisQuerySegmentation(SegmentationStrategy segmentationStrategy, double scoreDiffThreshold) {
        this.segmentationStrategy = segmentationStrategy;
        fallbackStrategy = new StrategyWikiBased();
        this.scoreDiffThreshold = scoreDiffThreshold;
    }

    public Map<Segmentation, Integer> getSegmentations(Query query) {
        Map<Segmentation, Integer> segmentationScores;

        QuerySegmentation querySegmentation = new QuerySegmentation(segmentationStrategy);
        Object[] segmentations = querySegmentation.performSegmentationWithFilteration(
                new de.webis.query.segmentation.core.Query(query.getText()),
                scoreDiffThreshold
        );

        segmentationScores = querySegmentation.performSegmentationWithHeuristic(segmentations);
        boolean zeroScores = segmentationScores.entrySet().stream().allMatch(e -> e.getValue() == 0);

        if (zeroScores) {
            segmentationScores.clear();

            querySegmentation = new QuerySegmentation(fallbackStrategy);
            segmentations = querySegmentation.performSegmentationWithFilteration(
                    new de.webis.query.segmentation.core.Query(query.getText()),
                    scoreDiffThreshold
            );

            segmentationScores = querySegmentation.performSegmentationWithHeuristic(segmentations);
        }

        return segmentationScores;
    }

    public static void main(String[] args) {
        WebisQuerySegmentation querySegmentation = new WebisQuerySegmentation(new StrategyWikiBased(), 0.7);
        System.out.println(querySegmentation.getSegmentations(new Query("new york times square dance")));
    }
}
