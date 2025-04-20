package com.github.paicoding.forum.service.constant;

/**
 * Redis字段常量
 *
 * @ClassName: RedisConstant
 * @author xqr
 * @Date: 2023/6/2 06:39
 * @Version: 1.0
 */
public class RedisConstant {

    /**
     * article前缀
     */
    public static final String REDIS_ZHI = "zhi";

    /**
     * article前缀
     */
    public static final String REDIS_PRE_ARTICLE = ":article:";

    /**
     * 缓存
     */
    public static final String REDIS_CACHE = "cache:";

    /**
     * 分布式锁
     */
    public static final String REDIS_LOCK = "lock:";

    /**
     * 缓存文章过期时间
     */
    public static final long ARTICLE_EXPIRE_TIME = (long) 30 * 60;

    /**
     * 登录验证码
     */
    public static final String LOGIN_USER_VALIDATECODE = "user:login:validatecode:";

}
