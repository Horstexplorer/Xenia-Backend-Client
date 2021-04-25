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
import de.netbeacon.xenia.backend.client.objects.external.Guild;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;

import java.util.Objects;

public class GuildCache extends Cache<Long, Guild>{

	private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();

	public GuildCache(BackendProcessor backendProcessor){
		super(backendProcessor);
	}

	public Guild get(long guildId) throws CacheException, DataException{
		return get(guildId, true, false);
	}

	public Guild get(long guildId, boolean init) throws CacheException, DataException{
		return get(guildId, init, false);
	}

	public Guild get(long guildId, boolean init, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(guildId).lock();
			Guild guild = getFromCache(guildId);
			if(guild != null){
				return guild;
			}
			guild = new Guild(getBackendProcessor(), guildId);
			try{
				guild.get(securityOverride);
			}
			catch(DataException e){
				if(e.getCode() == 404 && init){
					guild.create(securityOverride);
				}
				else{
					throw e;
				}
			}
			addToCache(guildId, guild);
			return guild;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Get Guild", e);
		}
		finally{
			idBasedLockHolder.getLock(guildId).unlock();
		}
	}

	public void remove(long guildId){
		removeFromCache(guildId);
	}

	public void delete(long guildId) throws CacheException, DataException{
		delete(guildId, false);
	}

	public void delete(long guildId, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(guildId).lock();
			Guild guild = getFromCache(guildId);
			Objects.requireNonNullElseGet(guild, () -> new Guild(getBackendProcessor(), guildId)).delete(securityOverride);
			guild.clear(true);
			removeFromCache(guildId);
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Guild", e);
		}
		finally{
			idBasedLockHolder.getLock(guildId).unlock();
		}
	}

	@Override
	public void clear(boolean deletion){
		getDataMap().forEach((k, v) -> v.clear(deletion)); // as the guild also contains caches
		super.clear(deletion);
	}

}
