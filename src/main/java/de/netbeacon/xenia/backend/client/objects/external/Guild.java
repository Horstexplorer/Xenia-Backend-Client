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
import de.netbeacon.xenia.backend.client.objects.cache.misc.NotificationCache;
import de.netbeacon.xenia.backend.client.objects.cache.misc.TagCache;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.List;
import java.util.function.Function;

public class Guild extends APIDataObject {

    private long guildId;

    private long creationTimestamp;
    private String preferredLanguage;

    private final ChannelCache channelCache;
    private final MemberCache memberCache;
    private final RoleCache roleCache;
    private final MiscCaches miscCaches;

    public Guild(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
        this.channelCache = new ChannelCache(backendProcessor, guildId);
        this.memberCache = new MemberCache(backendProcessor, guildId);
        this.roleCache = new RoleCache(backendProcessor, guildId);
        this.miscCaches = new MiscCaches(new TagCache(backendProcessor, guildId), new NotificationCache(backendProcessor, guildId));
        setBackendPath("data", "guilds", (Function<Void, Long>) o -> getId());
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
        lSetPreferredLanguage(preferredLanguage);
        update();
    }

    public void lSetPreferredLanguage(String preferredLanguage){
        this.preferredLanguage = preferredLanguage;
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

    public void initSync(){
        getRoleCache().retrieveAllFromBackend();
        List<Channel> channelList = getChannelCache().retrieveAllFromBackend();
        for(Channel channel : channelList){
            channel.getMessageCache().retrieveAllFromBackend();
        }
        getMemberCache().retrieveAllFromBackend();
        getRoleCache().retrieveAllFromBackend();

        getMiscCaches().getTagCache().retrieveAllFromBackend();
        getMiscCaches().getNotificationCache().retrieveAllFromBackend();
    }

    public void initAsync(){
        getBackendProcessor().getScalingExecutor().execute(this::initSync);
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("guildId", guildId)
                .put("creationTimestamp", creationTimestamp)
                .put("preferredLanguage", preferredLanguage);
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        this.guildId = jsonObject.getLong("guildId");
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        this.preferredLanguage = jsonObject.getString("preferredLanguage");
    }

    public void clear(){
        channelCache.clear();
        memberCache.clear();
        roleCache.clear();
        miscCaches.clear();
    }

    public static class MiscCaches{

        private final TagCache tagCache;
        private final NotificationCache notificationCache;

        public MiscCaches(TagCache tagCache, NotificationCache notificationCache){
            this.tagCache = tagCache;
            this.notificationCache = notificationCache;
        }

        public TagCache getTagCache() {
            return tagCache;
        }

        public NotificationCache getNotificationCache() {
            return notificationCache;
        }

        public void clear(){
            tagCache.clear();
            notificationCache.clear();
        }
    }
}
