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

import java.util.function.Function;

public class Channel extends APIDataObject {

    private long guildId;
    private long channelId;

    private long creationTimestamp;
    private boolean accessRestriction;
    private String channelMode;
    private String channelType;
    private boolean tmpLoggingActive;
    private long tmpLoggingChannelId;

    private final MessageCache messageCache;

    public Channel(BackendProcessor backendProcessor, long guildId, long channelId) {
        super(backendProcessor);
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageCache = new MessageCache(backendProcessor, guildId, channelId);
        setBackendPath("data", "guilds", (Function<Void, Long>) o -> getGuildId(), "channels", (Function<Void, Long>) o -> getChannelId());
    }

    public long getId(){
        return channelId;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
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
        lSetAccessRestriction(accessRestriction);
        update();
    }

    public void lSetAccessRestriction(boolean accessRestriction) {
        this.accessRestriction = accessRestriction;
    }

    public void setChannelMode(String channelMode) {
        lSetChannelMode(channelMode);
        update();
    }

    public void lSetChannelMode(String channelMode) {
        this.channelMode = channelMode;
    }

    public void setChannelType(String channelType) {
        lSetChannelType(channelType);
        update();
    }

    public void lSetChannelType(String channelType) {
        this.channelType = channelType;
    }

    public void setTmpLoggingActive(boolean tmpLoggingActive) {
        lSetTmpLoggingActive(tmpLoggingActive);
        update();
    }

    public void lSetTmpLoggingActive(boolean tmpLoggingActive) {
        this.tmpLoggingActive = tmpLoggingActive;
    }

    public void setTmpLoggingChannelId(long tmpLoggingChannelId) {
        lSetTmpLoggingChannelId(tmpLoggingChannelId);
        update();
    }

    public void lSetTmpLoggingChannelId(long tmpLoggingChannelId) {
        this.tmpLoggingChannelId = tmpLoggingChannelId;
    }

    public MessageCache getMessageCache(){
        return messageCache;
    }

    // SECONDARY

    public Guild getGuild(){
        return getBackendProcessor().getBackendClient().getGuildCache().get(guildId);
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
        this.guildId = jsonObject.getLong("guildId");
        this.channelId = jsonObject.getLong("channelId");
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        this.accessRestriction = jsonObject.getBoolean("accessRestriction");
        this.channelMode = jsonObject.getString("channelMode");
        this.channelType = jsonObject.getString("channelType");
        this.tmpLoggingActive = jsonObject.getBoolean("tmpLoggingActive");
        this.tmpLoggingChannelId = jsonObject.getLong("tmpLoggingChannelId");
    }

    public void clear(){
        messageCache.clear();
    }
}
