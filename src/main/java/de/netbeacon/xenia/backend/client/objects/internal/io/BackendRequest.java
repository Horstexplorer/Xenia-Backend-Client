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

package de.netbeacon.xenia.backend.client.objects.internal.io;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class BackendRequest{

	private final Method method;
	private final AuthType authType;
	private final List<String> path;
	private final HashMap<String, String> queryParams;
	private final byte[] payload;
	public BackendRequest(Method method, AuthType authType, List<String> path, HashMap<String, String> queryParams, JSONObject payload){
		this.method = method;
		this.authType = authType;
		this.path = path;
		this.queryParams = queryParams;
		if(payload != null){
			this.payload = payload.toString().getBytes();
		}
		else{
			this.payload = new byte[0];
		}
	}

	public Method getMethod(){
		return method;
	}

	public AuthType getAuthType(){
		return authType;
	}

	public List<String> getPath(){
		return path;
	}

	public HashMap<String, String> getQueryParams(){
		return queryParams;
	}

	public byte[] getPayload(){
		return payload;
	}

	public enum Method{
		GET,
		PUT,
		POST,
		DELETE
	}

	public enum AuthType{
		BASIC,
		BEARER
	}

}
