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
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp1.HeartbeatProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp1.PrimaryWSProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp1.StatusProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp1.cache.*;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PrimaryWebsocketListener extends WebsocketListener{

	private final Logger logger = LoggerFactory.getLogger(PrimaryWebsocketListener.class);
	private final HashMap<String, PrimaryWSProcessor> processors = new HashMap<>();
	private final Consumer<PrimaryWSProcessor> register = p -> processors.put(p.ofType(), p);


	public PrimaryWebsocketListener(XeniaBackendClient xeniaBackendClient){
		super(xeniaBackendClient, "ws");

		register.accept(new StatusProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new HeartbeatProcessor(xeniaBackendClient, scalingExecutor));

		register.accept(new CacheGuildMiscTwitchNotificationProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheGuildMiscNotificationProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheGuildMiscTagProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheGuildLicenseProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheGuildProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheGuildMemberProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheGuildMessageProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheGuildRoleProcessor(xeniaBackendClient, scalingExecutor));
		processors.put("guild_role_permission", new CacheGuildRoleProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheGuildChannelProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheGuildChannelAutoModProcessor(xeniaBackendClient, scalingExecutor));
		register.accept(new CacheUserProcessor(xeniaBackendClient, scalingExecutor));


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
			String type = message.getString("type").toLowerCase(Locale.ROOT);
			var v = processors.get(type);
			if(v == null){
				logger.warn("Unknown Event Type " + type + " On Message " + message);
				return;
			}
			v.accept(message);
		}
		catch(Exception e){
			logger.warn("Error Processing Message, Cache Might Be Inconsistent: " + message.toString());
		}
	}

}
