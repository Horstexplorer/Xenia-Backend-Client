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
import de.netbeacon.xenia.backend.client.objects.external.Member;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MemberCache extends Cache<Long, Member>{

	private final long guildId;
	private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();
	private final Logger logger = LoggerFactory.getLogger(MemberCache.class);

	public MemberCache(BackendProcessor backendProcessor, long guildId){
		super(backendProcessor);
		this.guildId = guildId;
	}

	public Member get(long userId) throws CacheException, DataException{
		return get(userId, true, false);
	}

	public Member get(long userId, boolean init) throws CacheException, DataException{
		return get(userId, init, false);
	}

	public Member get(long userId, boolean init, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(userId).lock();
			Member member = getFromCache(userId);
			if(member != null){
				return member;
			}
			member = new Member(getBackendProcessor(), guildId, userId);
			try{
				member.get(securityOverride);
			}
			catch(DataException e){
				if(e.getCode() == 404 && init){
					member.create(securityOverride);
				}
				else{
					throw e;
				}
			}
			addToCache(userId, member);
			return member;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Get Member", e);
		}
		finally{
			idBasedLockHolder.getLock(userId).unlock();
		}
	}

	public void getAsync(long userId, Consumer<Member> whenReady, Consumer<Exception> onException){
		getAsync(userId, true, whenReady, onException);
	}

	public void getAsync(long userId, boolean init, Consumer<Member> whenReady, Consumer<Exception> onException){
		getAsync(userId, init, false, whenReady, onException);
	}

	public void getAsync(long userId, boolean init, boolean securityOverride, Consumer<Member> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = get(userId, init, securityOverride);
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

	public List<Member> retrieveAllFromBackend(boolean cacheInsert) throws CacheException, DataException{
		try{
			if(cacheInsert){
				idBasedLockHolder.getLock().writeLock().lock();
			}
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
				if(cacheInsert){
					addToCache(member.getId(), member); // this will overwrite already existing ones
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
		finally{
			if(cacheInsert){
				idBasedLockHolder.getLock().writeLock().unlock();
			}
		}
	}

	public void retrieveAllFromBackendAsync(boolean cacheInsert, Consumer<List<Member>> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				var v = retrieveAllFromBackend(cacheInsert);
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

	public void remove(long userId){
		removeFromCache(userId);
	}

	public void delete(long userId) throws CacheException, DataException{
		delete(userId, false);
	}

	public void delete(long userId, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(userId).lock();
			Member member = getFromCache(userId);
			Objects.requireNonNullElseGet(member, () -> new Member(getBackendProcessor(), guildId, userId)).delete(securityOverride);
			removeFromCache(userId);
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete Member", e);
		}
		finally{
			idBasedLockHolder.getLock(userId).unlock();
		}
	}

	public void deleteAsync(long userId, Consumer<Long> whenReady, Consumer<Exception> onException){
		deleteAsync(userId, false, whenReady, onException);
	}

	public void deleteAsync(long userId, boolean securityOverride, Consumer<Long> whenReady, Consumer<Exception> onException){
		getBackendProcessor().getScalingExecutor().execute(() -> {
			try{
				delete(userId, securityOverride);
				if(whenReady != null){
					whenReady.accept(userId);
				}
			}
			catch(Exception e){
				if(onException != null){
					onException.accept(e);
				}
			}
		});
	}

}
