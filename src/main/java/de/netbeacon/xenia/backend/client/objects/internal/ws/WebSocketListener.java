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

import de.netbeacon.utils.shutdownhook.IShutdown;
import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import de.netbeacon.xenia.backend.client.objects.external.Channel;
import de.netbeacon.xenia.backend.client.objects.external.Guild;
import de.netbeacon.xenia.backend.client.objects.internal.BackendSettings;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class WebSocketListener extends okhttp3.WebSocketListener implements IShutdown {

    private final Logger logger = LoggerFactory.getLogger(WebSocketListener.class);
    private final XeniaBackendClient xeniaBackendClient;
    private WebSocket webSocket;

    public WebSocketListener(XeniaBackendClient xeniaBackendClient){
        this.xeniaBackendClient = xeniaBackendClient;
    }

    public WebSocketListener start(){
        if(webSocket != null){
            webSocket.close(1000, "Reconnecting Soon");
        }
        BackendSettings backendSettings = xeniaBackendClient.getBackendProcessor().getBackendSettings();
        String host = backendSettings.getHost();
        int port = backendSettings.getPort();
        String token = backendSettings.getToken();
        // build request
        Request request = new Request.Builder().url("wss://"+host+":"+port+"/ws?token="+token).build();
        webSocket = xeniaBackendClient.getOkHttpClient().newWebSocket(request, this);
        return this;
    }

    public WebSocketListener stop(){
        webSocket.close(1000, "Closed Connection");
        webSocket = null;
        return this;
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        logger.warn("Connected To Websocket");
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        handle(new JSONObject(text));
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        handle(new JSONObject(bytes.toString()));
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        logger.warn("Websocket Closed: "+code+" "+reason);
        webSocket.close(code, reason);
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        logger.warn("Websocket Closed: "+code+" "+reason);
        if(code != 1000){
            logger.warn("Reconnecting On: "+code);
            start(); // reconnect
        }
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        logger.warn("Websocket Failure - Try Reconnecting");
        try{
            TimeUnit.SECONDS.sleep(4);
            start();
        }catch (Exception ignore){}
        t.printStackTrace();
    }

    public void handle(JSONObject message){
        try{
            String type = message.getString("type");
            String action = message.getString("action");
            switch (type.toLowerCase()){
                case "status":
                    logger.info("Recieved Status From WS: "+action);
                    break;
                case "user":
                    if(!xeniaBackendClient.getUserCache().contains(message.getLong("userId"))){
                        return;
                    }else if(!action.equalsIgnoreCase("create")){
                        xeniaBackendClient.getUserCache().remove(message.getLong("userId"));
                    }
                    break;
                case "guild":
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }else if(!action.equalsIgnoreCase("create")){
                        xeniaBackendClient.getGuildCache().remove(message.getLong("guildId"));
                    }
                    break;
                case "guild_role":
                case "guild_role_permission":
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }else if(!action.equalsIgnoreCase("create")){
                        Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                        g.getRoleCache().remove(message.getLong("roleId"));
                    }
                    break;
                case "guild_channel":
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }else if(!action.equalsIgnoreCase("create")){
                        Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                        g.getChannelCache().remove(message.getLong("channelId"));
                    }
                    break;
                case "guild_message":
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }else if(!action.equalsIgnoreCase("create")){
                        Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                        if(!g.getChannelCache().contains(message.getLong("channelId"))){
                            return;
                        }
                        Channel c = g.getChannelCache().get(message.getLong("channelId"));
                        c.getMessageCache().remove(message.getLong("messageId"));
                    }
                    break;
                case "guild_license":
                    if(!xeniaBackendClient.getLicenseCache().contains(message.getLong("guildId"))){
                        return;
                    }else{
                        xeniaBackendClient.getLicenseCache().remove(message.getLong("guildId"));
                    }
                    break;
                case "guild_member":
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }else if(!action.equalsIgnoreCase("create")){
                        Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                        g.getMemberCache().remove(message.getLong("userId"));
                    }
                    break;
                case "guild_misc_tag":
                    break;
            }
        }catch (Exception e){
            logger.warn("Error Processing Message, Cache Might Be Inconsistent: "+message.toString());
        }
    }

    @Override
    public void onShutdown() throws Exception {
        stop();
    }
}
