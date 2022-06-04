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

/**
 * Link all processor slots as a chain.
 * 将所有处理器插槽链接为一个链。
 *
 * @author qinan.qn
 */
public abstract class ProcessorSlotChain extends AbstractLinkedProcessorSlot<Object> {

    /**
     * Add a processor to the head of this slot chain.
     * 添加一个 处理器到 插槽链的 头部  也就是 将一个处理器 添加到 单向链表的 头结点
     *
     * @param protocolProcessor processor to be added.
     */
    public abstract void addFirst(AbstractLinkedProcessorSlot<?> protocolProcessor);

    /**
     * Add a processor to the tail of this slot chain.
     * 添加 处理其 到链表的 尾节点
     * @param protocolProcessor processor to be added.
     */
    public abstract void addLast(AbstractLinkedProcessorSlot<?> protocolProcessor);
}
