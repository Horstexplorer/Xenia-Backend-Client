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

import de.netbeacon.utils.executor.ScalingExecutor;
import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import de.netbeacon.xenia.backend.client.objects.apidata.User;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.imp1.PrimaryWSProcessor;
import org.json.JSONObject;

public class CacheUserProcessor extends PrimaryWSProcessor{

	public CacheUserProcessor(XeniaBackendClient xeniaBackendClient, ScalingExecutor scalingExecutor){
		super(xeniaBackendClient, scalingExecutor);
	}

	@Override
	public void accept(JSONObject jsonObject){
		if(!xeniaBackendClient.getUserCache().contains(jsonObject.getLong("userId"))){
			return;
		}
		User u = xeniaBackendClient.getUserCache().get(jsonObject.getLong("userId"), false);
		switch(jsonObject.getString("action").toLowerCase()){
			case "update" -> u.getAsync(true);
			case "delete" -> {
				u.onDeletion();
				xeniaBackendClient.getUserCache().remove(jsonObject.getLong("userId"));
			}
		}
	}

	@Override
	public String ofType(){
		return "user";
	}

}
