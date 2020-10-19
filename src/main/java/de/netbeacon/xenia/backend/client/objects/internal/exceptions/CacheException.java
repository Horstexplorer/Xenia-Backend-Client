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

package de.netbeacon.xenia.backend.client.objects.internal.exceptions;

public class CacheException extends RuntimeException {

    /**
     * Types
     * -1,-2,-3,-4 get/create/delete
     * -11 get all
     *
     * -20 already exists
     * -21 does not exist
     * -30 cant do this
     */

    private final int type;
    private final String message;
    private Exception exception;

    public CacheException(int type, String message){
        super(message);
        this.message = message;
        this.type = type;
    }

    public CacheException(int type, String message, Exception exception){
        super(message, exception);
        this.type = type;
        this.message = message;
        this.exception = exception;
    }

    @Override
    public String getMessage() {
        return "Type: "+type+" Message: "+super.getMessage();
    }

    public int getType() {
        return type;
    }

    public Exception getSubException() {
        return exception;
    }

    @Override
    public synchronized Throwable getCause() {
        return exception;
    }
}
