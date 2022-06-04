/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel;

/**
 * @author Eric Zhao
 * @since 1.7.0
 */
public final class ResourceTypeConstants {

    public static final int COMMON = 0; //默认
    public static final int COMMON_WEB = 1; //web 应用
    public static final int COMMON_RPC = 2; //Dubbo 框架的 RPC 接口
    public static final int COMMON_API_GATEWAY = 3; //用于 API Gateway 网关
    public static final int COMMON_DB_SQL = 4;//数据库 SQL 操作

    private ResourceTypeConstants() {}
}
