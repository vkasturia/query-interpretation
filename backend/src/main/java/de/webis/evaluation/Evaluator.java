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

package de.webis.evaluation;

import de.webis.annotator.BaselineAnnotator;
import de.webis.annotator.EntityAnnotator;
import de.webis.annotator.InterpretationAnnotator;
import de.webis.annotator.LoggedAnnotator;
import de.webis.annotator.exer.WebisExplicitEntityRetriever;
import de.webis.annotator.exer.strategies.All1Grams;
import de.webis.annotator.exer.strategies.AllNGrams;
import de.webis.annotator.exer.strategies.HeuristicSegmentation;
import de.webis.annotator.interpretation.WebisQueryInterpretation;
import de.webis.annotator.ner.StanfordNERTagger;
import de.webis.datastructures.*;
import de.webis.parser.CorpusStreamReader;
import de.webis.parser.CorpusType;
import de.webis.parser.WebisJsonStreamReader;
import de.webis.query.segmentation.strategies.StrategyWtBaseline;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

//import de.webis.annotator.entitylinking.FastEntityLinker;

enum Task {EXER, IMER}

public class Evaluator {
    public static MatchType MATCHTYPE = MatchType.PM;

    public static void evaluateER(EntityAnnotator annotator, CorpusStreamReader reader, Task task) {
        EvaluationStatistics statistics = new EvaluationStatistics();

        EntityAnnotationFileHandle notFoundAnnotations = new EntityAnnotationFileHandle("!NOT FOUND!", annotator.getAnnotationTag());

        Set<Annotation> overallRetrievedAnnotations = new LinkedHashSet<>();
        Set<Annotation> overallRelevantAnnotations = new LinkedHashSet<>();

        while (reader.hasNext()) {
            Query query = reader.next();

            long start = System.currentTimeMillis();

            EntityAnnotationFileHandle annotationFileHandle = annotator.annotate(query);
            annotationFileHandle.prepareAnnotations();

            if (annotationFileHandle.getRuntime() > 0) {
                statistics.accept("runtime", annotationFileHandle.getRuntime());
            } else {
                statistics.accept("runtime", (double) (System.currentTimeMillis() - start));
            }

            Set<Annotation> relevantAnnotations = getRelevantEntities(query, task);
            Set<Annotation> retrievedAnnotations;

            try {
                retrievedAnnotations = new LinkedHashSet<>(annotationFileHandle.loadAnnotations());
            } catch (IOException e) {
                retrievedAnnotations = new LinkedHashSet<>();
                e.printStackTrace();
            }

            System.out.println(query.getText());
            if (!retrievedAnnotations.isEmpty()
                    && !((EntityAnnotation) retrievedAnnotations.iterator().next()).hasUrl()) {

                for (Annotation retrievedAnnotation : retrievedAnnotations) {
                    EntityAnnotation retrievedAnno = (EntityAnnotation) retrievedAnnotation;

                    for (Annotation relevantAnnotation : relevantAnnotations) {
                        EntityAnnotation relevantAnno = (EntityAnnotation) relevantAnnotation;

                        if (retrievedAnno.getMention().equals(relevantAnno.getMention())) {
                            retrievedAnno.setUrl(relevantAnno.getUrl());
                            break;
                        }
                    }

                }

                retrievedAnnotations = new LinkedHashSet<>(retrievedAnnotations);
            }

            overallRetrievedAnnotations.addAll(retrievedAnnotations);
            overallRelevantAnnotations.addAll(relevantAnnotations);

            double macroPrecision = getPrecision(retrievedAnnotations, relevantAnnotations);
            double macroRecall = getRecall(retrievedAnnotations, relevantAnnotations);
            double macroF1 = 2 * macroPrecision * macroRecall / (macroPrecision + macroRecall);

            double wRecall = getWRecall(retrievedAnnotations, relevantAnnotations);
            double wF1 = 2.0 * macroPrecision * wRecall / (macroPrecision + wRecall);

            if (Double.isNaN(macroF1)) {
                macroF1 = 0;
            }

            if (Double.isNaN(wF1)) {
                wF1 = 0;
            }

            if (!relevantAnnotations.isEmpty()) {
                statistics.accept("ep-precision", macroPrecision);
                statistics.accept("ep-recall", macroRecall);
                statistics.accept("ep-f1", macroF1);
                statistics.accept("ep-w-recall", wRecall);
                statistics.accept("ep-w-f1", wF1);

                Set<Annotation> notFound = new HashSet<>(relevantAnnotations);
                notFound.removeAll(retrievedAnnotations);

                notFound.forEach(annotation -> {
                    try {
                        notFoundAnnotations.writeAnnotation((EntityAnnotation) annotation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                if (!notFound.isEmpty())
                    System.out.println(notFound);
            }

            System.out.println("macro-precision: " + macroPrecision);
            System.out.println("macro-recall: " + macroRecall);
            System.out.println("macro-f1: " + macroF1);

            statistics.accept("macro-precision", macroPrecision);
            statistics.accept("macro-recall", macroRecall);
            statistics.accept("macro-f1", macroF1);

            statistics.accept("macro-w-recall", wRecall);
            statistics.accept("macro-w-f1", wF1);

            System.out.println("--------------------");
        }

        if (annotator instanceof LoggedAnnotator) {
            ((LoggedAnnotator) annotator).close();
        }

        notFoundAnnotations.flush();

        double microPrecision = getPrecision(overallRetrievedAnnotations, overallRelevantAnnotations);
        double microRecall = getRecall(overallRetrievedAnnotations, overallRelevantAnnotations);
        double microWRecall = getWRecall(overallRetrievedAnnotations, overallRelevantAnnotations);
        double microF1 = 2 * microPrecision * microRecall / (microPrecision + microRecall);
        double wF1 = 2.0 * microPrecision * microWRecall / (microPrecision + microWRecall);

        statistics.accept("micro-precision", microPrecision);
        statistics.accept("micro-recall", microRecall);
        statistics.accept("micro-w-recall", microWRecall);
        statistics.accept("micro-w-f1", wF1);
        statistics.accept("micro-f1", microF1);

        System.out.println(statistics);
    }

    public static EvaluationStatistics evaluateInterpretation(InterpretationAnnotator interpretationAnnotator,
                                                              EntityAnnotator entityAnnotator,
                                                              CorpusStreamReader corpusStreamReader,
                                                              MatchType type,
                                                              boolean ambiguousOnly) {

        MATCHTYPE = type;
        EvaluationStatistics statistics = new EvaluationStatistics();

        Map<String, Set<String>> concepts = null;

        try {
            concepts = loadConcepts();
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter("./data/annotations/webis-not-found-interpretations.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        while (corpusStreamReader.hasNext()) {
            Query query = corpusStreamReader.next();

            if (ambiguousOnly) {
                if (query instanceof WebisCorpusQuery) {
                    int numRelInterpretations = 0;

                    for (InterpretationAnnotation interpretation : ((WebisCorpusQuery) query).getInterpretations()) {
                        if (interpretation.getRelevance() > 1) {
                            numRelInterpretations++;
                        }

                        if (numRelInterpretations > 1) {
                            break;
                        }
                    }

                    if (numRelInterpretations < 2) {
                        continue;
                    }
                }

            }

            System.out.println("Annotate \"" + query.getText() + "\"...");

            EntityAnnotationFileHandle entityAnnotations = entityAnnotator.annotate(query);

            long start = System.currentTimeMillis();
            Set<Annotation> retrievedAnnotations =
                    new LinkedHashSet<>(interpretationAnnotator.annotate(query, entityAnnotations));
            statistics.accept("runtime", (double) (System.currentTimeMillis() - start));

            if (concepts != null)
                for (Annotation annotation : retrievedAnnotations) {
                    if (annotation instanceof InterpretationAnnotation) {
                        Set<String> conceptLinks = new HashSet<>(concepts.keySet());
                        List<String> interpretation = new LinkedList<>(((InterpretationAnnotation) annotation).getInterpretation());

                        conceptLinks.retainAll(interpretation);

                        if (!conceptLinks.isEmpty()) {
                            for (int i = 0; i < interpretation.size(); i++) {
                                if (conceptLinks.contains(interpretation.get(i))) {
                                    for (String conceptTerm : concepts.get(interpretation.get(i))) {
                                        if (query.getText().contains(conceptTerm)) {
                                            interpretation.set(i, conceptTerm);
                                            ((InterpretationAnnotation) annotation).setInterpretation(interpretation);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            retrievedAnnotations = new LinkedHashSet<>(retrievedAnnotations);

            Set<Annotation> desiredAnnotations = null;
            if (query instanceof WebisCorpusQuery) {
                desiredAnnotations =
                        new LinkedHashSet<>(((WebisCorpusQuery) query).getInterpretations());
            }

            Set<Annotation> notFound = new HashSet<>(retrievedAnnotations);
            notFound.removeAll(desiredAnnotations);

            double macroPrecision = getPrecision(retrievedAnnotations, desiredAnnotations);
            double macroRecall = getRecall(retrievedAnnotations, desiredAnnotations);
            double macroWRecall = getWRecall(retrievedAnnotations, desiredAnnotations);
            double macroF1 = 2 * macroPrecision * macroRecall / (macroPrecision + macroRecall);

            if (Double.isNaN(macroF1)) {
                macroF1 = 0;
            }

            if (writer != null)
                for (Annotation annotation : notFound) {
                    try {
                        writer.write("\"" + query.getId() + "\",\"" + query.getText() + "\",\"" + String.join("\",\"",
                                ((InterpretationAnnotation) (annotation)).getInterpretation() + "\""));
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            statistics.accept("macro-precision", macroPrecision);
            statistics.accept("macro-recall", macroRecall);
            statistics.accept("macro-w-recall", macroWRecall);
            statistics.accept("macro-f1", macroF1);

            System.out.println("macro-precision: " + macroPrecision);
            System.out.println("macro-recall: " + macroRecall);
            System.out.println("macro-f1: " + macroF1);
            System.out.println("--------------------");
        }

        if (entityAnnotator instanceof LoggedAnnotator) {
            ((LoggedAnnotator) entityAnnotator).close();
        }

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(entityAnnotator.getClass().getName() + "|" + interpretationAnnotator.getClass().getName() + "|" + type.name());
        System.out.println(statistics);

        return statistics;
    }

    public static void optimizeInterpretationApproach() {
        WebisQueryInterpretation queryInterpretation = new WebisQueryInterpretation();

        Random random = new Random(System.currentTimeMillis());
        double alpha = random.nextDouble();
        double beta = random.nextDouble();
        double gamma = random.nextDouble();

        double bestAlpha = 0.0, bestBeta = 0.0, bestGamma = 0.0;
        EvaluationStatistics bestStats = null;

        int interpretationCandidates = random.nextInt(200);
//        interpretationCandidates = 134;
        int bestNumCandidates = 0;
        double score = 0.0;

        for (int i = 0; i < 10; i++) {
//            queryInterpretation.setParameter(alpha, beta, gamma);
            queryInterpretation.setInterpretationCandidates(interpretationCandidates);

            EvaluationStatistics statistics = evaluateInterpretation(queryInterpretation,
                    new WebisExplicitEntityRetriever(new AllNGrams()),
                    new WebisJsonStreamReader(CorpusType.TRAIN),
                    MatchType.PM,
                    false);

            double f1 = statistics.get("macro-f1");

            if (f1 > score) {
                score = f1;
                bestAlpha = alpha;
                bestBeta = beta;
                bestGamma = gamma;
                bestStats = statistics;
                bestNumCandidates = interpretationCandidates;
            }

            interpretationCandidates = Math.min(Math.max(bestNumCandidates + random.nextInt(3) - 1, 0), 200);

            alpha = Math.min(Math.max(bestAlpha + random.nextGaussian(), 0.0), 1.0);
            beta = Math.min(Math.max(bestBeta + random.nextGaussian(), 0.0), 1.0);
            gamma = Math.min(Math.max(bestGamma + random.nextGaussian(), 0.0), 1.0);
        }

        System.out.println("Best parameters: ");
        System.out.println("---------------");
        System.out.println("Alpha: " + bestAlpha);
        System.out.println("Beta:  " + bestBeta);
        System.out.println("Gamma: " + bestGamma);
        System.out.println("#Candidates: " + bestNumCandidates);
        System.out.println();
        System.out.println(bestStats);
    }

    private static double getPrecision(Set<Annotation> retrieved, Set<Annotation> relevant) {
        if (retrieved.size() == 0 && relevant.size() == 0) {
            return 1.0;
        } else if (retrieved.size() == 0) {
            return 0.0;
        } else {
            Set<Annotation> intersection = new HashSet<>(relevant);
            intersection.retainAll(retrieved);

            return (double) (intersection.size()) / (double) (retrieved.size());
        }
    }

    private static double getRecall(Set<Annotation> retrieved, Set<Annotation> relevant) {
        if (retrieved.size() == 0 && relevant.size() == 0) {
            return 1.0;
        } else if (relevant.size() == 0) {
            return 0.0;
        } else {
            Set<Annotation> intersection = new HashSet<>(relevant);
            intersection.retainAll(retrieved);

            return (double) (intersection.size()) / (double) (relevant.size());
        }
    }

    private static double getWRecall(Set<Annotation> retrieved, Set<Annotation> relevant) {
        if (retrieved.size() == 0 && relevant.size() == 0) {
            return 1.0;
        } else if (relevant.size() == 0) {
            return 0.0;
        } else {
            int relevantDiff = (int) relevant.stream().collect(
                    Collectors.summarizingDouble(Annotation::getScore))
                    .getSum();

            Set<Annotation> intersection = new HashSet<>(relevant);
            intersection.retainAll(retrieved);

            int foundDiff = (int) intersection.stream().collect(
                    Collectors.summarizingDouble(Annotation::getScore))
                    .getSum();

            return (double) (foundDiff) / (double) (relevantDiff);
        }
    }

    private static Set<Annotation> getRelevantEntities(Query query, Task task) {
        if (query instanceof WebisCorpusQuery) {
            if (task == Task.EXER) {
                return new HashSet<>(((WebisCorpusQuery) query).getExplicitEntities());
            } else if (task == Task.IMER) {
                return new HashSet<>(((WebisCorpusQuery) query).getImplicitEntities());
            }
        }

        return new HashSet<>(query.getAnnotations());
    }

    private static Map<String, Set<String>> loadConcepts() throws IOException {
        Map<String, Set<String>> concepts = new HashMap<>();

        BufferedReader conceptReader = new BufferedReader(
                new FileReader("./data/annotations/webis-not-found-interpretations-concepts.csv")
        );

        String line;

        while ((line = conceptReader.readLine()) != null) {
            String[] attribs = line.split(",");
            String url = attribs[1].trim();

            concepts.putIfAbsent(url, new HashSet<>());
            concepts.get(url).add(attribs[0].trim());
        }

        conceptReader.close();
        return concepts;
    }

    public static void main(String[] args) {
//        Evaluator.evaluateER(new BaselineAnnotator(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new SmaphEntityLinker(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new TagMeEntityAnnotator(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new NordlysEntityLinker(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new NordlysEntityRetriever(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new FastEntityLinker(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new AmbiverseNLU(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new DandelionEntityExtractor(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new DexterEntityLinking(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new BabelfyEntityLinker(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new TextRazorEntityLinker(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new FalconEntityLinker(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new StanfordNERTagger(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new MITIEEntityRecognizer(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new OpenNLPEntityLinker(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new LingPipeNER(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new NLPArchitectNER(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new BingEntitySearch(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new AmazonComprohendEntityDetector(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new DeepPavlovNER(), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(new WebisExplicitEntityRetriever(new All1Grams()), new WebisJsonStreamReader(), Task.EXER);
        Evaluator.evaluateER(new WebisExplicitEntityRetriever(new AllNGrams()), new WebisJsonStreamReader(), Task.EXER);
//        Evaluator.evaluateER(
//                new WebisExplicitEntityRetriever(
//                        new HeuristicSegmentation(new StrategyWtBaseline(), 0.7)
//                ),
//                new WebisJsonStreamReader(),
//                Task.EXER);

//        evaluator.evaluateER(new BaselineEntityRecognizer(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new ImplicitEntityRecognizer(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new SmaphEntityLinker(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new TagMeEntityAnnotator(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new NordlysEntityLinker(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new NordlysEntityRetriever(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new FastEntityLinker(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new AmbiverseNLU(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new DandelionEntityExtractor(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new DexterEntityLinking(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new BabelfyEntityLinker(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new TextRazorEntityLinker(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new FalconEntityLinker(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new StanfordNERTagger(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new MITIEEntityRecognizer(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new OpenNLPEntityLinker(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new LingPipeNER(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new NLPArchitectNER(), new WebisJsonStreamReader(), Task.IMER);
//        evaluator.evaluateER(new BingEntitySearch(), new WebisJsonStreamReader(), Task.IMER);

//        Evaluator.evaluateInterpretation(new HasibiGIF(), new DexterEntityLinking(), new WebisJsonStreamReader(), MatchType.PM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new TextRazorEntityLinker(), new WebisJsonStreamReader(), MatchType.PM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new DandelionEntityExtractor(), new WebisJsonStreamReader(), MatchType.PM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new TagMeEntityAnnotator(), new WebisJsonStreamReader(), MatchType.PM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new BabelfyEntityLinker(), new WebisJsonStreamReader(), MatchType.PM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new AmbiverseNLU(), new WebisJsonStreamReader(), MatchType.PM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new NordlysEntityLinker(), new WebisJsonStreamReader(), MatchType.PM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new SmaphEntityLinker(), new WebisJsonStreamReader(), MatchType.PM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new FastEntityLinker(), new WebisJsonStreamReader(), MatchType.PM, false);

//        Evaluator.evaluateInterpretation(new HasibiGIF(), new DexterEntityLinking(), new WebisJsonStreamReader(), MatchType.CM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new SmaphEntityLinker(), new WebisJsonStreamReader(), MatchType.CM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new DandelionEntityExtractor(), new WebisJsonStreamReader(), MatchType.CM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new TagMeEntityAnnotator(), new WebisJsonStreamReader(), MatchType.CM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new TextRazorEntityLinker(), new WebisJsonStreamReader(), MatchType.CM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new BabelfyEntityLinker(), new WebisJsonStreamReader(), MatchType.CM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new AmbiverseNLU(), new WebisJsonStreamReader(), MatchType.CM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new NordlysEntityLinker(), new WebisJsonStreamReader(), MatchType.CM, false);
//        Evaluator.evaluateInterpretation(new HasibiGIF(), new FastEntityLinker(), new WebisJsonStreamReader(), MatchType.CM, false);

//        Evaluator.evaluateInterpretation(
//                new WebisQueryInterpretation(),
//                new WebisExplicitEntityRetriever(new AllNGrams()),
//                new WebisJsonStreamReader(CorpusType.TEST),
//                MatchType.CM,
//                false);
//        Evaluator.optimizeInterpretationApproach();
    }
}
