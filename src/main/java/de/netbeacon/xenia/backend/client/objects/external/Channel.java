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

package de.netbeacon.xenia.backend.client.objects.external;

import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.cache.MessageCache;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.List;

public class Channel extends APIDataObject {

    private final long guildId;
    private final long channelId;

    private long creationTimestamp;
    private boolean accessRestriction;
    private String channelMode;
    private String channelType;
    private boolean tmpLoggingActive;
    private long tmpLoggingChannelId;

    private final MessageCache messageCache;

    public Channel(BackendProcessor backendProcessor, long guildId, long channelId) {
        super(backendProcessor, List.of("data", "guilds", String.valueOf(guildId), "channels", String.valueOf(channelId)));
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageCache = new MessageCache(backendProcessor, guildId, channelId);
    }

    public long getId(){
        return channelId;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public boolean isAccessRestricted(){
        return accessRestriction;
    }

    public String getChannelMode(){
        return channelMode;
    }

    public String getChannelType(){
        return channelType;
    }

    public boolean tmpLoggingIsActive() {
        return tmpLoggingActive;
    }

    public long getTmpLoggingChannelId(){
        return tmpLoggingChannelId;
    }

    public void setAccessRestriction(boolean accessRestriction) {
        this.accessRestriction = accessRestriction;
        update();
    }

    public void setChannelMode(String channelMode) {
        this.channelMode = channelMode;
        update();
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
        update();
    }

    public void setTmpLoggingActive(boolean tmpLoggingActive) {
        this.tmpLoggingActive = tmpLoggingActive;
        update();
    }

    public void setTmpLoggingChannelId(long tmpLoggingChannelId) {
        this.tmpLoggingChannelId = tmpLoggingChannelId;
        update();
    }

    public MessageCache getMessageCache(){
        return messageCache;
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("guildId", guildId)
                .put("channelId", channelId)
                .put("creationTimestamp", creationTimestamp)
                .put("accessRestriction", accessRestriction)
                .put("channelMode", channelMode)
                .put("channelType", channelType)
                .put("tmpLoggingActive", tmpLoggingActive)
                .put("tmpLoggingChannelId", tmpLoggingChannelId);
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        if((jsonObject.getLong("guildId") != guildId) || (jsonObject.getLong("channelId") != channelId)){
            throw new JSONSerializationException("Object Do Not Match");
        }
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        this.accessRestriction = jsonObject.getBoolean("accessRestriction");
        this.channelMode = jsonObject.getString("channelMode");
        this.channelType = jsonObject.getString("channelType");
        this.tmpLoggingActive = jsonObject.getBoolean("tmpLoggingActive");
        this.tmpLoggingChannelId = jsonObject.getLong("tmpLoggingChannelId");
    }
}
