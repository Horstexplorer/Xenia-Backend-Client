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

package de.netbeacon.xenia.backend.client.core;

import de.netbeacon.utils.shutdownhook.IShutdown;
import de.netbeacon.utils.shutdownhook.ShutdownHook;
import de.netbeacon.xenia.backend.client.objects.cache.GuildCache;
import de.netbeacon.xenia.backend.client.objects.cache.LicenseCache;
import de.netbeacon.xenia.backend.client.objects.cache.UserCache;
import de.netbeacon.xenia.backend.client.objects.external.SetupData;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.BackendSettings;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

public class XeniaBackendClient implements IShutdown {

    private final OkHttpClient okHttpClient;
    private final BackendProcessor backendProcessor;

    private final UserCache userCache;
    private final GuildCache guildCache;
    private final LicenseCache licenseCache;

    public XeniaBackendClient(BackendSettings backendSettings){
        // create okhttp client
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(128);
        dispatcher.setMaxRequestsPerHost(128);
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder().dispatcher(dispatcher);
        this.okHttpClient = okHttpClientBuilder.build();
        // create processor
        backendProcessor = new BackendProcessor(okHttpClient, backendSettings);
        // check login
        backendProcessor.activateToken();
        // create main caches
        this.userCache = new UserCache(backendProcessor);
        this.guildCache = new GuildCache(backendProcessor);
        this.licenseCache = new LicenseCache(backendProcessor);
        // add shutdown hook
        ShutdownHook shutdownHook = new ShutdownHook();
        shutdownHook.addShutdownAble(this);
    }

    public SetupData getSetupData() {
        SetupData setupData = new SetupData(backendProcessor);
        setupData.get();
        return setupData;
    }

    public UserCache getUserCache() {
        return userCache;
    }

    public GuildCache getGuildCache() {
        return guildCache;
    }

    public LicenseCache getLicenseCache() {
        return licenseCache;
    }

    @Override
    public void onShutdown() throws Exception {
        backendProcessor.onShutdown();
    }
}
