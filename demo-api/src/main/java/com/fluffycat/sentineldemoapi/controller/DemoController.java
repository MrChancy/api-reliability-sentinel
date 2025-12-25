package com.fluffycat.sentineldemoapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class DemoController {

    private final Random random = new Random();

    @GetMapping("/demo/ok")
    public ResponseEntity<String> ok() {
        return ResponseEntity.status(HttpStatus.OK).body("ok");
    }

    @GetMapping("/demo/error")
    public ResponseEntity<String> error() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
    }

    @GetMapping("/demo/flaky")
    public ResponseEntity<String> flaky(@RequestParam(defaultValue = "50") int failRate) {
        // 确保failRate在0-100范围内
        failRate = Math.max(0, Math.min(100, failRate));

        // 生成0-99的随机数
        int randomValue = random.nextInt(100);

        // 如果随机数 < failRate，返回500错误，否则返回200成功
        if (randomValue < failRate) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body("ok");
        }
    }
}
