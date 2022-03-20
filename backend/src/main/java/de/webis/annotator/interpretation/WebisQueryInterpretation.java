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

package de.webis.annotator.interpretation;

import com.google.common.collect.Sets;
import de.webis.annotator.InterpretationAnnotator;
import de.webis.annotator.exer.WebisExplicitEntityRetriever;
import de.webis.annotator.exer.strategies.AllNGrams;
import de.webis.annotator.segmentation.WebisQuerySegmentation;
import de.webis.datastructures.EntityAnnotation;
import de.webis.datastructures.EntityAnnotationFileHandle;
import de.webis.datastructures.InterpretationAnnotation;
import de.webis.datastructures.Query;
import de.webis.datastructures.persistent.LuceneIndex;
import de.webis.datastructures.persistent.PersistentStore;
import de.webis.metrics.EntityCommonness;
import de.webis.metrics.Metric;
import de.webis.query.segmentation.core.Segmentation;
import de.webis.query.segmentation.strategies.StrategyWtBaseline;
import de.webis.utils.StreamSerializer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class WebisQueryInterpretation implements InterpretationAnnotator {
    private WebisQuerySegmentation querySegmentation;
    private static ExecutorService executorService;
    private static LuceneIndex luceneIndex;

    private Metric commonness;
    private PersistentStore<String, double[]> embeddingsStorage;

    private double alpha, beta, gamma;

    private int interpretationCandidates = 50;
//    private int interpretationCandidates = 10;

    private final int numThreads = 8;

    private final static Pattern WIKI_URL_PATTERN = Pattern.compile("http(s)?://en.wikipedia.org/wiki/");

    public WebisQueryInterpretation() {
        luceneIndex = new LuceneIndex("./data/persistent/wiki_table_lucene_without_redirects_namespaces_concepts");

        commonness = new EntityCommonness();
        embeddingsStorage = new PersistentStore<>("./data/persistent/embeddings/enwiki_500d_db");
        embeddingsStorage.setSerializer(StreamSerializer.class);

        querySegmentation = new WebisQuerySegmentation(new StrategyWtBaseline(), 0.7);

        alpha = 1.0;
        beta = 1.0;
        gamma = 1.0;

//        alpha = 1.0;
//        beta = 0.03;
//        gamma = 1.0;

//        alpha = 0.37;
//        beta = 0.0;
//        gamma = 0.1325;

//        alpha = 0.25;
//        beta = 0.0;
//        gamma = 1.0;
    }

    @Override
    public Set<InterpretationAnnotation> annotate(Query query, EntityAnnotationFileHandle entityAnnotations) {
        long start = System.currentTimeMillis();
        Map<Segmentation, Integer> segmentationScores = querySegmentation.getSegmentations(query);
        List<EntityAnnotation> entities;

        Map<String, Set<EntityAnnotation>> mentionEntityAnnoMap = new HashMap<>();
        Map<String, Set<String>> mentionEntityMap = new HashMap<>();

        try {
            entities = entityAnnotations.loadAnnotations();

            for (Map.Entry<String, Set<String>> entry : luceneIndex.get(query).entrySet()) {
                for (String entity : entry.getValue()) {
                    entities.add(new EntityAnnotation(0, query.getText().length() - 1, query.getText(), entity));
                }
            }

            for (EntityAnnotation entityAnnotation : entities) {
                mentionEntityAnnoMap.putIfAbsent(entityAnnotation.getMention(), new TreeSet<>(
                        Comparator.comparingDouble(EntityAnnotation::getScore).reversed()));

//                if(mentionEntityMap.get(entityAnnotation.getMention()).size() > 10){
//                    continue;
//                }
                String entity = entityAnnotation.getUrl();
                entity = WIKI_URL_PATTERN.matcher(entity).replaceAll("");
                entity = entity.replace("_", " ").toLowerCase();
                entityAnnotation.setScore(commonness.get(entity, entityAnnotation.getMention()));

                if (entityAnnotation.getScore() > 0) {
                    mentionEntityAnnoMap.get(entityAnnotation.getMention()).add(entityAnnotation);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            return new HashSet<>();
        }

        for (Map.Entry<String, Set<EntityAnnotation>> entry : mentionEntityAnnoMap.entrySet()) {
            mentionEntityMap.putIfAbsent(entry.getKey(), new LinkedHashSet<>());
//            int limit = (int) Math.floor(Math.pow(interpretationCandidates, 1.0 / mentionEntityMap.size()));
            int limit = 1;

            for (EntityAnnotation annotation : entry.getValue()) {
                if (mentionEntityMap.get(entry.getKey()).size() < limit) {
                    mentionEntityMap.get(entry.getKey()).add(annotation.getUrl());
                } else {
                    break;
                }
            }

            mentionEntityMap.get(entry.getKey()).add(entry.getKey());
        }

//        mentionEntityMap.putAll(luceneIndex.get(query));

        TreeSet<InterpretationAnnotation> interpretations = new TreeSet<>(
                Comparator.comparingDouble(InterpretationAnnotation::getRelevance).reversed()
        );

//        System.out.println("Segmentations: " + segmentationScores.size());
        for (Map.Entry<Segmentation, Integer> entry : segmentationScores.entrySet()) {
            Segmentation segmentation = entry.getKey();
            List<Set<String>> interpretationCandidates = new LinkedList<>();


            segmentation.getSegments().forEach(s -> interpretationCandidates.add(
                    mentionEntityMap.getOrDefault(s, new HashSet<>(Collections.singletonList(s)))));

            Queue<List<String>> combinations = new ConcurrentLinkedQueue<>(
                    Sets.cartesianProduct(interpretationCandidates));
//            System.out.println("Interpretation candidates: " + combinations.size());
            Set<InterpretationAnnotation> annotations = createAnnotations(combinations, segmentation);

            start = System.currentTimeMillis();
            interpretations.addAll(annotations);
//            System.out.println("Add interpretations: " + (System.currentTimeMillis() - start));
        }

        if (interpretations.isEmpty()) {
            for (Map.Entry<Segmentation, Integer> segmention : segmentationScores.entrySet()) {
                interpretations.add(new InterpretationAnnotation(segmention.getKey().getSegments()));
            }
        }
        System.out.println("Interpretations: " + interpretations.size());

//        Set<InterpretationAnnotation> results = new LinkedHashSet<>();
//        for(InterpretationAnnotation annotation: interpretations){
//            if(results.size() < 20){
//                results.add(annotation);
//            }
//        }
//        return results;

        return interpretations;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private Set<InterpretationAnnotation> createAnnotations(Queue<List<String>> interpretations, Segmentation segmentation) {
        executorService = Executors.newFixedThreadPool(numThreads);
        Set<InterpretationAnnotation> annotations = new CopyOnWriteArraySet<>();
        LongSummaryStatistics commonnessTime = new LongSummaryStatistics();
        LongSummaryStatistics relatednessTime = new LongSummaryStatistics();
        LongSummaryStatistics contextTime = new LongSummaryStatistics();

        Runnable scoringRunnable = () -> {
            while (!interpretations.isEmpty()) {
                List<String> interpretation = interpretations.poll();

                if (interpretation == null) {
                    break;
                }

                InterpretationAnnotation annotation = new InterpretationAnnotation(interpretation);

                long start = System.currentTimeMillis();
                double avgCommonness = getAvgCommonness(annotation, segmentation);
                commonnessTime.accept(System.currentTimeMillis() - start);
                start = System.currentTimeMillis();
                double avgRelatedness = getAvgRelatedness(annotation);
                relatednessTime.accept(System.currentTimeMillis() - start);
                start = System.currentTimeMillis();
                double avgContextScore = getAvgContextScore(annotation);
                contextTime.accept(System.currentTimeMillis() - start);

                annotation.setRelevance(
                        alpha * avgCommonness + beta * avgRelatedness + gamma * avgContextScore);

                if (annotation.getRelevance() > 0.0) {
                    annotations.add(annotation);
                }
            }

        };

        for (int i = 0; i < numThreads; i++) {
            executorService.execute(scoringRunnable);
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(1L, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            executorService.shutdown();
            e.printStackTrace();
        }

//        System.out.println("Avg commonness time: " + commonnessTime.getAverage());
//        System.out.println("Avg relatedness time: " + relatednessTime.getAverage());
//        System.out.println("Avg context time: " + contextTime.getAverage());

        return annotations;
    }

    private double getAvgCommonness(InterpretationAnnotation interpretation, Segmentation segmentation) {
        List<String> containedEntities = interpretation.getContainedEntities();
        List<String> segments = segmentation.getSegments();
        DoubleSummaryStatistics commonnessStats = new DoubleSummaryStatistics();

        for (String entity : containedEntities) {
            int indexOfEntity = interpretation.getInterpretation().indexOf(entity);

            entity = WIKI_URL_PATTERN.matcher(entity).replaceAll("");
            entity = entity.replace("_", " ").toLowerCase();

            commonnessStats.accept(commonness.get(entity, segments.get(indexOfEntity)));
        }

        return commonnessStats.getAverage();
    }

    private double getAvgRelatedness(InterpretationAnnotation interpretation) {
        List<String> containedEntities = interpretation.getContainedEntities();
        DoubleSummaryStatistics relatednessStats = new DoubleSummaryStatistics();

        for (int i = 0; i < containedEntities.size(); i++) {
            String firstEntity = containedEntities.get(i);
            firstEntity = WIKI_URL_PATTERN.matcher(firstEntity).replaceAll("ENTITY/");

            double[] firstEntityVector = embeddingsStorage.getOrDefault(firstEntity, new double[500]);

            for (int j = 0; j < containedEntities.size(); j++) {
                if (i != j) {
                    String secondEntity = containedEntities.get(j);
                    secondEntity = WIKI_URL_PATTERN.matcher(secondEntity).replaceAll("ENTITY/");

                    double[] secondEntityVector = embeddingsStorage.getOrDefault(secondEntity, new double[500]);

                    relatednessStats.accept(Metric.cosineSimilarity(firstEntityVector, secondEntityVector));
                }
            }
        }

        return relatednessStats.getAverage();
    }

    private double getAvgContextScore(InterpretationAnnotation interpretation) {
        List<String> entities = interpretation.getContainedEntities();
        DoubleSummaryStatistics contextScoreStats = new DoubleSummaryStatistics();

        if (entities.isEmpty()) {
            return 0.0;
        }

        Set<String> contextWords = interpretation.getContextWords();

        if (contextWords.isEmpty()) {
            return 0.0;
        }

        for (String entity : entities) {
            for (String contextWord : contextWords) {
                entity = WIKI_URL_PATTERN.matcher(entity).replaceAll("ENTITY/");

                double[] entityVector = embeddingsStorage.getOrDefault(entity, new double[500]);
                double[] contextVector = embeddingsStorage.getOrDefault(contextWord, new double[500]);

                contextScoreStats.accept(Metric.cosineSimilarity(entityVector, contextVector));
            }
        }

        return contextScoreStats.getAverage();
    }

    public void close() {
        commonness.close();
        embeddingsStorage.close();
    }

    public void setParameter(double alpha, double beta, double gamma) {
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    public void setInterpretationCandidates(int interpretationCandidates) {
        this.interpretationCandidates = interpretationCandidates;
    }

    public static void main(String[] args) {
        WebisQueryInterpretation webisQueryInterpretation = new WebisQueryInterpretation();

        Query query = new Query("new york times square dance");
        Set<InterpretationAnnotation> interpretations = webisQueryInterpretation.annotate(query,
                new WebisExplicitEntityRetriever(new AllNGrams()).annotate(query));

        System.out.println(interpretations);
        webisQueryInterpretation.shutdown();
        webisQueryInterpretation.close();
    }
}
