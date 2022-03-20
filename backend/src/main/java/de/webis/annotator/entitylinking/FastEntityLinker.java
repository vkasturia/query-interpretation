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

//package de.webis.annotator.entitylinking;
//
//import com.yahoo.semsearch.fastlinking.hash.QuasiSuccinctEntityHash;
//import com.yahoo.semsearch.fastlinking.view.EmptyContext;
//import de.webis.annotator.EntityAnnotator;
//import de.webis.datastructures.EntityAnnotation;
//import de.webis.datastructures.EntityAnnotationFileHandle;
//import de.webis.datastructures.Query;
//import it.unimi.dsi.fastutil.io.BinIO;
//
//import java.io.IOException;
//import java.util.List;
//
//public class FastEntityLinker implements EntityAnnotator {
//    private static com.yahoo.semsearch.fastlinking.FastEntityLinker FEL;
//
//    public FastEntityLinker() {
//        try {
//            QuasiSuccinctEntityHash hash = (QuasiSuccinctEntityHash) BinIO.loadObject("/media/storage1/corpora/corpus-yahoo-fel-models/en/english-nov15.hash");
//            FEL = new com.yahoo.semsearch.fastlinking.FastEntityLinker(hash, new EmptyContext());
//        } catch (IOException | ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    @Override
//    public EntityAnnotationFileHandle annotate(Query query) {
//        System.out.println("Annotate \"" + query + "\" ...");
//        EntityAnnotationFileHandle annotationFileHandle = new EntityAnnotationFileHandle(query.getText(), getAnnotationTag());
//
//        List<com.yahoo.semsearch.fastlinking.FastEntityLinker.EntityResult> results;
//
//        results = FEL.getResults(query.getText(), -10);
//
//        for (com.yahoo.semsearch.fastlinking.FastEntityLinker.EntityResult result : results) {
//            try {
//                annotationFileHandle.writeAnnotation(new EntityAnnotation(
//                        result.s.getStartOffset(),
//                        result.s.getEndOffset(),
//                        result.s.getSpan(),
//                        result.text.toString(),
//                        (result.score + 10.0) / 10.0));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        annotationFileHandle.flush();
//
//        return annotationFileHandle;
//    }
//
//    @Override
//    public String getAnnotationTag() {
//        return "yahoo-fel";
//    }
//
//    public static void main(String[] args) {
//        FastEntityLinker linker = new FastEntityLinker();
//
//        linker.annotate(new Query("new york times square dance"));
//    }
//}
