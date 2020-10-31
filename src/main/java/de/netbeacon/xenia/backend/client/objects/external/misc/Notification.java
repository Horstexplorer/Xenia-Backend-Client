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

package de.netbeacon.xenia.backend.client.objects.external.misc;

import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.List;

public class Notification extends APIDataObject {

    private long notificationId;
    private long creationTimestamp;
    private long guildId;
    private long channelId;
    private long userId;
    private long notificationTarget;
    private String notificationMessage;

    public Notification(BackendProcessor backendProcessor, long guildId, long notificationId) {
        super(backendProcessor, List.of("data", "guilds", String.valueOf(guildId), "misc", "notifications", String.valueOf(notificationId)));
    }

    public Notification setInitialData(long channelId, long userId, long notificationTarget, String notificationMessage){
        this.channelId = channelId;
        this.userId = userId;
        this.notificationTarget = notificationTarget;
        this.notificationMessage = notificationMessage;
        return this;
    }

    public long getId(){
        return notificationId;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getUserId() {
        return userId;
    }

    public long getNotificationTarget() {
        return notificationTarget;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationTarget(long notificationTarget) {
        this.notificationTarget = notificationTarget;
        update();
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
        update();
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("notificationId", notificationId)
                .put("creationTimestamp", creationTimestamp)
                .put("guildId", guildId)
                .put("channelId", channelId)
                .put("userId", userId)
                .put("notificationTarget", notificationTarget)
                .put("notificationMessage", notificationMessage);
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        this.notificationId = jsonObject.getLong("notificationId");
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        this.guildId = jsonObject.getLong("guildId");
        this.channelId = jsonObject.getLong("channelId");
        this.userId = jsonObject.getLong("userId");
        this.notificationTarget = jsonObject.getLong("notificationTarget");
        this.notificationMessage = jsonObject.getString("notificationMessage");
    }
}