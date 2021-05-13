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
import java.util.function.Supplier;

public class Member extends APIDataObject {

    private long guildId;
    private long userId;
    private long creationTimestamp;
    private Set<Long> roleIDs = new HashSet<>();
    private long levelPoints;
    // meta data - initialize with values
    private String metaNickname = "unknown_nickname";
    private boolean metaIsAdministrator = false;
    private boolean metaIsOwner = false;

    public Member(BackendProcessor backendProcessor, long guildId, long userId) {
        super(backendProcessor);
        this.guildId = guildId;
        this.userId = userId;
        setBackendPath("data", "guilds", (Supplier<Long>) this::getGuildId, "members", (Supplier<Long>) this::getId);
    }

    public long getId() {
        return userId;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public Set<Long> getRoleIds() {
        return roleIDs;
    }

    public long getLevelPoints() { return levelPoints; }

	public void lSetLevelPoints(long levelPoints) {
    	secure();
    	this.levelPoints = levelPoints;
	}

	public void setLevelPoints(long levelPoints) {
		lSetLevelPoints(levelPoints);
		update();
	}

    public void lSetMetaData(String nickname, boolean isAdministrator, boolean isOwner) {
        secure();
        this.metaNickname = nickname;
        this.metaIsOwner = isOwner;
        this.metaIsAdministrator = isAdministrator;
    }

    public void setMetaData(String nickname, boolean isAdministrator, boolean isOwner) {
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

    public void setRoleIds(Set<Long> roles) {
        lSetRoleIds(roles);
        update();
    }

    public void lSetRoleIds(Set<Long> roles) {
        secure();
        this.roleIDs = roles;
    }

    // SECONDARY

    public Guild getGuild() {
        return getBackendProcessor().getBackendClient().getGuildCache().get(guildId, false);
    }

    public User getUser() {
        return getBackendProcessor().getBackendClient().getUserCache().get(userId);
    }

    public Set<Role> getRoles() {
        Guild g = getBackendProcessor().getBackendClient().getGuildCache().get(guildId, false);
        Set<Role> roles = new HashSet<>();
        for (Long l : new HashSet<>(roleIDs)) {
            try {
                roles.add(g.getRoleCache().get(l));
            } catch (Exception e) {
                roleIDs.remove(l);
            }
        }
        return roles;
    }

    public boolean hasPermission(Role.Permissions.Bit... bits) {
        boolean hasAll = false;
        for (Role role : getRoles()) {
            if (role.getPermissions().hasAllPermission(bits)) {
                hasAll = true;
            }
        }
        return hasAll;
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("guildId", guildId)
                .put("userId", userId)
                .put("creationTimestamp", creationTimestamp)
                .put("roles", roleIDs)
	            .put("levelPoints", levelPoints)
                .put("meta", new JSONObject()
                        .put("nickname", metaNickname)
                        .put("isAdministrator", metaIsAdministrator)
                        .put("isOwner", metaIsOwner)
                );
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        this.guildId = jsonObject.getLong("guildId");
        this.userId = jsonObject.getLong("userId");
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        for (int i = 0; i < jsonObject.getJSONArray("roles").length(); i++) {
            this.roleIDs.add(jsonObject.getJSONArray("roles").getLong(i));
        }
	    this.levelPoints = jsonObject.getLong("levelPoints");
        JSONObject meta = jsonObject.getJSONObject("meta");
        this.metaNickname = meta.getString("nickname");
        this.metaIsAdministrator = meta.getBoolean("isAdministrator");
        this.metaIsOwner = meta.getBoolean("isOwner");
    }

}
