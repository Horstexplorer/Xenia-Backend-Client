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

package de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp;

import de.netbeacon.xenia.backend.client.objects.cache.misc.TwitchNotificationCache;
import de.netbeacon.xenia.backend.client.objects.external.misc.TwitchNotification;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSRequest;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSResponse;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class TwitchNotificationProcessor extends WSProcessor {

    private final Logger logger = LoggerFactory.getLogger(TwitchNotificationProcessor.class);

    public TwitchNotificationProcessor() {
        super("twitchnotify");
    }

    @Override
    public WSResponse process(WSRequest wsRequest) {
        try{
            JSONObject data = wsRequest.getPayload();
            if(getWsProcessorCore().getXeniaBackendClient().getShardManagerSupplier().get() == null) return null;
            // get the guild
            ShardManager shardManager = getWsProcessorCore().getXeniaBackendClient().getShardManagerSupplier().get() ;
            Guild guild = shardManager.getGuildById(data.getLong("guildId"));
            if(guild == null){ return null; }
            // get the notification details
            TwitchNotificationCache notificationCache = getWsProcessorCore().getXeniaBackendClient().getGuildCache().get(data.getLong("guildId"), false).getMiscCaches().getTwitchNotificationCache();
            TwitchNotification twitchNotification = notificationCache.get(data.getLong("twitchNotificationId"));
            // get the channel
            TextChannel textChannel = guild.getTextChannelById(twitchNotification.getChannelId());
            // check permissions
            if(textChannel == null || !guild.getSelfMember().hasAccess(textChannel) || !textChannel.canTalk(guild.getSelfMember())){
                // delete
                notificationCache.delete(twitchNotification.getId());
                return null;
            }
            // prepare message to send
            String customMessage = twitchNotification.getNotificationMessage();
            customMessage = customMessage.replace("$username$", data.getJSONObject("data").getString("channelName"));
            customMessage = customMessage.replace("$title$", data.getJSONObject("data").getString("streamTitle"));
            customMessage = customMessage.replace("$game$", data.getJSONObject("data").getString("game"));
            String url = "https://twitch.tv/"+twitchNotification.getTwitchChannelName();
            // prepare embed
            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setTitle(data.getJSONObject("data").getString("streamTitle"), url)
                    .setDescription(customMessage)
                    .addField("Game", data.getJSONObject("data").getString("game"), true)
                    .addField("View", "[ Link ]("+url+")", true)
                    .setColor(new Color(145,70,225))
                    .setImage("https://static-cdn.jtvnw.net/previews-ttv/live_user_"+twitchNotification.getTwitchChannelName()+"-640x360.jpg")
                    .build();
            // send
            textChannel.sendMessage(messageEmbed).queue();
        }catch (Exception e){
            logger.warn("Failed To Dispatch Stream Notification: ", e);
        }
        return null;
    }
}
