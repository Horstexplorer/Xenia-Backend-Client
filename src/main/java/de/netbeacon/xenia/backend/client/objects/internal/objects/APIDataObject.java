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
import de.netbeacon.xenia.backend.client.objects.internal.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class APIDataObject implements IJSONSerializable {

    private final BackendProcessor backendProcessor;
    private final List<String> backendPath;

    public APIDataObject(BackendProcessor backendProcessor, List<String> backendPath){
        this.backendProcessor = backendProcessor;
        this.backendPath = backendPath;
    }

    public void get() throws BackendException {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.Token, backendPath, new HashMap<>(), null);
        BackendResult backendResult = backendProcessor.process(backendRequest);
        if(backendResult.getStatusCode() != 200){
            throw new BackendException(backendResult.getStatusCode(), "Failed To GET APIDataObject With Path "+ Arrays.toString(backendPath.toArray()));
        }
        fromJSON(backendResult.getPayloadAsJSON());
    }

    public void create() throws BackendException{
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.POST, BackendRequest.AuthType.Token, backendPath, new HashMap<>(), asJSON());
        BackendResult backendResult = backendProcessor.process(backendRequest);
        if(backendResult.getStatusCode() != 202){
            throw new BackendException(backendResult.getStatusCode(), "Failed To CREATE APIDataObject With Path "+ Arrays.toString(backendPath.toArray()));
        }
        fromJSON(backendResult.getPayloadAsJSON());
    }

    public void update() throws BackendException {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.PUT, BackendRequest.AuthType.Token, backendPath, new HashMap<>(), asJSON());
        BackendResult backendResult = backendProcessor.process(backendRequest);
        if(backendResult.getStatusCode() != 200){
            throw new BackendException(backendResult.getStatusCode(), "Failed To UPDATE APIDataObject With Path "+ Arrays.toString(backendPath.toArray()));
        }
        fromJSON(backendResult.getPayloadAsJSON());
    }

    public void delete() throws BackendException {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.DELETE, BackendRequest.AuthType.Token, backendPath, new HashMap<>(), null);
        BackendResult backendResult = backendProcessor.process(backendRequest);
        if(backendResult.getStatusCode() != 200){
            throw new BackendException(backendResult.getStatusCode(), "Failed To DELETE APIDataObject With Path "+ Arrays.toString(backendPath.toArray()));
        }
    }

    public BackendProcessor getBackendProcessor(){
        return backendProcessor;
    }

    public List<String> getBackendPath() {
        return backendPath;
    }

    @Override
    public abstract JSONObject asJSON() throws JSONSerializationException;

    @Override
    public abstract void fromJSON(JSONObject jsonObject) throws JSONSerializationException;
}
