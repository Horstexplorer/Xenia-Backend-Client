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
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;

public class License extends APIDataObject{

	private final long guildId;

	private String licenseName;
	private String licenseDescription;
	private long activationTimestamp;
	private int durationDays;
	// perks
	private int perk_CHANNEL_LOGGING_C;
	private int perk_GUILD_ROLE_C;
	private int perk_MISC_TAGS_C;
	private int perk_MISC_NOTIFICATIONS_C;
	private int perk_MISC_TWITCHNOTIFICATIONS_C;
	private int perk_CHANNEL_D43Z1_SELFLEARNING_C;

	public License(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
		setBackendPath("data", "guilds", this.guildId, "license");
	}

	public void update(String licenseKey) throws BackendException{
		HashMap<String, String> map = new HashMap<>();
		map.put("licenseKey", licenseKey);
		BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.PUT, BackendRequest.AuthType.BEARER, getBackendPath(), map, asJSON());
		BackendResult backendResult = getBackendProcessor().process(backendRequest);
		if(backendResult.getStatusCode() != 200){
			throw new DataException(DataException.Type.HTTP, backendResult.getStatusCode(), "Failed To UPDATE APIDataObject With Path " + Arrays.toString(getBackendPath().toArray()));
		}
		fromJSON(backendResult.getPayloadAsJSON());
	}

	public long getGuildId(){
		return guildId;
	}

	public String getLicenseName(){
		return licenseName;
	}

	public String getLicenseDescription(){
		return licenseDescription;
	}

	public long getActivationTimestamp(){
		return activationTimestamp;
	}

	public int getDurationDays(){
		return durationDays;
	}

	public int getPerk_CHANNEL_LOGGING_C(){
		return perk_CHANNEL_LOGGING_C;
	}

	public int getPerk_GUILD_ROLE_C(){
		return perk_GUILD_ROLE_C;
	}

	public int getPerk_MISC_NOTIFICATIONS_C(){
		return perk_MISC_NOTIFICATIONS_C;
	}

	public int getPerk_MISC_TAGS_C(){
		return perk_MISC_TAGS_C;
	}

	public int getPerk_MISC_TWITCHNOTIFICATIONS_C(){
		return perk_MISC_TWITCHNOTIFICATIONS_C;
	}

	public int getPerk_CHANNEL_D43Z1_SELFLEARNING_C(){
		return perk_CHANNEL_D43Z1_SELFLEARNING_C;
	}

	// SECONDARY

	public Guild getGuild(){
		return getBackendProcessor().getBackendClient().getGuildCache().get(guildId, false);
	}

	@Override
	public void create() throws BackendException{
	}

	@Override
	public void createAsync(){
	}

	@Override
	public void update() throws BackendException{
	}

	@Override
	public void updateAsync(){
	}

	@Override
	public void delete() throws BackendException{
	}

	@Override
	public void deleteAsync(){
	}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("licenseName", licenseName)
			.put("licenseDescription", licenseDescription)
			.put("activationTimestamp", activationTimestamp)
			.put("durationDays", durationDays)
			.put("perks", new JSONObject()
				.put("channelLogging", perk_CHANNEL_LOGGING_C)
				.put("guildRoles", perk_GUILD_ROLE_C)
				.put("miscTags", perk_MISC_TAGS_C)
				.put("miscNotifications", perk_MISC_NOTIFICATIONS_C)
				.put("miscTwitchNotifications", perk_MISC_TWITCHNOTIFICATIONS_C)
				.put("channelD43z1SelfLearning", perk_CHANNEL_D43Z1_SELFLEARNING_C)
			);
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		this.licenseName = jsonObject.getString("licenseName");
		this.licenseDescription = jsonObject.getString("licenseDescription");
		this.activationTimestamp = jsonObject.getLong("activationTimestamp");
		this.durationDays = jsonObject.getInt("durationDays");
		JSONObject perks = jsonObject.getJSONObject("perks");
		this.perk_CHANNEL_LOGGING_C = perks.getInt("channelLogging");
		this.perk_GUILD_ROLE_C = perks.getInt("guildRoles");
		this.perk_MISC_TAGS_C = perks.getInt("miscTags");
		this.perk_MISC_NOTIFICATIONS_C = perks.getInt("miscNotifications");
		this.perk_MISC_TWITCHNOTIFICATIONS_C = perks.getInt("miscTwitchNotifications");
		this.perk_CHANNEL_D43Z1_SELFLEARNING_C = perks.getInt("channelD43z1SelfLearning");
	}

}
