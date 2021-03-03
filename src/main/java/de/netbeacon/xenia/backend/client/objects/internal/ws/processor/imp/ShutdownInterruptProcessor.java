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

import de.netbeacon.xenia.backend.client.objects.external.system.Ping;
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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ShutdownInterruptProcessor extends WSProcessor {

    private final Logger logger = LoggerFactory.getLogger(ShutdownInterruptProcessor.class);
    private Future<?> future;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    public ShutdownInterruptProcessor() {
        super("shutdownirq");
    }

    @Override
    public WSResponse process(WSRequest wsRequest) {
        // get payload
        JSONObject payload = wsRequest.getPayload();
        // test shardmanager
        ShardManager shardManager = getWsProcessorCore().getXeniaBackendClient().getShardManagerSupplier().get();
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
        long delay = System.currentTimeMillis()-payload.getLong("at");
        logger.warn(
                "! Received Shutdown Interrupt Request From Backend !\n" +
                "To Make Sure Nothing Breaks Events Will Be Blocked And The Socket Will Be Shut Down.\n" +
                "Using Automated Pings To Detect When The Backend Is Up Again"
        );
        // update presence, "shutdown" event managers
        shardManager.setPresence(OnlineStatus.IDLE, Activity.playing("Backend Offline"));
        shardManager.getShards().stream().map(JDA::getEventManager).forEach(eventManager -> {
            //if(eventManager instanceof MultiThreadedEventManager){
            //    ((MultiThreadedEventManager) eventManager).halt(true);
            //}
        });
        // initialize backend client shutdown
        scheduledExecutorService.schedule(()->{
            try{
                getWsProcessorCore().getXeniaBackendClient().suspendExecution(true);
                future = scheduledExecutorService.scheduleAtFixedRate(()->{
                    try{
                        Ping p = new Ping(getWsProcessorCore().getXeniaBackendClient().getBackendProcessor());
                        if(!p.ping()){
                            return;
                        }
                        future.cancel(false); // let it run but this is the last time
                        // reenable everything
                        getWsProcessorCore().getXeniaBackendClient().suspendExecution(false);
                        shardManager.getShards().stream().map(JDA::getEventManager).forEach(eventManager -> {
                            //if(eventManager instanceof MultiThreadedEventManager){
                            //    ((MultiThreadedEventManager) eventManager).halt(false);
                            //}
                        });
                        shardManager.setPresence(OnlineStatus.ONLINE, Activity.playing(descriptionOld));
                        logger.warn("! Restored From Shutdown Interrupt !");
                    }catch (Exception e){
                        logger.warn(
                                "! Failed To Restore From Shutdown Interrupt !\n" +
                                        "The Bot Needs To Get Restarted Manually"
                        );
                    }
                }, 10, 10, TimeUnit.SECONDS);
            }catch (Exception e){
                logger.error("Failed To Pause Backend Client In IRQ");
            }
        }, delay, TimeUnit.MILLISECONDS);

        return null;
    }
}
