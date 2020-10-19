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
import de.netbeacon.xenia.backend.client.objects.cache.Cache;
import de.netbeacon.xenia.backend.client.objects.external.misc.Tag;
import de.netbeacon.xenia.backend.client.objects.internal.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;

public class TagCache extends Cache<String, Tag> {

    private final long guildId;
    private final IdBasedLockHolder<String> idBasedLockHolder = new IdBasedLockHolder<>();

    public TagCache(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
    }

    public Tag get(String tagName) throws BackendException {
        try{
            idBasedLockHolder.getLock(tagName).lock();
            Tag tag = getFromCache(tagName);
            if(tag != null){
                return tag;
            }
            tag = new Tag(getBackendProcessor(), guildId, tagName);
            tag.get();
            addToCache(tagName, tag);
            return tag;
        }finally {
            idBasedLockHolder.getLock(tagName).unlock();
        }
    }

    public Tag createNew(String tagName, long userId, String content) throws BackendException {
        if(contains(tagName)){
            throw new BackendException(400, "Tag Already Exists");
        }
        Tag tag = new Tag(getBackendProcessor(), guildId, tagName).setInitialData(userId, content);
        tag.create(); // fails if a tag already exists on the backend which hasnt synced already with the client
        addToCache(tagName, tag);
        return tag;
    }

    public void remove(String tagName){
        removeFromCache(tagName);
    }

    public void delete(String tagName, long userId) throws BackendException {
        try{
            idBasedLockHolder.getLock(tagName).lock();
            Tag tag = getFromCache(tagName);
            if(tag == null){
                tag = new Tag(getBackendProcessor(), guildId, tagName);
                tag.get();
            }
            if(tag.getUserId() == userId){
                tag.delete();
                removeFromCache(tagName);
            }else{
                throw new RuntimeException("UserIds Do Not Match");
            }
        }finally {
            idBasedLockHolder.getLock(tagName).unlock();
        }
    }

}
