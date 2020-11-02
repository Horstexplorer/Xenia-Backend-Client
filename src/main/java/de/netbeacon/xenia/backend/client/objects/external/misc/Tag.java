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
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.function.Function;

public class Tag extends APIDataObject {

    private final long guildId;
    private final String tagName;
    private long creationTimestamp;
    private long userId;
    private String tagContent;

    public Tag(BackendProcessor backendProcessor, long guildId, String tagName) {
        super(backendProcessor);
        this.guildId = guildId;
        this.tagName = tagName;
        setBackendPath("data", "guilds", (Function<Void, Long>) o -> getGuildId(), "misc", "tags", (Function<Void, String>) o -> getId());
    }

    public Tag lSetInitialData(long userId, String tagContent){
        this.userId = userId;
        this.tagContent = tagContent;
        return this;
    }

    public String getId(){
        return tagName;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getUserId() {
        return userId;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public String getTagContent() {
        return tagContent;
    }

    public void setTagContent(String tagContent) throws BackendException {
        lSetTagContent(tagContent);
        update();
    }

    public void lSetTagContent(String tagContent) throws BackendException {
        this.tagContent = tagContent;
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("tagName", tagName)
                .put("creationTimestamp", creationTimestamp)
                .put("guildId", guildId)
                .put("userId", userId)
                .put("tagContent", tagContent);
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        this.userId = jsonObject.getLong("userId");
        this.tagContent = jsonObject.getString("tagContent");
    }
}