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

package de.netbeacon.xenia.backend.client.objects.apidata;

import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class User extends APIDataObject<User>{

	private long userId;

	private long creationTimestamp;
	private String internalRole;
	private String preferredLanguage;
	private long trustFactor = 10000;
	// meta data - initialize with values
	private String metaUsername = "unknown_username";
	private String metaIconUrl = null;
	private static final Set<FeatureSet.Values> FEATURE_SET = new HashSet<>(List.of(FeatureSet.Values.GET, FeatureSet.Values.GET_OR_CREATE, FeatureSet.Values.CREATE, FeatureSet.Values.UPDATE, FeatureSet.Values.DELETE));

	public User(BackendProcessor backendProcessor, long userId){
		super(backendProcessor);
		this.userId = userId;
		setBackendPath("data", "users", (Supplier<Long>) this::getId);
	}

	public long getId(){
		return userId;
	}

	public long getCreationTimestamp(){
		return creationTimestamp;
	}

	public String getInternalRole(){
		return internalRole;
	}

	public void setInternalRole(String internalRole){
		lSetInternalRole(internalRole);
		update().queue();
	}

	public String getPreferredLanguage(){
		return preferredLanguage;
	}

	public void setPreferredLanguage(String language){
		lSetPreferredLanguage(language);
		update().queue();
	}

	public void lSetMetaData(String username, String iconUrl){
		this.metaUsername = username;
		this.metaIconUrl = iconUrl;
	}

	public void setMetaData(String username, String iconUrl){
		lSetMetaData(username, iconUrl);
		update().queue();
	}

	public String getMetaUsername(){
		return metaUsername;
	}

	public String getMetaIconUrl(){
		return metaIconUrl;
	}

	public void lSetInternalRole(String internalRole){
		this.internalRole = internalRole;
	}

	public void lSetPreferredLanguage(String language){
		this.preferredLanguage = language;
	}

	public long getTrustFactor(){
		return trustFactor;
	}

	public void setTrustFactor(long trustFactor){
		lSetTrustFactor(trustFactor);
		update().queue();
	}

	public void lSetTrustFactor(long trustFactor){
		this.trustFactor = trustFactor;
	}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("userId", userId)
			.put("creationTimestamp", creationTimestamp)
			.put("internalRole", internalRole)
			.put("preferredLanguage", preferredLanguage)
			.put("trustFactor", trustFactor)
			.put("meta", new JSONObject()
				.put("username", metaUsername)
				.put("iconUrl", (metaIconUrl != null) ? metaIconUrl : JSONObject.NULL)
			);
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		this.userId = jsonObject.getLong("userId");
		this.creationTimestamp = jsonObject.getLong("creationTimestamp");
		this.internalRole = jsonObject.getString("internalRole");
		this.preferredLanguage = jsonObject.getString("preferredLanguage");
		this.trustFactor = jsonObject.getLong("trustFactor");
		JSONObject meta = jsonObject.getJSONObject("meta");
		this.metaUsername = meta.getString("username");
		this.metaIconUrl = meta.get("iconUrl") != JSONObject.NULL ? meta.getString("iconUrl") : null;
	}

	@Override
	protected Set<FeatureSet.Values> getSupportedFeatures(){
		return FEATURE_SET;
	}

}
