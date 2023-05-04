/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.slotchain;

import com.alibaba.csp.sentinel.context.Context;

/**
 * @author qinan.qn
 * @author jialiang.linjl
 * 单向链表，  默认包含一个 节点 ，默认包含一个节点， 且有两个指针first 与 end 同时指向这个节点
 */
public class DefaultProcessorSlotChain extends ProcessorSlotChain {

    //代表指向链表头结点的 first 属性,这里 头结点是一个空的 AbstractLinkedProcessorSlot
    AbstractLinkedProcessorSlot<?> first = new AbstractLinkedProcessorSlot<Object>() {

        @Override
        public void entry(Context context, ResourceWrapper resourceWrapper, Object t, int count, boolean prioritized, Object... args)
            throws Throwable {
            super.fireEntry(context, resourceWrapper, t, count, prioritized, args);
        }

        @Override
        public void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
            super.fireExit(context, resourceWrapper, count, args);
        }

    };
    //end 执行链表的尾结点
    AbstractLinkedProcessorSlot<?> end = first;

    @Override
    public void addFirst(AbstractLinkedProcessorSlot<?> protocolProcessor) {
        protocolProcessor.setNext(first.getNext());
        first.setNext(protocolProcessor);
        if (end == first) {
            end = protocolProcessor;
        }
    }

    @Override
    public void addLast(AbstractLinkedProcessorSlot<?> protocolProcessor) {
        //当前节点 指向 下一个节点
        end.setNext(protocolProcessor);
        //当前节点的 end 节点，指向 下一个节点
        end = protocolProcessor;
    }

    /**
     * Same as {@link #addLast(AbstractLinkedProcessorSlot)}.
     *
     * @param next processor to be added.
     */
    @Override
    public void setNext(AbstractLinkedProcessorSlot<?> next) {
        addLast(next);
    }

    @Override
    public AbstractLinkedProcessorSlot<?> getNext() {
        return first.getNext();
    }

    //调用头结点的 entry
    @Override
    public void entry(Context context, ResourceWrapper resourceWrapper, Object t, int count, boolean prioritized, Object... args)
        throws Throwable {
        //first.transformEntry 之后，entry 调用时 进入到 first 实现的entry中，进而走fireEntry 实现first的next节点处理
        first.transformEntry(context, resourceWrapper, t, count, prioritized, args);
    }

    // 调用头结点的 exit
    @Override
    public void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
        first.exit(context, resourceWrapper, count, args);
    }

}
