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

import de.netbeacon.utils.crypt.Base64;
import de.netbeacon.utils.crypt.Crypt;
import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Message extends APIDataObject{

	private long guildId;
	private long channelId;
	private long messageId;

	private long userId;
	private long creationTimestamp;
	private long creationTimestampDiscord;
	private String messageSalt;
	private String messageContent;
	private String oldMessageSalt;
	private String oldMessageContent;
	private List<String> attachments = new ArrayList<>();

	public Message(BackendProcessor backendProcessor, long guildId, long channelId, long messageId){
		super(backendProcessor);
		this.guildId = guildId;
		this.channelId = channelId;
		this.messageId = messageId;
		setBackendPath("data", "guilds", (Supplier<Long>) this::getGuildId, "channels", (Supplier<Long>) this::getChannelId, "messages", (Supplier<Long>) this::getId);
	}

	public Message lSetInitialData(long userId, long creationTimestamp, String messageContent, List<String> attachmentUrls, String cryptKey){
		try{
			byte[] salt = Crypt.genSalt();
			this.messageSalt = new String(Base64.encode(salt));
			this.messageContent = new String(Base64.encode(Crypt.encrypt(messageContent.getBytes(), cryptKey, salt)));
			this.attachments.addAll(attachmentUrls);
		}
		catch(Exception e){
			throw new BackendException(-5, "Failed To Encrypt Content");
		}
		this.userId = userId;
		this.creationTimestampDiscord = creationTimestamp;
		return this;
	}

	public long getId(){
		return messageId;
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

	public long getCreationTimestamp(){
		return creationTimestamp;
	}

	public long getCreationTimestampDiscord(){
		return creationTimestampDiscord;
	}

	public String getOldMessageContent(String cryptKey){
		if(oldMessageSalt == null || oldMessageSalt.isBlank() || oldMessageContent == null || oldMessageContent.isBlank()){
			return getMessageContent(cryptKey);
		}
		try{
			return new String(Crypt.decrypt(Base64.decode(this.oldMessageContent.getBytes()), cryptKey, Base64.decode(this.oldMessageSalt.getBytes())));
		}
		catch(Exception e){
			throw new BackendException(-5, "Failed Decrypt Content");
		}
	}

	public String getMessageContent(String cryptKey){
		try{
			return new String(Crypt.decrypt(Base64.decode(this.messageContent.getBytes()), cryptKey, Base64.decode(this.messageSalt.getBytes())));
		}
		catch(Exception e){
			throw new BackendException(-5, "Failed Decrypt Content");
		}
	}

	public void setMessageContent(String content, String cryptKey){
		lSetMessageContent(content, cryptKey);
		update();
	}

	public void lSetMessageContent(String content, String cryptKey){
		secure();
		String tmpS = this.messageSalt;
		String tmpC = this.messageContent;
		try{
			byte[] salt = Crypt.genSalt();
			this.messageSalt = new String(Base64.encode(salt));
			this.messageContent = new String(Base64.encode(Crypt.encrypt(content.getBytes(), cryptKey, salt)));
		}
		catch(Exception e){
			throw new BackendException(-5, "Failed To Encrypt Content");
		}
		this.oldMessageSalt = tmpS;
		this.oldMessageContent = tmpC;
	}

	public List<String> getAttachmentUrls(){
		return attachments;
	}

	// SECONDARY

	public Guild getGuild(){
		return getBackendProcessor().getBackendClient().getGuildCache().get(guildId, false);
	}

	public Channel getChannel(){
		return getGuild().getChannelCache().get(channelId, false);
	}

	public Member getMember(){
		return getGuild().getMemberCache().get(userId, false);
	}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("guildId", guildId)
			.put("channelId", channelId)
			.put("messageId", messageId)
			.put("userId", userId)
			.put("creationTimestamp", creationTimestamp)
			.put("creationTimestampDiscord", creationTimestampDiscord)
			.put("messageAttachments", attachments)
			.put("messageSalt", messageSalt)
			.put("messageContent", messageContent);
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		this.guildId = jsonObject.getLong("guildId");
		this.channelId = jsonObject.getLong("channelId");
		this.messageId = jsonObject.getLong("messageId");
		this.userId = jsonObject.getLong("userId");
		this.creationTimestamp = jsonObject.getLong("creationTimestamp");
		this.creationTimestampDiscord = jsonObject.getLong("creationTimestampDiscord");
		this.attachments = new ArrayList<>();
		for(int i = 0; i < jsonObject.getJSONArray("messageAttachments").length(); i++){
			this.attachments.add(jsonObject.getJSONArray("messageAttachments").getString(i));
		}
		this.messageSalt = jsonObject.getString("messageSalt");
		this.messageContent = jsonObject.getString("messageContent");
	}

}
