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

import de.netbeacon.utils.bitflags.IntegerBitFlags;
import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.cache.MessageCache;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Channel extends APIDataObject{

	private final MessageCache messageCache;
	private long guildId;
	private long channelId;
	private long creationTimestamp;
	private AccessMode accessMode = new AccessMode(1);
	private ChannelFlags channelFlags = new ChannelFlags(0);
	private ChannelSettings channelSettings = new ChannelSettings(0);
	private boolean tmpLoggingActive;
	private long tmpLoggingChannelId;
	private D43Z1Settings d43z1Settings = new D43Z1Settings(0);
	private String metaChannelName;
	private String metaChannelTopic;

	public Channel(BackendProcessor backendProcessor, long guildId, long channelId){
		super(backendProcessor);
		this.guildId = guildId;
		this.channelId = channelId;
		this.messageCache = new MessageCache(backendProcessor, guildId, channelId);
		setBackendPath("data", "guilds", (Supplier<Long>) this::getGuildId, "channels", (Supplier<Long>) this::getChannelId);
	}

	public long getId(){
		return channelId;
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

	public AccessMode getAccessMode(){
		return accessMode;
	}

	public void setAccessMode(AccessMode accessMode){
		lSetAccessMode(accessMode);
		update();
	}

	public ChannelFlags getChannelFlags(){
		return channelFlags;
	}

	public void setChannelFlags(ChannelFlags channelFlags){
		lSetChannelFlags(channelFlags);
		update();
	}

	public ChannelSettings getChannelSettings(){
		return channelSettings;
	}

	public void setChannelSettings(ChannelSettings channelSettings){
		lSetChannelSettings(channelSettings);
		update();
	}

	public boolean tmpLoggingIsActive(){
		return tmpLoggingActive;
	}

	public long getTmpLoggingChannelId(){
		return tmpLoggingChannelId;
	}

	public void setTmpLoggingChannelId(long tmpLoggingChannelId){
		lSetTmpLoggingChannelId(tmpLoggingChannelId);
		update();
	}

	public void lSetAccessMode(AccessMode accessMode){
		this.accessMode = accessMode;
	}

	public void lSetChannelFlags(ChannelFlags channelFlags){
		this.channelFlags = channelFlags;
	}

	public void lSetChannelSettings(ChannelSettings channelSettings){
		this.channelSettings = channelSettings;
	}

	public void setTmpLoggingActive(boolean tmpLoggingActive){
		lSetTmpLoggingActive(tmpLoggingActive);
		update();
	}

	public void lSetTmpLoggingActive(boolean tmpLoggingActive){
		this.tmpLoggingActive = tmpLoggingActive;
	}

	public void lSetTmpLoggingChannelId(long tmpLoggingChannelId){
		this.tmpLoggingChannelId = tmpLoggingChannelId;
	}

	public D43Z1Settings getD43Z1Settings(){
		return d43z1Settings;
	}

	public void setD43Z1Settings(D43Z1Settings d43Z1Settings){
		lSetD43Z1Settings(d43Z1Settings);
		update();
	}

	public void lSetD43Z1Settings(D43Z1Settings d43Z1Settings){
		this.d43z1Settings = d43Z1Settings;
	}

	public MessageCache getMessageCache(){
		return messageCache;
	}

	public String getMetaChannelName(){
		return metaChannelName;
	}

	public String getMetaChannelTopic(){
		return metaChannelTopic;
	}

	public void setMetaData(String channelName, String channelTopic){
		lSetMetaData(channelName, channelTopic);
		update();
	}

	public void lSetMetaData(String channelName, String channelTopic){
		this.metaChannelName = channelName;
		this.metaChannelTopic = channelTopic != null ? channelTopic : "Unknown topic";
	}

	// SECONDARY

	public Guild getGuild(){
		return getBackendProcessor().getBackendClient().getGuildCache().get(guildId, false);
	}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("guildId", guildId)
			.put("channelId", channelId)
			.put("creationTimestamp", creationTimestamp)
			.put("accessMode", accessMode.getValue())
			.put("channelFlags", channelFlags.getValue())
			.put("channelSettings", channelSettings.getValue())
			.put("tmpLoggingActive", tmpLoggingActive)
			.put("tmpLoggingChannelId", tmpLoggingChannelId)
			.put("d43z1Settings", d43z1Settings.getValue())
			.put("meta", new JSONObject()
				.put("name", metaChannelName)
				.put("topic", metaChannelTopic)
			);
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		this.guildId = jsonObject.getLong("guildId");
		this.channelId = jsonObject.getLong("channelId");
		this.creationTimestamp = jsonObject.getLong("creationTimestamp");
		this.accessMode = new AccessMode(jsonObject.getInt("accessMode"));
		this.channelFlags = new ChannelFlags(jsonObject.getInt("channelFlags"));
		this.channelSettings = new ChannelSettings(jsonObject.getInt("channelSettings"));
		this.tmpLoggingActive = jsonObject.getBoolean("tmpLoggingActive");
		this.tmpLoggingChannelId = jsonObject.getLong("tmpLoggingChannelId");
		this.d43z1Settings = new D43Z1Settings(jsonObject.getInt("d43z1Settings"));
		JSONObject meta = jsonObject.getJSONObject("meta");
		this.metaChannelName = meta.getString("name");
		this.metaChannelTopic = meta.getString("topic");
	}

	public void clear(boolean deletion){
		messageCache.clear(deletion);
	}

	public static class AccessMode extends IntegerBitFlags{

		public AccessMode(int value){
			super(value);
		}

		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(Mode.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Mode implements IntBit{

			DISABLED(2),
			INACTIVE(1),
			ACTIVE(0);

			private final int pos;

			Mode(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

	public static class ChannelFlags extends IntegerBitFlags{

		public ChannelFlags(int value){
			super(value);
		}

		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(AccessMode.Mode.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Flags implements IntBit{

			NEWS(1),
			NSFW(0);

			private final int pos;

			Flags(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

	public static class ChannelSettings extends IntegerBitFlags{

		public ChannelSettings(int value){
			super(value);
		}

		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(ChannelSettings.Settings.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Settings implements IntBit{

			;

			private final int pos;

			Settings(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

	public static class D43Z1Settings extends IntegerBitFlags{

		public D43Z1Settings(int value){
			super(value);
		}

		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(D43Z1Settings.Settings.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Settings implements IntBit{

			ENABLE_SELF_LEARNING(1), // default 0
			ACTIVE(0); // default 0

			private final int pos;

			Settings(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

}
