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

public class ChannelCache extends Cache<Long, Channel> {

    private final long guildId;
    private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();
    private final Logger logger = LoggerFactory.getLogger(ChannelCache.class);

    public ChannelCache(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
    }

    public Channel get(long channelId) throws BackendException {
        try{
            idBasedLockHolder.getLock(channelId).lock();
            Channel channel = getFromCache(channelId);
            if(channel != null){
                return channel;
            }
            channel = new Channel(getBackendProcessor(), guildId, channelId);
            try{
                channel.get();
            }catch (BackendException e){
                if(e.getId() == 404){
                    channel.create();
                }else{
                    throw e;
                }
            }
            addToCache(channelId, channel);
            return channel;
        }finally {
            idBasedLockHolder.getLock(channelId).unlock();
        }
    }

    public List<Channel> retrieveAllFromBackend() throws BackendException {
        try{
            idBasedLockHolder.getLock().writeLock().lock();
            BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.Token, List.of("data", "guilds", String.valueOf(guildId), "channels"),new HashMap<>(), null);
            BackendResult backendResult = getBackendProcessor().process(backendRequest);
            if(backendResult.getStatusCode() != 200){
                logger.warn("Failed To Get All Roles From The Backend");
                return null;
            }
            JSONArray channels = backendResult.getPayloadAsJSON().getJSONArray("channels");
            List<Channel> channelList = new ArrayList<>();
            for(int i = 0; i < channels.length(); i++){
                JSONObject jsonObject = channels.getJSONObject(i);
                Channel channel = new Channel(getBackendProcessor(), guildId, jsonObject.getLong("channelId"));
                channel.fromJSON(jsonObject); // manually insert the data
                addToCache(channel.getId(), channel); // this will overwrite already existing ones
                channelList.add(channel);
            }
            return channelList;
        }finally {
            idBasedLockHolder.getLock().writeLock().unlock();
        }
    }

    public void remove(long channelId){
        removeFromCache(channelId);
    }

    public void delete(long channelId) throws BackendException {
        try{
            idBasedLockHolder.getLock(channelId).lock();
            Channel channel = getFromCache(channelId);
            Objects.requireNonNullElseGet(channel, ()-> new Channel(getBackendProcessor(), guildId, channelId)).delete();
            removeFromCache(channelId);
        }finally {
            idBasedLockHolder.getLock(channelId).unlock();
        }
    }
}
