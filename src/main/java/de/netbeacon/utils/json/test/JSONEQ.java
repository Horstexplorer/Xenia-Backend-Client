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

package de.netbeacon.utils.json.test;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONEQ{

	public static boolean equals(JSONObject a, JSONObject b){
		JSONObject ac = new JSONObject(a.toString());
		JSONObject bc = new JSONObject(b.toString());
		if(ac.keySet().size() != bc.keySet().size()) return false;
		for(String sa : ac.keySet()){
			if(!bc.has(sa)) return false;
			Object oa = ac.get(sa);
			Object ob = bc.get(sa);
			if(!equals(oa, ob)) return false;
		}
		return true;
	}

	public static boolean equals(JSONArray a, JSONArray b){
		JSONArray ac = new JSONArray(a.toString());
		JSONArray bc = new JSONArray(b.toString());
		if(ac.length() != bc.length()) return false;
		for(int i = 0; i < ac.length(); i++){
			Object oa = ac.get(i);
			boolean good = false;
			for(int ii = 0; ii < b.length(); ii++){
				Object ob = bc.get(ii);
				if(equals(oa, ob)){
					good = true;
					bc.remove(ii);
					break;
				}
			}
			if(!good) return false;
		}
		return true;
	}

	private static boolean equals(Object a, Object b){
		if(!a.getClass().equals(b.getClass())) return false;
		if(a instanceof JSONObject){
			return equals((JSONObject) a, (JSONObject) b);
		}else if(a instanceof JSONArray){
			return equals((JSONArray) a, (JSONArray) b);
		}else{
			return a.equals(b);
		}
	}
}
