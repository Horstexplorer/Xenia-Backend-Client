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

public class NotificationCache extends Cache<Long, Notification> {

    private final long guildId;
    private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();
    private final Logger logger = LoggerFactory.getLogger(NotificationCache.class);

    public NotificationCache(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
    }

    public Notification get(long notificationId) throws CacheException {
        try{
            idBasedLockHolder.getLock(notificationId).lock();
            if(contains(notificationId)){
                return getFromCache(notificationId);
            }
            Notification notification = new Notification(getBackendProcessor(), guildId, notificationId);
            notification.get();
            addToCache(notificationId, notification);
            return notification;
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-1, "Failed To Get Notification", e);
        }finally {
            idBasedLockHolder.getLock(notificationId).unlock();
        }
    }

    public List<Notification> retrieveAllFromBackend() throws CacheException {
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
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-11, "Failed To Retrieve All Notifications", e);
        }finally {
            idBasedLockHolder.getLock().writeLock().unlock();
        }
    }

    private final ReentrantLock creationLock = new ReentrantLock();

    public Notification createNew(long channelId, long userId, long notificationTarget, String notificationMessage){
        try{
            creationLock.lock();
            if(getOrderedKeyMap().size()+1 > getBackendProcessor().getBackendClient().getLicenseCache().get(guildId).getPerk_MISC_NOTIFICATIONS_C()){
                throw new RuntimeException("Cache Is Full");
            }
            Notification notification = new Notification(getBackendProcessor(), guildId, -1).lSetInitialData(channelId, userId, notificationTarget, notificationMessage);
            notification.create();
            addToCache(notification.getId(), notification);
            return notification;
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-2, "Failed To Create A New Notification", e);
        }finally {
            creationLock.unlock();
        }
    }

    public void remove(long notificationId){
        removeFromCache(notificationId);
    }

    public void delete(long notificationId){
        try{
            idBasedLockHolder.getLock(notificationId).lock();
            Notification notification = getFromCache(notificationId);
            Objects.requireNonNullElseGet(notification, ()->new Notification(getBackendProcessor(), guildId, notificationId)).delete();
            removeFromCache(notificationId);
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-3, "Failed To Delete Notification", e);
        }finally {
            idBasedLockHolder.getLock(notificationId).unlock();
        }
    }
}
