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

package de.netbeacon.xenia.backend.client.objects.cache.misc;

import de.netbeacon.utils.locks.IdBasedLockHolder;
import de.netbeacon.xenia.backend.client.objects.external.misc.Tag;
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
import java.util.function.Consumer;

public class TagCache extends Cache<String, Tag>{

	private final long guildId;
	private final IdBasedLockHolder<String> idBasedLockHolder = new IdBasedLockHolder<>();
	private final Logger logger = LoggerFactory.getLogger(TagCache.class);

	public TagCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	public Tag get(String tagName) throws CacheException, DataException{
		return get(tagName, false);
	}

	public Tag get(String tagName, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(tagName).lock();
			if(contains(tagName)){
				return getFromCache(tagName);
			}
			Tag tag = new Tag(getBackendProcessor(), guildId, tagName);
			tag.get(securityOverride);
			addToCache(tagName, tag);
			return tag;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Get Tag", e);
		}
		finally{
			idBasedLockHolder.getLock(tagName).unlock();
		}
	}

	public void getAsync(String tagName, Consumer<Tag> whenReady, Consumer<Exception> onException){
		getAsync(tagName, false, whenReady, onException);
	}

	public void getAsync(String tagName, boolean securityOverride, Consumer<Tag> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = get(tagName, securityOverride);
				if(whenReady != null) whenReady.accept(v);
			}catch(Exception e){
				if(onException != null) onException.accept(e);
			}
		});
	}

	public List<Tag> retrieveAllFromBackend() throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock().writeLock().lock();
			BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, List.of("data", "guilds", String.valueOf(guildId), "misc", "tags"), new HashMap<>(), null);
			BackendResult backendResult = getBackendProcessor().process(backendRequest);
			if(backendResult.getStatusCode() != 200){
				logger.warn("Failed To Get Tags From The Backend");
				return null;
			}
			JSONArray tags = backendResult.getPayloadAsJSON().getJSONArray("tags");
			List<Tag> tagList = new ArrayList<>();
			for(int i = 0; i < tags.length(); i++){
				JSONObject jsonObject = tags.getJSONObject(i);
				Tag tag = new Tag(getBackendProcessor(), guildId, jsonObject.getString("tagName"));
				tag.fromJSON(jsonObject);
				addToCache(tag.getId(), tag);
				tagList.add(tag);
			}
			return tagList;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve All Tags", e);
		}
		finally{
			idBasedLockHolder.getLock().writeLock().unlock();
		}
	}

	public void retrieveAllFromBackendAsync(Consumer<List<Tag>> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = retrieveAllFromBackend();
				if(whenReady != null) whenReady.accept(v);
			}catch(Exception e){
				if(onException != null) onException.accept(e);
			}
		});
	}

	public Tag create(String tagName, long userId, String content) throws CacheException, DataException{
		return create(tagName, userId, content, false);
	}

	public Tag create(String tagName, long userId, String content, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(tagName).lock();
			if(getOrderedKeyMap().size() + 1 > getBackendProcessor().getBackendClient().getLicenseCache().get(guildId).getPerk_MISC_TAGS_C()){
				throw new CacheException(CacheException.Type.IS_FULL, "Cache Is Full");
			}
			if(contains(tagName)){
				throw new CacheException(CacheException.Type.ALREADY_EXISTS, "Tag Already Exists");
			}
			Tag tag = new Tag(getBackendProcessor(), guildId, tagName).lSetInitialData(userId, content);
			tag.create(securityOverride); // fails if a tag already exists on the backend which hasnt synced already with the client
			addToCache(tagName, tag);
			return tag;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Create A New Tag", e);
		}
		finally{
			idBasedLockHolder.getLock(tagName).unlock();
		}
	}

	public void createAsync(String tagName, long userId, String content, Consumer<Tag> whenReady, Consumer<Exception> onException){
		createAsync(tagName, userId, content, false, whenReady, onException);
	}

	public void createAsync(String tagName, long userId, String content, boolean securityOverride, Consumer<Tag> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = create(tagName, userId, content, securityOverride);
				if(whenReady != null) whenReady.accept(v);
			}catch(Exception e){
				if(onException != null) onException.accept(e);
			}
		});
	}

	public void remove(String tagName){
		removeFromCache(tagName);
	}

	public void delete(String tagName) throws CacheException, DataException{
		delete(tagName, false);
	}

	public void delete(String tagName, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(tagName).lock();
			Tag tag = getFromCache(tagName);
			if(tag == null){
				tag = new Tag(getBackendProcessor(), guildId, tagName);
				tag.delete(securityOverride);
			}
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete A Tag", e);
		}
		finally{
			idBasedLockHolder.getLock(tagName).unlock();
		}
	}

	public void deleteAsync(String tagName, Consumer<String> whenReady, Consumer<Exception> onException){
		deleteAsync(tagName, false, whenReady, onException);
	}

	public void deleteAsync(String tagName, boolean securityOverride, Consumer<String> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				delete(tagName, securityOverride);
				if(whenReady != null) whenReady.accept(tagName);
			}catch(Exception e){
				if(onException != null) onException.accept(e);
			}
		});
	}

}
