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

package de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp2;

import de.netbeacon.xenia.backend.client.objects.apidata.system.Info;
import de.netbeacon.xenia.backend.client.objects.apidata.system.SetupData;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSRequest;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSResponse;
import org.json.JSONObject;

import java.lang.management.ManagementFactory;

public class StatisticsProcessor extends WSProcessor{

	public StatisticsProcessor(){
		super("statistics");
	}

	@Override
	public WSResponse process(WSRequest wsRequest){
		Runtime runtime = Runtime.getRuntime();
		SetupData setupData = getWsProcessorCore().getXeniaBackendClient().getSetupData();
		Info info = getWsProcessorCore().getXeniaBackendClient().getInfo(Info.Mode.Private);
		JSONObject jsonObject = new JSONObject()
			.put("id", setupData.getClientId())
			.put("name", setupData.getClientName())
			.put("statistics", new JSONObject()
				.put("memory", new JSONObject()
					.put("used", (runtime.totalMemory() - runtime.freeMemory()) / 1048576)
					.put("total", runtime.totalMemory() / 1048576)
				)
				.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime())
				.put("threads", Thread.activeCount())
				.put("ce", new JSONObject()
					.put("guilds", getWsProcessorCore().getXeniaBackendClient().getGuildCache().getDataMap().size())
					.put("users", getWsProcessorCore().getXeniaBackendClient().getUserCache().getDataMap().size())
				)
				.put("ping", info.getPing())
			);
		return new WSResponse.Builder()
			.requestId(wsRequest.getRequestId())
			.recipient(wsRequest.getSender())
			.action(getAction())
			.payload(jsonObject)
			.build();
	}

}
