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
 * A container of some process and ways of notification when the process is finished.
 * 一些流程的容器以及流程完成时的通知方式。
 *
 * @author qinan.qn
 * @author jialiang.linjl
 * @author leyou(lihao)
 * @author Eric Zhao
 */
public interface ProcessorSlot<T> {

    /**
     * Entrance of this slot.
     * 进入slot的入口
     *
     * @param context         current {@link Context} 当前调用链路上下文
     * @param resourceWrapper current resource 资源Id
     * @param param           generics parameter, usually is a {@link com.alibaba.csp.sentinel.node.Node}  泛型参数，一般用于传递 DefaultNode
     * @param count           tokens needed ； Sentinel 将需要被保护的资源包装起来，这与锁的实现是一样的，需要先获取锁才能继续执行。
     *                                      而 count 则与并发编程 AQS 中 tryAcquire 方法的参数作用一样，count 表示申请占用共享资源的数量，只有申请到足够的共享资源才能继续执行。
     *                                      例如，线程池有 200 个线程，当前方法执行需要申请 3 个线程才能执行，那么 count 就是 3。
     *                                      count 的值一般为 1，当限流规则配置的限流阈值类型为 threads 时，表示需要申请一个线程，当限流规则配置的限流阈值类型为 qps 时，表示需要申请 1 令牌（假设使用令牌桶算法）。
     * @param prioritized     whether the entry is prioritized  表示是否对请求进行优先级排序，SphU#entry 传递过来的值是 false。
     * @param args            parameters of the original call   调用方法传递的参数，用于实现热点参数限流。
     * @throws Throwable blocked exception or unexpected error
     */
    void entry(Context context, ResourceWrapper resourceWrapper, T param, int count, boolean prioritized,
               Object... args) throws Throwable;

    /**
     * Means finish of {@link #entry(Context, ResourceWrapper, Object, int, boolean, Object...)}.
     * 调用下一个 ProcessorSlot#entry 方法
     *
     * @param context         current {@link Context}
     * @param resourceWrapper current resource
     * @param obj             relevant object (e.g. Node)
     * @param count           tokens needed
     * @param prioritized     whether the entry is prioritized
     * @param args            parameters of the original call
     * @throws Throwable blocked exception or unexpected error
     */
    void fireEntry(Context context, ResourceWrapper resourceWrapper, Object obj, int count, boolean prioritized,
                   Object... args) throws Throwable;

    /**
     * Exit of this slot.
     * 出口方法
     *
     * @param context         current {@link Context}
     * @param resourceWrapper current resource
     * @param count           tokens needed
     * @param args            parameters of the original call
     */
    void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args);

    /**
     * Means finish of {@link #exit(Context, ResourceWrapper, int, Object...)}.
     * 调用下一个 ProcessorSlot#exit 方法
     * @param context         current {@link Context}
     * @param resourceWrapper current resource
     * @param count           tokens needed
     * @param args            parameters of the original call
     */
    void fireExit(Context context, ResourceWrapper resourceWrapper, int count, Object... args);
}
