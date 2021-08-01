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

public class Role extends APIDataObject<Role>{

	private long guildId;
	private long roleId;

	private String roleName;
	private Permissions permissions;

	public Role(BackendProcessor backendProcessor, long guildId, long roleId){
		super(backendProcessor);
		this.guildId = guildId;
		this.roleId = roleId;
		this.permissions = new Permissions(this, 1283);
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

		public enum Bit implements LongBit{

			// OWNER
			GUILD_OWNER_OVERRIDE(62), // 0
			// SETTINGS
			GUILD_SETTINGS_OVERRIDE(61), // 0
			// ROLES
			GUILD_ROLES_OVERRIDE(60), // 0
			// CHANNELS
			GUILD_CHANNEL_OVERRIDE(59), // 0
			// USERS
			GUILD_MEMBERS_MANAGE(58), // 0



			// ANIME
			ANIME_NSFW_USE(11), // 0
			ANIME_SFW_USE(10), // 1
			// TWITCH NOTIFICATIONS
			TWITCH_NOTIFICATIONS_MANAGE(9), // 0
			// HASTEBIN
			HASTEBIN_UPLOAD_USE(8), // 1
			// MESSAGE_RESTORE
			MESSAGE_RESTORE_OVERRIDE(7), // 0
			MESSAGE_RESTORE_USE(6), // 0
			// NOTIFICATION
			NOTIFICATION_OVERRIDE(5), // 0
			NOTIFICATION_USE(4), // 0
			// TAG
			TAG_OVERRIDE(3), // 0
			TAG_CREATE(2), // 0
			TAG_USE(1), // 1
			// INTERACT
			BOT_INTERACT(0); // 1

			// DEFAULT_INT = 1283

			private final int pos;

			Bit(int pos){
				this.pos = pos;
			}

			@Override
			public int getPos(){
				return pos;
			}
		}

	}

}
