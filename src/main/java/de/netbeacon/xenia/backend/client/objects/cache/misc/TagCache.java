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
import de.netbeacon.xenia.backend.client.objects.external.misc.Tag;
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

public class TagCache extends Cache<String, Tag> {

    private final long guildId;
    private final IdBasedLockHolder<String> idBasedLockHolder = new IdBasedLockHolder<>();
    private final Logger logger = LoggerFactory.getLogger(TagCache.class);

    public TagCache(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
    }

    public Tag get(String tagName) throws CacheException {
        try{
            idBasedLockHolder.getLock(tagName).lock();
            if(contains(tagName)){
                return getFromCache(tagName);
            }
            Tag tag = new Tag(getBackendProcessor(), guildId, tagName);
            tag.get();
            addToCache(tagName, tag);
            return tag;
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-1, "Failed To Get Tag", e);
        }finally {
            idBasedLockHolder.getLock(tagName).unlock();
        }
    }

    public List<Tag> retrieveAllFromBackend() throws CacheException {
        try{
            idBasedLockHolder.getLock().writeLock().lock();
            BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.Token, List.of("data", "guilds", String.valueOf(guildId), "misc", "tags"), new HashMap<>(), null);
            BackendResult backendResult = getBackendProcessor().process(backendRequest);
            if(backendResult.getStatusCode() != 200){
                logger.warn("Failed To Get Tags From The Backend");
                return null;
            }
            JSONArray tags = backendResult.getPayloadAsJSON().getJSONArray("tags");
            List<Tag> tagList = new ArrayList<>();
            for(int i = 0; i < tags.length(); i++){
                JSONObject jsonObject = tags.getJSONObject(i);
                Tag tag = new Tag(getBackendProcessor(), guildId, jsonObject.getString("tagName"));
                tag.fromJSON(jsonObject);
                addToCache(tag.getId(), tag);
                tagList.add(tag);
            }
            return tagList;
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-11, "Failed To Retrieve All Tags", e);
        }finally {
            idBasedLockHolder.getLock().writeLock().unlock();
        }
    }

    public Tag createNew(String tagName, long userId, String content) throws CacheException {
        try{
            idBasedLockHolder.getLock(tagName).lock();
            if(contains(tagName)){
                throw new CacheException(-20, "Tag Already Exists");
            }
            Tag tag = new Tag(getBackendProcessor(), guildId, tagName).setInitialData(userId, content);
            tag.create(); // fails if a tag already exists on the backend which hasnt synced already with the client
            addToCache(tagName, tag);
            return tag;
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-2, "Failed To Create A New Tag", e);
        }finally {
            idBasedLockHolder.getLock(tagName).unlock();
        }
    }

    public void remove(String tagName){
        removeFromCache(tagName);
    }

    public void delete(String tagName, long userId) throws CacheException {
        try{
            idBasedLockHolder.getLock(tagName).lock();
            Tag tag = getFromCache(tagName);
            if(tag == null){
                try{
                    tag = new Tag(getBackendProcessor(), guildId, tagName);
                    tag.get();
                }catch (Exception e){
                    throw new CacheException(-21, "Tag Does Not Exist");
                }
            }
            if(tag.getUserId() == userId){
                tag.delete();
                removeFromCache(tagName);
            }else{
                throw new CacheException(-30, "Cant Delete Tag When Not Owner");
            }
        }catch (CacheException e){
            throw e;
        }catch (Exception e){
            throw new CacheException(-3, "Failed To Delete A Tag", e);
        }finally {
            idBasedLockHolder.getLock(tagName).unlock();
        }
    }

}
