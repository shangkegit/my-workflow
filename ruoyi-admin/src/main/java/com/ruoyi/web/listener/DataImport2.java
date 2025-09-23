package com.ruoyi.web.listener;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.system.domain.Actor;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DataImport2 implements JavaDelegate {

    @Autowired
    RestTemplate restTemplate;

    private Expression url;

    private Expression name;

    private static final Logger log = LoggerFactory.getLogger(DataImport2.class);

    @Override
    public void execute(DelegateExecution execution) {
        // 调用REST接口
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Actor data = new Actor();
        data.setAge(10);
        try {
            String urlStr = url.getExpressionText();
            String nameStr = (String) name.getValue(execution);
            data.setName(nameStr);
            log.info("字段值{}---,{}", urlStr, nameStr);
            HttpEntity<Actor> entity = new HttpEntity<Actor>(data, headers);
            Actor a = restTemplate.postForObject(urlStr, entity ,Actor.class);
            log.info("接口调用返回{}", JSON.toJSONString(a));
            execution.setVariable("result", 200);
        } catch (Exception e) {
            e.printStackTrace();
            execution.setVariable("result", 500);
        }

    }
}
