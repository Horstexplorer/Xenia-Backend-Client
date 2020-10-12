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

import de.netbeacon.utils.appinfo.AppInfo;
import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Info extends APIDataObject {

    public enum Mode {
        Public,
        Private,
    }

    private final Mode mode;

    private String version;
    private int guildCount;
    private int userCount;
    private int memberCount;
    private int channelCount;
    private int forbiddenChannels;
    private int messageCount;

    private final Logger logger = LoggerFactory.getLogger(Info.class);

    public Info(BackendProcessor backendProcessor, Mode mode) {
        super(backendProcessor, List.of("info", mode.toString().toLowerCase()));
        this.mode = mode;
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

    public String getVersion(){
        return version;
    }

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

    public int getMessageCount() {
        return messageCount;
    }

    public long getPing() {
        return getLastRequestDuration();
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        if(mode == Mode.Public){
            return new JSONObject()
                    .put("version", AppInfo.get("buildVersion")+"_"+ AppInfo.get("buildNumber"))
                    .put("guilds", guildCount)
                    .put("users", userCount)
                    .put("members", memberCount);
        }else{
            return new JSONObject()
                    .put("version", AppInfo.get("buildVersion")+"_"+ AppInfo.get("buildNumber"))
                    .put("guilds", guildCount)
                    .put("users", userCount)
                    .put("members", memberCount)
                    .put("channels", new JSONObject()
                            .put("total", channelCount)
                            .put("forbidden", forbiddenChannels))
                    .put("messages", messageCount);
        }
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        if(mode == Mode.Public){
            this.version = jsonObject.getString("version");
            this.guildCount = jsonObject.getInt("guilds");
            this.userCount = jsonObject.getInt("users");
            this.memberCount = jsonObject.getInt("members");
        }else{
            this.version = jsonObject.getString("version");
            this.guildCount = jsonObject.getInt("guilds");
            this.userCount = jsonObject.getInt("users");
            this.memberCount = jsonObject.getInt("members");
            this.channelCount = jsonObject.getJSONObject("channels").getInt("total");
            this.forbiddenChannels = jsonObject.getJSONObject("channels").getInt("forbidden");
            this.messageCount = jsonObject.getInt("messages");
        }
    }
}
