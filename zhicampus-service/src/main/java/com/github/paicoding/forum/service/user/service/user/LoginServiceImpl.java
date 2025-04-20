package com.github.paicoding.forum.service.user.service.user;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.github.paicoding.forum.api.model.context.ReqInfoContext;
import com.github.paicoding.forum.api.model.exception.ExceptionUtil;
import com.github.paicoding.forum.api.model.vo.constants.StatusEnum;
import com.github.paicoding.forum.api.model.vo.user.UserPwdLoginReq;
import com.github.paicoding.forum.api.model.vo.user.UserSaveReq;
import com.github.paicoding.forum.core.cache.RedisClient;
import com.github.paicoding.forum.service.user.repository.dao.UserAiDao;
import com.github.paicoding.forum.service.user.repository.dao.UserDao;
import com.github.paicoding.forum.service.user.repository.entity.UserDO;
import com.github.paicoding.forum.service.user.service.LoginService;
import com.github.paicoding.forum.service.user.service.RegisterService;
import com.github.paicoding.forum.service.user.service.UserAiService;
import com.github.paicoding.forum.service.user.service.UserService;
import com.github.paicoding.forum.service.user.service.help.StarNumberHelper;
import com.github.paicoding.forum.service.user.service.help.UserPwdEncoder;
import com.github.paicoding.forum.service.user.service.help.UserSessionHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于验证码、用户名密码的登录方式
 *
 * @author xqr
 * @date 2022/8/15
 */
@Service
@Slf4j
public class LoginServiceImpl implements LoginService {
    @Autowired
    private UserDao userDao;

    @Autowired
    private UserAiDao userAiDao;

    @Autowired
    private UserSessionHelper userSessionHelper;
    @Autowired
    private StarNumberHelper starNumberHelper;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private UserPwdEncoder userPwdEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private UserAiService userAiService;

    @Autowired
    private RedisTemplate<String , String> redisTemplate ;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String USER_LOGIN_ATTEMPTS = "user:login:attempts:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long autoRegisterWxUserInfo(String uuid) {
        UserSaveReq req = new UserSaveReq().setLoginType(0).setThirdAccountId(uuid);
        Long userId = registerOrGetUserInfo(req);
        ReqInfoContext.getReqInfo().setUserId(userId);
        return userId;
    }

    /**
     * 没有注册时，先注册一个用户；若已经有，则登录
     *
     * @param req
     */
    private Long registerOrGetUserInfo(UserSaveReq req) {
        UserDO user = userDao.getByThirdAccountId(req.getThirdAccountId());
        if (user == null) {
            return registerService.registerByWechat(req.getThirdAccountId());
        }
        return user.getId();
    }

    @Override
    public void logout(String session) {
        userSessionHelper.removeSession(session);
    }

    /**
     * 给微信公众号的用户生成一个用于登录的会话
     *
     * @param userId 用户id
     * @return
     */
    @Override
    public String loginByWx(Long userId) {
        return userSessionHelper.genSession(userId);
    }

    /**
     * 用户名密码方式登录
     *
     * @param username 用户名
     * @param password 密码
     * @param captcha 验证码
     * @param codeKey 验证码key
     * @return
     */
    @Override
    public String loginByUserPwd(String username, String password, String captcha, String codeKey) {
        // 检查登录风险，在60秒内输错10次密码会被限制
        boolean isLoginAllowed = loginRateLimit(username, 5, 60);

        if (!isLoginAllowed) {
            // 账号密码错误太多次被限流
            throw ExceptionUtil.of(StatusEnum.LOGIN_TOO_MANY) ;
        }

        // 从Redis中获取验证码
        String redisCode = redisTemplate.opsForValue().get("user:login:validatecode:" + codeKey);
        if(StrUtil.isEmpty(redisCode) || !StrUtil.equalsIgnoreCase(redisCode , captcha)) {
            //验证码错误
            throw ExceptionUtil.of(StatusEnum.VALIDATE_CODE_ERROR) ;
        }
        // 验证通过删除redis中的验证码
        redisTemplate.delete("user:login:validatecode:" + codeKey) ;

        UserDO user = userDao.getUserByUserName(username);
        if (user == null) {
            throw ExceptionUtil.of(StatusEnum.USER_NOT_EXISTS, "userName=" + username);
        }

        // passwordEncoder.matches(password, user.getPassword());
        String zSetMember = System.currentTimeMillis() + ":" + UUID.randomUUID().toString().substring(0, 8);
        if (!userPwdEncoder.match(password, user.getPassword())) {
            RedisClient.zAdd(USER_LOGIN_ATTEMPTS + username, zSetMember, System.currentTimeMillis());
            throw ExceptionUtil.of(StatusEnum.USER_PWD_ERROR);
        }

        Long userId = user.getId();
        // 1. 为了兼容历史数据，对于首次登录成功的用户，初始化ai信息
        userAiService.initOrUpdateAiInfo(new UserPwdLoginReq().setUserId(userId).setUsername(username).setPassword(password));

        // 登录成功，删除登录限流信息，返回对应的session
        RedisClient.zRemoveKey(USER_LOGIN_ATTEMPTS + username);
        ReqInfoContext.getReqInfo().setUserId(userId);
        return userSessionHelper.genSession(userId);
    }

    private boolean loginRateLimit(String username, int limitCount, int windowInSeconds) {
        long now = System.currentTimeMillis();
        long windowSize = (long)windowInSeconds * 1000;
        String key = USER_LOGIN_ATTEMPTS + username;
        Long attemptCount = RedisClient.zCountByScore(key, now - windowSize, now);
        // 判断是否超过限制
        return attemptCount < limitCount;
    }

    /**
     * 用户名密码方式登录
     *
     * @param username 用户名
     * @param password 密码
     * @return
     */
    @Override
    public String loginByUserPwd(String username, String password) {
        UserDO user = userDao.getUserByUserName(username);
        if (user == null) {
            throw ExceptionUtil.of(StatusEnum.USER_NOT_EXISTS, "userName=" + username);
        }

        // passwordEncoder.matches(password, user.getPassword());

        if (!userPwdEncoder.match(password, user.getPassword())) {
            throw ExceptionUtil.of(StatusEnum.USER_PWD_ERROR);
        }

        Long userId = user.getId();
        // 1. 为了兼容历史数据，对于首次登录成功的用户，初始化ai信息
        userAiService.initOrUpdateAiInfo(new UserPwdLoginReq().setUserId(userId).setUsername(username).setPassword(password));

        // 登录成功，返回对应的session
        ReqInfoContext.getReqInfo().setUserId(userId);

        // 生成JWT并加入到redis key:token value: userId
        return userSessionHelper.genSession(userId);
    }



    /**
     * 用户名密码方式登录，若用户不存在，则进行注册
     *
     * @param loginReq 登录信息
     * @return
     */
    @Override
    public String registerByUserPwd(UserPwdLoginReq loginReq) {
        // 1. 前置非空校验
        if (StringUtils.isBlank(loginReq.getUsername()) || StringUtils.isBlank(loginReq.getPassword())) {
            throw ExceptionUtil.of(StatusEnum.USER_PWD_ERROR);
        }

        // 从Redis中获取验证码
        String codeKey = loginReq.getCodeKey();
        String captcha = loginReq.getCaptcha();
        String redisCode = redisTemplate.opsForValue().get("user:login:validatecode:" + codeKey);
        if(StrUtil.isEmpty(redisCode) || !StrUtil.equalsIgnoreCase(redisCode , captcha)) {
            //验证码错误
            throw ExceptionUtil.of(StatusEnum.VALIDATE_CODE_ERROR) ;
        }
        // 验证通过删除redis中的验证码
        redisTemplate.delete("user:login:validatecode:" + codeKey) ;


        // 2. 判断当前用户是否登录，若已经登录，则直接走绑定流程
        Long userId = ReqInfoContext.getReqInfo().getUserId();
        loginReq.setUserId(userId);
        if (userId != null) {
            // 2.1 如果用户已经登录，则走绑定用户信息流程
            userService.bindUserInfo(loginReq);
            return ReqInfoContext.getReqInfo().getSession();
        }


        // 3. 尝试使用用户名进行登录，若成功，则依然走绑定流程
        UserDO user = userDao.getUserByUserName(loginReq.getUsername());
        if (user != null) {
            // 3.1 用户名已经存在
            throw ExceptionUtil.of(StatusEnum.USER_LOGIN_NAME_REPEAT, loginReq.getUsername());
        } else {
            //4. 走用户注册流程
            userId = registerService.registerByUserNameAndPassword(loginReq);
        }
        ReqInfoContext.getReqInfo().setUserId(userId);
        return userSessionHelper.genSession(userId);
    }



}
