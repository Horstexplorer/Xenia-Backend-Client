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

package de.netbeacon.utils.bitflags;

public abstract class IntegerBitFlags {

    private int value;

    public IntegerBitFlags(int value){
        this.value = value;
    }

    public interface IntBit{
        int getPos();
    }

    public int getValue() {
        return value;
    }

    public synchronized void set(IntBit...bits){
        for(IntBit b : bits){
            value |= 1 << b.getPos();
        }
    }

    public synchronized void unset(IntBit...bits){
        for(IntBit b : bits){
            value &= ~(1 << b.getPos());
        }
    }

    public boolean has(IntBit bit){
        return ((value >> bit.getPos()) & 1) == 1;
    }
}
