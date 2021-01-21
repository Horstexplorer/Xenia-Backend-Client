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
import de.netbeacon.xenia.backend.client.objects.cache.misc.TwitchNotificationCache;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.List;
import java.util.function.Function;

public class Guild extends APIDataObject {

    private long guildId;

    private long creationTimestamp;
    private String preferredLanguage;
    private boolean useVPerms;
    private String prefix;
    // meta data - initialize with values
    private String metaGuildName = "unknown_name";
    private String metaIconUrl = null;

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
        this.miscCaches = new MiscCaches(new TagCache(backendProcessor, guildId), new NotificationCache(backendProcessor, guildId), new TwitchNotificationCache(backendProcessor, guildId));
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

    public boolean useVPerms(){
        return useVPerms;
    }

    public void setVPermsEnabled(boolean value){
        lSetVPermsEnabled(value);
        update();
    }

    public void lSetVPermsEnabled(boolean value){
        this.useVPerms = value;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix){
        lSetPrefix(prefix);
        update();
    }

    public void lSetPrefix(String prefix){
        this.prefix = prefix;
    }

    public String getMetaGuildName(){
        return metaGuildName;
    }

    public String getMetaIconUrl(){
        return metaIconUrl;
    }

    public void lSetMetaData(String guildName, String iconUrl){
        this.metaGuildName = guildName;
        this.metaIconUrl = iconUrl;
    }

    public void setMetaData(String metaGuildName, String iconUrl){
        lSetMetaData(metaGuildName, iconUrl);
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

    public void initSync(){
        List<Channel> channelList = getChannelCache().retrieveAllFromBackend(true);
        for(Channel channel : channelList){
            channel.getMessageCache().retrieveAllFromBackend(true, true);
        }
        getMemberCache().retrieveAllFromBackend(true);
        getRoleCache().retrieveAllFromBackend(true);

        getMiscCaches().getTagCache().retrieveAllFromBackend();
        getMiscCaches().getNotificationCache().retrieveAllFromBackend();
        getMiscCaches().getTwitchNotificationCache().retrieveAllFromBackend();
    }

    public void initAsync(){
        getBackendProcessor().getScalingExecutor().execute(this::initSync);
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("guildId", guildId)
                .put("creationTimestamp", creationTimestamp)
                .put("preferredLanguage", preferredLanguage)
                .put("prefix", prefix)
                .put("useVPerms", useVPerms)
                .put("meta", new JSONObject()
                        .put("name", metaGuildName)
                        .put("iconUrl", metaIconUrl != null ? metaIconUrl : JSONObject.NULL)
                );
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        this.guildId = jsonObject.getLong("guildId");
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        this.preferredLanguage = jsonObject.getString("preferredLanguage");
        this.prefix = jsonObject.getString("prefix");
        this.useVPerms = jsonObject.getBoolean("useVPerms");

        JSONObject meta = jsonObject.getJSONObject("meta");
        this.metaGuildName = meta.getString("name");
        this.metaIconUrl = meta.get("iconUrl") != JSONObject.NULL ? meta.getString("iconUrl") : null;
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
        private final TwitchNotificationCache twitchNotificationCache;

        public MiscCaches(TagCache tagCache, NotificationCache notificationCache, TwitchNotificationCache twitchNotificationCache){
            this.tagCache = tagCache;
            this.notificationCache = notificationCache;
            this.twitchNotificationCache = twitchNotificationCache;
        }

        public TagCache getTagCache() {
            return tagCache;
        }

        public NotificationCache getNotificationCache() {
            return notificationCache;
        }

        public TwitchNotificationCache getTwitchNotificationCache() {
            return twitchNotificationCache;
        }

        public void clear(){
            tagCache.clear();
            notificationCache.clear();
            twitchNotificationCache.clear();
        }
    }
}
