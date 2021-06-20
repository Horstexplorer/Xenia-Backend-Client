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
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class APIDataObject implements IJSONSerializable{

	private final BackendProcessor backendProcessor;
	private final List<BackendPathArg> backendPath = new ArrayList<>();
	private final Logger logger = LoggerFactory.getLogger(APIDataObject.class);
	private final ArrayList<APIDataEventListener> apiDataEventListeners = new ArrayList<>();
	private final AtomicBoolean isStable = new AtomicBoolean(true);
	private long lastRequestDuration;
	private JSONObject shadowCopy;

	public APIDataObject(BackendProcessor backendProcessor){
		this.backendProcessor = backendProcessor;
	}

	public void get() throws DataException{
		get(false);
	}

	public void get(boolean securityOverride) throws DataException{
		process(securityOverride, BackendRequest.Method.GET, null, null);
	}

	public void getAsync(){
		getAsync(false);
	}

	public void getAsync(boolean securityOverride) throws DataException{
		processAsync(securityOverride, BackendRequest.Method.GET, null, null);
	}

	public void create(){
		create(false);
	}

	public void create(boolean securityOverride) throws DataException{
		process(securityOverride, BackendRequest.Method.POST, null, asJSON());
	}

	public void createAsync(){
		createAsync(false);
	}

	public void createAsync(boolean securityOverride) throws DataException{
		processAsync(securityOverride, BackendRequest.Method.POST, null, asJSON());
	}

	public void createOrGet(){
		createOrGet(false);
	}

	public void createOrGet(boolean securityOverride){
		process(securityOverride, BackendRequest.Method.POST, new HashMap<>(){{ put("goc", "true"); }}, asJSON());
	}

	public void createOrGetAsync(){
		createOrGetAsync(false);
	}

	public void createOrGetAsync(boolean securityOverride){
		processAsync(securityOverride, BackendRequest.Method.POST, new HashMap<>(){{ put("goc", "true"); }}, asJSON());
	}

	public void update(){
		update(false);
	}

	public void update(boolean securityOverride) throws DataException{
		process(securityOverride, BackendRequest.Method.PUT, null, asJSON());
	}

	public void updateAsync(){
		updateAsync(false);
	}

	public void updateAsync(boolean securityOverride) throws DataException{
		processAsync(securityOverride, BackendRequest.Method.PUT, null, asJSON());
	}

	public void delete(){
		delete(false);
	}

	public void delete(boolean securityOverride) throws DataException{
		process(securityOverride, BackendRequest.Method.DELETE, null, null);
	}

	public void deleteAsync(){
		deleteAsync(false);
	}

	public void deleteAsync(boolean securityOverride) throws DataException{
		processAsync(securityOverride, BackendRequest.Method.DELETE, null, null);
	}



	private void process(boolean securityOverride, BackendRequest.Method method, HashMap<String, String> queryParams, JSONObject payload){
		try{
			if(!isStable.compareAndSet(true, false) && !securityOverride){
				throw new DataException(DataException.Type.UNSTABLE, 0, "Failed To "+method+" APIDataObject With Path " + Arrays.toString(getBackendPath().toArray()));
			}
			BackendRequest backendRequest = new BackendRequest(method, BackendRequest.AuthType.BEARER, getBackendPath(), queryParams, payload);
			BackendResult backendResult = backendProcessor.process(backendRequest);
			if(backendResult.getStatusCode() > 299 || backendResult.getStatusCode() < 200){
				throw new DataException(DataException.Type.HTTP, backendResult.getStatusCode(), "Failed To "+method+" APIDataObject With Path " + Arrays.toString(getBackendPath().toArray()));
			}
			if(backendResult.getStatusCode() != 204){
				fromJSON(backendResult.getPayloadAsJSON());
			}
			lastRequestDuration = backendResult.getRequestDuration();
			switch(method){
				case GET -> this.onRetrieval();
				case POST -> this.onCreation();
				case PUT -> this.onUpdate();
				case DELETE -> this.onDeletion();
			}
		}
		catch(Exception e){
			this.restore();
			throw e;
		}
		finally{
			if(!securityOverride){
				isStable.set(true);
			}
		}
	}

	private void processAsync(boolean securityOverride, BackendRequest.Method method, HashMap<String, String> queryParams, JSONObject payload){
		try{
			if(!isStable.compareAndSet(true, false) && !securityOverride){
				throw new DataException(DataException.Type.UNSTABLE, 0, "Failed To "+method+" APIDataObject With Path " + Arrays.toString(getBackendPath().toArray()));
			}
			BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, getBackendPath(), queryParams, payload);
			backendProcessor.processAsync(backendRequest, backendResult -> {
				try{
					if(backendResult.getStatusCode() > 299 || backendResult.getStatusCode() < 200){
						throw new DataException(DataException.Type.HTTP, backendResult.getStatusCode(), "Failed To "+method+" APIDataObject With Path " + Arrays.toString(getBackendPath().toArray()));
					}
					if(backendResult.getStatusCode() != 204){
						fromJSON(backendResult.getPayloadAsJSON());
					}
					synchronized(this){
						lastRequestDuration = backendResult.getRequestDuration();
					}
					switch(method){
						case GET -> this.onRetrieval();
						case POST -> this.onCreation();
						case PUT -> this.onUpdate();
						case DELETE -> this.onDeletion();
					}
				}
				catch(Exception e){
					this.restore();
					throw e;
				}
				finally{
					if(!securityOverride){
						isStable.set(true);
					}
				}
			});
		}
		catch(Exception e){
			this.restore();
			if(!securityOverride){
				isStable.set(true);
			}
			throw e;
		}
	}




	public synchronized void secure(){
		if(isStable.get()){
			this.shadowCopy = asJSON();
		}
	}

	public synchronized void restore(){
		if(shadowCopy != null){
			this.fromJSON(shadowCopy);
			this.shadowCopy = null;
		}
	}



	public BackendProcessor getBackendProcessor(){
		return backendProcessor;
	}

	public List<String> getBackendPath(){
		List<String> list = new ArrayList<>();
		for(BackendPathArg bpa : backendPath){
			list.add(String.valueOf(bpa.getObject()));
		}
		return list;
	}

	protected void setBackendPath(Object... backendPath){
		for(Object o : backendPath){
			this.backendPath.add(new BackendPathArg(o));
		}
	}

	public long getLastRequestDuration(){
		return lastRequestDuration;
	}



	protected void onRetrieval(){
		for(var listener : new ArrayList<>(apiDataEventListeners)){
			try{
				listener.onRetrieval(this);
			}
			catch(Exception e){
				logger.error("Uncaught exception on APIDataObject onRetrieval listener " + e);
			}
		}
	}

	protected void onCreation(){
		for(var listener : new ArrayList<>(apiDataEventListeners)){
			try{
				listener.onCreation(this);
			}
			catch(Exception e){
				logger.error("Uncaught exception on APIDataObject onCreation listener " + e);
			}
		}
	}

	protected void onUpdate(){
		for(var listener : new ArrayList<>(apiDataEventListeners)){
			try{
				listener.onUpdate(this);
			}
			catch(Exception e){
				logger.error("Uncaught exception on APIDataObject onUpdate listener " + e);
			}
		}
	}

	public void onDeletion(){
		for(var listener : new ArrayList<>(apiDataEventListeners)){
			try{
				listener.onDeletion(this);
			}
			catch(Exception e){
				logger.error("Uncaught exception on APIDataObject onDeletion listener " + e);
			}
		}
	}

	public void addEventListener(APIDataEventListener... listeners){
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
			if(object instanceof Supplier<?>){
				return ((Supplier<?>) object).get();
			}
			return object;
		}

	}

}
