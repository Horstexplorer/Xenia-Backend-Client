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

import java.util.List;

public class Role extends APIDataObject {

    private final long guildId;
    private final long roleId;

    public Role(BackendProcessor backendProcessor, long guildId, long roleId) {
        super(backendProcessor, List.of("data", "guild", String.valueOf(guildId), "role", String.valueOf(roleId)));
        this.guildId = guildId;
        this.roleId = roleId;
    }

    public long getId(){
        return roleId;
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return null;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {

    }
}
