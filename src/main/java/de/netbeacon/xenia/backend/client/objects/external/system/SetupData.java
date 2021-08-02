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

package de.netbeacon.xenia.backend.client.objects.external.system;

import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetupData extends APIDataObject<SetupData>{

	private long clientId;
	private String clientName;
	private String clientDescription;
	private String discordToken;
	private int totalShards;
	private int[] shardIds;
	private String messageCryptHash;
	private String clientLocation;
	private static final Set<FeatureSet.Values> FEATURE_SET = new HashSet<>(List.of(FeatureSet.Values.GET));

	public SetupData(BackendProcessor backendProcessor){
		super(backendProcessor);
		setBackendPath("setup", "bot");
	}

	public long getClientId(){
		return clientId;
	}

	public String getClientName(){
		return clientName;
	}

	public String getClientDescription(){
		return clientDescription;
	}

	public String getDiscordToken(){
		return discordToken;
	}

	public int getTotalShards(){
		return totalShards;
	}

	public int[] getShards(){
		return shardIds;
	}

	public String getMessageCryptHash(){
		return messageCryptHash;
	}

	public String getClientLocation(){ return clientLocation; }

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("clientId", clientId)
			.put("clientName", clientName)
			.put("clientDescription", clientDescription)
			.put("discordToken", discordToken)
			.put("cryptHash", messageCryptHash)
			.put("shards", new JSONObject()
				.put("total", totalShards)
				.put("use", shardIds))
			.put("clientLocation", clientLocation);
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		this.clientId = jsonObject.getLong("clientId");
		this.clientName = jsonObject.getString("clientName");
		this.clientDescription = jsonObject.getString("clientDescription");
		this.discordToken = jsonObject.getString("discordToken");
		this.messageCryptHash = jsonObject.getString("cryptHash");
		this.totalShards = jsonObject.getJSONObject("shards").getInt("total");
		int[] shardIdsI = new int[jsonObject.getJSONObject("shards").getJSONArray("use").length()];
		for(int i = 0; i < jsonObject.getJSONObject("shards").getJSONArray("use").length(); i++){
			shardIdsI[i] = jsonObject.getJSONObject("shards").getJSONArray("use").getInt(i);
		}
		this.shardIds = shardIdsI;
		this.clientLocation = jsonObject.getString("clientLocation");
	}

	@Override
	protected Set<FeatureSet.Values> getSupportedFeatures(){
		return FEATURE_SET;
	}

}
