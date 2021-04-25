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

import de.netbeacon.utils.executor.ScalingExecutor;
import de.netbeacon.utils.shutdownhook.IShutdown;
import de.netbeacon.xenia.backend.client.core.XeniaBackendClient;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class BackendProcessor implements IShutdown{

	private final XeniaBackendClient xeniaBackendClient;
	private final OkHttpClient okHttpClient;
	private final BackendSettings backendSettings;
	private final Logger logger = LoggerFactory.getLogger(BackendProcessor.class);
	private final ScalingExecutor scalingExecutor = new ScalingExecutor(4, 128, 125000, 30, TimeUnit.SECONDS);
	private final ReentrantLock lock = new ReentrantLock();

	public BackendProcessor(XeniaBackendClient xeniaBackendClient){
		this.xeniaBackendClient = xeniaBackendClient;
		this.okHttpClient = xeniaBackendClient.getOkHttpClient();
		this.backendSettings = xeniaBackendClient.getBackendSettings();
	}

	// auth

	public BackendProcessor activateToken() throws BackendException{
		try{
			lock.lock();
			if(backendSettings.getToken() == null || backendSettings.getToken().isBlank()){
				// send request to backend to get a new token
				BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BASIC, Arrays.asList("auth", "token"), new HashMap<>(), null);
				BackendResult backendResult = process(backendRequest);
				if(backendResult.getStatusCode() == 200){
					backendSettings.setToken(backendResult.getPayloadAsJSON().getString("token"));
					logger.debug("Received New Token Successfully");
				}
				else{
					logger.error("Failed To Receive New Token With Status Code " + backendResult.getStatusCode());
					throw new BackendException(-2, "Requesting Token Failed With Status Code: " + backendResult.getStatusCode());
				}
			}
			else{
				// renew this token
				BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, Arrays.asList("auth", "token", "renew"), new HashMap<>(), null);
				BackendResult backendResult = process(backendRequest);
				if(backendResult.getStatusCode() < 300 || backendResult.getStatusCode() > 199){
					logger.debug("Renewed Token Successfully");
				}
				else{
					// renewing failed - remove token and call again to request new one
					logger.error("Failed To Renew Token With Status Code " + backendResult.getStatusCode() + " - Requesting New Token");
					backendSettings.setToken(null);
					return activateToken();
				}
			}
			return this;
		}
		finally{
			lock.unlock();
		}
	}

	public BackendResult process(BackendRequest backendRequest) throws BackendException{
		try{
			try(Response response = okHttpClient.newCall(buildOkHttpRequest(backendRequest)).execute()){
				// parse response
				int code = response.code();
				long requestDuration = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
				if(response.body() != null){
					return new BackendResult(code, response.body().bytes(), requestDuration);
				}
				else{
					return new BackendResult(code, null, requestDuration);
				}
			}
		}
		catch(Exception e){
			throw new BackendException(-1, e);
		}
	}

	public void processAsync(BackendRequest backendRequest, Consumer<BackendResult> resultConsumer) throws BackendException{
		try{
			okHttpClient.newCall(buildOkHttpRequest(backendRequest)).enqueue(new Callback(){

				@Override
				public void onFailure(@NotNull Call call, @NotNull IOException e){
					logger.error("Failed To Process Request Async: ", e);
					scalingExecutor.execute(() -> resultConsumer.accept(new BackendResult(-1, null, 0)));
				}

				@Override
				public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException{
					int code = response.code();
					long requestDuration = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
					if(response.body() != null){
						byte[] body = response.body().bytes();
						scalingExecutor.execute(() -> resultConsumer.accept(new BackendResult(code, body, requestDuration)));
					}
					else{
						scalingExecutor.execute(() -> resultConsumer.accept(new BackendResult(code, null, requestDuration)));
					}
				}
			});

		}
		catch(Exception e){
			throw new BackendException(-1, e);
		}
	}

	private Request buildOkHttpRequest(BackendRequest backendRequest){
		try{
			// build url from request
			HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
				.scheme(backendSettings.getScheme())
				.host(backendSettings.getHost())
				.port(backendSettings.getPort());
			for(String pathSeg : backendRequest.getPath()){
				urlBuilder.addPathSegment(pathSeg);
			}
			for(Map.Entry<String, String> queryParam : backendRequest.getQueryParams().entrySet()){
				urlBuilder.addQueryParameter(queryParam.getKey(), queryParam.getValue());
			}
			// build request
			Request.Builder requestBuilder = new Request.Builder()
				.url(urlBuilder.build());
			switch(backendRequest.getAuthType()){
				case BEARER:
					requestBuilder.header("Authorization", "Bearer " + backendSettings.getToken());
					break;
				case BASIC:
					requestBuilder.header("Authorization", Credentials.basic(backendSettings.getClientIdAsString(), backendSettings.getPassword()));
					break;
			}
			switch(backendRequest.getMethod()){
				case GET:
					requestBuilder.get();
					break;
				case PUT:
					if(backendRequest.getPayload().length != 0){
						requestBuilder.put(RequestBody.create(backendRequest.getPayload(), MediaType.get("application/json")));
					}
					else{
						throw new BackendException(-1, "No Body Specified Which Is Needed For PUT");
					}
					break;
				case POST:
					if(backendRequest.getPayload().length != 0){
						requestBuilder.post(RequestBody.create(backendRequest.getPayload(), MediaType.get("application/json")));
					}
					else{
						throw new BackendException(-1, "No Body Specified Which Is Needed For POST");
					}
					break;
				case DELETE:
					if(backendRequest.getPayload().length != 0){
						requestBuilder.delete(RequestBody.create(backendRequest.getPayload(), MediaType.get("application/json")));
					}
					else{
						requestBuilder.delete();
					}
					break;
			}
			return requestBuilder.build();
		}
		catch(Exception e){
			logger.error("Failed To Process Request: ", e);
			throw new BackendException(-1, e);
		}
	}

	public XeniaBackendClient getBackendClient(){
		return xeniaBackendClient;
	}

	public ScalingExecutor getScalingExecutor(){
		return scalingExecutor;
	}

	public BackendSettings getBackendSettings(){
		return backendSettings;
	}

	@Override
	public void onShutdown() throws Exception{
		scalingExecutor.shutdown();
		scalingExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		okHttpClient.dispatcher().executorService().shutdown();
	}

	public static class Interceptor implements okhttp3.Interceptor{

		private final XeniaBackendClient client;

		private final Lock lock = new ReentrantLock();
		private long lastTokenUpdate;
		private static final long forceUpdateDelay = 2500;
		private static final int maxRetries = 4;
		private static final long timeoutRetryDelay = 1000;

		private final Logger logger = LoggerFactory.getLogger(BackendProcessor.Interceptor.class);

		public Interceptor(XeniaBackendClient client){
			this.client = client;
		}

		@NotNull
		@Override
		public Response intercept(@NotNull Chain chain) throws IOException{
			try{
				Request request = null;
				Response response;
				try{
					request = chain.request();
					response = chain.proceed(request);
					if(response.code() == 401 && request.headers().get("Authorization") != null && request.headers().get("Authorization").startsWith("Bearer")){
						throw new RecoverableException(RecoverableException.Type.UNAUTHORIZED, request, response); // only for when we used a bearer
					}
					else if(response.code() == 429){
						throw new RecoverableException(RecoverableException.Type.TOO_MANY_REQUESTS, request, response);
					}
					return response;
				}
				catch(SocketTimeoutException socketTimeoutException){
					throw new RecoverableException(RecoverableException.Type.TIMEOUT, request, null);
				}
			}
			catch(RecoverableException recoverableException){
				try{
					lock.lock();
					int currentRetries = 0;
					if(recoverableException.getRequest().headers().get("Request-Retries") != null){
						currentRetries = Integer.parseInt(recoverableException.getRequest().headers().get("Request-Retries"));
					}
					currentRetries++;
					if(currentRetries > maxRetries){
						if(recoverableException.getResponse() != null){
							return recoverableException.getResponse();
						}
						throw new IOException("Failed To Retry Request " + recoverableException.getRequest() + " (" + recoverableException.getType() + "). Max retries reached");
					}
					else if(recoverableException.getResponse() != null){
						recoverableException.getResponse().close();
					}
					switch(recoverableException.getType()){
						case TOO_MANY_REQUESTS:{
							// add later, for now we use the default behaviour of timeout
						}
						case TIMEOUT:{
							try{
								TimeUnit.MILLISECONDS.sleep(timeoutRetryDelay);
							}
							catch(InterruptedException ignore){
							}
						}
						break;
						case UNAUTHORIZED:{
							if(recoverableException.getRequest().headers().get("Authorization") != null && recoverableException.getRequest().headers().get("Authorization").startsWith("Bearer")){
								if(lastTokenUpdate + forceUpdateDelay < System.currentTimeMillis()){
									logger.warn("Received 401 response from backend for token auth - Requesting new token");
									lastTokenUpdate = System.currentTimeMillis();
									client.getBackendProcessor().activateToken(); // update token
								}
								else{
									logger.debug("Received 401 response from backend for token auth - Token has been exchanged recently, wont request a new one.");
									try{
										TimeUnit.MILLISECONDS.sleep(forceUpdateDelay / 4);
									}
									catch(InterruptedException ignore){
									}
								}
							}
						}
						break;
						default:{
							throw new IOException("Unknown Error");
						}
					}
					if(recoverableException.getRequest().headers().get("Authorization") != null && recoverableException.getRequest().headers().get("Authorization").startsWith("Bearer")){
						return client.getOkHttpClient().newCall(recoverableException.getRequest().newBuilder()
							.removeHeader("Authorization")
							.addHeader("Authorization", "Bearer " + client.getBackendProcessor().getBackendSettings().getToken())
							.addHeader("Request-Retries", String.valueOf(currentRetries))
							.build()
						).execute();
					}
					else if(recoverableException.getRequest().headers().get("Authorization") != null && recoverableException.getRequest().headers().get("Authorization").startsWith("Basic")){
						return client.getOkHttpClient().newCall(recoverableException.getRequest().newBuilder()
							.removeHeader("Authorization")
							.addHeader("Authorization", Credentials.basic(client.getBackendSettings().getClientIdAsString(), client.getBackendSettings().getPassword()))
							.addHeader("Request-Retries", String.valueOf(currentRetries))
							.build()
						).execute();
					}
					else{
						return client.getOkHttpClient().newCall(recoverableException.getRequest().newBuilder()
							.addHeader("Request-Retries", String.valueOf(currentRetries))
							.build()
						).execute();
					}
				}
				finally{
					lock.unlock();
				}
			}
		}

		public static class RecoverableException extends Exception{

			public enum Type{
				TIMEOUT,
				UNAUTHORIZED,
				TOO_MANY_REQUESTS
			}

			private final Type type;
			private final Request request;
			private final Response response;

			public RecoverableException(Type type, Request request, Response response){
				this.type = type;
				this.request = request;
				this.response = response;
			}

			public Type getType(){
				return type;
			}

			public Request getRequest(){
				return request;
			}

			public Response getResponse(){
				return response;
			}

		}

	}

}
