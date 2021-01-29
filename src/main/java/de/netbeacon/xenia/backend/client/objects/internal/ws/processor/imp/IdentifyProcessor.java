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

import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import de.netbeacon.xenia.backend.client.objects.external.system.SetupData;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSRequest;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSResponse;
import org.json.JSONObject;

public class IdentifyProcessor extends WSProcessor {

    private final XeniaBackendClient xeniaBackendClient;

    public IdentifyProcessor(XeniaBackendClient xeniaBackendClient) {
        super("identify");
        this.xeniaBackendClient = xeniaBackendClient;
    }

    @Override
    public WSResponse process(WSRequest wsRequest) {
        SetupData setupData = xeniaBackendClient.getSetupData();
        return new WSResponse.Builder()
                .requestId(wsRequest.getRequestId())
                .recipient(wsRequest.getSender())
                .action(getAction())
                .payload(new JSONObject().put("id", setupData.getClientId()).put("name", setupData.getClientName()).put("description", setupData.getClientDescription()))
                .build();
    }
}
