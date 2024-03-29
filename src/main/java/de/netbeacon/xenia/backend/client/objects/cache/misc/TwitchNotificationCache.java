/*
 *     Copyright 2021 Horstexplorer @ https://www.netbeacon.de
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
import de.netbeacon.xenia.backend.client.objects.apidata.misc.TwitchNotification;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class TwitchNotificationCache extends Cache<Long, TwitchNotification>{

	private final long guildId;

	public TwitchNotificationCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	@CheckReturnValue
	@Override
	public ExecutionAction<TwitchNotification> retrieve(Long id, boolean cache){
		Supplier<TwitchNotification> fun = () -> {
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
					var entry = new TwitchNotification(getBackendProcessor(), guildId, id).get(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve TwitchNotification", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

	@CheckReturnValue
	@Deprecated
	@Override
	public ExecutionAction<TwitchNotification> retrieveOrCreate(Long id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), () -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	@CheckReturnValue
	@Deprecated
	@Override
	public ExecutionAction<TwitchNotification> create(Long id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), () -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	@CheckReturnValue
	public ExecutionAction<TwitchNotification> create(long channelId, String twitchName, String customMessage){
		Supplier<TwitchNotification> fun = () -> {
			try{
				creationLock.lock();
				if(getOrderedKeyMap().size() + 1 > getBackendProcessor().getBackendClient().getLicenseCache().retrieve(guildId, true).execute().getPerk_MISC_TWITCHNOTIFICATIONS_C()){
					throw new CacheException(CacheException.Type.IS_FULL, "Cache Is Full");
				}
				// check if we already know the name
				if(getDataMap().values().stream().anyMatch(tn -> tn.getTwitchChannelName().equalsIgnoreCase(twitchName))){
					throw new CacheException(CacheException.Type.ALREADY_EXISTS, "Twitch Notification Already Exists");
				}
				TwitchNotification tnotific = new TwitchNotification(getBackendProcessor(), guildId, -1).lSetInitialData(twitchName, channelId);
				if(customMessage != null){
					tnotific.lSetNotificationMessage(customMessage);
				}
				tnotific.create(true).execute();
				add_(tnotific.getId(), tnotific);
				// send the funny secondary request to init or destroy this notification
				var secWSL = getBackendProcessor().getBackendClient().getSecondaryWebsocketListener().getWsProcessorCore();
				WSRequest wsRequest = new WSRequest.Builder()
					.action("twitchaccelerator")
					.recipient(0)
					.mode(WSRequest.Mode.UNICAST)
					.payload(new JSONObject().put("guildId", guildId).put("twitchNotificationId", tnotific.getId()))
					.exitOn(WSRequest.ExitOn.INSTANT)
					.build();
				secWSL.process(wsRequest);
				return tnotific;
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Create A New Twitch Notification", e);
			}
			finally{
				creationLock.unlock();
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
					new TwitchNotification(getBackendProcessor(), guildId, id).delete(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete TwitchNotification", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

	@CheckReturnValue
	public ExecutionAction<List<TwitchNotification>> retrieveAllFromBackend(boolean cache){
		Supplier<List<TwitchNotification>> fun = () -> {
			try{
				BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, List.of("data", "guilds", String.valueOf(guildId), "misc", "twitchnotifications"), new HashMap<>(), null);
				BackendResult backendResult = getBackendProcessor().process(backendRequest);
				if(backendResult.getStatusCode() != 200){
					logger.warn("Failed To Get Twitch Notifications From The Backend");
					return null;
				}
				JSONArray notifications = backendResult.getPayloadAsJSON().getJSONArray("twitchNotifications");
				List<TwitchNotification> notificationList = new ArrayList<>();
				for(int i = 0; i < notifications.length(); i++){
					JSONObject jsonObject = notifications.getJSONObject(i);
					TwitchNotification tnotific = new TwitchNotification(getBackendProcessor(), guildId, jsonObject.getLong("twitchNotificationId"));
					tnotific.fromJSON(jsonObject);
					if(cache){
						add_(tnotific.getId(), tnotific);
					}
					notificationList.add(tnotific);
				}
				return notificationList;
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve All Twitch Notifications", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

}
