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
package com.alibaba.csp.sentinel.node;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.csp.sentinel.ResourceTypeConstants;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.util.AssertUtil;

/**
 * <p>
 * This class stores summary runtime statistics of the resource, including rt, thread count, qps
 * and so on. Same resource shares the same {@link ClusterNode} globally, no matter in which
 * {@link com.alibaba.csp.sentinel.context.Context}.
 * </p>
 * <p>
 * To distinguish invocation from different origin (declared in
 * {@link ContextUtil#enter(String name, String origin)}),
 * one {@link ClusterNode} holds an {@link #originCountMap}, this map holds {@link StatisticNode}
 * of different origin. Use {@link #getOrCreateOriginNode(String)} to get {@link Node} of the specific
 * origin.<br/>
 * Note that 'origin' usually is Service Consumer's app name.
 * </p>
 * <p>
 *    此类存储资源的汇总运行时统计信息，包括 rt、线程数、qps
 *   等等。 相同的资源在全球范围内共享相同的 {@link ClusterNode}，无论在哪个
 *    {@link com.alibaba.csp.sentinel.context.Context}。
 *    </p>
 *    <p>
 *    区分不同来源的调用（在
 *    {@link ContextUtil#enter(String name, String origin)}),
 *    一个 {@link ClusterNode} 拥有一个 {@link #originCountMap}，这个map拥有 {@link StatisticNode}
 *    不同来源。 使用 {@link #getOrCreateOriginNode(String)} 获取特定的 {@link Node}
 *    来源。<br/>
 *    请注意，“来源”通常是服务消费者的应用名称。
 *    </p>
 *
 * @author qinan.qn
 * @author jialiang.linjl
 */
public class ClusterNode extends StatisticNode {

    private final String name;
    private final int resourceType;

    public ClusterNode(String name) {
        this(name, ResourceTypeConstants.COMMON);
    }

    public ClusterNode(String name, int resourceType) {
        AssertUtil.notEmpty(name, "name cannot be empty");
        this.name = name;
        this.resourceType = resourceType;
    }

    /**
     * <p>The origin map holds the pair: (origin, originNode) for one specific resource.</p>
     * <p>
     * The longer the application runs, the more stable this mapping will become.
     * So we didn't use concurrent map here, but a lock, as this lock only happens
     * at the very beginning while concurrent map will hold the lock all the time.
     * </p>
     * <p>原始映射为一个特定资源保存一对：(origin, originNode)。<p>
     * <p>应用程序运行的时间越长，这个映射就会变得越稳定。所以我们这里没有使用并发映射，而是一个锁，因为这个锁只在最开始发生，而并发映射会一直持有锁。
     * <p>
     */
    private Map<String, StatisticNode> originCountMap = new HashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Get resource name of the resource node.
     *
     * @return resource name
     * @since 1.7.0
     */
    public String getName() {
        return name;
    }

    /**
     * Get classification (type) of the resource.
     *
     * @return resource type
     * @since 1.7.0
     */
    public int getResourceType() {
        return resourceType;
    }

    /**
     * <p>Get {@link Node} of the specific origin. Usually the origin is the Service Consumer's app name.</p>
     * <p>If the origin node for given origin is absent, then a new {@link StatisticNode}
     * for the origin will be created and returned.</p>
     * <p>获取特定来源的{@link Node}。通常源是服务消费者的应用程序名称。<p>
     * <p>如果给定源的源节点不存在，则将为源创建并返回一个新的 {@link StatisticNode}。<p>
     *
     * @param origin The caller's name, which is designated in the {@code parameter} parameter
     *               {@link ContextUtil#enter(String name, String origin)}.
     *               origin调用者的名字，在 {@code parameter} 参数 {@link ContextUtilenter(String name, String origin)} 中指定
     * @return the {@link Node} of the specific origin
     */
    public Node getOrCreateOriginNode(String origin) {
        StatisticNode statisticNode = originCountMap.get(origin);
        if (statisticNode == null) {
            lock.lock();
            try {
                statisticNode = originCountMap.get(origin);
                if (statisticNode == null) {
                    // The node is absent, create a new node for the origin.
                    statisticNode = new StatisticNode();
                    HashMap<String, StatisticNode> newMap = new HashMap<>(originCountMap.size() + 1);
                    newMap.putAll(originCountMap);
                    newMap.put(origin, statisticNode);
                    originCountMap = newMap;
                }
            } finally {
                lock.unlock();
            }
        }
        return statisticNode;
    }

    public Map<String, StatisticNode> getOriginCountMap() {
        return originCountMap;
    }

}
