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

import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSRequest;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSResponse;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InterruptProcessor extends WSProcessor {

    private final XeniaBackendClient xeniaBackendClient;
    private final Logger logger = LoggerFactory.getLogger(InterruptProcessor.class);
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public InterruptProcessor(XeniaBackendClient xeniaBackendClient) {
        super("irq");
        this.xeniaBackendClient = xeniaBackendClient;
    }

    @Override
    public WSResponse process(WSRequest wsRequest) {
        // get payload
        JSONObject payload = wsRequest.getPayload();
        // test shardmanager
        ShardManager shardManager = xeniaBackendClient.getShardManagerSupplier().get();
        if(shardManager == null){
            logger.error("Received Interrupt Request While ShardManager Is Null");
            return null;
        }
        // save old values
        Activity activity = shardManager.getShards().get(0).getPresence().getActivity();
        String descriptionOldT = "null";
        if(activity != null){
            descriptionOldT = activity.getName();
        }
        final String descriptionOld = descriptionOldT;
        // get new values
        OnlineStatus onlineStatus = OnlineStatus.fromKey(payload.getString("state"));
        String description = payload.getString("stateDescription");
        long initialDelay = payload.getLong("initialDelay");
        long delay = payload.getLong("delay");
        logger.warn("Received Interrupt Request. Initial Delay: "+initialDelay+" Delay: "+delay);
        // update presence, "shutdown" event managers
        shardManager.setPresence(onlineStatus, Activity.playing(description));
        shardManager.getShards().stream().map(JDA::getEventManager).forEach(eventManager -> {
            //if(eventManager instanceof MultiThreadedEventManager){
            //    ((MultiThreadedEventManager) eventManager).halt(true);
            //}
        });
        // initialize backend client shutdown
        scheduledExecutorService.schedule(()->{
            try{
                xeniaBackendClient.pauseExecution();
                scheduledExecutorService.schedule(()->{
                    try{
                        xeniaBackendClient.resumeExecution();
                        shardManager.getShards().stream().map(JDA::getEventManager).forEach(eventManager -> {
                            //if(eventManager instanceof MultiThreadedEventManager){
                            //    ((MultiThreadedEventManager) eventManager).halt(false);
                            //}
                        });
                        shardManager.setPresence(OnlineStatus.ONLINE, Activity.playing(descriptionOld));
                    }catch (Exception e){
                        logger.error("Failed To Start Backend Client & EventManagers Back Up.");
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }catch (Exception e){
                logger.error("Failed To Pause Backend Client In IRQ");
            }
        }, initialDelay, TimeUnit.MILLISECONDS);

        return null;
    }
}
