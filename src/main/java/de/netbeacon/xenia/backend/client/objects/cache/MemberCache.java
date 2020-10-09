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
import de.netbeacon.xenia.backend.client.objects.internal.BackendException;
import de.netbeacon.xenia.backend.client.objects.internal.BackendProcessor;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendRequest;
import de.netbeacon.xenia.backend.client.objects.internal.io.BackendResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MemberCache extends Cache<Member> {

    private final long guildId;
    private final IdBasedLockHolder<Long> idBasedLockHolder = new IdBasedLockHolder<>();

    public MemberCache(BackendProcessor backendProcessor, long guildId) {
        super(backendProcessor);
        this.guildId = guildId;
    }

    public Member get(long userId){
        try{
            idBasedLockHolder.getLock(userId).lock();
            Member member = getFromCache(userId);
            if(member != null){
                return member;
            }
            member = new Member(getBackendProcessor(), guildId, userId);
            try{
                member.get();
            }catch (BackendException e){
                if(e.getId() == 404){
                    member.create();
                }else{
                    throw e;
                }
            }
            addToCache(userId, member);
            return member;
        }finally {
            idBasedLockHolder.getLock(userId).unlock();
        }
    }

    public List<Member> retrieveAllFromBackend() throws BackendException {
        try{
            idBasedLockHolder.getLock().writeLock().lock();
            BackendRequest backendRequest = new BackendRequest(BackendRequest.Method.GET, BackendRequest.AuthType.Token, List.of("data", "guild", String.valueOf(guildId), "member"),new HashMap<>(), null);
            BackendResult backendResult = getBackendProcessor().process(backendRequest);
            JSONArray members = backendResult.getPayloadAsJSON().getJSONArray("members");
            List<Member> memberList = new ArrayList<>();
            for(int i = 0; i < members.length(); i++){
                JSONObject jsonObject = members.getJSONObject(i);
                Member member = new Member(getBackendProcessor(), guildId, jsonObject.getLong("userId"));
                member.fromJSON(jsonObject); // manually insert the data
                addToCache(member.getId(), member); // this will overwrite already existing ones
                memberList.add(member);
            }
            return memberList;
        }finally {
            idBasedLockHolder.getLock().writeLock().unlock();
        }
    }

    public void remove(long userId){
        removeFromCache(userId);
    }

    public void delete(long userId){
        try{
            idBasedLockHolder.getLock(userId).lock();
            Member member = getFromCache(userId);
            Objects.requireNonNullElseGet(member, ()->new Member(getBackendProcessor(), guildId, userId)).delete();
            removeFromCache(userId);
        }finally {
            idBasedLockHolder.getLock(userId).unlock();
        }
    }
}
