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

import java.util.ArrayList;
import java.util.List;

public class Role extends APIDataObject {

    private final long guildId;
    private final long roleId;

    private String roleName;
    private final List<Permission> permissions = new ArrayList<>();

    public Role(BackendProcessor backendProcessor, long guildId, long roleId) {
        super(backendProcessor, List.of("data", "guild", String.valueOf(guildId), "role", String.valueOf(roleId)));
        this.guildId = guildId;
        this.roleId = roleId;
    }

    public long getId(){
        return roleId;
    }

    public String getRoleName(){
        return roleName;
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
            permissions.add(new Permission(jsonObject.getJSONArray("permissions").getJSONObject(i)));
        }
    }

    public static class Permission{
        private final int permissionId;
        private final String permissionName;
        private final String permissionDescription;
        private boolean permissionGranted;

        public Permission(JSONObject jsonObject){
            this.permissionId = jsonObject.getInt("permissionId");
            this.permissionName = jsonObject.getString("permissionName");
            this.permissionDescription = jsonObject.getString("permissionDescription");
            this.permissionGranted = jsonObject.getBoolean("permissionGranted");
        }

        public int getPermissionId() {
            return permissionId;
        }

        public String getPermissionName() {
            return permissionName;
        }

        public String getPermissionDescription() {
            return permissionDescription;
        }

        public boolean isPermissionGranted() {
            return permissionGranted;
        }

        public void setPermissionGranted(boolean permissionGranted) {
            this.permissionGranted = permissionGranted;
        }

        public JSONObject asJSON(){
            return  new JSONObject()
                    .put("permissionId", permissionId)
                    .put("permissionName", permissionName)
                    .put("permissionDescription", permissionDescription)
                    .put("permissionGranted", permissionGranted);
        }
    }
}
