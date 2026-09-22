package com.jezetek.modules.system.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jezetek.modules.system.model.Config;
import com.jezetek.modules.system.repository.ConfigRepository;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 登录图形验证码(工单01)：
 * <ul>
 *   <li>开关存 {@code sys_config} 的 {@code captcha.enabled}(种子默认 false)，
 *       每次读取直查库，参数页改后即时生效不发版</li>
 *   <li>答案存进程内一次性 Caffeine(key=uuid，TTL 2 分钟)，校验即删防重放</li>
 * </ul>
 *
 * <p>generate() 返回的 code 仅供服务端内部流转(测试需拿到答案)，
 * Controller 对外只暴露 key 与图片</p>
 */
@Component
public class CaptchaService {

    /** 开关所在参数键：sys_config.business_key 全局唯一 */
    public static final String CONFIG_KEY = "captcha.enabled";

    private final ConfigRepository configRepository;

    private final Cache<String, String> codes = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .build();

    public CaptchaService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /** 验证码开关是否开启(直读库，无缓存) */
    public boolean enabled() {
        Optional<Config> config = configRepository.findByConfigKey(CONFIG_KEY, null);
        return config.map(Config::configValue)
                .map(value -> "true".equalsIgnoreCase(value.trim()))
                .orElse(false);
    }

    /**
     * 生成验证码：纯字母编码(规避 0O/1I 混淆)，Base64 图可直接进 img src
     */
    public Challenge generate() {
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 5);
        captcha.setCharType(Captcha.TYPE_ONLY_CHAR);
        String code = captcha.text();
        String key = UUID.randomUUID().toString();
        codes.put(key, code);
        return new Challenge(key, code, captcha.toBase64());
    }

    /**
     * 校验并消费：无论对错都删(key 一次性)；过期/缺失/不匹配一律 false
     */
    public boolean verify(String key, String code) {
        if (key == null || key.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        String expected = codes.getIfPresent(key);
        codes.invalidate(key);
        return expected != null && expected.equalsIgnoreCase(code.trim());
    }

    /** 清空全部未消费答案(测试隔离用) */
    public void clear() {
        codes.invalidateAll();
    }

    /**
     * 一次验证码挑战：key 是答案在缓存里的键，image 为 Base64 PNG
     */
    public record Challenge(String key, String code, String image) {
    }
}
