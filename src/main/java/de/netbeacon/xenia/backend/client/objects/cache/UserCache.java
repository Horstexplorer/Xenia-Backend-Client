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
import de.netbeacon.xenia.backend.client.objects.internal.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;

import java.util.Objects;

public class UserCache extends Cache<User> {

    private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();

    public UserCache(BackendProcessor backendProcessor) {
        super(backendProcessor);
    }

    public User get(long userId) throws BackendException {
       try{
           idBasedLockHolder.getLock(userId).lock();
           User user = getFromCache(userId);
           if(user != null){
               return user;
           }
           user = new User(getBackendProcessor(), userId);
           try{
               user.get();
           }catch (BackendException e){
               if(e.getId() == 404){
                   user.create();
               }else{
                   throw e;
               }
           }
           addToCache(userId, user);
           return user;
       }finally {
           idBasedLockHolder.getLock(userId).unlock();
       }
    }

    public void remove(long userId){
        removeFromCache(userId);
    }

    public void delete(long userId) throws BackendException {
        try{
            idBasedLockHolder.getLock(userId).lock();
            User user = getFromCache(userId);
            Objects.requireNonNullElseGet(user, ()->new User(getBackendProcessor(), userId)).delete();
            removeFromCache(userId);
        }finally {
            idBasedLockHolder.getLock(userId).unlock();
        }
    }
}
