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

import de.netbeacon.utils.locks.IdBasedLockHolder;
import de.netbeacon.xenia.backend.client.objects.external.misc.TwitchNotification;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class TwitchNotificationCache extends Cache<Long, TwitchNotification> {

    private final long guildId;
    private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();
    private final Logger logger = LoggerFactory.getLogger(TwitchNotificationCache.class);

    public TwitchNotificationCache(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
    }

    public TwitchNotification get(long twitchNotificationId) throws CacheException, DataException {
        return get(twitchNotificationId, true);
    }

    public TwitchNotification get(long twitchNotificationId, boolean securityOverride) throws CacheException, DataException {
        try{
            idBasedLockHolder.getLock(twitchNotificationId).lock();
            if(contains(twitchNotificationId)){
                return getFromCache(twitchNotificationId);
            }
            TwitchNotification tnotific = new TwitchNotification(getBackendProcessor(), guildId, twitchNotificationId);
            tnotific.get(securityOverride);
            addToCache(tnotific.getChannelId(), tnotific);
            return tnotific;
        }catch (CacheException | DataException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Get Twitch Notification", e);
        }finally {
            idBasedLockHolder.getLock(twitchNotificationId).unlock();
        }
    }

    public List<TwitchNotification> retrieveAllFromBackend() throws CacheException, DataException {
        try{
            idBasedLockHolder.getLock().writeLock().lock();
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
                addToCache(tnotific.getId(), tnotific);
                notificationList.add(tnotific);
            }
            return notificationList;
        }catch (CacheException | DataException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve All Twitch Notifications", e);
        }finally {
            idBasedLockHolder.getLock().writeLock().unlock();
        }
    }

    private final ReentrantLock creationLock = new ReentrantLock();

    public TwitchNotification createNew(long channelId, String twitchName, String customMessage) throws CacheException, DataException{
        return createNew(channelId, twitchName, customMessage, false);
    }

    public TwitchNotification createNew(long channelId, String twitchName, String customMessage, boolean securityOverride) throws CacheException, DataException{
        try{
            creationLock.lock();
            if(getOrderedKeyMap().size()+1 > getBackendProcessor().getBackendClient().getLicenseCache().get(guildId).getPerk_MISC_TWITCHNOTIFICATIONS_C()){
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
            tnotific.create(securityOverride);
            addToCache(tnotific.getId(), tnotific);
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
        }catch (CacheException | DataException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Create A New Twitch Notification", e);
        }finally {
            creationLock.unlock();
        }
    }

    public void remove(long twitchNotificationId){
        removeFromCache(twitchNotificationId);
    }

    public void delete(long twitchNotificationId) throws CacheException, DataException{
        delete(twitchNotificationId, false);
    }

    public void delete(long twitchNotificationId, boolean securityOverride) throws CacheException, DataException{
        try{
            idBasedLockHolder.getLock(twitchNotificationId).lock();
            TwitchNotification tnotific = getFromCache(twitchNotificationId);
            Objects.requireNonNullElseGet(tnotific, () -> new TwitchNotification(getBackendProcessor(), guildId, twitchNotificationId)).delete(securityOverride);
            removeFromCache(tnotific.getId());
        }catch (CacheException | DataException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete A Twitch Notification", e);
        }finally {
            idBasedLockHolder.getLock(twitchNotificationId).unlock();
        }
    }

}
