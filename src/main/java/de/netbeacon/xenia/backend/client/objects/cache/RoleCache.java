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
import de.netbeacon.utils.concurrency.action.ExecutionException;
import de.netbeacon.utils.concurrency.action.imp.SupplierExecutionAction;
import de.netbeacon.xenia.backend.client.objects.apidata.Role;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class RoleCache extends Cache<Long, Role>{

	private final long guildId;

	public RoleCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	@CheckReturnValue
	@Override
	public ExecutionAction<Role> retrieve(Long id, boolean cache){
		Supplier<Role> fun = () -> {
			try{
				if(contains(id)){
					return get_(id);
				}
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					if(contains(id)){
						return get_(id);
					}
					var entry = new Role(getBackendProcessor(), guildId, id).get(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve Role", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

	@CheckReturnValue
	@Deprecated
	@Override
	public ExecutionAction<Role> retrieveOrCreate(Long id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), () -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	@CheckReturnValue
	@Deprecated
	@Override
	public ExecutionAction<Role> create(Long id, boolean cache, Object... other){
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), () -> {throw new ExecutionException(new UnsupportedOperationException());});
	}

	public ExecutionAction<Role> create(){
		Supplier<Role> fun = () -> {
			try{
				creationLock.lock();
				if(getOrderedKeyMap().size() + 1 > getBackendProcessor().getBackendClient().getLicenseCache().retrieve(guildId, true).execute().getPerk_GUILD_ROLE_C()){
					throw new RuntimeException("Cache Is Full");
				}
				Role role = new Role(getBackendProcessor(), guildId, -1).create(true).execute();
				add_(role.getId(), role);
				return role;
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Create Role", e);
			}
			finally{
				creationLock.unlock();
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

	@CheckReturnValue
	@Override
	public ExecutionAction<Void> delete(Long id){
		Supplier<Void> fun = () -> {
			try{
				if(!idBasedProvider.getElseCreate(id).tryAcquire(10, TimeUnit.SECONDS)){
					throw new TimeoutException("Failed to acquire block for " + id + " in a reasonable time");
				}
				try{
					remove_(id);
					new Role(getBackendProcessor(), guildId, id).delete(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Role", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

	@CheckReturnValue
	public ExecutionAction<List<Role>> retrieveAllFromBackend(boolean cache){
		Supplier<List<Role>> fun = () -> {
			try{
				BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.BEARER, List.of("data", "guilds", String.valueOf(guildId), "roles"), new HashMap<>(), null);
				BackendResult backendResult = getBackendProcessor().process(backendRequest);
				if(backendResult.getStatusCode() != 200){
					logger.warn("Failed To Get All Roles From The Backend");
					return null;
				}
				JSONArray roles = backendResult.getPayloadAsJSON().getJSONArray("roles");
				List<Role> rolesList = new ArrayList<>();
				for(int i = 0; i < roles.length(); i++){
					JSONObject jsonObject = roles.getJSONObject(i);
					Role role = new Role(getBackendProcessor(), guildId, jsonObject.getLong("roleId"));
					role.fromJSON(jsonObject); // manually insert the data as we already received it
					if(cache){
						add_(role.getId(), role); // this will overwrite already existing ones
					}
					rolesList.add(role);
				}
				return rolesList;
			}
			catch(CacheException | DataException e){
				throw e;
			}
			catch(Exception e){
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve All Roles", e);
			}
		};
		return new SupplierExecutionAction<>(getBackendProcessor().getScalingExecutor(), fun);
	}

}
