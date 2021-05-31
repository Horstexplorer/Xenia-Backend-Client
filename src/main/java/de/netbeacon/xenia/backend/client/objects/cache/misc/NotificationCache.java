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
import de.netbeacon.xenia.backend.client.objects.external.misc.Notification;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class NotificationCache extends Cache<Long, Notification>{

	private final long guildId;
	private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();
	private final Logger logger = LoggerFactory.getLogger(NotificationCache.class);
	private final ReentrantLock creationLock = new ReentrantLock();

	public NotificationCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	public Notification get(long notificationId) throws CacheException, DataException{
		return get(notificationId, false);
	}

	public Notification get(long notificationId, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(notificationId).lock();
			if(contains(notificationId)){
				return getFromCache(notificationId);
			}
			Notification notification = new Notification(getBackendProcessor(), guildId, notificationId);
			notification.get(securityOverride);
			addToCache(notificationId, notification);
			return notification;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Get Notification", e);
		}
		finally{
			idBasedLockHolder.getLock(notificationId).unlock();
		}
	}

	public void getAsync(long notificationId, Consumer<Notification> whenReady, Consumer<Exception> onException){
		getAsync(notificationId, false, whenReady, onException);
	}

	public void getAsync(long notificationId, boolean securityOverride, Consumer<Notification> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = get(notificationId, securityOverride);
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

	public List<Notification> retrieveAllFromBackend() throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock().writeLock().lock();
			BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, List.of("data", "guilds", String.valueOf(guildId), "misc", "notifications"), new HashMap<>(), null);
			BackendResult backendResult = getBackendProcessor().process(backendRequest);
			if(backendResult.getStatusCode() != 200){
				logger.warn("Failed To Get Notifications From The Backend");
				return null;
			}
			JSONArray notifications = backendResult.getPayloadAsJSON().getJSONArray("notifications");
			List<Notification> notificationList = new ArrayList<>();
			for(int i = 0; i < notifications.length(); i++){
				JSONObject jsonObject = notifications.getJSONObject(i);
				Notification notification = new Notification(getBackendProcessor(), guildId, -1);
				notification.fromJSON(jsonObject);
				addToCache(notification.getId(), notification);
				notificationList.add(notification);
			}
			return notificationList;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve All Notifications", e);
		}
		finally{
			idBasedLockHolder.getLock().writeLock().unlock();
		}
	}

	public void retrieveAllFromBackendAsync(Consumer<List<Notification>> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = retrieveAllFromBackend();
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

	public Notification create(long channelId, long userId, long notificationTarget, String notificationMessage) throws CacheException, DataException{
		return create(channelId, userId, notificationTarget, notificationMessage, false);
	}

	public Notification create(long channelId, long userId, long notificationTarget, String notificationMessage, boolean securityOverride) throws CacheException, DataException{
		try{
			creationLock.lock();
			if(getOrderedKeyMap().size() + 1 > getBackendProcessor().getBackendClient().getLicenseCache().get(guildId).getPerk_MISC_NOTIFICATIONS_C()){
				throw new CacheException(CacheException.Type.IS_FULL, "Cache Is Full");
			}
			Notification notification = new Notification(getBackendProcessor(), guildId, -1).lSetInitialData(channelId, userId, notificationTarget, notificationMessage);
			notification.create(securityOverride);
			addToCache(notification.getId(), notification);
			return notification;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Create A New Notification", e);
		}
		finally{
			creationLock.unlock();
		}
	}

	public void createAsync(long channelId, long userId, long notificationTarget, String notificationMessage, Consumer<Notification> whenReady, Consumer<Exception> onException){
		createAsync(channelId, userId, notificationTarget, notificationMessage, false, whenReady, onException);
	}

	public void createAsync(long channelId, long userId, long notificationTarget, String notificationMessage, boolean securityOverride, Consumer<Notification> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = create(channelId, userId, notificationTarget, notificationMessage, securityOverride);
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

	public void remove(long notificationId){
		removeFromCache(notificationId);
	}

	public void delete(long notificationId) throws CacheException, DataException{
		delete(notificationId, false);
	}

	public void delete(long notificationId, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(notificationId).lock();
			Notification notification = getFromCache(notificationId);
			Objects.requireNonNullElseGet(notification, () -> new Notification(getBackendProcessor(), guildId, notificationId)).delete(securityOverride);
			removeFromCache(notificationId);
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Notification", e);
		}
		finally{
			idBasedLockHolder.getLock(notificationId).unlock();
		}
	}

	public void deleteAsync(long notificationId, Consumer<Long> whenReady, Consumer<Exception> onException){
		deleteAsync(notificationId, false, whenReady, onException);
	}

	public void deleteAsync(long notificationId, boolean securityOverride, Consumer<Long> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				delete(notificationId, securityOverride);
				if(whenReady != null){
					whenReady.accept(notificationId);
				}
			}
			catch(Exception e){
				if(onException != null){
					onException.accept(e);
				}
			}
		});
	}

}
