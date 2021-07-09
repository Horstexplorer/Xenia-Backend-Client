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
	private FilterContent_Words filterContent_words = new FilterContent_Words(0);
	private FilterContent_URLs filterContent_urls = new FilterContent_URLs(0);
	private FilterBehaviour_Spam filterBehaviour_spam = new FilterBehaviour_Spam(0);
	private FilterBehaviour_Raid filterBehaviour_raid = new FilterBehaviour_Raid(0);

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

	public FilterContent_Words getFilterContent_words(){
		return filterContent_words;
	}

	public void lSetFilterContent_words(FilterContent_Words filterContent_words){
		this.filterContent_words = filterContent_words;
	}

	public void setFilterContent_words(FilterContent_Words filterContent_words){
		lSetFilterContent_words(filterContent_words);
		update();
	}

	public FilterContent_URLs getFilterContent_urls(){
		return filterContent_urls;
	}

	public void lSetFilterContent_urls(FilterContent_URLs filterContent_urls){
		this.filterContent_urls = filterContent_urls;
	}

	public void setFilterContent_urls(FilterContent_URLs filterContent_urls){
		lSetFilterContent_urls(filterContent_urls);
		update();
	}

	public FilterBehaviour_Spam getFilterBehaviour_spam(){
		return filterBehaviour_spam;
	}

	public void lSetFilterBehaviour_spam(FilterBehaviour_Spam filterBehaviour_spam){
		this.filterBehaviour_spam = filterBehaviour_spam;
	}

	public void setFilterBehaviour_spam(FilterBehaviour_Spam filterBehaviour_spam){
		lSetFilterBehaviour_spam(filterBehaviour_spam);
		update();
	}

	public FilterBehaviour_Raid getFilterBehaviour_raid(){
		return filterBehaviour_raid;
	}

	public void lSetFilterBehaviour_raid(FilterBehaviour_Raid filterBehaviour_raid){
		this.filterBehaviour_raid = filterBehaviour_raid;
	}

	public void setFilterBehaviour_raid(FilterBehaviour_Raid filterBehaviour_raid){
		lSetFilterBehaviour_raid(filterBehaviour_raid);
		update();
	}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("guildId", guildId)
			.put("channelId", channelId)
			.put("filterContentWords", filterContent_words.getValue())
			.put("filterContentURLs", filterContent_urls.getValue())
			.put("filterBehaviourSpam", filterBehaviour_spam.getValue())
			.put("filterBehaviourRaid", filterBehaviour_raid.getValue());
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		this.filterContent_words = new FilterContent_Words(jsonObject.getInt("filterContentWords"));
		this.filterContent_urls = new FilterContent_URLs(jsonObject.getInt("filterContentURLs"));
		this.filterBehaviour_spam = new FilterBehaviour_Spam(jsonObject.getInt("filterBehaviourSpam"));
		this.filterBehaviour_raid = new FilterBehaviour_Raid(jsonObject.getInt("filterBehaviourRaid"));
	}

	public static class FilterContent_Words extends IntegerBitFlags {

		public FilterContent_Words(int value){
			super(value);
		}

		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterContent_URLs.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			// punishments
			BAN(21),
			KICK(20),
			WARN_USER_COUNT_BIT_3(19),
			WARN_USER_COUNT_BIT_2(18),
			WARN_USER_COUNT_BIT_1(17),
			WARN_USER_COUNT_BIT_0(16),
			NOTIFY_USER(15),

			// cleanup
			DELETE_MESSAGE(8),

			// bad word set identifier
			BAD_WORD_SET_IDENTIFIER_BIT_2(3),
			BAD_WORD_SET_IDENTIFIER_BIT_1(2),
			BAD_WORD_SET_IDENTIFIER_BIT_0(1),
			// general
			APPLY(0);

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

		public class Helper{

		}

	}

	public static class FilterContent_URLs extends IntegerBitFlags {

		public FilterContent_URLs(int value){
			super(value);
		}

		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterContent_URLs.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			// punishments
			BAN(21),
			KICK(20),
			WARN_USER_COUNT_BIT_3(19),
			WARN_USER_COUNT_BIT_2(18),
			WARN_USER_COUNT_BIT_1(17),
			WARN_USER_COUNT_BIT_0(16),
			NOTIFY_USER(15),

			// cleanup
			DELETE_MESSAGE(8),

			// url filter
			IPS(5),
			OTHER_URLS(4),
			TWITCH_URLS(3),
			YOUTUBE_URLS(2),
			INVITE_URLS(1),
			// general
			APPLY(0);

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

		public class Helper{

		}

	}

	public static class FilterBehaviour_Spam extends IntegerBitFlags {

		public FilterBehaviour_Spam(int value){
			super(value);
		}

		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterBehaviour_Spam.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit {
			// punishments
			BAN(21),
			KICK(20),
			WARN_USER_COUNT_BIT_3(19),
			WARN_USER_COUNT_BIT_2(18),
			WARN_USER_COUNT_BIT_1(17),
			WARN_USER_COUNT_BIT_0(16),
			NOTIFY_USER(15),

			// cleanup
			DELETE_MESSAGE(8),

			// triggers
			SPAM_DELAY_BIT_4(4),
			SPAM_DELAY_BIT_3(4),
			SPAM_DELAY_BIT_2(4),
			SPAM_DELAY_BIT_1(4),
			SPAM_MULTILINE(3),
			SPAM_REPOST(2),
			SPAM(1),
			// general
			APPLY(0);

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

		public class Helper{


		}

	}

	public static class FilterBehaviour_Raid extends IntegerBitFlags {

		public FilterBehaviour_Raid(int value){
			super(value);
		}

		@Override
		public <T extends IntBit> List<T> getBits(){
			return (List<T>) Arrays.stream(FilterBehaviour_Raid.Setting.values()).filter(this::has).collect(Collectors.toList());
		}

		public enum Setting implements IntBit{
			// punishments
			BAN(21),
			KICK(20),

			// action
			COOLDOWN_CHAT_DURATION_BIT_3(12),
			COOLDOWN_CHAT_DURATION_BIT_2(11),
			COOLDOWN_CHAT_DURATION_BIT_1(10),
			COOLDOWN_CHAT_DURATION_BIT_0(9),
			COOLDOWN_CHAT(8),

			// triggers
			SPAM_SCALE_BIT_3(4),
			SPAM_SCALE_BIT_2(3),
			SPAM_SCALE_BIT_1(2),
			SPAM_SCALE_BIT_0(1),
			// general
			APPLY(0);

			private final int pos;

			Setting(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

		public class Helper{

			enum Punishment{
				BAN,
				KICK,
				NONE
			}

			public Helper setPunishment(Punishment punishment){
				FilterBehaviour_Raid.this.unset(Setting.BAN, Setting.KICK);
				switch(punishment){
					case BAN -> {
						FilterBehaviour_Raid.this.set(Setting.BAN);
					}
					case KICK -> {
						FilterBehaviour_Raid.this.set(Setting.KICK);
					}
				}
				return this;
			}

			public Helper setChatCooldownDuration(int value){
				if(value > 60 || value < 0){
					return this;
				}
				FilterBehaviour_Raid.this.unset(Setting.COOLDOWN_CHAT_DURATION_BIT_0, Setting.COOLDOWN_CHAT_DURATION_BIT_1, Setting.COOLDOWN_CHAT_DURATION_BIT_2, Setting.COOLDOWN_CHAT_DURATION_BIT_3);
				if((((value >> 2) >> 0) & 1) == 1){
					FilterBehaviour_Raid.this.set(Setting.COOLDOWN_CHAT_DURATION_BIT_0);
				}
				if((((value >> 2) >> 1) & 1) == 1){
					FilterBehaviour_Raid.this.set(Setting.COOLDOWN_CHAT_DURATION_BIT_1);
				}
				if((((value >> 2) >> 2) & 1) == 1){
					FilterBehaviour_Raid.this.set(Setting.COOLDOWN_CHAT_DURATION_BIT_2);
				}
				if((((value >> 2) >> 3) & 1) == 1){
					FilterBehaviour_Raid.this.set(Setting.COOLDOWN_CHAT_DURATION_BIT_3);
				}
				return this;
			}

			public int getChatCooldownDuration(){
				int i = 0;
				if(FilterBehaviour_Raid.this.has(Setting.COOLDOWN_CHAT_DURATION_BIT_0)){
					i |= 1;
				}
				if(FilterBehaviour_Raid.this.has(Setting.COOLDOWN_CHAT_DURATION_BIT_1)){
					i |= 1 << 1;
				}
				if(FilterBehaviour_Raid.this.has(Setting.COOLDOWN_CHAT_DURATION_BIT_2)){
					i |= 1 << 2;
				}
				if(FilterBehaviour_Raid.this.has(Setting.COOLDOWN_CHAT_DURATION_BIT_3)){
					i |= 1 << 3;
				}
				return i << 2;
			}

			public Helper setSpamScaleThreshold(int value){
				if(value > 60 || value < 0){
					return this;
				}
				FilterBehaviour_Raid.this.unset(Setting.SPAM_SCALE_BIT_0, Setting.SPAM_SCALE_BIT_1, Setting.SPAM_SCALE_BIT_2, Setting.SPAM_SCALE_BIT_3);
				if((((value >> 2) >> 0) & 1) == 1){
					FilterBehaviour_Raid.this.set(Setting.SPAM_SCALE_BIT_0);
				}
				if((((value >> 2) >> 1) & 1) == 1){
					FilterBehaviour_Raid.this.set(Setting.SPAM_SCALE_BIT_1);
				}
				if((((value >> 2) >> 2) & 1) == 1){
					FilterBehaviour_Raid.this.set(Setting.SPAM_SCALE_BIT_2);
				}
				if((((value >> 2) >> 3) & 1) == 1){
					FilterBehaviour_Raid.this.set(Setting.SPAM_SCALE_BIT_3);
				}
				return this;
			}

			public int getSpamScaleThreshold(){
				int i = 0;
				if(FilterBehaviour_Raid.this.has(Setting.SPAM_SCALE_BIT_0)){
					i |= 1;
				}
				if(FilterBehaviour_Raid.this.has(Setting.SPAM_SCALE_BIT_1)){
					i |= 1 << 1;
				}
				if(FilterBehaviour_Raid.this.has(Setting.SPAM_SCALE_BIT_2)){
					i |= 1 << 2;
				}
				if(FilterBehaviour_Raid.this.has(Setting.SPAM_SCALE_BIT_3)){
					i |= 1 << 3;
				}
				return i << 2;
			}

			public Helper enable(boolean value){
				FilterBehaviour_Raid.this.unset(Setting.APPLY);
				if(value){
					FilterBehaviour_Raid.this.set(Setting.APPLY);
				}
				return this;
			}

			public boolean isEnabled(){
				return FilterBehaviour_Raid.this.has(Setting.APPLY);
			}

		}

	}

}
