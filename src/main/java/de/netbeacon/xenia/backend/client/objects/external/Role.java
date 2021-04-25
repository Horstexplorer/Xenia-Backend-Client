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

import de.netbeacon.utils.bitflags.LongBitFlags;
import de.netbeacon.utils.json.serial.JSONSerializationException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.objects.APIDataObject;
import org.json.JSONObject;

import java.util.function.Supplier;

public class Role extends APIDataObject{

	private long guildId;
	private long roleId;

	private String roleName;
	private Permissions permissions;

	public Role(BackendProcessor backendProcessor, long guildId, long roleId){
		super(backendProcessor);
		this.guildId = guildId;
		this.roleId = roleId;
		this.permissions = new Permissions(this, 1);
		setBackendPath("data", "guilds", (Supplier<Long>) this::getGuildId, "roles", (Supplier<Long>) this::getId);
	}

	public long getId(){
		return roleId;
	}

	public long getGuildId(){
		return guildId;
	}

	public String getRoleName(){
		return roleName;
	}

	public void setRoleName(String name){
		lSetRoleName(name);
		update();
	}

	public void lSetRoleName(String name){
		secure();
		this.roleName = name;
	}

	public Permissions getPermissions(){
		return permissions;
	}

	// SECONDARY

	public Guild getGuild(){
		return getBackendProcessor().getBackendClient().getGuildCache().get(guildId, false);
	}

	@Override
	public JSONObject asJSON() throws JSONSerializationException{
		return new JSONObject()
			.put("guildId", guildId)
			.put("roleId", roleId)
			.put("roleName", roleName)
			.put("rolePermissions", permissions.getValue());
	}

	@Override
	public void fromJSON(JSONObject jsonObject) throws JSONSerializationException{
		this.guildId = jsonObject.getLong("guildId");
		this.roleId = jsonObject.getLong("roleId");
		this.roleName = jsonObject.getString("roleName");
		this.permissions = new Permissions(this, jsonObject.getLong("rolePermissions"));
	}

	public static class Permissions extends LongBitFlags{

		private final Role role;

		public enum Bit implements LongBit{

			UNUSED_PERMISSION_BIT_63(63),
			// OWNER
			GUILD_OWNER_OVERRIDE(62),
			// SETTINGS
			GUILD_SETTINGS_OVERRIDE(61),
			// ROLES
			GUILD_ROLES_OVERRIDE(60),
			// CHANNELS
			GUILD_CHANNEL_OVERRIDE(59),

			UNUSED_PERMISSION_BIT_58(58),
			UNUSED_PERMISSION_BIT_57(57),
			UNUSED_PERMISSION_BIT_56(56),
			UNUSED_PERMISSION_BIT_55(55),
			UNUSED_PERMISSION_BIT_54(54),
			UNUSED_PERMISSION_BIT_53(53),
			UNUSED_PERMISSION_BIT_52(52),
			UNUSED_PERMISSION_BIT_51(51),
			UNUSED_PERMISSION_BIT_50(50),
			UNUSED_PERMISSION_BIT_49(49),
			UNUSED_PERMISSION_BIT_48(48),
			UNUSED_PERMISSION_BIT_47(47),
			UNUSED_PERMISSION_BIT_46(46),
			UNUSED_PERMISSION_BIT_45(45),
			UNUSED_PERMISSION_BIT_44(44),
			UNUSED_PERMISSION_BIT_43(43),
			UNUSED_PERMISSION_BIT_42(42),
			UNUSED_PERMISSION_BIT_41(41),
			UNUSED_PERMISSION_BIT_40(40),
			UNUSED_PERMISSION_BIT_39(39),
			UNUSED_PERMISSION_BIT_38(38),
			UNUSED_PERMISSION_BIT_37(37),
			UNUSED_PERMISSION_BIT_36(36),
			UNUSED_PERMISSION_BIT_35(35),
			UNUSED_PERMISSION_BIT_34(34),
			UNUSED_PERMISSION_BIT_33(33),
			UNUSED_PERMISSION_BIT_32(32),
			UNUSED_PERMISSION_BIT_31(31),
			UNUSED_PERMISSION_BIT_30(30),
			UNUSED_PERMISSION_BIT_29(29),
			UNUSED_PERMISSION_BIT_28(28),
			UNUSED_PERMISSION_BIT_27(27),
			UNUSED_PERMISSION_BIT_26(26),
			UNUSED_PERMISSION_BIT_25(25),
			UNUSED_PERMISSION_BIT_24(24),
			UNUSED_PERMISSION_BIT_23(23),
			UNUSED_PERMISSION_BIT_22(22),
			UNUSED_PERMISSION_BIT_21(21),
			UNUSED_PERMISSION_BIT_20(20),
			UNUSED_PERMISSION_BIT_19(19),
			UNUSED_PERMISSION_BIT_18(18),
			UNUSED_PERMISSION_BIT_17(17),
			UNUSED_PERMISSION_BIT_16(16),
			UNUSED_PERMISSION_BIT_15(15),
			UNUSED_PERMISSION_BIT_14(14),
			UNUSED_PERMISSION_BIT_13(13),
			UNUSED_PERMISSION_BIT_12(12),
			// ANIME
			ANIME_NSFW_USE(11),
			ANIME_SFW_USE(10),
			// TWITCH NOTIFICATIONS
			TWITCH_NOTIFICATIONS_MANAGE(9),
			// HASTEBIN
			HASTEBIN_UPLOAD_USE(8),
			// MESSAGE_RESTORE
			MESSAGE_RESTORE_OVERRIDE(7),
			MESSAGE_RESTORE_USE(6),
			// NOTIFICATION
			NOTIFICATION_OVERRIDE(5),
			NOTIFICATION_USE(4),
			// TAG
			TAG_OVERRIDE(3),
			TAG_CREATE(2),
			TAG_USE(1),
			// INTERACT
			BOT_INTERACT(0);

			private final int pos;

			Bit(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

		public Permissions(Role role, long permVal){
			super(permVal);
			this.role = role;
		}

		public synchronized void enable(Bit... bits){
			set(bits);
			role.update();
		}

		public synchronized void disable(Bit... bits){
			unset(bits);
			role.update();
		}

		public boolean hasPermission(Bit bit){
			return has(bit);
		}

		public boolean hasAllPermission(Bit... bits){
			for(Bit b : bits){
				if(!has(b)){
					return false;
				}
			}
			return true;
		}

	}

}
