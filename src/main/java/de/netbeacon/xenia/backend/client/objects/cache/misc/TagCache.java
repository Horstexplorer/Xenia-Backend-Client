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

import de.netbeacon.utils.concurrency.action.ExecutionAction;
import de.netbeacon.utils.concurrency.action.ExecutionException;
import de.netbeacon.utils.concurrency.action.imp.SupplierExecutionAction;
import de.netbeacon.xenia.backend.client.objects.apidata.misc.Tag;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class TagCache extends Cache<String, Tag>{

	private final long guildId;

	public TagCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	@Override
	public ExecutionAction<Tag> retrieve(String id, boolean cache){
		Supplier<Tag> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					var entry = get_(id);
					if(entry != null){
						return entry;
					}
					entry = new Tag(getBackendProcessor(), guildId, id).get(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve Tag", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	@Deprecated
	@Override
	public ExecutionAction<Tag> retrieveOrCreate(String id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(() -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	@Deprecated
	@Override
	public ExecutionAction<Tag> create(String id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(() -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	public ExecutionAction<Tag> create(String tagName, long userId, String content){
		Supplier<Tag> fun = () -> {
			try{
				creationLock.lock();
				if(getOrderedKeyMap().size() + 1 > getBackendProcessor().getBackendClient().getLicenseCache().retrieve(guildId, true).execute().getPerk_MISC_TAGS_C()){
					throw new CacheException(CacheException.Type.IS_FULL, "Cache Is Full");
				}
				if(contains(tagName)){
					throw new CacheException(CacheException.Type.ALREADY_EXISTS, "Tag Already Exists");
				}
				Tag tag = new Tag(getBackendProcessor(), guildId, tagName).lSetInitialData(userId, content);
				tag.create(true).execute(); // fails if a tag already exists on the backend which hasnt synced already with the client
				add_(tagName, tag);
				return tag;
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Create A New Tag", e);
			}
			finally{
				creationLock.unlock();
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	@Override
	public ExecutionAction<Void> delete(String id){
		Supplier<Void> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					remove_(id);
					new Tag(getBackendProcessor(), guildId, id).delete(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Tag", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	public ExecutionAction<List<Tag>> retrieveAllFromBackend(boolean cache){
		Supplier<List<Tag>> fun = () -> {
			try{
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
					if(cache){
						add_(tag.getId(), tag);
					}
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
		};
		return new SupplierExecutionAction<>(fun);
	}

}
