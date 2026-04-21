package com.shop.common.interceptor;

import com.shop.common.exception.BusinessException;
import com.shop.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 * 将解析出的 userId 存入 ThreadLocal，供后续 Controller 使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    public static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    public static final ThreadLocal<Long> ADMIN_ID_HOLDER = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        if (!jwtUtils.validateToken(token)) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        // 存入 ThreadLocal
        if (jwtUtils.isAdminToken(token)) {
            ADMIN_ID_HOLDER.set(jwtUtils.getAdminId(token));
        } else {
            USER_ID_HOLDER.set(jwtUtils.getUserId(token));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        USER_ID_HOLDER.remove();
        ADMIN_ID_HOLDER.remove();
    }

    /** 获取当前登录用户 ID（供 Service 层使用） */
    public static Long getCurrentUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        return userId;
    }

    /** 获取当前登录管理员 ID（供 Service 层使用） */
    public static Long getCurrentAdminId() {
        Long adminId = ADMIN_ID_HOLDER.get();
        if (adminId == null) {
            throw new BusinessException(401, "管理员未登录");
        }
        return adminId;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
