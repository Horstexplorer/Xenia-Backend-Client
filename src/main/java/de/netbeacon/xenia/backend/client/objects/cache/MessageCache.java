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
import de.netbeacon.xenia.backend.client.objects.external.Message;
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

public class MessageCache extends Cache<Long, Message>{

	private final long guildId;
	private final long channelid;
	private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();
	private final Logger logger = LoggerFactory.getLogger(MessageCache.class);
	private final HashMap<String, Message> lastMap = new HashMap<>();

	public MessageCache(BackendProcessor backendProcessor, long guildId, long channelId){
		super(backendProcessor);
		this.guildId = guildId;
		this.channelid = channelId;
	}

	public Message get(long messageId) throws CacheException, DataException{
		return get(messageId, false);
	}

	public Message get(long messageId, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(messageId).lock();
			Message message = getFromCache(messageId);
			if(message != null){
				return message;
			}
			message = new Message(getBackendProcessor(), guildId, channelid, messageId);
			try{
				message.get(securityOverride);
			}
			catch(DataException e){
				if(e.getCode() == 404){
					return null;
				}
				else{
					throw e;
				}
			}
			addToCache(messageId, message);
			return message;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Get Message", e);
		}
		finally{
			idBasedLockHolder.getLock(messageId).unlock();
		}
	}

	public void getAsync(long messageId, Consumer<Message> whenReady, Consumer<Exception> onException){
		getAsync(messageId, false, whenReady, onException);
	}

	public void getAsync(long messageId, boolean securityOverride, Consumer<Message> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = get(messageId, securityOverride);
				if(whenReady != null){
					whenReady.accept(v);
				}
			}
			catch(Exception e){
				if(onException != null){
					onException.accept(e);
				}
			}
		});
	}

	public Message create(long messageId, long creationTime, long userId, String messageContent, List<String> attachmentUrls) throws CacheException, DataException{
		return create(messageId, creationTime, userId, messageContent, attachmentUrls, false);
	}

	public Message create(long messageId, long creationTime, long userId, String messageContent, List<String> attachmentUrls, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(messageId).lock();
			if(contains(messageId)){
				return getFromCache(messageId);
			}
			Message message = new Message(getBackendProcessor(), guildId, channelid, messageId).lSetInitialData(userId, creationTime, messageContent, attachmentUrls, getBackendProcessor().getBackendClient().getBackendSettings().getMessageCryptKey());
			message.createAsync(securityOverride); // can be async as we process a lot of em
			addToCache(messageId, message);
			return message;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Create Message", e);
		}
		finally{
			idBasedLockHolder.getLock(messageId).unlock();
		}
	}

	public void createAsync(long messageId, long creationTime, long userId, String messageContent, List<String> attachmentUrls, Consumer<Message> whenReady, Consumer<Exception> onException){
		createAsync(messageId, creationTime, userId, messageContent, attachmentUrls, false, whenReady, onException);
	}

	public void createAsync(long messageId, long creationTime, long userId, String messageContent, List<String> attachmentUrls, boolean securityOverride, Consumer<Message> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = create(messageId, creationTime, userId, messageContent, attachmentUrls, securityOverride);
				if(whenReady != null){
					whenReady.accept(v);
				}
			}
			catch(Exception e){
				if(onException != null){
					onException.accept(e);
				}
			}
		});
	}

	public List<Message> retrieveAllFromBackend(boolean enforceLimit, boolean cacheInsert) throws CacheException, DataException{
		try{
			if(cacheInsert){
				idBasedLockHolder.getLock().writeLock().lock();
			}
			int limit = getBackendProcessor().getBackendClient().getLicenseCache().get(guildId).getPerk_CHANNEL_LOGGING_C();
			HashMap<String, String> hashMap = new HashMap<>();
			if(enforceLimit){
				hashMap.put("limit", String.valueOf(limit));
			}
			BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, List.of("data", "guilds", String.valueOf(guildId), "channels", String.valueOf(channelid), "messages"), hashMap, null);
			BackendResult backendResult = getBackendProcessor().process(backendRequest);
			if(backendResult.getStatusCode() != 200){
				logger.warn("Failed To Get " + limit + " Messages From The Backend");
				return null;
			}
			JSONArray messages = backendResult.getPayloadAsJSON().getJSONArray("messages");
			List<Message> messageList = new ArrayList<>();
			for(int i = 0; i < messages.length(); i++){
				JSONObject jsonObject = messages.getJSONObject(i);
				Message message = new Message(getBackendProcessor(), guildId, channelid, jsonObject.getLong("messageId"));
				message.fromJSON(jsonObject);
				if(cacheInsert){
					addToCache(message.getId(), message);
				}
				messageList.add(message);
			}
			return messageList;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve Messages", e);
		}
		finally{
			if(cacheInsert){
				idBasedLockHolder.getLock().writeLock().unlock();
			}
		}
	}

	public void retrieveAllFromBackendAsync(boolean enforceLimit, boolean cacheInsert, Consumer<List<Message>> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = retrieveAllFromBackend(enforceLimit, cacheInsert);
				if(whenReady != null){
					whenReady.accept(v);
				}
			}
			catch(Exception e){
				if(onException != null){
					onException.accept(e);
				}
			}
		});
	}

	public void remove(long messageId){
		removeFromCache(messageId);
	}

	public void delete(long messageId) throws CacheException, DataException{
		delete(messageId, false);
	}

	public void delete(long messageId, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(messageId).lock();
			Message message = getFromCache(messageId);
			Objects.requireNonNullElseGet(message, () -> new Message(getBackendProcessor(), guildId, channelid, messageId)).delete(securityOverride);
			removeFromCache(messageId);
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Message", e);
		}
		finally{
			idBasedLockHolder.getLock(messageId).unlock();
		}
	}

	public void deleteAsync(long messageId, Consumer<Long> whenReady, Consumer<Exception> onException){
		deleteAsync(messageId, false, whenReady, onException);
	}

	public void deleteAsync(long messageId, boolean securityOverride, Consumer<Long> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				delete(messageId, securityOverride);
				if(whenReady != null){
					whenReady.accept(messageId);
				}
			}
			catch(Exception e){
				if(onException != null){
					onException.accept(e);
				}
			}
		});
	}

	public void setLast(String type, long messageId){
		Message message = get(messageId);
		if(message == null){
			return;
		}
		lastMap.put(type.toLowerCase(), message);
	}

	public Message getLast(String type){
		return lastMap.get(type.toLowerCase());
	}

	@Override
	public Message addToCache(Long id, Message message){
		super.addToCache(id, message);
		// remove entries which are too much
		int defaultLimit = getBackendProcessor().getBackendClient().getLicenseCache().get(guildId).getPerk_CHANNEL_LOGGING_C();
		int limit = (message.getChannel().getD43Z1Settings().has(Channel.D43Z1Settings.Settings.ACTIVE) && message.getChannel().getD43Z1Settings().has(Channel.D43Z1Settings.Settings.ENABLE_SELF_LEARNING))
			? defaultLimit * 2 : defaultLimit;
		while(getOrderedKeyMap().size() > limit){
			var objTD = getOrderedKeyMap().get(0);
			if(objTD != null){
				removeFromCache(objTD);
			}
		}
		return message;
	}

}
