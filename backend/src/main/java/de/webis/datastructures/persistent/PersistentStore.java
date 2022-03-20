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

import de.webis.utils.FSTSerializer;
import de.webis.utils.Serializer;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class PersistentStore<KeyType, ValueType> {
    private RocksDB hashMap;
    private String filesDir;

    private boolean isEmpty;

    private Method serMethod;
    private Method deserMethod;

    public PersistentStore(String filesDir) {
        setSerializer(FSTSerializer.class);

        this.filesDir = filesDir;

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

        isEmpty = !exists();

        RocksDB.loadLibrary();
        Options options = new Options().setCreateIfMissing(true);

        try {
            hashMap = RocksDB.open(options, filesDir);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void setSerializer(Class<? extends Serializer> serializer) {
        try {
            this.serMethod = serializer.getMethod("serialize", Object.class);
            this.deserMethod = serializer.getMethod("deserialize", byte[].class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public boolean exists() {
        Path idPath = Paths.get(filesDir, "IDENTITY");

        return idPath.toFile().exists();
    }

    public void forEachKey(Consumer<KeyType> callback) {
        RocksIterator iterator = hashMap.newIterator();

        for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
            try {
                callback.accept((KeyType) deserMethod.invoke(null, iterator.key()));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        iterator.close();
    }

    public int size() {
        final int[] size = {0};

        forEachKey(k -> size[0]++);

        return size[0];
    }

    public boolean contains(KeyType key) {
        try {
            return hashMap.get((byte[]) serMethod.invoke(null, key)) != null;
        } catch (RocksDBException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void put(KeyType key, ValueType value) {
        try {
            hashMap.put(
                    (byte[]) serMethod.invoke(null, key),
                    (byte[]) serMethod.invoke(null, value)
            );
        } catch (RocksDBException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public ValueType get(KeyType key) {
        try {
            byte[] serValue = hashMap.get((byte[]) serMethod.invoke(null, key));

            if (serValue != null) {
                return (ValueType) deserMethod.invoke(null, serValue);
            }
        } catch (RocksDBException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ValueType getOrDefault(KeyType key, ValueType defaultValue) {
        ValueType val = get(key);

        return val != null ? val : defaultValue;
    }

    public void remove(KeyType key) {
        try {
            hashMap.remove((byte[]) serMethod.invoke(null, key));
        } catch (RocksDBException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void flush() {
        try {
            hashMap.flush(new FlushOptions().setWaitForFlush(true));
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (hashMap != null) {
            hashMap.close();
        }
    }
}
