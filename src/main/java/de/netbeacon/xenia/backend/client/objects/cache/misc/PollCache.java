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

package de.netbeacon.xenia.backend.client.objects.cache.misc;

import de.netbeacon.utils.locks.IdBasedLockHolder;
import de.netbeacon.xenia.backend.client.objects.cache.ChannelCache;
import de.netbeacon.xenia.backend.client.objects.external.misc.Poll;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.exceptions.CacheException;
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
import java.util.concurrent.locks.ReentrantLock;

public class PollCache extends Cache<Long, Poll> {

    private final long guildId;
    private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();
    private final Logger logger = LoggerFactory.getLogger(ChannelCache.class);

    public PollCache(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
    }

    public Poll get(long pollId){
        try{
            idBasedLockHolder.getLock(pollId).lock();
            Poll poll = getFromCache(pollId);
            if(poll != null){
                return poll;
            }
            poll = new Poll(getBackendProcessor(), guildId, pollId);
            poll.get();
            addToCache(poll.getId(), poll);
            return poll;
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-1, "Failed To Get Poll", e);
        }finally {
            idBasedLockHolder.getLock(pollId).unlock();
        }
    }

    public List<Poll> retrieveAllFromBackend() throws CacheException {
        try{
            idBasedLockHolder.getLock().writeLock().lock();
            BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.Token, List.of("data", "guilds", String.valueOf(guildId), "misc", "polls"), new HashMap<>(), null);
            BackendResult backendResult = getBackendProcessor().process(backendRequest);
            if(backendResult.getStatusCode() != 200){
                logger.warn("Failed To Get Polls From The Backend");
                return null;
            }
            JSONArray polls = backendResult.getPayloadAsJSON().getJSONArray("polls");
            List<Poll> pollList = new ArrayList<>();
            for(int i = 0; i < polls.length(); i++){
                JSONObject jsonObject = polls.getJSONObject(i);
                Poll poll = new Poll(getBackendProcessor(), guildId, -1);
                poll.fromJSON(jsonObject);
                addToCache(poll.getId(), poll);
                pollList.add(poll);
            }
            return pollList;
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-11, "Failed To Retrieve All Polls", e);
        }finally {
            idBasedLockHolder.getLock().writeLock().unlock();
        }
    }

    private final ReentrantLock creationLock = new ReentrantLock();

    public Poll create(long userId, long channelId, long closeTimestamp, String description, String...options){
        try{
            creationLock.lock();
            if(getOrderedKeyMap().size()+1 > getBackendProcessor().getBackendClient().getLicenseCache().get(guildId).getPerk_MISC_POLLS_C()){
                throw new RuntimeException("Cache Is Full");
            }
            Poll poll = new Poll(getBackendProcessor(), guildId, -1);
            poll.setInitialData(userId, channelId, closeTimestamp, description, options);
            poll.create();
            addToCache(poll.getId(), poll);
            return poll;
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-2, "Failed To Create A New Poll", e);
        }finally {
            creationLock.unlock();
        }
    }

    public void remove(long pollId){
        removeFromCache(pollId);
    }

    public Poll delete(long pollId){
        try{
            idBasedLockHolder.getLock(pollId).lock();
            Poll poll = getFromCache(pollId);
            Objects.requireNonNullElseGet(poll, ()-> new Poll(getBackendProcessor(), guildId, pollId)).delete();
            removeFromCache(pollId);
            return poll;
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-3, "Failed To Delete Poll", e);
        }finally {
            idBasedLockHolder.getLock(pollId).unlock();
        }
    }
}
