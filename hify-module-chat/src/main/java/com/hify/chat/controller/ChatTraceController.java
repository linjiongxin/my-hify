package com.hify.chat.controller;

import com.hify.chat.service.ChatTraceService;
import com.hify.chat.vo.ChatTraceVO;
import com.hify.common.web.entity.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话链路追踪控制器
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatTraceController {

    private final ChatTraceService chatTraceService;

    /**
     * 根据 trace_id 查询对话链路追踪
     */
    @GetMapping("/trace")
    public Result<ChatTraceVO> getTrace(@RequestParam("traceId") String traceId) {
        return Result.success(chatTraceService.buildTrace(traceId));
    }
}
