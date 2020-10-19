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
import de.netbeacon.xenia.backend.client.objects.cache.ChannelCache;
import de.netbeacon.xenia.backend.client.objects.cache.MemberCache;
import de.netbeacon.xenia.backend.client.objects.cache.RoleCache;
import de.netbeacon.xenia.backend.client.objects.cache.misc.TagCache;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.List;

public class Guild extends APIDataObject {

    private final long guildId;

    private long creationTimestamp;
    private String preferredLanguage;

    private final ChannelCache channelCache;
    private final MemberCache memberCache;
    private final RoleCache roleCache;
    private final MiscCaches miscCaches;

    public Guild(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor, List.of("data", "guilds", String.valueOf(guildId)));
        this.guildId = guildId;
        this.channelCache = new ChannelCache(backendProcessor, guildId);
        this.memberCache = new MemberCache(backendProcessor, guildId);
        this.roleCache = new RoleCache(backendProcessor, guildId);
        this.miscCaches = new MiscCaches(new TagCache(backendProcessor, guildId));
    }

    public long getId(){
        return guildId;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage){
        this.preferredLanguage = preferredLanguage;
        update();
    }

    public ChannelCache getChannelCache() {
        return channelCache;
    }

    public MemberCache getMemberCache() {
        return memberCache;
    }

    public RoleCache getRoleCache() {
        return roleCache;
    }

    public MiscCaches getMiscCaches() { return miscCaches; }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("guildId", guildId)
                .put("creationTimestamp", creationTimestamp)
                .put("preferredLanguage", preferredLanguage);
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        if(jsonObject.getLong("guildId") != guildId){
            throw new JSONSerializationException("Object Do Not Match");
        }
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        this.preferredLanguage = jsonObject.getString("preferredLanguage");
    }

    public static class MiscCaches{

        private final TagCache tagCache;

        public MiscCaches(TagCache tagCache){
            this.tagCache = tagCache;
        }

        public TagCache getTagCache() {
            return tagCache;
        }
    }
}
