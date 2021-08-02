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

package de.netbeacon.xenia.backend.client.objects.cache;

import de.netbeacon.utils.concurrency.action.ExecutionAction;
import de.netbeacon.utils.concurrency.action.imp.SupplierExecutionAction;
import de.netbeacon.xenia.backend.client.objects.apidata.Member;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class MemberCache extends Cache<Long, Member>{

	private final long guildId;

	public MemberCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	@Override
	public ExecutionAction<Member> retrieve(Long id, boolean cache){
		Supplier<Member> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					var entry = get_(id);
					if(entry != null){
						return entry;
					}
					entry = new Member(getBackendProcessor(), guildId, id).get(true).execute();
					if(cache){
						add_(id, entry);
					}
					return entry;
				}
				finally{
					idBasedProvider.get(id).release();
				}
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve Member", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	@Override
	public ExecutionAction<Member> retrieveOrCreate(Long id, boolean cache, Object... other){
		return create(id, cache, other);
	}

	@Override
	public ExecutionAction<Member> create(Long id, boolean cache, Object... other){
		Supplier<Member> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					var entry = get_(id);
					if(entry != null){
						return entry;
					}
					entry = new Member(getBackendProcessor(), guildId, id).create(true).execute();
					if(cache){
						add_(id, entry);
					}
					return entry;
				}
				finally{
					idBasedProvider.get(id).release();
				}
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve / Create Member", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	@Override
	public ExecutionAction<Void> delete(Long id){
		Supplier<Void> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					remove_(id);
					new Member(getBackendProcessor(), guildId, id).delete(true).execute();
					return null;
				}
				finally{
					idBasedProvider.get(id).release();
				}
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Member", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	public ExecutionAction<List<Member>> retrieveAllFromBackend(boolean cache){
		Supplier<List<Member>> fun = () -> {
			try{
				BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, List.of("data", "guilds", String.valueOf(guildId), "members"), new HashMap<>(), null);
				BackendResult backendResult = getBackendProcessor().process(backendRequest);
				if(backendResult.getStatusCode() != 200){
					logger.warn("Failed To Get All Roles From The Backend");
					return null;
				}
				JSONArray members = backendResult.getPayloadAsJSON().getJSONArray("members");
				List<Member> memberList = new ArrayList<>();
				for(int i = 0; i < members.length(); i++){
					JSONObject jsonObject = members.getJSONObject(i);
					Member member = new Member(getBackendProcessor(), guildId, jsonObject.getLong("userId"));
					member.fromJSON(jsonObject); // manually insert the data
					if(cache){
						add_(member.getId(), member); // this will overwrite already existing ones
					}
					memberList.add(member);
				}
				return memberList;
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve All Members", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

}
