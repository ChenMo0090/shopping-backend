package com.shop.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.admin.entity.Admin;
import com.shop.admin.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时自动检查并修复 t_admin 表的密码哈希格式
 * 问题原因：某些 SQL 工具在写入 BCrypt 哈希时会错误地追加 $2a$ 前缀，导致哈希变成 $2a$2a$10$...
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPasswordFixer implements ApplicationRunner {

    private final AdminMapper adminMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        adminMapper.selectList(new LambdaQueryWrapper<Admin>())
                .forEach(admin -> {
                    String pwd = admin.getPassword();
                    if (pwd != null && !pwd.startsWith("$2a$10$") && !pwd.startsWith("$2b$")) {
                        // 哈希格式异常（如 $2a$2a$10$...），重置为默认密码 admin123
                        String newHash = passwordEncoder.encode("admin123");
                        adminMapper.update(null, new LambdaUpdateWrapper<Admin>()
                                .eq(Admin::getId, admin.getId())
                                .set(Admin::getPassword, newHash));
                        log.warn("[AdminPasswordFixer] 管理员 '{}' 密码哈希格式异常，已自动重置为 admin123，请登录后立即修改密码！",
                                admin.getUsername());
                    }
                });
    }
}
