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

package de.netbeacon.xenia.backend.client.objects.apidata.misc;

import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.apidata.Channel;
import de.netbeacon.xenia.backend.client.objects.apidata.Guild;
import de.netbeacon.xenia.backend.client.objects.apidata.Member;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class Notification extends APIDataObject<Notification>{

	private long notificationId;
	private long creationTimestamp;
	private long guildId;
	private long channelId;
	private long userId;
	private long notificationTarget;
	private String notificationMessage;
	private static final Set<FeatureSet.Values> FEATURE_SET = new HashSet<>(List.of(FeatureSet.Values.GET, FeatureSet.Values.CREATE, FeatureSet.Values.UPDATE, FeatureSet.Values.DELETE));

	public Notification(BackendProcessor backendProcessor, long guildId, long notificationId){
		super(backendProcessor);
		this.guildId = guildId;
		this.notificationId = notificationId;
		setBackendPath("data", "guilds", (Supplier<Long>) this::getGuildId, "misc", "notifications", (Supplier<Long>) this::getId);
	}

	public Notification lSetInitialData(long channelId, long userId, long notificationTarget, String notificationMessage){
		this.channelId = channelId;
		this.userId = userId;
		this.notificationTarget = notificationTarget;
		this.notificationMessage = notificationMessage;
		return this;
	}

	public long getId(){
		return notificationId;
	}

	public long getCreationTimestamp(){
		return creationTimestamp;
	}

	public long getGuildId(){
		return guildId;
	}

	public long getChannelId(){
		return channelId;
	}

	public long getUserId(){
		return userId;
	}

	public long getNotificationTarget(){
		return notificationTarget;
	}

	public void setNotificationTarget(long notificationTarget){
		lSetNotificationTarget(notificationTarget);
		update().queue();
	}

	public String getNotificationMessage(){
		return notificationMessage;
	}

	public void setNotificationMessage(String notificationMessage){
		lSetNotificationMessage(notificationMessage);
		update().queue();
	}

	public void lSetNotificationTarget(long notificationTarget){
		this.notificationTarget = notificationTarget;
	}

	public void lSetNotificationMessage(String notificationMessage){
		this.notificationMessage = notificationMessage;
	}

	// SECONDARY

	public Guild getGuild(){
		return getBackendProcessor().getBackendClient().getGuildCache().retrieve(guildId, true).execute();
	}

	public Channel getChannel(){
		return getGuild().getChannelCache().retrieve(channelId, true).execute();
	}

	public Member getMember(){
		return getGuild().getMemberCache().retrieve(userId, true).execute();
	}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("notificationId", notificationId)
			.put("creationTimestamp", creationTimestamp)
			.put("guildId", guildId)
			.put("channelId", channelId)
			.put("userId", userId)
			.put("notificationTarget", notificationTarget)
			.put("notificationMessage", notificationMessage);
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		this.notificationId = jsonObject.getLong("notificationId");
		this.creationTimestamp = jsonObject.getLong("creationTimestamp");
		this.guildId = jsonObject.getLong("guildId");
		this.channelId = jsonObject.getLong("channelId");
		this.userId = jsonObject.getLong("userId");
		this.notificationTarget = jsonObject.getLong("notificationTarget");
		this.notificationMessage = jsonObject.getString("notificationMessage");
	}

	@Override
	protected Set<FeatureSet.Values> getSupportedFeatures(){
		return FEATURE_SET;
	}

}
