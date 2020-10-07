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
import java.util.List;
import java.util.Set;

public class Member extends APIDataObject {

    private final long guildId;
    private final long userId;

    private long creationTimestamp;
    private Set<Long> roles = new HashSet<>();

    public Member(BackendProcessor backendProcessor, long guildId, long userId) {
        super(backendProcessor, List.of("data", "guild", String.valueOf(guildId), "member", String.valueOf(userId)));
        this.guildId = guildId;
        this.userId = userId;
    }

    public long getId(){
        return userId;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public Set<Long> getRoles() {
        return roles;
    }

    public void setRoles(Set<Long> roles){
        this.roles = roles;
        update();
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("guildId", guildId)
                .put("userId", userId)
                .put("creationTimestamp", creationTimestamp)
                .put("roles", roles);
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
    }
}
