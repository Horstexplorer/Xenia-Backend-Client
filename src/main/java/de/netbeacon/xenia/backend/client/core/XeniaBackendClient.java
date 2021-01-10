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
import de.netbeacon.xenia.backend.client.objects.cache.GuildCache;
import de.netbeacon.xenia.backend.client.objects.cache.LicenseCache;
import de.netbeacon.xenia.backend.client.objects.cache.UserCache;
import de.netbeacon.xenia.backend.client.objects.external.Info;
import de.netbeacon.xenia.backend.client.objects.external.SetupData;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.BackendSettings;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.ws.PrimaryWebsocketListener;
import de.netbeacon.xenia.backend.client.objects.internal.ws.SecondaryWebsocketListener;
import de.netbeacon.xenia.backend.client.objects.internal.ws.processor.WSProcessorCore;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.mindrot.jbcrypt.BCrypt;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class XeniaBackendClient implements IShutdown {

    private final BackendSettings backendSettings;

    private final OkHttpClient okHttpClient;
    private final BackendProcessor backendProcessor;
    private final PrimaryWebsocketListener primaryWebSocketListener;
    private final SecondaryWebsocketListener secondaryWebsocketListener;

    private final UserCache userCache;
    private final GuildCache guildCache;
    private final LicenseCache licenseCache;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public XeniaBackendClient(BackendSettings backendSettings){
        this.backendSettings = backendSettings;
        // create okhttp client
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(128);
        dispatcher.setMaxRequestsPerHost(128);
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .addInterceptor(new BackendProcessor.Interceptor(this));
        this.okHttpClient = okHttpClientBuilder.build();
        // create processor
        backendProcessor = new BackendProcessor(this);
        // check login
        backendProcessor.activateToken();
        // create update task
        scheduledExecutorService.scheduleAtFixedRate(()->{
            try{backendProcessor.activateToken();}catch (Exception ignore){}
        }, 2, 2, TimeUnit.MINUTES);
        // activate websocket
        primaryWebSocketListener = new PrimaryWebsocketListener(this);
        primaryWebSocketListener.start();

        WSProcessorCore wsProcessorCore = new WSProcessorCore();
        secondaryWebsocketListener = new SecondaryWebsocketListener(this, wsProcessorCore);
        secondaryWebsocketListener.start();
        // create main caches
        this.userCache = new UserCache(backendProcessor);
        this.guildCache = new GuildCache(backendProcessor);
        this.licenseCache = new LicenseCache(backendProcessor);
    }

    public BackendSettings getBackendSettings() {
        return backendSettings;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public BackendProcessor getBackendProcessor(){
        return backendProcessor;
    }

    public SetupData getSetupData() {
        SetupData setupData = new SetupData(backendProcessor);
        setupData.get();
        // check if the setup data matches the given key
        if(!BCrypt.checkpw(backendSettings.getMessageCryptKey(), setupData.getMessageCryptHash())){
            throw new BackendException(-1, "Invalid Message Crypt Hash Specified");
        }
        return setupData;
    }

    public Info getInfo(Info.Mode mode){
        Info info = new Info(backendProcessor, mode);
        info.get();
        return info;
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

    public PrimaryWebsocketListener getPrimaryWebSocketListener() {
        return primaryWebSocketListener;
    }

    public SecondaryWebsocketListener getSecondaryWebsocketListener() {
        return secondaryWebsocketListener;
    }

    @Override
    public void onShutdown() throws Exception {
        scheduledExecutorService.shutdownNow();
        primaryWebSocketListener.onShutdown();
        backendProcessor.onShutdown();
    }
}
