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

import de.netbeacon.utils.concurrency.action.ExecutionAction;
import de.netbeacon.utils.concurrency.action.imp.SupplierExecutionAction;
import de.netbeacon.xenia.backend.client.objects.apidata.Channel;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class ChannelCache extends Cache<Long, Channel>{

	private final long guildId;

	public ChannelCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	@CheckReturnValue
	@Override
	public ExecutionAction<Channel> retrieve(Long id, boolean cache){
		Supplier<Channel> fun = () -> {
			try{
				if(contains(id)){
					return get_(id);
				}
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					if(contains(id)){
						return get_(id);
					}
					var entry = new Channel(getBackendProcessor(), guildId, id).get(true).execute();
					if(cache){
						add_(id, entry);
					}
					return entry;
				}
				finally{
					idBasedProvider.get(id).release();
				}
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve Channel", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

	@Override
	public ExecutionAction<Channel> retrieveOrCreate(Long id, boolean cache, Object... other){
		return create(id, cache, other);
	}

	@CheckReturnValue
	@Override
	public ExecutionAction<Channel> create(Long id, boolean cache, Object... other){
		Supplier<Channel> fun = () -> {
			try{
				if(contains(id)){
					return get_(id);
				}
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					if(contains(id)){
						return get_(id);
					}
					var entry = new Channel(getBackendProcessor(), guildId, id).getOrCreate(true).execute();
					if(cache){
						add_(id, entry);
					}
					return entry;
				}
				finally{
					idBasedProvider.get(id).release();
				}
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve / Create Channel", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

	@CheckReturnValue
	@Override
	public ExecutionAction<Void> delete(Long id){
		Supplier<Void> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					remove_(id);
					new Channel(getBackendProcessor(), guildId, id).delete(true).execute();
					return null;
				}
				finally{
					idBasedProvider.get(id).release();
				}
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Channel", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

	@CheckReturnValue
	public ExecutionAction<List<Channel>> retrieveAllFromBackend(boolean cache){
		Supplier<List<Channel>> fun = () -> {
			try{
				BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, List.of("data", "guilds", String.valueOf(guildId), "channels"), new HashMap<>(), null);
				BackendResult backendResult = getBackendProcessor().process(backendRequest);
				if(backendResult.getStatusCode() != 200){
					logger.warn("Failed To Get All Roles From The Backend");
					return null;
				}
				JSONArray channels = backendResult.getPayloadAsJSON().getJSONArray("channels");
				List<Channel> channelList = new ArrayList<>();
				for(int i = 0; i < channels.length(); i++){
					JSONObject jsonObject = channels.getJSONObject(i);
					Channel channel = new Channel(getBackendProcessor(), guildId, jsonObject.getLong("channelId"));
					channel.fromJSON(jsonObject); // manually insert the data
					if(cache){
						add_(channel.getId(), channel); // this will overwrite already existing ones
					}
					channelList.add(channel);
				}
				return channelList;
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve All Channels", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

	@Override
	public void clear(boolean deletion){
		getDataMap().forEach((k, v) -> v.clear(deletion));
		super.clear(deletion);
	}

}
