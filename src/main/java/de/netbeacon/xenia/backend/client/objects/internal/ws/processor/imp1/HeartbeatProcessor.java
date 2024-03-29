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

package de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp1;

import de.netbeacon.utils.concurrency.executor.ScalingExecutor;
import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import org.json.JSONObject;

public class HeartbeatProcessor extends PrimaryWSProcessor{

	private long lastHeartBeat = System.currentTimeMillis();

	public HeartbeatProcessor(XeniaBackendClient xeniaBackendClient, ScalingExecutor scalingExecutor){
		super(xeniaBackendClient, scalingExecutor);
	}

	@Override
	public void accept(JSONObject jsonObject){
		long newHeartBeat = jsonObject.getLong("timestamp");
		long delay = (newHeartBeat - lastHeartBeat);
		if(delay > 30000 * 2){
			logger.warn("Received Heartbeat After " + delay + "ms (Delay To Target " + (delay - 30000) + ") Missed At Least " + (delay / 30000) + " Heartbeat(s). The Network Might Be Faulty!");
		}
		else if(delay > 30000 * 1.5){
			logger.info("Received Heartbeat After " + delay + "ms (Delay To Target " + (delay - 30000) + ") The Service Might Be Slow.");
		}
		else{
			logger.debug("Received Heartbeat After " + delay + "ms (Delay To Target " + (delay - 30000) + ")");
		}
		lastHeartBeat = newHeartBeat;
	}

	@Override
	public String ofType(){
		return "heartbeat";
	}

}
