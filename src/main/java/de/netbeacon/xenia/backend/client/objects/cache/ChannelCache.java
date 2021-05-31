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

import de.netbeacon.utils.locks.IdBasedLockHolder;
import de.netbeacon.xenia.backend.client.objects.external.Channel;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ChannelCache extends Cache<Long, Channel>{

	private final long guildId;
	private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();
	private final Logger logger = LoggerFactory.getLogger(ChannelCache.class);

	public ChannelCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	public Channel get(long channelId) throws CacheException, DataException{
		return get(channelId, true, false);
	}

	public Channel get(long channelId, boolean init) throws CacheException, DataException{
		return get(channelId, init, false);
	}

	public Channel get(long channelId, boolean init, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(channelId).lock();
			Channel channel = getFromCache(channelId);
			if(channel != null){
				return channel;
			}
			channel = new Channel(getBackendProcessor(), guildId, channelId);
			try{
				channel.get(securityOverride);
			}
			catch(DataException e){
				if(e.getCode() == 404 && init){
					channel.create(securityOverride);
				}
				else{
					throw e;
				}
			}
			addToCache(channelId, channel);
			return channel;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Get Channel", e);
		}
		finally{
			idBasedLockHolder.getLock(channelId).unlock();
		}
	}

	public void getAsync(long channelId, Consumer<Channel> whenReady, Consumer<Exception> onException){
		getAsync(channelId, whenReady, onException);
	}

	public void getAsync(long channelId, boolean init, Consumer<Channel> whenReady, Consumer<Exception> onException){
		getAsync(channelId, init, whenReady, onException);
	}

	public void getAsync(long channelId, boolean init, boolean securityOverride, Consumer<Channel> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = get(channelId, init, securityOverride);
				if(whenReady != null) whenReady.accept(v);
			}catch(Exception e){
				if(onException != null) onException.accept(e);
			}
		});
	}

	public List<Channel> retrieveAllFromBackend(boolean cacheInsert) throws CacheException, DataException{
		try{
			if(cacheInsert){
				idBasedLockHolder.getLock().writeLock().lock();
			}
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
				if(cacheInsert){
					addToCache(channel.getId(), channel); // this will overwrite already existing ones
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
		finally{
			if(cacheInsert){
				idBasedLockHolder.getLock().writeLock().unlock();
			}
		}
	}

	public void retrieveAllFromBackendAsync(boolean cacheInsert, Consumer<List<Channel>> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = retrieveAllFromBackend(cacheInsert);
				if(whenReady != null) whenReady.accept(v);
			}catch(Exception e){
				if(onException != null) onException.accept(e);
			}
		});
	}

	public void remove(long channelId){
		removeFromCache(channelId);
	}

	public void delete(long channelId) throws CacheException, DataException{
		delete(channelId, false);
	}

	public void delete(long channelId, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(channelId).lock();
			Channel channel = getFromCache(channelId);
			Objects.requireNonNullElseGet(channel, () -> new Channel(getBackendProcessor(), guildId, channelId)).delete(securityOverride);
			removeFromCache(channelId);
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Channel", e);
		}
		finally{
			idBasedLockHolder.getLock(channelId).unlock();
		}
	}

	public void deleteAsync(long channelId, Consumer<Long> whenReady, Consumer<Exception> onException){
		deleteAsync(channelId, false, whenReady, onException);
	}

	public void deleteAsync(long channelId, boolean securityOverride, Consumer<Long> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				delete(channelId, securityOverride);
				if(whenReady != null) whenReady.accept(channelId);
			}catch(Exception e){
				if(onException != null) onException.accept(e);
			}
		});
	}

	@Override
	public void clear(boolean deletion){
		getDataMap().forEach((k, v) -> v.clear(deletion));
		super.clear(deletion);
	}

}
