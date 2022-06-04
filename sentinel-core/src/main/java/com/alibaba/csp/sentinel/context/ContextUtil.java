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
package com.alibaba.csp.sentinel.context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.csp.sentinel.Constants;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphO;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.node.EntranceNode;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.slotchain.StringResourceWrapper;
import com.alibaba.csp.sentinel.slots.nodeselector.NodeSelectorSlot;

/**
 * Utility class to get or create {@link Context} in current thread.
 *
 * <p>
 * Each {@link SphU}#entry() or {@link SphO}#entry() should be in a {@link Context}.
 * If we don't invoke {@link ContextUtil}#enter() explicitly, DEFAULT context will be used.
 * </p>
 *
 * @author jialiang.linjl
 * @author leyou(lihao)
 * @author Eric Zhao
 */
public class ContextUtil {

    /**
     * Store the context in ThreadLocal for easy access. 将context 存储在上下文中，以便于访问
     */
    private static ThreadLocal<Context> contextHolder = new ThreadLocal<>();

    /**
     * Holds all {@link EntranceNode}. Each {@link EntranceNode} is associated with a distinct context name.
     * 持有所有 {@link EntranceNode}。每个 {@link EntranceNode} 都与一个不同的上下文名称相关联。
     */
    private static volatile Map<String, DefaultNode> contextNameNodeMap = new HashMap<>();

    //可重入锁
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Context NULL_CONTEXT = new NullContext();

    static {
        // Cache the entrance node for default context.
        initDefaultContext();
    }

    private static void initDefaultContext() {
        String defaultContextName = Constants.CONTEXT_DEFAULT_NAME;
        EntranceNode node = new EntranceNode(new StringResourceWrapper(defaultContextName, EntryType.IN), null);
        Constants.ROOT.addChild(node);
        contextNameNodeMap.put(defaultContextName, node);
    }

    /**
     * Not thread-safe, only for test.
     */
    static void resetContextMap() {
        if (contextNameNodeMap != null) {
            RecordLog.warn("Context map cleared and reset to initial state");
            contextNameNodeMap.clear();
            initDefaultContext();
        }
    }

    /**
     * <p>
     * Enter the invocation context, which marks as the entrance of an invocation chain.
     * The context is wrapped with {@code ThreadLocal}, meaning that each thread has it's own {@link Context}.
     * New context will be created if current thread doesn't have one.
     * 进入调用上下文，标记为调用链的入口。上下文用 {@code ThreadLocal} 包装，这意味着每个线程都有自己的 {@link Context}。
     * 如果当前线程没有上下文，则将创建新上下文。 <p
     * </p>
     * <p>
     * A context will be bound with an {@link EntranceNode}, which represents the entrance statistic node
     * of the invocation chain. New {@link EntranceNode} will be created if
     * current context does't have one. Note that same context name will share
     * same {@link EntranceNode} globally.
     * 一个上下文会绑定一个{@link EntranceNode}，代表调用链的入口统计节点。
     * 如果当前上下文没有，则将创建新的 {@link EntranceNode}。
     * 请注意，相同的上下文名称将在全局范围内共享相同的 {@link EntranceNode}。
     * </p>
     * <p>
     * The origin node will be created in {@link com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot}.
     * Note that each distinct {@code origin} of different resources will lead to creating different new
     * {@link Node}, meaning that total amount of created origin statistic nodes will be:<br/>
     * {@code distinct resource name amount * distinct origin count}.<br/>
     * So when there are too many origins, memory footprint should be carefully considered.
     * 源节点将在 {@link com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot} 中创建。
     * 请注意，不同资源的每一个不同的{@code origin}都会导致创建不同的新{@link Node}，这意味着创建的源统计节点的总数将为
     * ：<br> {@code distinct resource name amount distinct origin count} .<br>
     * 所以当origin太多的时候，内存占用要慎重考虑。
     * </p>
     * <p>
     * Same resource in different context will count separately, see {@link NodeSelectorSlot}.
     * 不同上下文中的相同资源将分别计算，请参阅 {@link NodeSelectorSlot}。
     * </p>
     *
     * @param name   the context name
     * @param origin the origin of this invocation, usually the origin could be the Service
     *               Consumer's app name. The origin is useful when we want to control different
     *               invoker/consumer separately.
     *               此调用的来源，通常来源可以是服务消费者的应用程序名称。当我们想分别控制不同的invokerconsumer时，origin很有用。
     * @return The invocation context of the current thread
     */
    public static Context enter(String name, String origin) {
        if (Constants.CONTEXT_DEFAULT_NAME.equals(name)) {
            throw new ContextNameDefineException(
                "The " + Constants.CONTEXT_DEFAULT_NAME + " can't be permit to defined!");
        }
        return trueEnter(name, origin);
    }

    protected static Context trueEnter(String name, String origin) {
        Context context = contextHolder.get();
        if (context == null) {
            Map<String, DefaultNode> localCacheNameMap = contextNameNodeMap;
            DefaultNode node = localCacheNameMap.get(name);
            if (node == null) {
                if (localCacheNameMap.size() > Constants.MAX_CONTEXT_NAME_SIZE) {
                    setNullContext();
                    return NULL_CONTEXT;
                } else {
                    LOCK.lock();
                    try {
                        node = contextNameNodeMap.get(name);
                        if (node == null) {
                            if (contextNameNodeMap.size() > Constants.MAX_CONTEXT_NAME_SIZE) {
                                setNullContext();
                                return NULL_CONTEXT;
                            } else {
                                node = new EntranceNode(new StringResourceWrapper(name, EntryType.IN), null);
                                // Add entrance node.
                                Constants.ROOT.addChild(node);

                                Map<String, DefaultNode> newMap = new HashMap<>(contextNameNodeMap.size() + 1);
                                newMap.putAll(contextNameNodeMap);
                                newMap.put(name, node);
                                contextNameNodeMap = newMap;
                            }
                        }
                    } finally {
                        LOCK.unlock();
                    }
                }
            }
            context = new Context(node, name);
            context.setOrigin(origin);
            contextHolder.set(context);
        }

        return context;
    }

    private static boolean shouldWarn = true;

    private static void setNullContext() {
        contextHolder.set(NULL_CONTEXT);
        // Don't need to be thread-safe.
        if (shouldWarn) {
            RecordLog.warn("[SentinelStatusChecker] WARN: Amount of context exceeds the threshold "
                + Constants.MAX_CONTEXT_NAME_SIZE + ". Entries in new contexts will NOT take effect!");
            shouldWarn = false;
        }
    }

    /**
     * <p>
     * Enter the invocation context, which marks as the entrance of an invocation chain.
     * The context is wrapped with {@code ThreadLocal}, meaning that each thread has it's own {@link Context}.
     * New context will be created if current thread doesn't have one.
     * </p>
     * <p>
     * A context will be bound with an {@link EntranceNode}, which represents the entrance statistic node
     * of the invocation chain. New {@link EntranceNode} will be created if
     * current context does't have one. Note that same context name will share
     * same {@link EntranceNode} globally.
     * </p>
     * <p>
     * Same resource in different context will count separately, see {@link NodeSelectorSlot}.
     * </p>
     *
     * @param name the context name
     * @return The invocation context of the current thread
     */
    public static Context enter(String name) {
        return enter(name, "");
    }

    /**
     * Exit context of current thread, that is removing {@link Context} in the
     * ThreadLocal.
     */
    public static void exit() {
        Context context = contextHolder.get();
        if (context != null && context.getCurEntry() == null) {
            contextHolder.set(null);
        }
    }

    /**
     * Get current size of context entrance node map.
     *
     * @return current size of context entrance node map
     * @since 0.2.0
     */
    public static int contextSize() {
        return contextNameNodeMap.size();
    }

    /**
     * Check if provided context is a default auto-created context.
     *
     * @param context context to check
     * @return true if it is a default context, otherwise false
     * @since 0.2.0
     */
    public static boolean isDefaultContext(Context context) {
        if (context == null) {
            return false;
        }
        return Constants.CONTEXT_DEFAULT_NAME.equals(context.getName());
    }

    /**
     * Get {@link Context} of current thread.
     *
     * @return context of current thread. Null value will be return if current
     * thread does't have context.
     */
    public static Context getContext() {
        return contextHolder.get();
    }

    /**
     * <p>
     * Replace current context with the provided context.
     * This is mainly designed for context switching (e.g. in asynchronous invocation).
     * </p>
     * <p>
     * Note: When switching context manually, remember to restore the original context.
     * For common scenarios, you can use {@link #runOnContext(Context, Runnable)}.
     * </p>
     *
     * @param newContext new context to set
     * @return old context
     * @since 0.2.0
     */
    static Context replaceContext(Context newContext) {
        Context backupContext = contextHolder.get();
        if (newContext == null) {
            contextHolder.remove();
        } else {
            contextHolder.set(newContext);
        }
        return backupContext;
    }

    /**
     * Execute the code within provided context.
     * This is mainly designed for context switching (e.g. in asynchronous invocation).
     *
     * @param context the context
     * @param f       lambda to run within the context
     * @since 0.2.0
     */
    public static void runOnContext(Context context, Runnable f) {
        Context curContext = replaceContext(context);
        try {
            f.run();
        } finally {
            replaceContext(curContext);
        }
    }
}
