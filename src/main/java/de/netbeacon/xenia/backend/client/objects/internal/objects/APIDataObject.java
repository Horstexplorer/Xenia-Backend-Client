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

import de.netbeacon.utils.json.serial.IJSONSerializable;
import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public abstract class APIDataObject implements IJSONSerializable {

    private final BackendProcessor backendProcessor;
    private final List<BackendPathArg> backendPath = new ArrayList<>();
    private final Logger logger = LoggerFactory.getLogger(APIDataObject.class);
    private long lastRequestDuration;
    private final ArrayList<APIDataEventListener> apiDataEventListeners = new ArrayList<>();

    public APIDataObject(BackendProcessor backendProcessor){
        this.backendProcessor = backendProcessor;
    }

    protected void setBackendPath(Object... backendPath){
        for(Object o : backendPath){
            this.backendPath.add(new BackendPathArg(o));
        }
    }

    public void get() throws BackendException {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, getBackendPath(), new HashMap<>(), null);
        BackendResult backendResult = backendProcessor.process(backendRequest);
        if(backendResult.getStatusCode() != 200){
            logger.debug("Failed To GET APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+backendResult.getStatusCode()+")");
            throw new BackendException(backendResult.getStatusCode(), "Failed To GET APIDataObject With Path "+ Arrays.toString(getBackendPath().toArray())+" ("+backendResult.getStatusCode()+")");
        }
        fromJSON(backendResult.getPayloadAsJSON());
        lastRequestDuration = backendResult.getRequestDuration();
        onRetrieval();
    }

    public void getAsync(){
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, getBackendPath(), new HashMap<>(), null);
        backendProcessor.processAsync(backendRequest, br->{
            if(br.getStatusCode() != 200){
                logger.debug("Failed To GET APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+br.getStatusCode()+")");
                throw new BackendException(br.getStatusCode(), "Failed To GET APIDataObject With Path "+ Arrays.toString(getBackendPath().toArray())+" ("+br.getStatusCode()+")");
            }
            fromJSON(br.getPayloadAsJSON());
            lastRequestDuration = br.getRequestDuration();
        });
        onRetrieval();
    }

    public void create() throws BackendException{
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.POST, BackendRequest.AuthType.BEARER, getBackendPath(), new HashMap<>(), asJSON());
        BackendResult backendResult = backendProcessor.process(backendRequest);
        if(backendResult.getStatusCode() != 202){
            logger.debug("Failed To CREATE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+backendResult.getStatusCode()+")");
            throw new BackendException(backendResult.getStatusCode(), "Failed To CREATE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+backendResult.getStatusCode()+")");
        }
        fromJSON(backendResult.getPayloadAsJSON());
        lastRequestDuration = backendResult.getRequestDuration();
        onCreation();
    }

    public void createAsync() {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.POST, BackendRequest.AuthType.BEARER, getBackendPath(), new HashMap<>(), asJSON());
        backendProcessor.processAsync(backendRequest, br->{
            if(br.getStatusCode() != 202){
                logger.debug("Failed To CREATE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+br.getStatusCode()+")");
                throw new BackendException(br.getStatusCode(), "Failed To CREATE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+br.getStatusCode()+")");
            }
            fromJSON(br.getPayloadAsJSON());
            lastRequestDuration = br.getRequestDuration();
            onCreation();
        });
    }

    public void update() throws BackendException {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.PUT, BackendRequest.AuthType.BEARER, getBackendPath(), new HashMap<>(), asJSON());
        BackendResult backendResult = backendProcessor.process(backendRequest);
        if(backendResult.getStatusCode() != 200){
            logger.debug("Failed To UPDATE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+backendResult.getStatusCode()+")");
            throw new BackendException(backendResult.getStatusCode(), "Failed To UPDATE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+backendResult.getStatusCode()+")");
        }
        fromJSON(backendResult.getPayloadAsJSON());
        lastRequestDuration = backendResult.getRequestDuration();
        onUpdate();
    }

    public void updateAsync() {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.PUT, BackendRequest.AuthType.BEARER, getBackendPath(), new HashMap<>(), asJSON());
        backendProcessor.processAsync(backendRequest, br->{
            if(br.getStatusCode() != 200){
                logger.debug("Failed To UPDATE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+br.getStatusCode()+")");
                throw new BackendException(br.getStatusCode(), "Failed To UPDATE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+br.getStatusCode()+")");
            }
            fromJSON(br.getPayloadAsJSON());
            lastRequestDuration = br.getRequestDuration();
            onUpdate();
        });
    }

    public void delete() throws BackendException {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.DELETE, BackendRequest.AuthType.BEARER, getBackendPath(), new HashMap<>(), null);
        BackendResult backendResult = backendProcessor.process(backendRequest);
        if(backendResult.getStatusCode() != 200){
            logger.debug("Failed To DELETE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+backendResult.getStatusCode()+")");
            throw new BackendException(backendResult.getStatusCode(), "Failed To DELETE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+backendResult.getStatusCode()+")");
        }
        lastRequestDuration = backendResult.getRequestDuration();
        onDeletion();
    }

    public void deleteAsync() {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.DELETE, BackendRequest.AuthType.BEARER, getBackendPath(), new HashMap<>(), null);
        backendProcessor.processAsync(backendRequest, br->{
            if(br.getStatusCode() != 200){
                logger.debug("Failed To DELETE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+br.getStatusCode()+")");
                throw new BackendException(br.getStatusCode(), "Failed To DELETE APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+br.getStatusCode()+")");
            }
            lastRequestDuration = br.getRequestDuration();
            onDeletion();
        });
    }

    public BackendProcessor getBackendProcessor(){
        return backendProcessor;
    }

    public List<String> getBackendPath() {
        List<String> list = new ArrayList<>();
        for(BackendPathArg bpa : backendPath){
            list.add(String.valueOf(bpa.getObject()));
        }
        return list;
    }

    public long getLastRequestDuration() {
        return lastRequestDuration;
    }

    private void onRetrieval(){
        for(var listener : new ArrayList<>(apiDataEventListeners)){
            try{
                listener.onRetrieval(this);
            }catch (Exception ignore){}
        }
    }

    private void onCreation(){
        for(var listener : new ArrayList<>(apiDataEventListeners)){
            try{
                listener.onCreation(this);
            }catch (Exception ignore){}
        }
    }

    private void onUpdate(){
        for(var listener : new ArrayList<>(apiDataEventListeners)){
            try{
                listener.onUpdate(this);
            }catch (Exception ignore){}
        }
    }

    private void onDeletion(){
        for(var listener : new ArrayList<>(apiDataEventListeners)){
            try{
                listener.onDeletion(this);
            }catch (Exception ignore){}
        }
    }

    public void addEventListener(APIDataEventListener...listeners){
        apiDataEventListeners.addAll(Arrays.asList(listeners));
    }

    public void removeEventListeners(){
        apiDataEventListeners.clear();
    }

    @Override
    public abstract JSONObject asJSON() throws JSONSerializationException;

    @Override
    public abstract void fromJSON(JSONObject jsonObject) throws JSONSerializationException;

    public static class BackendPathArg{

        private final Object object;

        public BackendPathArg(Object object){
            this.object = object;
        }

        public Object getObject(){
            if(object instanceof Function<?,?>){
                return ((Function<?, ?>) object).apply(null);
            }
            return object;
        }
    }
}
