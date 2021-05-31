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

import java.util.function.Supplier;

public class User extends APIDataObject{

	private long userId;

	private long creationTimestamp;
	private String internalRole;
	private String preferredLanguage;
	// meta data - initialize with values
	private String metaUsername = "unknown_username";
	private String metaIconUrl = null;

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
		update();
	}

	public String getPreferredLanguage(){
		return preferredLanguage;
	}

	public void setPreferredLanguage(String language){
		lSetPreferredLanguage(language);
		update();
	}

	public void lSetMetaData(String username, String iconUrl){
		secure();
		this.metaUsername = username;
		this.metaIconUrl = iconUrl;
	}

	public void setMetaData(String username, String iconUrl){
		lSetMetaData(username, iconUrl);
		update();
	}

	public String getMetaUsername(){
		return metaUsername;
	}

	public String getMetaIconUrl(){
		return metaIconUrl;
	}

	public void lSetInternalRole(String internalRole){
		secure();
		this.internalRole = internalRole;
	}

	public void lSetPreferredLanguage(String language){
		secure();
		this.preferredLanguage = language;
	}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("userId", userId)
			.put("creationTimestamp", creationTimestamp)
			.put("internalRole", internalRole)
			.put("preferredLanguage", preferredLanguage)
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
		JSONObject meta = jsonObject.getJSONObject("meta");
		this.metaUsername = meta.getString("username");
		this.metaIconUrl = meta.get("iconUrl") != JSONObject.NULL ? meta.getString("iconUrl") : null;
	}

}
