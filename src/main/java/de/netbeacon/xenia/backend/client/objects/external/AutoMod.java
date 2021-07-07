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

package de.netbeacon.xenia.backend.client.objects.external;

import de.netbeacon.utils.bitflags.IntegerBitFlags;
import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AutoMod extends APIDataObject{

	private final long guildId;
	private final long channelId;
	private FilterWordBlacklist filterWordBlacklist;
	private FilterWordWhitelist filterWordWhitelist;
	private FilterInviteUrl filterInviteUrl;
	private FilterOtherUrl filterOtherUrl;
	private FilterSpecialChars filterSpecialChars;
	private FilterMultilineSpam filterMultilineSpam;
	private FilterChatFlood filterChatFlood;

	public AutoMod(long guildId, long channelId, BackendProcessor backendProcessor){
		super(backendProcessor);
		this.guildId = guildId;
		this.channelId = channelId;
		setBackendPath("data", "guilds", (Supplier<Long>) this::getGuildId, "channels", (Supplier<Long>) this::getChannelId, "automod");
	}

	public long getGuildId(){
		return guildId;
	}

	public long getChannelId(){
		return channelId;
	}

	public FilterWordBlacklist getFilterWordBlacklist(){
		return filterWordBlacklist;
	}

	public void lSetFilterWordBlacklist(FilterWordBlacklist filterWordBlacklist){
		this.filterWordBlacklist = filterWordBlacklist;
	}

	public void setFilterWordBlacklist(FilterWordBlacklist filterWordBlacklist){
		lSetFilterWordWhitelist(filterWordWhitelist);
	}

	public FilterWordWhitelist getFilterWordWhitelist(){
		return filterWordWhitelist;
	}

	public void lSetFilterWordWhitelist(FilterWordWhitelist filterWordWhitelist){
		this.filterWordWhitelist = filterWordWhitelist;
	}

	public void setFilterWordWhitelist(FilterWordWhitelist filterWordWhitelist){
		lSetFilterWordWhitelist(filterWordWhitelist);
		update();
	}

	public FilterInviteUrl getFilterInviteUrl(){
		return filterInviteUrl;
	}

	public void lSetFilterInviteUrl(FilterInviteUrl filterInviteUrl){
		this.filterInviteUrl = filterInviteUrl;
	}

	public void setFilterInviteUrl(FilterInviteUrl filterInviteUrl){
		lSetFilterInviteUrl(filterInviteUrl);
		update();
	}

	public FilterOtherUrl getFilterOtherUrl(){
		return filterOtherUrl;
	}

	public void lSetFilterOtherUrl(FilterOtherUrl filterOtherUrl){
		this.filterOtherUrl = filterOtherUrl;
	}

	public void setFilterOtherUrl(FilterOtherUrl filterOtherUrl){
		lSetFilterOtherUrl(filterOtherUrl);
		update();
	}

	public FilterSpecialChars getFilterSpecialChars(){
		return filterSpecialChars;
	}

	public void lSetFilterSpecialChars(FilterSpecialChars filterSpecialChars){
		this.filterSpecialChars = filterSpecialChars;
	}

	public void setFilterSpecialChars(FilterSpecialChars filterSpecialChars){
		lSetFilterSpecialChars(filterSpecialChars);
		update();
	}

	public FilterMultilineSpam getFilterMultilineSpam(){
		return filterMultilineSpam;
	}

	public void lSetFilterMultilineSpam(FilterMultilineSpam filterMultilineSpam){
		this.filterMultilineSpam = filterMultilineSpam;
	}

	public void setFilterMultilineSpam(FilterMultilineSpam filterMultilineSpam){
		lSetFilterMultilineSpam(filterMultilineSpam);
		update();
	}

	public FilterChatFlood getFilterChatFlood(){
		return filterChatFlood;
	}

	public void lSetFilterChatFlood(FilterChatFlood filterChatFlood){
		this.filterChatFlood = filterChatFlood;
	}

	public void setFilterChatFlood(FilterChatFlood filterChatFlood){
		lSetFilterChatFlood(filterChatFlood);
		update();
	}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("guildId", guildId)
			.put("channelId", channelId)
			.put("filterWordBlacklist", filterWordBlacklist.getValue())
			.put("filterWordWhitelist", filterWordWhitelist.getValue())
			.put("filterInviteUrl", filterInviteUrl.getValue())
			.put("filterOtherUrl", filterOtherUrl.getValue())
			.put("filterSpecialChars", filterSpecialChars.getValue())
			.put("filterMultilineSpam", filterMultilineSpam.getValue())
			.put("filterChatFlood", filterChatFlood.getValue());
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		this.filterWordBlacklist = new FilterWordBlacklist(jsonObject.getInt("filterWordBlacklist"));
		this.filterWordWhitelist = new FilterWordWhitelist(jsonObject.getInt("filterWordWhitelist"));
		this.filterInviteUrl = new FilterInviteUrl(jsonObject.getInt("filterInviteUrl"));
		this.filterOtherUrl = new FilterOtherUrl(jsonObject.getInt("filterOtherUrl"));
		this.filterSpecialChars = new FilterSpecialChars(jsonObject.getInt("filterSpecialChars"));
		this.filterMultilineSpam = new FilterMultilineSpam(jsonObject.getInt("filterMultilineSpam"));
		this.filterChatFlood = new FilterChatFlood(jsonObject.getInt("filterChatFlood"));
	}

	public static class FilterWordBlacklist extends IntegerBitFlags {

		public FilterWordBlacklist(int value){
			super(value);
		}
		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterWordBlacklist.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			;

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

	public static class FilterWordWhitelist extends IntegerBitFlags {

		public FilterWordWhitelist(int value){
			super(value);
		}
		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterWordWhitelist.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			;

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

	public static class FilterInviteUrl extends IntegerBitFlags {

		public FilterInviteUrl(int value){
			super(value);
		}
		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterInviteUrl.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			;

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

	public static class FilterOtherUrl extends IntegerBitFlags {

		public FilterOtherUrl(int value){
			super(value);
		}
		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterOtherUrl.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			;

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

	public static class FilterSpecialChars extends IntegerBitFlags {

		public FilterSpecialChars(int value){
			super(value);
		}
		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterSpecialChars.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			;

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

	public static class FilterMultilineSpam extends IntegerBitFlags {

		public FilterMultilineSpam(int value){
			super(value);
		}
		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterMultilineSpam.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			;

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

	public static class FilterChatFlood extends IntegerBitFlags {

		public FilterChatFlood(int value){
			super(value);
		}
		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterChatFlood.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			;

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

}
