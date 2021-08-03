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
import de.netbeacon.xenia.backend.client.objects.apidata.Guild;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;

import javax.annotation.CheckReturnValue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class GuildCache extends Cache<Long, Guild>{

	public GuildCache(BackendProcessor backendProcessor){
		super(backendProcessor);
	}

	@CheckReturnValue
	@Override
	public ExecutionAction<Guild> retrieve(Long id, boolean cache){
		Supplier<Guild> fun = () -> {
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
					var entry = new Guild(getBackendProcessor(), id).get(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve Guild", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	@CheckReturnValue
	@Override
	public ExecutionAction<Guild> retrieveOrCreate(Long id, boolean cache, Object... other){
		return create(id, cache, other);
	}

	@CheckReturnValue
	@Override
	public ExecutionAction<Guild> create(Long id, boolean cache, Object... other){
		Supplier<Guild> fun = () -> {
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
					var entry = new Guild(getBackendProcessor(), id).getOrCreate(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Retrieve / Create Guild", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
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
					new Guild(getBackendProcessor(), id).delete(true).execute();
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
				throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Guild", e);
			}
		};
		return new SupplierExecutionAction<>(fun);
	}

	@Override
	public void clear(boolean deletion){
		getDataMap().forEach((k, v) -> v.clear(deletion)); // as the guild also contains caches
		super.clear(deletion);
	}

}
