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

package de.netbeacon.xenia.backend.client.objects.external.misc;

import de.netbeacon.utils.json.serial.IJSONSerializable;
import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class Poll extends APIDataObject {

    private long pollId;
    private long creationTimestamp;
    private long guildId;
    private long channelId;
    private long userId;
    private String description = "";
    private long closeTimestamp = 0;
    private final HashMap<Integer, Option> options = new HashMap<>();

    public Poll(BackendProcessor backendProcessor, long guildId, long pollId) {
        super(backendProcessor);
        this.guildId = guildId;
        this.pollId = pollId;
        setBackendPath("data", "guilds", (Function<Void, Long>) o -> getGuildId(), "misc", "polls", (Function<Void, Long>) o -> getId());
    }

    public void setInitialData(long userId, long channelId, long closeTimestamp, String description, String...options){
        this.userId = userId;
        this.channelId = channelId;
        this.closeTimestamp = closeTimestamp;
        this.description = description;
        for(String option : options){
            Option optionO = new Option(this, this.options.size()+1, option);
            this.options.put(optionO.getId(), optionO);
        }
    }

    public long getId() {
        return pollId;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public long getCloseTimestamp() {
        return closeTimestamp;
    }

    public HashMap<Integer, Option> getOptions(){
        return options;
    }

    @Override
    public void delete() throws BackendException {
        super.delete();
    }

    @Override
    public void deleteAsync() {
        super.deleteAsync();
    }

    @Override
    public JSONObject asJSON() throws JSONSerializationException {
        JSONArray options = new JSONArray();
        this.options.forEach((k,v)->options.put(v.asJSON()));
        return new JSONObject()
                .put("pollId", pollId)
                .put("creationTimestamp", creationTimestamp)
                .put("guildId", guildId)
                .put("channelId", channelId)
                .put("userId", userId)
                .put("closeTimestamp", closeTimestamp)
                .put("options", options);
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
        this.pollId = jsonObject.getLong("pollId");
        this.creationTimestamp = jsonObject.getLong("creationTimestamp");
        this.guildId = jsonObject.getLong("guildId");
        this.channelId = jsonObject.getLong("channelId");
        this.userId = jsonObject.getLong("userId");
        this.closeTimestamp = jsonObject.getLong("closeTimestamp");
        JSONArray options = jsonObject.getJSONArray("options");
        for(int i = 0; i < options.length(); i++){
            Option option = new Option(this);
            option.fromJSON(options.getJSONObject(i));
            this.options.put(option.getId(), option);
        }
    }

    protected class Option implements IJSONSerializable {

        private int id;
        private String description;
        private int count;
        private final Poll poll;

        protected Option(Poll poll){
            this.poll = poll;
        }

        protected Option(Poll poll, int id, String description){
            this.id = id;
            this.description = description;
            this.poll = poll;
        }

        public int getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public int getCount() {
            return count;
        }

        public void vote(long userId){
            try{
                BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.POST, BackendRequest.AuthType.Token, List.of("data", "guilds", String.valueOf(guildId), "misc", "polls", String.valueOf(poll.getGuildId()), String.valueOf(getId()), "vote", String.valueOf(userId)), new HashMap<>(), new JSONObject());
                BackendResult backendResult = getBackendProcessor().process(backendRequest);
                if(backendResult.getStatusCode() != 202){
                    throw new RuntimeException("Vote Failed");
                }
            }catch (Exception e){
                throw new RuntimeException("Vote Failed");
            }
        }

        @Override
        public JSONObject asJSON() throws JSONSerializationException {
            return new JSONObject()
                    .put("optionId", id)
                    .put("count", count)
                    .put("description", description);
        }

        @Override
        public void fromJSON(JSONObject jsonObject) throws JSONSerializationException {
            this.id = jsonObject.getInt("id");
            this.count = jsonObject.getInt("cont");
            this.description = jsonObject.getString("description");
        }

    }
}
