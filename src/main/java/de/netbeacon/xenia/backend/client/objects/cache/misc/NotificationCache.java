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
import de.netbeacon.xenia.backend.client.objects.apidata.misc.Notification;
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

public class NotificationCache extends Cache<Long, Notification>{

	private final long guildId;

	public NotificationCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	@Override
	public ExecutionAction<Notification> retrieve(Long id, boolean cache){
		Supplier<Notification> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					var entry = get_(id);
					if(entry != null){
						return entry;
					}
					entry = new Notification(getBackendProcessor(), guildId, id).get(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve Notification", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	@Deprecated
	@Override
	public ExecutionAction<Notification> retrieveOrCreate(Long id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(() -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	@Deprecated
	@Override
	public ExecutionAction<Notification> create(Long id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(() -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	public ExecutionAction<Notification> create(long channelId, long userId, long notificationTarget, String notificationMessage){
		Supplier<Notification> fun = () -> {
			try{
				creationLock.lock();
				if(getOrderedKeyMap().size() + 1 > getBackendProcessor().getBackendClient().getLicenseCache().retrieve(guildId, true).execute().getPerk_MISC_NOTIFICATIONS_C()){
					throw new CacheException(CacheException.Type.IS_FULL, "Cache Is Full");
				}
				Notification notification = new Notification(getBackendProcessor(), guildId, -1).lSetInitialData(channelId, userId, notificationTarget, notificationMessage);
				notification.create(true).execute();
				add_(notification.getId(), notification);
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
					new Notification(getBackendProcessor(), guildId, id).delete(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Notification", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	public ExecutionAction<List<Notification>> retrieveAllFromBackend(boolean cache){
		Supplier<List<Notification>> fun = () -> {
			try{
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
					if(cache){
						add_(notification.getId(), notification);
					}
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
		};
		return new SupplierExecutionAction<>(fun);
	}

}
