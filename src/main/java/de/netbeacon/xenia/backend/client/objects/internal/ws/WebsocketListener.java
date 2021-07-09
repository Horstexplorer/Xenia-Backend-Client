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

import de.netbeacon.utils.executor.ScalingExecutor;
import de.netbeacon.utils.shutdownhook.IShutdown;
import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import de.netbeacon.xenia.backend.client.objects.internal.BackendSettings;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WebsocketListener extends okhttp3.WebSocketListener implements IShutdown{

	protected final XeniaBackendClient xeniaBackendClient;
	private final String wsPath;
	protected ScalingExecutor scalingExecutor;
	protected WebSocket webSocket;
	protected AtomicBoolean shutdown = new AtomicBoolean(true);

	public WebsocketListener(XeniaBackendClient xeniaBackendClient, String wsPath){
		this.xeniaBackendClient = xeniaBackendClient;
		this.wsPath = wsPath;
	}

	public void start(){
		if(scalingExecutor != null){
			scalingExecutor.shutdown();
		}
		if(webSocket != null){
			webSocket.close(1000, "Reconnecting Soon");
		}
		scalingExecutor = new ScalingExecutor(2, 6, -1, 30, TimeUnit.SECONDS);
		BackendSettings backendSettings = xeniaBackendClient.getBackendProcessor().getBackendSettings();
		String host = backendSettings.getHost();
		int port = backendSettings.getPort();
		String token = backendSettings.getToken();
		// build request
		Request request = new Request.Builder().url("wss://" + host + ":" + port + "/" + wsPath + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)).build();
		webSocket = xeniaBackendClient.getOkHttpClient().newWebSocket(request, this);
		shutdown.set(false);
	}

	public void stop(){
		shutdown.set(true);
		scalingExecutor.shutdown();
		scalingExecutor = null;
		webSocket.close(1000, "Closed Connection");
		webSocket = null;
	}

	public void send(String message){
		webSocket.send(message);
	}

	@Override
	public abstract void onOpen(@NotNull WebSocket webSocket, @NotNull Response response);

	@Override
	public abstract void onMessage(@NotNull WebSocket webSocket, @NotNull String text);

	@Override
	public abstract void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes);

	@Override
	public abstract void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason);

	@Override
	public abstract void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason);

	@Override
	public abstract void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response);

	@Override
	public void onShutdown() throws Exception{
		stop();
	}

}
