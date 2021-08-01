/*
 *     Copyright 2021 Horstexplorer @ https://www.netbeacon.de
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

package de.netbeacon.xenia.backend.client.objects.external.system;

import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.HashMap;

public class Ping extends APIDataObject<Ping>{

	public Ping(BackendProcessor backendProcessor){
		super(backendProcessor);
		setBackendPath("info", "ping");
	}

	public boolean ping(){
		// this might send invalid auth tokens to the backend - but this is just supposed to be an isOnline check so should we care? y/n?
		try{
			BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, getBackendPath(), new HashMap<>(), null);
			BackendResult backendResult = getBackendProcessor().process(backendRequest);
			return backendResult.getStatusCode() == 200;
		}
		catch(Exception e){
			return false;
		}
	}

	@Override
	public void get() throws BackendException{}

	@Override
	public void getAsync(){}

	@Override
	public void create() throws BackendException{}

	@Override
	public void createAsync(){}

	@Override
	public void update() throws BackendException{}

	@Override
	public void updateAsync(){}

	@Override
	public void delete() throws BackendException{}

	@Override
	public void deleteAsync(){}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{ return null; }

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{}

}
