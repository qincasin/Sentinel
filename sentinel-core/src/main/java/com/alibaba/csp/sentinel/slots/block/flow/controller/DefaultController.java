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
package com.alibaba.csp.sentinel.slots.block.flow.controller;

import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.node.OccupyTimeoutProperty;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.PriorityWaitException;
import com.alibaba.csp.sentinel.slots.block.flow.TrafficShapingController;
import com.alibaba.csp.sentinel.util.TimeUtil;

/**
 * Default throttling controller (immediately reject strategy).
 * 默认节流控制器（立即拒绝策略）。
 *
 * @author jialiang.linjl
 * @author Eric Zhao
 */
public class DefaultController implements TrafficShapingController {

    private static final int DEFAULT_AVG_USED_TOKENS = 0;

    private double count;
    private int grade;

    public DefaultController(double count, int grade) {
        this.count = count;
        this.grade = grade;
    }

    @Override
    public boolean canPass(Node node, int acquireCount) {
        return canPass(node, acquireCount, false);
    }

    @Override
    public boolean canPass(Node node, int acquireCount, boolean prioritized) {
        //如果当前规则的限流阈值类型为 QPS，则 avgUsedTokens 返回 node 当前时间窗口统计的每秒被放行的请求数；
        //如果当前规则的限流阈值类型为 THREADS，则 avgUsedTokens 返回 node 统计的当前并行占用的线程数
        int curCount = avgUsedTokens(node);
        if (curCount + acquireCount > count) {
            //已经统计的数据与本次请求的数量和 > 设置的阈值，则返回false ，表示没有通过检测
            //若小于等于阈值，则返回true， 代表通过检测
            if (prioritized && grade == RuleConstant.FLOW_GRADE_QPS) {
                long currentTime;
                long waitInMs;
                currentTime = TimeUtil.currentTimeMillis();
                //如果可以占用未来时间窗口的统计指标，则 tryOccupyNext 返回当前请求需要等待的时间，单位毫秒。
                waitInMs = node.tryOccupyNext(currentTime, acquireCount, count);
                //如果休眠时间在限制可占用的最大时间范围内，则挂起当前请求，当前线程休眠 waitInMs 毫秒。
                // 休眠结束后抛出 PriorityWait 异常，表示当前请求是等待了 waitInMs 之后通过的。
                if (waitInMs < OccupyTimeoutProperty.getOccupyTimeout()) {
                    //将休眠之后对应的时间窗口的 pass(通过)这项指标数据的值加上 acquireCount
                    node.addWaitingRequest(currentTime + waitInMs, acquireCount);
                    // 添加占用未来的 pass 指标的数量
                    node.addOccupiedPass(acquireCount);
                    //休眠等待，当前线程阻塞
                    sleep(waitInMs);

                    // PriorityWaitException indicates that the request will pass after waiting for {@link @waitInMs}.
                    // 抛出 PriorityWait 异常，表示当前请求是等待了 waitInMs 之后通过的
                    throw new PriorityWaitException(waitInMs);
                }
            }
            return false;
        }
        return true;
    }

    private int avgUsedTokens(Node node) {
        if (node == null) {
            return DEFAULT_AVG_USED_TOKENS;
        }
        return grade == RuleConstant.FLOW_GRADE_THREAD ? node.curThreadNum() : (int)(node.passQps());
    }

    private void sleep(long timeMillis) {
        try {
            Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }
}
