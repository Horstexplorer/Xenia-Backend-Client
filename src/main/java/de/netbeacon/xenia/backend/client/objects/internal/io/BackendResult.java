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

import java.util.Objects;

public class BackendResult {

    private final int statusCode;
    private final byte[] payload;
    private final long requestDuration;

    public BackendResult(int statusCode, byte[] payload, long requestDuration){
        this.statusCode = statusCode;
        this.payload = Objects.requireNonNullElseGet(payload, () -> new JSONObject().toString().getBytes());
        this.requestDuration = requestDuration;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] getPayload() {
        return payload;
    }

    public JSONObject getPayloadAsJSON(){
        return new JSONObject(new String(payload));
    }

    public long getRequestDuration() {
        return requestDuration;
    }
}
