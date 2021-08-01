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

import de.netbeacon.utils.concurrency.action.ExecutionAction;
import de.netbeacon.utils.concurrency.action.ExecutionException;
import de.netbeacon.utils.concurrency.action.imp.SupplierExecutionAction;
import de.netbeacon.utils.json.serial.IJSONSerializable;
import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.utils.json.test.JSONEQ;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class APIDataObject<T extends APIDataObject<T>> implements IJSONSerializable{

	private final BackendProcessor backendProcessor;
	private final List<BackendPathArg> backendPath = new ArrayList<>();
	private final Logger logger = LoggerFactory.getLogger(APIDataObject.class);
	private final ArrayList<APIDataEventListener<T>> apiDataEventListeners = new ArrayList<>();
	private final AtomicBoolean isStable = new AtomicBoolean(true);
	private long lastRequestDuration;

	private JSONObject shadowCopy; // contains the object last returned from the backend

	public APIDataObject(BackendProcessor backendProcessor){
		this.backendProcessor = backendProcessor;
	}

	@CheckReturnValue
	public ExecutionAction<T> get() throws DataException{
		return get(false);
	}

	@CheckReturnValue
	public ExecutionAction<T> get(boolean securityOverride) throws DataException{
		return process(securityOverride, BackendRequest.Method.GET, null, null);
	}

	@CheckReturnValue
	public ExecutionAction<T> create(){
		return create(false);
	}

	@CheckReturnValue
	public ExecutionAction<T> create(boolean securityOverride) throws DataException{
		return process(securityOverride, BackendRequest.Method.POST, null, asJSON());
	}

	@CheckReturnValue
	public ExecutionAction<T> getOrCreate(){
		return getOrCreate(false);
	}

	@Deprecated
	@CheckReturnValue
	public ExecutionAction<T> getOrCreate(boolean securityOverride){
		return new SupplierExecutionAction<>(() -> {throw new ExecutionException(new UnsupportedOperationException());});
		//return process(securityOverride, BackendRequest.Method.POST, new HashMap<>(){{ put("goc", "true"); }}, asJSON());
	}

	@CheckReturnValue
	public ExecutionAction<T> update(){
		return update(false);
	}

	@CheckReturnValue
	public ExecutionAction<T> update(boolean securityOverride) throws DataException{
		if(!hasChanges()){
			return new SupplierExecutionAction<>(() -> {throw new ExecutionException(new UnsupportedOperationException());});
		}
		return process(securityOverride, BackendRequest.Method.PUT, null, asJSON());
	}

	@CheckReturnValue
	public ExecutionAction<T> delete(){
		return delete(false);
	}

	@CheckReturnValue
	public ExecutionAction<T> delete(boolean securityOverride) throws DataException{
		return process(securityOverride, BackendRequest.Method.DELETE, null, null);
	}


	private ExecutionAction<T> process(boolean securityOverride, BackendRequest.Method method, HashMap<String, String> queryParams, JSONObject payload){
		Supplier<T> fun = () -> {
			try{
				if(!isStable.compareAndSet(true, false) && !securityOverride){
					throw new DataException(DataException.Type.UNSTABLE, 0, "Failed To " + method + " APIDataObject With Path " + Arrays.toString(getBackendPath().toArray()));
				}
				BackendRequest backendRequest = new BackendRequest(method, BackendRequest.AuthType.BEARER, getBackendPath(), queryParams, payload);
				BackendResult backendResult = backendProcessor.process(backendRequest);
				if(backendResult.getStatusCode() > 299 || backendResult.getStatusCode() < 200){
					throw new DataException(DataException.Type.HTTP, backendResult.getStatusCode(), "Failed To " + method + " APIDataObject With Path " + Arrays.toString(getBackendPath().toArray()));
				}
				if(backendResult.getStatusCode() != 204){
					shadowCopy = backendResult.getPayloadAsJSON();
					fromJSON(shadowCopy);
				}
				lastRequestDuration = backendResult.getRequestDuration();
				switch(method){
					case GET -> this.onRetrieval();
					case POST -> this.onCreation();
					case PUT -> this.onUpdate();
					case DELETE -> this.onDeletion();
				}
				return (T) this;
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
		};
		return new SupplierExecutionAction<>(fun);
	}


	public boolean hasChanges(){
		if(shadowCopy == null){
			return true;
		}
		return !JSONEQ.equals(asJSON(), shadowCopy);
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
				listener.onRetrieval((T) this);
			}
			catch(Exception e){
				logger.error("Uncaught exception on APIDataObject onRetrieval listener " + e);
			}
		}
	}

	protected void onCreation(){
		for(var listener : new ArrayList<>(apiDataEventListeners)){
			try{
				listener.onCreation((T) this);
			}
			catch(Exception e){
				logger.error("Uncaught exception on APIDataObject onCreation listener " + e);
			}
		}
	}

	protected void onUpdate(){
		for(var listener : new ArrayList<>(apiDataEventListeners)){
			try{
				listener.onUpdate((T) this);
			}
			catch(Exception e){
				logger.error("Uncaught exception on APIDataObject onUpdate listener " + e);
			}
		}
	}

	public void onDeletion(){
		for(var listener : new ArrayList<>(apiDataEventListeners)){
			try{
				listener.onDeletion((T) this);
			}
			catch(Exception e){
				logger.error("Uncaught exception on APIDataObject onDeletion listener " + e);
			}
		}
	}

	public void addEventListener(APIDataEventListener<T>... listeners){
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
