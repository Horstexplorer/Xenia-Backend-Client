/*
 *     Copyright 2020 Horstexplorer @ https://www.netbeacon.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.netbeacon.xenia.backend.client.objects.internal.objects;

import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Cache<K, T extends APIDataObject> {

    private final BackendProcessor backendProcessor;
    private final ConcurrentHashMap<K, T> dataMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<T, K> inverseDataMap = new ConcurrentHashMap<>();
    private final ArrayList<K> orderedKeyMap = new ArrayList<>();
    private final ArrayList<CacheEventListener<K, T>> cacheListeners = new ArrayList<>();

    public Cache(BackendProcessor backendProcessor){
        this.backendProcessor = backendProcessor;
    }

    // data

    public T getFromCache(K id){
        return dataMap.get(id);
    }

    public T addToCache(K id, T t){
        dataMap.put(id, t);
        inverseDataMap.put(t, id);
        orderedKeyMap.add(id);
        onInsertion(id, t);
        return t;
    }

    public void removeFromCache(K id){
        var t = dataMap.remove(id);
        if(t == null){
            return;
        }
        inverseDataMap.remove(t);
        orderedKeyMap.remove(id);
        onRemoval(id);
    }

    public void removeFromCache(T t){
        var id = inverseDataMap.remove(t);
        if(id == null){
            return;
        }
        dataMap.remove(id);
        orderedKeyMap.remove(id);
        onRemoval(id);
    }

    // qol

    public BackendProcessor getBackendProcessor() {
        return backendProcessor;
    }

    public boolean contains(K id){
        return dataMap.containsKey(id);
    }

    public List<T> getAllAsList(){
        return new ArrayList<>(dataMap.values());
    }

    public HashMap<K, T> getAllAsMap(){
        return new HashMap<>(dataMap);
    }

    public ConcurrentHashMap<K, T> getDataMap(){
        return dataMap;
    }

    public ArrayList<K> getOrderedKeyMap() {
        return orderedKeyMap;
    }

    public void clear(boolean deletion){
        dataMap.values().forEach(this::removeFromCache); // remove all objects, triggering the remove event for the cache
        dataMap.forEach((k,v)-> {
            if(deletion){
                v.onDeletion(); // trigger deletion event for when this cache gets removed with the deletion as cause
            }
            v.removeEventListeners();
        }); // remove event listeners of objects
    }

    private void onInsertion(K newKey, T newObject){
        for(var listener : new ArrayList<>(cacheListeners)){
            try{
                listener.onInsertion(newKey, newObject);
            }catch (Exception ignore){}
        }
    }

    private void onRemoval(K oldKey){
        for(var listener : new ArrayList<>(cacheListeners)){
            try{
                listener.onRemoval(oldKey);
            }catch (Exception e){}
        }
    }

    public void addEventListeners(CacheEventListener<K, T>...listeners){
        cacheListeners.addAll(Arrays.asList(listeners));
    }

    public void removeEventListeners(){
        cacheListeners.clear();
    }
}
