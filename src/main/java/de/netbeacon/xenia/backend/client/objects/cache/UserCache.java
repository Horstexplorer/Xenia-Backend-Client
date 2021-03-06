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
import de.netbeacon.xenia.backend.client.objects.external.User;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.DataException;
import de.netbeacon.xenia.backend.client.objects.internal.objects.Cache;

import java.util.Objects;
import java.util.function.Consumer;

public class UserCache extends Cache<Long, User>{

	private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();

	public UserCache(BackendProcessor backendProcessor){
		super(backendProcessor);
	}

	public User get(long userId) throws CacheException, DataException{
		return get(userId, true, false);
	}

	public User get(long userId, boolean init) throws CacheException, DataException{
		return get(userId, init, false);
	}

	public User get(long userId, boolean init, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(userId).lock();
			User user = getFromCache(userId);
			if(user != null){
				return user;
			}
			user = new User(getBackendProcessor(), userId);
			if(init){
				user.getOrCreate(securityOverride);
			}else{
				user.get(securityOverride);
			}
			addToCache(userId, user);
			return user;
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Get User", e);
		}
		finally{
			idBasedLockHolder.getLock(userId).unlock();
		}
	}

	public void getAsync(long userId, Consumer<User> whenReady, Consumer<Exception> onException){
		getAsync(userId, true, whenReady, onException);
	}

	public void getAsync(long userId, boolean init, Consumer<User> whenReady, Consumer<Exception> onException){
		getAsync(userId, init, false, whenReady, onException);
	}

	public void getAsync(long userId, boolean init, boolean securityOverride, Consumer<User> whenReady, Consumer<Exception> onException){
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

	public void remove(long userId){
		removeFromCache(userId);
	}

	public void delete(long userId) throws CacheException, DataException{
		delete(userId, false);
	}

	public void delete(long userId, boolean securityOverride) throws CacheException, DataException{
		try{
			idBasedLockHolder.getLock(userId).lock();
			User user = getFromCache(userId);
			Objects.requireNonNullElseGet(user, () -> new User(getBackendProcessor(), userId)).delete(securityOverride);
			removeFromCache(userId);
		}
		catch(CacheException | DataException e){
			throw e;
		}
		catch(Exception e){
			throw new CacheException(CacheException.Type.UNKNOWN, "Failed To Delete User", e);
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
