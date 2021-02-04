/*
 *     Copyright 2021 Horstexplorer @ https://www.netbeacon.de
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

package de.netbeacon.xenia.backend.client.objects.internal.exceptions;

public class DataException extends RuntimeException{

    public enum Type{
        UNKNOWN,
        UNSTABLE,
        HTTP,
        TIMEOUT
    }

    private int code;
    private final Type type;

    public DataException(Type type){
        super(type.name());
        this.type = type;
    }

    public DataException(Type type, String message){
        super(message);
        this.type = type;
    }

    public DataException(Type type, int code, String message){
        super(message);
        this.type = type;
        this.code = code;
    }

    public Type getType() {
        return type;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return type.name()+" "+code+" "+super.getMessage();
    }
}
