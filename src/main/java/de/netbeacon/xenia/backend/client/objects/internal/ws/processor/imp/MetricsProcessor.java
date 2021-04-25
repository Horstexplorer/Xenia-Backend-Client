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

import de.netbeacon.utils.tuples.Triplet;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSRequest;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsProcessor extends WSProcessor{

	public MetricsProcessor(){
		super("metrics");
	}

	@Override
	public WSResponse process(WSRequest wsRequest){
		var shardManager = getWsProcessorCore().getXeniaBackendClient().getShardManagerSupplier().get();
		var register = getWsProcessorCore().getProcessorRegister();
		var setupData = getWsProcessorCore().getXeniaBackendClient().getSetupData();
		Triplet<Long, Long, Long> heartbeatStats = register.containsKey("heartbeat") ? ((HeartbeatProcessor) register.get("heartbeat")).getStatistics() : new Triplet<>(-1L, -1L, -1L);
		JSONArray shardsTotal = new JSONArray();
		Arrays.stream(setupData.getShards()).forEach(shardsTotal::put);
		JSONArray shardsOnline = new JSONArray();
		AtomicInteger guildCount = new AtomicInteger();
		if(shardManager != null){
			shardManager.getShards().forEach(s -> {
				shardsOnline.put(s.getShardInfo().getShardId());
				guildCount.addAndGet(s.getGuilds().size());
			});
		}
		JSONObject jsonObject = new JSONObject()
			.put("clientId", setupData.getClientId())
			.put("heartbeatDelay", new JSONObject()
				.put("ten", heartbeatStats.getValue1())
				.put("fifty", heartbeatStats.getValue2())
				.put("oneHundred", heartbeatStats.getValue3()))
			.put("jda", new JSONObject()
				.put("shards", new JSONObject()
					.put("online", shardsOnline)
					.put("total", shardsTotal)
				)
				.put("guildCount", guildCount.get())
			);
		return new WSResponse.Builder()
			.requestId(wsRequest.getRequestId())
			.recipient(wsRequest.getSender())
			.action(getAction())
			.payload(jsonObject)
			.build();
	}

}
