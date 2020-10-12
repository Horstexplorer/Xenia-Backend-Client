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
import de.netbeacon.xenia.backend.client.objects.external.Message;
import de.netbeacon.xenia.backend.client.objects.internal.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MessageCache extends Cache<Message> {

    private final long guildId;
    private final long channelid;
    private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();
    private final Logger logger = LoggerFactory.getLogger(MessageCache.class);

    public MessageCache(BackendProcessor backendProcessor, long guildId, long channelId) {
        super(backendProcessor);
        this.guildId = guildId;
        this.channelid = channelId;
    }

    public Message get(long messageId){
        try{
            idBasedLockHolder.getLock(messageId).lock();
            Message message = getFromCache(messageId);
            if(message != null){
                return message;
            }
            message = new Message(getBackendProcessor(), guildId, channelid, messageId);
            try{
                message.get();
            }catch (BackendException e){
                if(e.getId() == 404){
                    return null;
                }else{
                    throw e;
                }
            }
            addToCache(messageId, message);
            return message;
        }finally {
            idBasedLockHolder.getLock(messageId).unlock();
        }
    }

    public Message create(long messageId, long userId, String messageContent){
        try{
            idBasedLockHolder.getLock(messageId).lock();
            if(contains(messageId)){
                return getFromCache(messageId);
            }
            Message message = new Message(getBackendProcessor(), guildId, channelid, messageId).setInitialData(userId, messageContent, getBackendProcessor().getBackendClient().getBackendSettings().getMessageCryptKey());
            message.create();
            addToCache(messageId, message);
            return message;
        }finally {
            idBasedLockHolder.getLock(messageId).unlock();
        }
    }

    public List<Message> retrieveAllFromBackend(int limit) throws BackendException {
        try{
            idBasedLockHolder.getLock().writeLock().lock();
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("limit", String.valueOf(limit));
            BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.Token, List.of("data", "guilds", String.valueOf(guildId), "channels", String.valueOf(channelid), "messages"), hashMap, null);
            BackendResult backendResult = getBackendProcessor().process(backendRequest);
            if(backendResult.getStatusCode() != 200){
                logger.warn("Failed To Get "+limit+" Messages From The Backend");
                return null;
            }
            JSONArray messages = backendResult.getPayloadAsJSON().getJSONArray("messages");
            List<Message> messageList = new ArrayList<>();
            for(int i = 0; i < messages.length(); i++){
                JSONObject jsonObject = messages.getJSONObject(i);
                Message message = new Message(getBackendProcessor(), guildId, channelid, jsonObject.getLong("messageId"));
                message.fromJSON(jsonObject);
                addToCache(message.getId(), message);
                messageList.add(message);
            }
            return messageList;
        }finally {
            idBasedLockHolder.getLock().writeLock().unlock();
        }
    }

    public void remove(long messageId){
        removeFromCache(messageId);
    }

    public void delete(long messageId){
        try{
            idBasedLockHolder.getLock(messageId).lock();
            Message message = getFromCache(messageId);
            Objects.requireNonNullElseGet(message, ()->new Message(getBackendProcessor(), guildId, channelid, messageId)).delete();
            removeFromCache(messageId);
        }finally {
            idBasedLockHolder.getLock(messageId).unlock();
        }
    }
}