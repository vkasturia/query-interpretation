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

package de.webis.datastructures.persistent;

import de.webis.datastructures.IndexedDocument;
import de.webis.utils.FSTSerializer;
import io.multimap.Callables;
import io.multimap.Iterator;
import io.multimap.Map;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PersistentIndex<KeyType, ValueType> {
    private Map multiMap;
    private String filesDir;

    private boolean isEmpty;

    public PersistentIndex(String filesDir) {
        if (filesDir.endsWith("/")) {
            filesDir = filesDir.substring(0, filesDir.length() - 1);
        }

        File optimizedDir = new File(filesDir + "-optimized");

        if (optimizedDir.exists()) {
            filesDir = optimizedDir.getPath();
        }

        File sortedDir = new File(filesDir + "-sorted");

        if (sortedDir.exists()) {
            filesDir = sortedDir.getPath();
        }

        File dir = new File(filesDir);

        try {
            if (!dir.exists()) {
                boolean created = dir.mkdirs();

                if (created) {
                    System.out.println(dir + " created successfully");
                } else {
                    throw new IOException("Could't create " + filesDir);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.filesDir = filesDir;

        if (exists()) {
            open(filesDir);
            isEmpty = false;
        } else {
            Map.Options options = new Map.Options();
            options.setCreateIfMissing(true);
//            options.setBlockSize(1024);

            try {
                multiMap = new Map(filesDir, options);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isEmpty = true;
        }
    }

    public boolean exists() {
        return Paths.get(filesDir, "multimap.map.id").toFile().exists();
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void put(KeyType key, ValueType value) {
        try {
            multiMap.put(FSTSerializer.serialize(key), FSTSerializer.serialize(value));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ValueType> get(KeyType key) {
        Iterator iterator = multiMap.get(FSTSerializer.serialize(key));
        List<ValueType> results = new LinkedList<>();

        while (iterator.hasNext()) {
            results.add(
                    (ValueType) FSTSerializer.deserialize(iterator.nextAsByteArray())
            );
        }


        iterator.close();

        return results;
    }

    public void flush() {
        multiMap.close();
        open(filesDir);
    }

    public void optimize() {
        multiMap.close();

        System.out.println("Optimize index...");

        Map.Options options = new Map.Options();
        Callables.LessThan sortFunction = new Callables.LessThan() {
            @Override
            public boolean call(ByteBuffer a, ByteBuffer b) {
                byte[] bytesA = new byte[a.remaining()];
                byte[] bytesB = new byte[b.remaining()];

                a.get(bytesA);
                IndexedDocument docA = (IndexedDocument) FSTSerializer.deserialize(bytesA);


                b.get(bytesB);
                IndexedDocument docB = (IndexedDocument) FSTSerializer.deserialize(bytesB);

                return docA.getTfValue() > docB.getTfValue();

//                return false;
            }
        };


        options.setLessThanCallable(sortFunction);
        options.setBlockSize(128);

        File inFile = new File(filesDir);
        File outFile = new File(inFile.getPath() + "-optimized");

        if (!outFile.exists()) {
            boolean dirCreated = outFile.mkdir();

            if (dirCreated) {
                System.out.println("Created " + outFile.toString());
            }
        }

        try {
            Map.optimize(inFile.toPath(), outFile.toPath(), options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        open(outFile.getPath());
        System.out.println("--------------------");
    }

    public int size() {
        return (int) multiMap.getStats().getNumKeysTotal();
    }


    public boolean contains(KeyType key) {
        return multiMap.contains(FSTSerializer.serialize(key));
    }

    public boolean contains(KeyType key, ValueType value) {
        return get(key).contains(value);
    }

    public void forEach(Consumer<KeyType> callback) {
        multiMap.forEachKey(new Callables.Procedure() {
            @Override
            public void call(ByteBuffer bytes) {
                byte[] bytes1 = new byte[bytes.remaining()];

                bytes.get(bytes1);

                KeyType key = (KeyType) FSTSerializer.deserialize(bytes1);

                callback.accept(key);
            }
        });
    }

    private void open(String path) {
        try {
            multiMap = new Map(path);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void close() {
        multiMap.close();
    }

    public static void sortIndex(PersistentIndex<String, IndexedDocument> input) {
        System.out.println("Sort Index");
        PersistentIndex<String, IndexedDocument> sorted = new PersistentIndex<>(input.filesDir + "-sorted");
        final int[] numKeys = {0};

        input.forEach(key -> {
            if (numKeys[0] % 1000 == 0) {
                System.out.print("\rSorted Keys: " + numKeys[0]);
            }

            List<IndexedDocument> values = input.get(key);

            values = values.stream().sorted((o1, o2) -> Double.compare(o2.getTfValue(), o1.getTfValue())).collect(Collectors.toList());

            values.forEach(doc -> sorted.put(key, doc));
            numKeys[0]++;
        });

        sorted.close();

        System.out.println("--------------------");
    }
}
