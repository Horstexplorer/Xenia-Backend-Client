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
import de.netbeacon.xenia.backend.client.objects.internal.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class License extends APIDataObject {

    private final long guildId;

    private String licenseName;
    private String licenseDescription;
    private long activationTimestamp;
    private int durationDays;
    // perks
    private boolean perk_CHANNEL_LOGGING_PCB;
    private int perk_CHANNEL_LOGGING_MC;

    public License(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor, List.of("data", "guild", String.valueOf(guildId), "license"));
        this.guildId = guildId;
    }

    public void update(String licenseKey) throws BackendException {
        HashMap<String, String> map = new HashMap<>();
        map.put("licenseKey", licenseKey);
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.PUT, BackendRequest.AuthType.Token, getBackendPath(), map, asJSON());
        BackendResult backendResult = getBackendProcessor().process(backendRequest);
        if(backendResult.getStatusCode() != 200){
            throw new BackendException(backendResult.getStatusCode(), "Failed To UPDATE APIDataObject With Path "+ Arrays.toString(getBackendPath().toArray()));
        }
        fromJSON(backendResult.getPayloadAsJSON());
    }


    public String getLicenseName(){
        return licenseName;
    }

    public String getLicenseDescription(){
        return licenseDescription;
    }

    public long getActivationTimestamp() {
        return activationTimestamp;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public boolean isPerk_CHANNEL_LOGGING_PCB() {
        return perk_CHANNEL_LOGGING_PCB;
    }

    public int getPerk_CHANNEL_LOGGING_MC() {
        return perk_CHANNEL_LOGGING_MC;
    }

    @Override
    public void create() throws BackendException {}

    @Override
    public void update() throws BackendException {}

    @Override
    public void delete() throws BackendException {
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        return new JSONObject()
                .put("licenseName", licenseName)
                .put("licenseDescription", licenseDescription)
                .put("activationTimestamp", activationTimestamp)
                .put("durationDays", durationDays)
                .put("perks", new JSONObject()
                        .put("channelLoggingMC", perk_CHANNEL_LOGGING_MC)
                        .put("channelLoggingPCB", perk_CHANNEL_LOGGING_PCB)
                );
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        this.licenseName = jsonObject.getString("licenseName");
        this.licenseDescription = jsonObject.getString("licenseDescription");
        this.activationTimestamp = jsonObject.getLong("activationTimestamp");
        this.durationDays = jsonObject.getInt("durationDays");
        this.perk_CHANNEL_LOGGING_MC = jsonObject.getJSONObject("perks").getInt("channelLoggingMC");
        this.perk_CHANNEL_LOGGING_PCB = jsonObject.getJSONObject("perks").getBoolean("channelLoggingPCB");
    }
}
