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

import de.netbeacon.xenia.backend.client.objects.external.Channel;
import de.netbeacon.xenia.backend.client.objects.internal.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;

import java.util.List;
import java.util.Objects;

public class ChannelCache extends Cache<Channel> {

    private final long guildId;

    public ChannelCache(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
    }

    public Channel get(long channelId) throws BackendException {
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
    }

    public List<Channel> retrieveAll() throws BackendException {
        return null;
    }

    public void remove(long channelId){
        removeFromCache(channelId);
    }

    public void delete(long channelId) throws BackendException {
        Channel channel = getFromCache(channelId);
        Objects.requireNonNullElseGet(channel, ()-> new Channel(getBackendProcessor(), guildId, channelId)).delete();
        removeFromCache(channelId);
    }
}
