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
package com.alibaba.csp.sentinel.demo.datasource.nacos;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Nacos config sender for demo.
 *
 * @author Eric Zhao
 */
public class NacosConfigSender {

    public static void main(String[] args) throws Exception {
        /**
         * spring.cloud.sentinel.datasource.ds1.nacos.server-addr=${nacos.url}
         * spring.cloud.sentinel.datasource.ds1.nacos.dataId=${spring.application.name}-flow-rule
         * spring.cloud.sentinel.datasource.ds1.nacos.groupId=SENTINEL_GROUP
         * spring.cloud.sentinel.datasource.ds1.nacos.groupId=DEFAULT_GROUP
         * spring.cloud.sentinel.datasource.ds1.nacos.rule-type=flow
         * spring.cloud.sentinel.datasource.ds1.nacos.data-type=json
         */
        final String remoteAddress = "10.120.0.56:8848";
        final String groupId = "SENTINEL_GROUP";
        final String dataId = "surveyapi2-flow-rule";
        final String rule = "[\n" + "    {\n" + "        \"app\": \"surveyapi2\",\n" + "        \"clusterConfig\": {\n"
                + "            \"fallbackToLocalWhenFail\": true,\n" + "            \"sampleCount\": 10,\n"
                + "            \"strategy\": 0,\n" + "            \"thresholdType\": 0,\n"
                + "            \"windowIntervalMs\": 1000\n" + "        },\n" + "        \"clusterMode\": false,\n"
                + "        \"controlBehavior\": 0,\n" + "        \"count\": 2,\n"
                + "        \"gmtCreate\": 1652152470745,\n" + "        \"gmtModified\": 1652152470745,\n"
                + "        \"grade\": 1,\n" + "        \"id\": 39,\n" + "        \"ip\": \"172.17.121.64\",\n"
                + "        \"limitApp\": \"default\",\n" + "        \"port\": 8719,\n"
                + "        \"resource\": \"/redis/get\",\n" + "        \"strategy\": 0\n" + "    }\n" + "]";

        final String rule2 = "userVersionTest5=1234567";
//        final String remoteAddress = "10.120.0.56:8848";
//        final String groupId = "DEFAULT_GROUP";
//        final String dataId = "testPush.properties";
//        final String content = "";
        ConfigService configService = NacosFactory.createConfigService(remoteAddress);
//        Properties properties = new Properties();
//        NacosFactory.createConfigService(properties);
//        configService.
        String config = configService.getConfig(dataId, groupId, 10000);
//        List<String> collect = Arrays.stream(config.split("\\n")).filter(a -> StringUtils.isBlank(a))
//                .collect(Collectors.toList());
//        List<String> resp = new ArrayList<>();
//        for (String line : collect) {
//            if (line.contains("password")){
//                String[] split = line.split("=");
//                if (split.length>=2){
//                    String s = split[1];
//                    //TODO 调用生成密码方法
//                    String result = s;
//                    line = split[0]+"="+result;
//                }
//                resp.add(line);
//            }
//        }
        System.out.println(config);

        configService.publishConfig(dataId, "DEFAULT_GROUP", rule2);

        System.out.println(configService.publishConfig(dataId, groupId, rule));
    }
}
