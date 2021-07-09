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

import de.netbeacon.utils.tuples.Triplet;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSRequest;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatProcessor extends WSProcessor{

	private final Logger logger = LoggerFactory.getLogger(HeartbeatProcessor.class);
	private long lastHeartBeat = System.currentTimeMillis();
	private long ten = 0;
	private long fifty = 0;
	private long oneHundred = 0;

	public HeartbeatProcessor(){
		super("heartbeat");
	}

	@Override
	public WSResponse process(WSRequest wsRequest){
		// this does nothing except some logging
		long newHeartBeat = System.currentTimeMillis();
		long delay = (newHeartBeat - lastHeartBeat);
		updateStatistics(delay);
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
		return null;
	}

	private void updateStatistics(long delay){
		ten = ((ten * 10) + delay) / 10;
		fifty = ((fifty * 50) + delay) / 50;
		oneHundred = ((oneHundred * 100) + delay) / 100;
	}

	public Triplet<Long, Long, Long> getStatistics(){
		return new Triplet<>(ten, fifty, oneHundred);
	}

}
