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

package de.netbeacon.xenia.backend.client.objects.internal.ws;

import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSProcessorCore;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SecondaryWebsocketListener extends WebsocketListener{

	private final WSProcessorCore wsProcessorCore;
	private final Logger logger = LoggerFactory.getLogger(SecondaryWebsocketListener.class);

	public SecondaryWebsocketListener(XeniaBackendClient xeniaBackendClient, WSProcessorCore wsProcessorCore){
		super(xeniaBackendClient, "ws/secondary");
		this.wsProcessorCore = wsProcessorCore;
		this.wsProcessorCore.setWSL(this);
	}

	@Override
	public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response){
		logger.warn("Connected To Websocket");
	}

	@Override
	public void onMessage(@NotNull WebSocket webSocket, @NotNull String text){
		wsProcessorCore.handle(new JSONObject(text));
	}

	@Override
	public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes){
		wsProcessorCore.handle(new JSONObject(bytes.toString()));
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

	public WSProcessorCore getWsProcessorCore(){
		return wsProcessorCore;
	}

	@Override
	public void onShutdown() throws Exception{
		super.onShutdown();
		wsProcessorCore.onShutdown();
	}

}
