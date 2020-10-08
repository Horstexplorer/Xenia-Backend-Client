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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Info extends APIDataObject {

    public enum Mode {
        Public,
        Private,
    }

    private final Mode mode;

    private long ping;
    private int guildCount;
    private int userCount;
    private int memberCount;
    private int channelCount;
    private int forbiddenChannels;

    private final Logger logger = LoggerFactory.getLogger(Info.class);

    public Info(BackendProcessor backendProcessor, Mode mode) {
        super(backendProcessor, List.of("info", mode.toString().toLowerCase()));
        this.mode = mode;
    }

    @Override
    public void get() throws BackendException {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.Token, getBackendPath(), new HashMap<>(), null);
        BackendResult backendResult = getBackendProcessor().process(backendRequest);
        if(backendResult.getStatusCode() != 200){
            logger.debug("Failed To GET APIDataObject With Path "+ Arrays.toString(getBackendPath().toArray())+" ("+backendResult.getStatusCode()+")");
            throw new BackendException(backendResult.getStatusCode(), "Failed To GET APIDataObject With Path "+ Arrays.toString(getBackendPath().toArray()));
        }
        fromJSON(backendResult.getPayloadAsJSON());
        this.ping = backendResult.getRequestDuration();
    }

    @Override
    public void getAsync() {
        BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.Token, getBackendPath(), new HashMap<>(), null);
        getBackendProcessor().processAsync(backendRequest, br->{
            if(br.getStatusCode() != 200){
                logger.debug("Failed To GET APIDataObject With Path "+Arrays.toString(getBackendPath().toArray())+" ("+br.getStatusCode()+")");
                throw new BackendException(br.getStatusCode(), "Failed To GET APIDataObject With Path "+ Arrays.toString(getBackendPath().toArray()));
            }
            fromJSON(br.getPayloadAsJSON());
        });
    }

    @Override
    public void create() throws BackendException {}

    @Override
    public void createAsync() {}

    @Override
    public void update() throws BackendException {}

    @Override
    public void updateAsync() {}

    @Override
    public void delete() throws BackendException {}

    @Override
    public void deleteAsync() {}

    public int getGuildCount() {
        return guildCount;
    }

    public int getUserCount() {
        return userCount;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getForbiddenChannels() {
        return forbiddenChannels;
    }

    public long getPing() {
        return ping;
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        if(mode == Mode.Public){
            return new JSONObject()
                    .put("guilds", guildCount)
                    .put("users", userCount)
                    .put("members", memberCount);
        }else{
            return new JSONObject()
                    .put("guilds", guildCount)
                    .put("users", userCount)
                    .put("members", memberCount)
                    .put("channels", new JSONObject()
                            .put("total", channelCount)
                            .put("forbidden", forbiddenChannels));
        }
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        if(mode == Mode.Public){
            this.guildCount = jsonObject.getInt("guilds");
            this.userCount = jsonObject.getInt("users");
            this.memberCount = jsonObject.getInt("members");
        }else{
            this.guildCount = jsonObject.getInt("guilds");
            this.userCount = jsonObject.getInt("users");
            this.memberCount = jsonObject.getInt("members");
            this.channelCount = jsonObject.getJSONObject("channels").getInt("total");
            this.forbiddenChannels = jsonObject.getJSONObject("channels").getInt("forbidden");
        }
    }
}
