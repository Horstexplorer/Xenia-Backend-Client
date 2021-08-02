/*
 *     Copyright 2021 Horstexplorer @ https://www.netbeacon.de
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

package de.netbeacon.xenia.backend.client.objects.apidata.misc;

import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.apidata.Channel;
import de.netbeacon.xenia.backend.client.objects.apidata.Guild;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class TwitchNotification extends APIDataObject<TwitchNotification>{

	private long twitchNotificationId;
	private long guildId;
	private long channelId;
	private long creationTimestamp;
	private Long twitchChannelId;
	private String twitchChannelName;
	private String notificationMessage = "$username$ is now live on twitch playing $game$";
	private static final Set<FeatureSet.Values> FEATURE_SET = new HashSet<>(List.of(FeatureSet.Values.GET, FeatureSet.Values.CREATE, FeatureSet.Values.UPDATE, FeatureSet.Values.DELETE));

	public TwitchNotification(BackendProcessor backendProcessor, long guildId, long twitchNotificationId){
		super(backendProcessor);
		this.guildId = guildId;
		this.twitchNotificationId = twitchNotificationId;
		setBackendPath("data", "guilds", (Supplier<Long>) this::getGuildId, "misc", "twitchnotifications", (Supplier<Long>) this::getId);
	}

	public TwitchNotification lSetInitialData(String channelName, long channelId){
		this.twitchChannelName = channelName;
		this.channelId = channelId;
		return this;
	}

	public long getId(){
		return twitchNotificationId;
	}

	public long getGuildId(){
		return guildId;
	}

	public long getChannelId(){
		return channelId;
	}

	public long getCreationTimestamp(){
		return creationTimestamp;
	}

	public long getTwitchChannelId(){
		return twitchChannelId;
	}

	public String getTwitchChannelName(){
		return twitchChannelName;
	}

	public String getNotificationMessage(){
		return notificationMessage;
	}

	public void setNotificationMessage(String message){
		lSetNotificationMessage(message);
		update().queue();
	}

	public void lSetNotificationMessage(String message){
		this.notificationMessage = message;
	}

	// SECONDARY

	public Guild getGuild(){
		return getBackendProcessor().getBackendClient().getGuildCache().retrieve(guildId, true).execute();
	}

	public Channel getChannel(){
		return getGuild().getChannelCache().retrieve(channelId, true).execute();
	}


	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("twitchNotificationId", twitchNotificationId)
			.put("creationTimestamp", creationTimestamp)
			.put("guildId", guildId)
			.put("channelId", channelId)
			.put("twitchChannelId", twitchChannelId)
			.put("twitchChannelName", twitchChannelName)
			.put("notificationMessage", notificationMessage);
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		twitchNotificationId = jsonObject.getLong("twitchNotificationId");
		creationTimestamp = jsonObject.getLong("creationTimestamp");
		guildId = jsonObject.getLong("guildId");
		channelId = jsonObject.getLong("channelId");
		twitchChannelId = jsonObject.get("twitchChannelId") != JSONObject.NULL ? jsonObject.getLong("twitchChannelId") : null;
		twitchChannelName = jsonObject.getString("twitchChannelName");
		notificationMessage = jsonObject.getString("notificationMessage");
	}

	@Override
	protected Set<FeatureSet.Values> getSupportedFeatures(){
		return FEATURE_SET;
	}

}
