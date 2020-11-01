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

import de.netbeacon.utils.executor.ScalingExecutor;
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
    private ScalingExecutor scalingExecutor;
    private WebSocket webSocket;

    public WebSocketListener(XeniaBackendClient xeniaBackendClient){
        this.xeniaBackendClient = xeniaBackendClient;
    }

    // todo: improve status codes on backend so we have a better knowledge of what is going on

    public void start(){
        if(scalingExecutor != null){
            scalingExecutor.shutdown();
        }
        if(webSocket != null){
            webSocket.close(1000, "Reconnecting Soon");
        }
        scalingExecutor = new ScalingExecutor(2, 14, 12000, 30, TimeUnit.SECONDS);
        BackendSettings backendSettings = xeniaBackendClient.getBackendProcessor().getBackendSettings();
        String host = backendSettings.getHost();
        int port = backendSettings.getPort();
        String token = backendSettings.getToken();
        // build request
        Request request = new Request.Builder().url("wss://"+host+":"+port+"/ws?token="+token).build();
        webSocket = xeniaBackendClient.getOkHttpClient().newWebSocket(request, this);
    }

    public void stop(){
        scalingExecutor.shutdown();
        scalingExecutor = null;
        webSocket.close(1000, "Closed Connection");
        webSocket = null;
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
        logger.debug("Websocket Closing: "+code+" "+reason);
        webSocket.close(code, reason);
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        logger.debug("Websocket Closed: "+code+" "+reason);
        if(code != 1000){
            logger.warn("Reconnecting On: "+code);
            start(); // reconnect
        }else{
            logger.warn("Websocket Closed - Wont Open Again "+code+" "+reason);
        }
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        logger.warn("Websocket Failure - Trying To Reconnect "+response, t);
        try{
            TimeUnit.SECONDS.sleep(2);
            start();
        }catch (Exception ignore){}
    }

    private long lastHeartBeat = System.currentTimeMillis();

    public void handle(JSONObject message){
        try{
            String type = message.getString("type");
            String action = message.getString("action");
            logger.debug("Received Message From WS: "+message.toString());
            switch (type.toLowerCase()){
                case "status":
                    logger.debug("Received Status From WS: "+action);
                    break;
                case "heartbeat":
                {
                    long newHeartBeat = message.getLong("timestamp");
                    long delay = (newHeartBeat-lastHeartBeat);
                    if(delay > 30000*2){
                        logger.warn("Received Heartbeat After "+delay+"ms (Delay To Target "+(delay-30000)+") Missed At Least "+(delay/30000)+" Heartbeat(s). The Network Might Be Faulty!");
                    }else if(delay > 30000*1.5){
                        logger.info("Received Heartbeat After "+delay+"ms (Delay To Target "+(delay-30000)+") The Service Might Be Slow.");
                    }else{
                        logger.debug("Received Heartbeat After "+delay+"ms (Delay To Target "+(delay-30000)+")");
                    }
                    lastHeartBeat = newHeartBeat;
                }
                break;
                case "user":
                {
                    if(!xeniaBackendClient.getUserCache().contains(message.getLong("userId"))){
                        return;
                    }
                    switch (action.toLowerCase()){
                        case "create":
                            break;
                        case "update":
                            xeniaBackendClient.getUserCache().get(message.getLong("userId")).getAsync();
                            break;
                        case "delete":
                            xeniaBackendClient.getUserCache().remove(message.getLong("userId"));
                            break;
                    }
                }
                break;
                case "guild":
                {
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }
                    switch (action.toLowerCase()){
                        case "create":
                            break;
                        case "update":
                            xeniaBackendClient.getGuildCache().get(message.getLong("guildId")).getAsync(); // this just gets the new data as we dont want to reload all channels, roles, members,...
                            break;
                        case "delete":
                            xeniaBackendClient.getGuildCache().remove(message.getLong("guildId"));
                            break;
                    }
                }
                break;
                case "guild_role":
                case "guild_role_permission":
                {
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }
                    Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                    switch (action.toLowerCase()){
                        case "create":
                            scalingExecutor.execute(()->g.getRoleCache().get(message.getLong("roleId")));
                            break;
                        case "update":
                            g.getRoleCache().remove(message.getLong("roleId"));
                            scalingExecutor.execute(()->g.getRoleCache().get(message.getLong("roleId")));
                            break;
                        case "delete":
                            g.getRoleCache().remove(message.getLong("roleId"));
                            break;
                    }
                }
                break;
                case "guild_channel":
                {
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }
                    Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                    switch (action.toLowerCase()){
                        case "create":
                            scalingExecutor.execute(()->g.getChannelCache().get(message.getLong("channelId")));
                            break;
                        case "update":
                            g.getChannelCache().remove(message.getLong("channelId"));
                            scalingExecutor.execute(()->g.getChannelCache().get(message.getLong("channelId")));
                            break;
                        case "delete":
                            g.getChannelCache().remove(message.getLong("channelId"));
                            break;
                    }
                }
                break;
                case "guild_message":
                {
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }
                    Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                    if(!g.getChannelCache().contains(message.getLong("channelId"))){
                        return;
                    }
                    Channel c = g.getChannelCache().get(message.getLong("channelId"));
                    switch (action.toLowerCase()){
                        case "create":
                            scalingExecutor.execute(()->c.getMessageCache().get(message.getLong("messageId")));
                            break;
                        case "update":
                            c.getMessageCache().remove(message.getLong("messageId"));
                            scalingExecutor.execute(()->c.getMessageCache().get(message.getLong("messageId")));
                            break;
                        case "delete":
                            c.getMessageCache().remove(message.getLong("messageId"));
                            break;
                    }
                }
                break;
                case "guild_license":
                {
                    if(!xeniaBackendClient.getLicenseCache().contains(message.getLong("guildId"))){
                        return;
                    }else{
                        xeniaBackendClient.getLicenseCache().remove(message.getLong("guildId"));
                    }
                }
                break;
                case "guild_member":
                {
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }
                    Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                    switch (action.toLowerCase()){
                        case "create":
                            scalingExecutor.execute(()->g.getMemberCache().get(message.getLong("userId")));
                            break;
                        case "update":
                            g.getMemberCache().remove(message.getLong("userId"));
                            scalingExecutor.execute(()->g.getMemberCache().get(message.getLong("userId")));
                            break;
                        case "delete":
                            g.getMemberCache().remove(message.getLong("userId"));
                            break;
                    }
                }
                break;
                case "guild_misc_tag":
                {
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }
                    Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                    switch (action.toLowerCase()){
                        case "create":
                            scalingExecutor.execute(()->g.getMiscCaches().getTagCache().get(message.getString("tagName")));
                            break;
                        case "update":
                            g.getMiscCaches().getTagCache().remove(message.getString("tagName"));
                            scalingExecutor.execute(()->g.getMiscCaches().getTagCache().get(message.getString("tagName")));
                            break;
                        case "delete":
                            g.getMiscCaches().getTagCache().remove(message.getString("tagName"));
                            break;
                    }
                }
                break;
                case "guild_misc_notification":
                {
                    if(!xeniaBackendClient.getGuildCache().contains(message.getLong("guildId"))){
                        return;
                    }
                    Guild g = xeniaBackendClient.getGuildCache().get(message.getLong("guildId"));
                    switch (action.toLowerCase()){
                        case "create":
                            scalingExecutor.execute(()->g.getMiscCaches().getNotificationCache().get(message.getLong("notificationId")));
                            break;
                        case "update":
                            g.getMiscCaches().getNotificationCache().remove(message.getLong("notificationId"));
                            scalingExecutor.execute(()->g.getMiscCaches().getNotificationCache().get(message.getLong("notificationId")));
                            break;
                        case "delete":
                            g.getMiscCaches().getNotificationCache().remove(message.getLong("notificationId"));
                            break;
                    }
                }
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
