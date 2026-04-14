package com.hify.server.modules.system.controller;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.common.web.entity.Result;
import com.hify.common.web.event.EventPublisher;
import com.hify.common.web.security.CurrentUser;
import com.hify.common.web.security.JwtUtil;
import com.hify.common.web.security.UserContext;
import com.hify.server.event.UserLoginEvent;
import com.hify.server.modules.system.dto.LoginRequest;
import com.hify.server.modules.system.dto.LoginResponse;
import com.hify.server.modules.system.entity.SysUser;
import com.hify.server.modules.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 *
 * @author hify
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EventPublisher eventPublisher;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = sysUserService.getByUsername(request.getUsername());
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "用户已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), user.getNickname());
        String token = jwtUtil.generateToken(currentUser);

        eventPublisher.publish(new UserLoginEvent(this, user.getId(), user.getUsername()));

        log.info("用户登录成功, userId={}, username={}", user.getId(), user.getUsername());
        return Result.success(new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname()));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<CurrentUser> me() {
        CurrentUser user = UserContext.get();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return Result.success(user);
    }
}
