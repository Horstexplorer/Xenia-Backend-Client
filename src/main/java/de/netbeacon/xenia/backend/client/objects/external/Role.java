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

import java.util.function.Function;

public class Role extends APIDataObject {

    private long guildId;
    private long roleId;

    private String roleName;
    private Permissions permissions;

    public Role(BackendProcessor backendProcessor, long guildId, long roleId) {
        super(backendProcessor);
        this.guildId = guildId;
        this.roleId = roleId;
        this.permissions = new Permissions(this, 0);
        setBackendPath("data", "guilds", (Function<Void, Long>) o -> getGuildId(), "roles", (Function<Void, Long>) o -> getId());
    }

    public long getId(){
        return roleId;
    }

    public long getGuildId() {
        return guildId;
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

    public Permissions getPermissions() {
        return permissions;
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("guildId", guildId)
                .put("roleId", roleId)
                .put("roleName", roleName)
                .put("rolePermissions", permissions.getPermVal());
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        this.guildId = jsonObject.getLong("guildId");
        this.roleId = jsonObject.getLong("roleId");
        this.roleName = jsonObject.getString("roleName");
        this.permissions = new Permissions(this, jsonObject.getLong("rolePermissions"));
    }

    static class Permissions {

        private final Role role;
        private long permVal;

        public enum Bit{

            UNUSED_PERMISSION_BIT_31(31),
            UNUSED_PERMISSION_BIT_30(30),
            UNUSED_PERMISSION_BIT_29(29),
            UNUSED_PERMISSION_BIT_28(28),
            UNUSED_PERMISSION_BIT_27(27),
            UNUSED_PERMISSION_BIT_26(26),
            UNUSED_PERMISSION_BIT_25(25),
            UNUSED_PERMISSION_BIT_24(24),
            UNUSED_PERMISSION_BIT_23(23),
            UNUSED_PERMISSION_BIT_22(22),
            UNUSED_PERMISSION_BIT_21(21),
            UNUSED_PERMISSION_BIT_20(20),
            UNUSED_PERMISSION_BIT_19(19),
            UNUSED_PERMISSION_BIT_18(18),
            UNUSED_PERMISSION_BIT_17(17),
            UNUSED_PERMISSION_BIT_16(16),
            UNUSED_PERMISSION_BIT_15(15),
            UNUSED_PERMISSION_BIT_14(14),
            UNUSED_PERMISSION_BIT_13(13),
            UNUSED_PERMISSION_BIT_12(12),
            UNUSED_PERMISSION_BIT_11(11),
            UNUSED_PERMISSION_BIT_10(10),
            UNUSED_PERMISSION_BIT_9(9),
            UNUSED_PERMISSION_BIT_8(8),
            UNUSED_PERMISSION_BIT_7(7),
            UNUSED_PERMISSION_BIT_6(6),
            UNUSED_PERMISSION_BIT_5(5),
            UNUSED_PERMISSION_BIT_4(4),
            UNUSED_PERMISSION_BIT_3(3),
            UNUSED_PERMISSION_BIT_2(2),
            UNUSED_PERMISSION_BIT_1(1),
            UNUSED_PERMISSION_BIT_0(0);

            private final int pos;

            private Bit(int pos){
                this.pos = pos;
            }

            public int getPos() {
                return pos;
            }
        }

        public Permissions(Role role, long permVal){
            this.role = role;
            this.permVal = permVal;
        }

        public long getPermVal(){
            return permVal;
        }

        public synchronized void enable(Bit...bits){
            lenable(bits);
            role.update();
        }

        public synchronized void lenable(Bit...bits){
            for(Bit b : bits){
                if(b.getPos() == 31){
                    continue;
                }
                permVal |= 1 << b.pos;
            }

        }

        public synchronized void disable(Bit...bits){
            ldisable(bits);
            role.update();
        }

        public synchronized void ldisable(Bit...bits){
            for(Bit b : bits){
                if(b.getPos() == 31){
                    continue;
                }
                permVal |= 1 << b.pos;
            }
        }

        public boolean hasPermission(Bit bit){
            return ((permVal >> bit.getPos()) & 1) == 1;
        }

    }
}
