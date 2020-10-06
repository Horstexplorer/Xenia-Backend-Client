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

package de.netbeacon.xenia.backend.client.objects.cache;

import de.netbeacon.xenia.backend.client.objects.external.Role;
import de.netbeacon.xenia.backend.client.objects.internal.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;

import java.util.List;
import java.util.Objects;

public class RoleCache extends Cache<Role>{

    private final long guildId;

    public RoleCache(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
    }

    public Role get(long roleId) throws BackendException {
        Role role = getFromCache(roleId);
        if(role != null){
            return role;
        }
        role = new Role(getBackendProcessor(), guildId, roleId);
        role.get();
        addToCache(roleId, role);
        return role;
    }

    public List<Role> retrieveAll() throws BackendException {
        return null;
    }

    public Role createNew() throws BackendException {
        Role role = new Role(getBackendProcessor(), guildId, -1);
        role.create();
        addToCache(role.getId(), role);
        return role;
    }

    public void remove(long roleId){
        removeFromCache(roleId);
    }

    public void delete(long roleId) throws BackendException {
        Role role = getFromCache(roleId);
        Objects.requireNonNullElseGet(role, ()->new Role(getBackendProcessor(), guildId, roleId)).delete();
        removeFromCache(roleId);
    }
}
