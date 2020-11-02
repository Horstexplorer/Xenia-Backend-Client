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
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class Member extends APIDataObject {

    private final long guildId;
    private final long userId;
    private long creationTimestamp;
    private Set<Long> roles = new HashSet<>();
    // meta data - initialize with values
    private String metaNickname = "unknown_nickname";
    private boolean metaIsAdministrator = false;
    private boolean metaIsOwner = false;

    public Member(BackendProcessor backendProcessor, long guildId, long userId) {
        super(backendProcessor);
        this.guildId = guildId;
        this.userId = userId;
        setBackendPath("data", "guilds", this.guildId, "members", this.userId);
    }

    public long getId(){
        return userId;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public Set<Long> getRoleIds() {
        return roles;
    }

    public void lSetMetaData(String nickname, boolean isAdministrator, boolean isOwner){
        this.metaNickname = nickname;
        this.metaIsOwner = isOwner;
        this.metaIsAdministrator = isAdministrator;
    }

    public void setMetaData(String nickname, boolean isAdministrator, boolean isOwner){
        lSetMetaData(nickname, isAdministrator, isOwner);
        update();
    }

    public String metaNickname() {
        return metaNickname;
    }

    public boolean metaIsAdministrator() {
        return metaIsAdministrator;
    }

    public boolean metaIsOwner() {
        return metaIsOwner;
    }

    public void setRoleIds(Set<Long> roles){
        lSetRoleIds(roles);
        update();
    }

    public void lSetRoleIds(Set<Long> roles){
        this.roles = roles;
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("guildId", guildId)
                .put("userId", userId)
                .put("creationTimestamp", creationTimestamp)
                .put("roles", roles)
                .put("meta", new JSONObject()
                        .put("nickname", metaNickname)
                        .put("isAdministrator", metaIsAdministrator)
                        .put("isOwner", metaIsOwner)
                );
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        if((jsonObject.getLong("guildId") != guildId) || (jsonObject.getLong("userId") != userId)){
            throw new JSONSerializationException("Object Do Not Match");
        }
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        for(int i = 0; i < jsonObject.getJSONArray("roles").length(); i++){
            this.roles.add(jsonObject.getJSONArray("roles").getLong(i));
        }
        JSONObject meta = jsonObject.getJSONObject("meta");
        this.metaNickname = meta.getString("nickname");
        this.metaIsAdministrator = meta.getBoolean("isAdministrator");
        this.metaIsOwner = meta.getBoolean("isOwner");
    }
}
