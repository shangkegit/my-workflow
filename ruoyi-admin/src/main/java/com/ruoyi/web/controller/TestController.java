package com.ruoyi.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/test")
public class TestController
{
    @GetMapping("/hello")
    public String hello()
    {
        return "Hello World";
    }
}
