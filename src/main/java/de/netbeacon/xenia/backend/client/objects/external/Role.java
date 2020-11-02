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
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Role extends APIDataObject {

    private final long guildId;
    private final long roleId;

    private String roleName;
    private final List<Permission> permissions = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(Role.class);

    public Role(BackendProcessor backendProcessor, long guildId, long roleId) {
        super(backendProcessor);
        this.guildId = guildId;
        this.roleId = roleId;
        setBackendPath("data", "guilds", this.guildId, "roles", this.roleId);
    }

    public long getId(){
        return roleId;
    }

    public String getRoleName(){
        return roleName;
    }

    public void setRoleName(String name){
        lSetRoleName(name);
        update();
    }

    public void lSetRoleName(String name){
        this.roleName = roleName;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        JSONArray jsonArray = new JSONArray();
        for(Permission permission : permissions){
            jsonArray.put(permission.asJSON());
        }
        return new JSONObject()
                .put("guildId", guildId)
                .put("roleId", roleId)
                .put("roleName", roleName)
                .put("permissions", jsonArray);
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        if((jsonObject.getLong("guildId") != guildId) || (jsonObject.getLong("roleId") != roleId)){
            throw new JSONSerializationException("Object Do Not Match");
        }
        this.roleName = jsonObject.getString("roleName");
        permissions.clear();
        for(int i = 0; i < jsonObject.getJSONArray("permissions").length(); i++){
            int permId = jsonObject.getJSONArray("permissions").getJSONObject(i).getInt("permissionId");
            Permission permission = new Permission(getBackendProcessor(), guildId, roleId, permId);
            try{
                permission.get();
                permissions.add(permission);
            }catch (Exception e){
                logger.error("Failed To Load Permission "+guildId+" "+roleId+" "+permId);
            }
        }
    }
}
