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

package de.netbeacon.xenia.backend.client.objects.internal;

public class BackendSettings{

	private final String scheme;
	private final String host;
	private final int port;

	private final long clientId;
	private final String password;
	private String token;

	private String messageCryptKey;

	public BackendSettings(String scheme, String host, int port, long clientId, String password, String messageCryptKey){
		this.scheme = scheme;
		this.host = host;
		this.port = port;
		this.clientId = clientId;
		this.password = password;
		this.messageCryptKey = messageCryptKey;
	}

	// backend identification

	public String getScheme(){
		return scheme;
	}

	public String getHost(){
		return host;
	}

	public int getPort(){
		return port;
	}

	// client credentials

	public long getClientId(){
		return clientId;
	}

	public String getClientIdAsString(){
		return String.valueOf(clientId);
	}

	public String getPassword(){
		return password;
	}

	public String getToken(){
		return token;
	}

	public void setToken(String token){
		this.token = token;
	}

	// msg crypt


	public String getMessageCryptKey(){
		return messageCryptKey;
	}

}
