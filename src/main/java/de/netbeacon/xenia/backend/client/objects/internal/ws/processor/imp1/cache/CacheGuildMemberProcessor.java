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

package de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp1.cache;

import de.netbeacon.utils.concurrency.executor.ScalingExecutor;
import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import de.netbeacon.xenia.backend.client.objects.apidata.Guild;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp1.PrimaryWSProcessor;
import org.json.JSONObject;

public class CacheGuildMemberProcessor extends PrimaryWSProcessor{

	public CacheGuildMemberProcessor(XeniaBackendClient xeniaBackendClient, ScalingExecutor scalingExecutor){
		super(xeniaBackendClient, scalingExecutor);
	}

	@Override
	public void accept(JSONObject jsonObject){
		if(!xeniaBackendClient.getGuildCache().contains(jsonObject.getLong("guildId"))){
			return;
		}
		Guild g = xeniaBackendClient.getGuildCache().get_(jsonObject.getLong("guildId"));
		var mc = g.getMemberCache();
		switch(jsonObject.getString("action").toLowerCase()){
			case "create" -> mc.retrieve(jsonObject.getLong("userId"), true).queue();
			case "update" -> mc.retrieve(jsonObject.getLong("userId"), true).queue(
				e -> e.get(true).queue()
			);
			case "delete" -> {
				if(mc.contains(jsonObject.getLong("userId"))){
					mc.get_(jsonObject.getLong("userId")).onDeletion();
					mc.remove_(jsonObject.getLong("userId"));
				}
			}
		}
	}

	@Override
	public String ofType(){
		return "guild_member";
	}

}
