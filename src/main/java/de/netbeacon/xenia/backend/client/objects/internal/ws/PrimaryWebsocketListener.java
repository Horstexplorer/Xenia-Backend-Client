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

package de.netbeacon.xenia.backend.client.objects.internal.ws;

import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import de.netbeacon.xenia.backend.client.objects.external.Channel;
import de.netbeacon.xenia.backend.client.objects.external.Guild;
import de.netbeacon.xenia.backend.client.objects.external.User;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class PrimaryWebsocketListener extends WebsocketListener{

	private final Logger logger = LoggerFactory.getLogger(PrimaryWebsocketListener.class);
	private long lastHeartBeat = System.currentTimeMillis();

	public PrimaryWebsocketListener(XeniaBackendClient xeniaBackendClient){
		super(xeniaBackendClient, "ws");
	}

	@Override
	public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response){
		logger.warn("Connected To Websocket");
	}

	@Override
	public void onMessage(@NotNull WebSocket webSocket, @NotNull String text){
		handle(new JSONObject(text));
	}

	@Override
	public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes){
		handle(new JSONObject(bytes.toString()));
	}

	@Override
	public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason){
		logger.debug("Websocket Closing: " + code + " " + reason);
		webSocket.close(code, reason);
	}

	@Override
	public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason){
		logger.debug("Websocket Closed: " + code + " " + reason);
		// normal closure & shutdown || unauthorized || forbidden
		if(code == 1000 && shutdown.get() || code == 3401 || code == 3403){
			logger.warn("Websocket Closed - Wont Open Again " + code + " " + reason);
		}
		else{
			logger.debug("Reconnecting On: " + code);
			try{
				TimeUnit.SECONDS.sleep(1);
				start();
			}
			catch(Exception ignore){
			}
		}
	}

	@Override
	public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response){
		if(response != null){
			logger.warn("Websocket Failure - Trying To Reconnect: " + response.code() + " " + response.message(), t);
		}
		else{
			logger.warn("Websocket Failure - Trying To Reconnect: No Response", t);
		}
		try{
			TimeUnit.SECONDS.sleep(2);
			start();
		}
		catch(Exception ignore){
		}
	}

	public void handle(JSONObject message){
		try{
			String type = message.getString("type");
			String action = message.getString("action");
			logger.debug("Received Message From WS: " + message);
			switch(type.toLowerCase()){
				case "status":
					logger.debug("Received Status From WS: " + action);
					break;
				case "heartbeat":{
					long newHeartBeat = message.getLong("timestamp");
					long delay = (newHeartBeat - lastHeartBeat);
					if(delay > 30000 * 2){
						logger.warn("Received Heartbeat After " + delay + "ms (Delay To Target " + (delay - 30000) + ") Missed At Least " + (delay / 30000) + " Heartbeat(s). The Network Might Be Faulty!");
					}
					else if(delay > 30000 * 1.5){
						logger.info("Received Heartbeat After " + delay + "ms (Delay To Target " + (delay - 30000) + ") The Service Might Be Slow.");
					}
					else{
						logger.debug("Received Heartbeat After " + delay + "ms (Delay To Target " + (delay - 30000) + ")");
					}
					lastHeartBeat = newHeartBeat;
				}
				break;
				case "user":{
					if(!xeniaBackendClient.getUserCache().contains(message.getLong("userId"))){
						return;
					}
					User u = xeniaBackendClient.getUserCache().get(message.getLong("userId"), false);
					switch(action.toLowerCase()){
						case "create":
							break;
						case "update":
							u.getAsync(true);
							break;
						case "delete":
							u.onDeletion();
							xeniaBackendClient.getUserCache().remove(message.getLong("userId"));
							break;
					}
				}
				break;
				case "guild":{
					if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
						return;
					}
					Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"), false);
					switch(action.toLowerCase()){
						case "create":
							break;
						case "update":
							g.getAsync(true); // this just gets the new data as we dont want to reload all channels, roles, members,...
							break;
						case "delete":
							g.clear(true);
							xeniaBackendClient.getGuildCache().remove(message.getLong("guildId"));
							break;
					}
				}
				break;
				case "guild_role":
				case "guild_role_permission":{
					if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
						return;
					}
					Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"), false);
					switch(action.toLowerCase()){
						case "create":
							scalingExecutor.execute(() -> g.getRoleCache().get(message.getLong("roleId")));
							break;
						case "update":
							g.getRoleCache().get(message.getLong("roleId")).getAsync(true);
							break;
						case "delete":
							g.getRoleCache().get(message.getLong("roleId")).onDeletion();
							g.getRoleCache().remove(message.getLong("roleId"));
							break;
					}
				}
				break;
				case "guild_channel":{
					if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
						return;
					}
					Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"), false);
					switch(action.toLowerCase()){
						case "create":
							scalingExecutor.execute(() -> g.getChannelCache().get(message.getLong("channelId")));
							break;
						case "update":
							g.getChannelCache().get(message.getLong("channelId")).getAsync(true);
							break;
						case "delete":
							g.getChannelCache().get(message.getLong("channelId")).clear(true);
							g.getChannelCache().remove(message.getLong("channelId"));
							break;
					}
				}
				case "guild_channel_auto_mod":{
					if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
						return;
					}
					Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"), false);
					switch(action.toLowerCase()){
						case "update":
							g.getChannelCache().get(message.getLong("channelId")).getAutoMod().getAsync();
							break;
					}
				}
				break;
				case "guild_message":{
					if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
						return;
					}
					Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"), false);
					if(!g.getChannelCache().contains(message.getLong("channelId"))){
						return;
					}
					Channel c = g.getChannelCache().get(message.getLong("channelId"));
					switch(action.toLowerCase()){
						case "create":
							scalingExecutor.execute(() -> c.getMessageCache().get(message.getLong("messageId")));
							break;
						case "update":
							c.getMessageCache().get(message.getLong("messageId")).updateAsync(true);
							break;
						case "delete":
							c.getMessageCache().remove(message.getLong("messageId"));
							break;
					}
				}
				break;
				case "guild_license":{
					if(!xeniaBackendClient.getLicenseCache().contains(message.getLong("guildId"))){
						return;
					}
					else{
						xeniaBackendClient.getLicenseCache().remove(message.getLong("guildId"));
					}
				}
				break;
				case "guild_member":{
					if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
						return;
					}
					Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"), false);
					switch(action.toLowerCase()){
						case "create":
							scalingExecutor.execute(() -> g.getMemberCache().get(message.getLong("userId")));
							break;
						case "update":
							g.getMemberCache().get(message.getLong("userId")).getAsync(true);
							break;
						case "delete":
							g.getMemberCache().get(message.getLong("userId")).onDeletion();
							g.getMemberCache().remove(message.getLong("userId"));
							break;
					}
				}
				break;
				case "guild_misc_tag":{
					if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
						return;
					}
					Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"), false);
					switch(action.toLowerCase()){
						case "create":
							scalingExecutor.execute(() -> g.getMiscCaches().getTagCache().get(message.getString("tagName")));
							break;
						case "update":
							g.getMiscCaches().getTagCache().get(message.getString("tagName")).getAsync(true);
							break;
						case "delete":
							g.getMiscCaches().getTagCache().get(message.getString("tagName")).onDeletion();
							g.getMiscCaches().getTagCache().remove(message.getString("tagName"));
							break;
					}
				}
				break;
				case "guild_misc_notification":{
					if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
						return;
					}
					Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"), false);
					switch(action.toLowerCase()){
						case "create":
							scalingExecutor.execute(() -> g.getMiscCaches().getNotificationCache().get(message.getLong("notificationId")));
							break;
						case "update":
							g.getMiscCaches().getNotificationCache().get(message.getLong("notificationId")).getAsync(true);
							break;
						case "delete":
							g.getMiscCaches().getNotificationCache().get(message.getLong("notificationId")).onDeletion();
							g.getMiscCaches().getNotificationCache().remove(message.getLong("notificationId"));
							break;
					}
				}
				break;
				case "guild_misc_twitchnotification":{
					if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
						return;
					}
					Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"), false);
					switch(action.toLowerCase()){
						case "create":
							scalingExecutor.execute(() -> g.getMiscCaches().getTwitchNotificationCache().get(message.getLong("twitchNotificationId")));
							break;
						case "update":
							g.getMiscCaches().getTwitchNotificationCache().get(message.getLong("twitchNotificationId")).getAsync(true);
							break;
						case "delete":
							g.getMiscCaches().getTwitchNotificationCache().get(message.getLong("twitchNotificationId")).onDeletion();
							g.getMiscCaches().getTwitchNotificationCache().remove(message.getLong("twitchNotificationId"));
							break;
					}
				}
				break;
			}
		}
		catch(Exception e){
			logger.warn("Error Processing Message, Cache Might Be Inconsistent: " + message.toString());
		}
	}

}
