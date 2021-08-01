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
import de.netbeacon.utils.concurrency.action.ExecutionException;
import de.netbeacon.utils.concurrency.action.imp.SupplierExecutionAction;
import de.netbeacon.xenia.backend.client.objects.external.Channel;
import de.netbeacon.xenia.backend.client.objects.external.Message;
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

public class MessageCache extends Cache<Long, Message>{

	private final long guildId;
	private final long channelId;
	private final HashMap<String, Message> lastMap = new HashMap<>();

	public MessageCache(BackendProcessor backendProcessor, long guildId, long channelId){
		super(backendProcessor);
		this.guildId = guildId;
		this.channelId = channelId;
	}

	@Override
	public ExecutionAction<Message> retrieve(Long id, boolean cache){
		Supplier<Message> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					var entry = get_(id);
					if(entry != null){
						return entry;
					}
					entry = new Message(getBackendProcessor(), guildId, channelId, id).get(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve Message", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	@Deprecated
	@Override
	public ExecutionAction<Message> retrieveOrCreate(Long id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(() -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	@Deprecated
	@Override
	public ExecutionAction<Message> create(Long id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(() -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	public ExecutionAction<Message> create(long id, long creationTime, long userId, String messageContent, List<String> attachmentUrls){
		Supplier<Message> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					if(contains(id)){
						return get_(id);
					}
					Message message = new Message(getBackendProcessor(), guildId, channelId, id)
						.lSetInitialData(userId, creationTime, messageContent, attachmentUrls, getBackendProcessor().getBackendClient().getBackendSettings().getMessageCryptKey());
					message.create(true).queue(); // can be async as we process a lot of em
					add_(id, message);
					return message;
				}
				finally{
					idBasedProvider.get(id).release();
				}
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Create Message", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	@Override
	public ExecutionAction<Void> delete(Long id){
		Supplier<Void> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					remove_(id);
					new Message(getBackendProcessor(), guildId, channelId, id).delete(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Message", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	public ExecutionAction<List<Message>> retrieveAllFromBackend(boolean enforceLimit, boolean cache){
		Supplier<List<Message>> fun = () -> {
			try{
				int limit = getBackendProcessor().getBackendClient().getLicenseCache().retrieve(guildId, true).execute().getPerk_CHANNEL_LOGGING_C();
				HashMap<String, String> hashMap = new HashMap<>();
				if(enforceLimit){
					hashMap.put("limit", String.valueOf(limit));
				}
				BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, List.of("data", "guilds", String.valueOf(guildId), "channels", String.valueOf(channelId), "messages"), hashMap, null);
				BackendResult backendResult = getBackendProcessor().process(backendRequest);
				if(backendResult.getStatusCode() != 200){
					logger.warn("Failed To Get " + limit + " Messages From The Backend");
					return null;
				}
				JSONArray messages = backendResult.getPayloadAsJSON().getJSONArray("messages");
				List<Message> messageList = new ArrayList<>();
				for(int i = 0; i < messages.length(); i++){
					JSONObject jsonObject = messages.getJSONObject(i);
					Message message = new Message(getBackendProcessor(), guildId, channelId, jsonObject.getLong("messageId"));
					message.fromJSON(jsonObject);
					if(cache){
						add_(message.getId(), message);
					}
					messageList.add(message);
				}
				return messageList;
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve All Messages", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	public void setLast(String type, long messageId){
		Message message = get_(messageId);
		if(message == null){
			return;
		}
		lastMap.put(type.toLowerCase(), message);
	}

	public Message getLast(String type){
		return lastMap.get(type.toLowerCase());
	}

	@Override
	public Message add_(Long id, Message message){
		super.add_(id, message);
		// remove entries which are too much
		int defaultLimit = getBackendProcessor().getBackendClient().getLicenseCache().retrieve(guildId, true).execute().getPerk_CHANNEL_LOGGING_C();
		int limit = (message.getChannel().getD43Z1Settings().has(Channel.D43Z1Settings.Settings.ACTIVE) && message.getChannel().getD43Z1Settings().has(Channel.D43Z1Settings.Settings.ENABLE_SELF_LEARNING))
			? defaultLimit * 2 : defaultLimit;
		while(getOrderedKeyMap().size() > limit){
			var objTD = getOrderedKeyMap().get(0);
			if(objTD != null){
				remove_(objTD);
			}
		}
		return message;
	}

}
