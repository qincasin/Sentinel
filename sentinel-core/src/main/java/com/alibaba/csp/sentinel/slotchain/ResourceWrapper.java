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

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.util.AssertUtil;

/**
 * A wrapper of resource name and type.
 * 资源名称和类型的包装器。
 *
 * @author qinan.qn
 * @author jialiang.linjl
 * @author Eric Zhao
 */
public abstract class ResourceWrapper {

    protected final String name; //资源名称

    protected final EntryType entryType; //流量类型 IN 和 OUT 简单理解为接收处理请求与发送请求; 当接收到别的服务或者前端发来的请求，那么 entryType 为 IN；当向其他服务发起请求时，那么 entryType 就为 OUT
    protected final int resourceType;   //资源类型  例如 Dubbo RPC、Web MVC 或者 API Gateway 网关

    public ResourceWrapper(String name, EntryType entryType, int resourceType) {
        AssertUtil.notEmpty(name, "resource name cannot be empty");
        AssertUtil.notNull(entryType, "entryType cannot be null");
        this.name = name;
        this.entryType = entryType;
        this.resourceType = resourceType;
    }

    /**
     * Get the resource name.
     *
     * @return the resource name
     */
    public String getName() {
        return name;
    }

    /**
     * Get {@link EntryType} of this wrapper.
     *
     * @return {@link EntryType} of this wrapper.
     */
    public EntryType getEntryType() {
        return entryType;
    }

    /**
     * Get the classification of this resource.
     *
     * @return the classification of this resource
     * @since 1.7.0
     */
    public int getResourceType() {
        return resourceType;
    }

    /**
     * Get the beautified resource name to be showed.
     *
     * @return the beautified resource name
     */
    public abstract String getShowName();

    /**
     * Only {@link #getName()} is considered.
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Only {@link #getName()} is considered.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResourceWrapper) {
            ResourceWrapper rw = (ResourceWrapper)obj;
            return rw.getName().equals(getName());
        }
        return false;
    }
}
