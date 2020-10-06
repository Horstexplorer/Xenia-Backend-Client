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

package de.netbeacon.xenia.backend.client.objects.cache;

import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Cache<T extends APIDataObject> {

    private final BackendProcessor backendProcessor;
    private final ConcurrentHashMap<Long, T> dataMap = new ConcurrentHashMap<>();

    public Cache(BackendProcessor backendProcessor){
        this.backendProcessor = backendProcessor;
    }

    // data

    public T getFromCache(long id){
        return dataMap.get(id);
    }

    public T addToCache(long id, T t){
        dataMap.put(id, t);
        return t;
    }

    public void removeFromCache(long id){
        dataMap.remove(id);
    }

    //

    public BackendProcessor getBackendProcessor() {
        return backendProcessor;
    }
}
