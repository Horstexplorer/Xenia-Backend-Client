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

public class User extends APIDataObject {

    private long userId;

    private long creationTimestamp;
    private String internalRole;
    private String preferredLanguage;
    // meta data - initialize with values
    private String metaUsername = "unknown_username";

    public User(BackendProcessor backendProcessor, long userId) {
        super(backendProcessor);
        this.userId = userId;
        setBackendPath("data", "users", (Function<Void, Long>) o -> getId());
    }

    public long getId(){
        return userId;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public String getInternalRole() {
        return internalRole;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void lSetMetaData(String username){
        this.metaUsername = username;
    }

    public void setMetaData(String username){
        lSetMetaData(username);
        update();
    }

    public String getMetaUsername() {
        return metaUsername;
    }

    public void setInternalRole(String internalRole){
        lSetMetaData(internalRole);
        update();
    }

    public void lSetInternalRole(String internalRole){
        this.internalRole = internalRole;
    }

    public void setPreferredLanguage(String language){
        lSetPreferredLanguage(language);
        update();
    }

    public void lSetPreferredLanguage(String language){
        this.preferredLanguage = language;
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("userId", userId)
                .put("creationTimestamp", creationTimestamp)
                .put("internalRole", internalRole)
                .put("preferredLanguage", preferredLanguage)
                .put("meta", new JSONObject()
                        .put("username", metaUsername)
                );
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        this.userId = jsonObject.getLong("userId");
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        this.internalRole = jsonObject.getString("internalRole");
        this.preferredLanguage = jsonObject.getString("preferredLanguage");
        JSONObject meta = jsonObject.getJSONObject("meta");
        this.metaUsername = meta.getString("username");
    }
}
