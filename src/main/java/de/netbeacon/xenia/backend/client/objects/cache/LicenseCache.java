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

import de.netbeacon.utils.locks.IdBasedLockHolder;
import de.netbeacon.xenia.backend.client.objects.external.License;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;

import java.util.function.Consumer;

public class LicenseCache extends Cache<Long, License>{

	private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();

	public LicenseCache(BackendProcessor backendProcessor){
		super(backendProcessor);
	}

	public License get(long guildId) throws CacheException, DataException{
		return get(guildId, false);
	}

	public License get(long guildId, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(guildId).lock();
			License license = getFromCache(guildId);
			if(license != null){
				return license;
			}
			license = new License(getBackendProcessor(), guildId);
			license.get(securityOverride);
			addToCache(guildId, license);
			return license;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Get License", e);
		}
		finally{
			idBasedLockHolder.getLock(guildId).unlock();
		}
	}

	public void getAsync(long guildId, Consumer<License> whenReady, Consumer<Exception> onException){
		getAsync(guildId, false, whenReady, onException);
	}

	public void getAsync(long guildId, boolean securityOverride, Consumer<License> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = get(guildId, securityOverride);
				if(whenReady != null){
					whenReady.accept(v);
				}
			}
			catch(Exception e){
				if(onException != null){
					onException.accept(e);
				}
			}
		});
	}

	public void remove(long guildId){
		removeFromCache(guildId);
	}

}
