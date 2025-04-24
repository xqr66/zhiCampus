package com.github.paicoding.forum.service.user.service.blocklist;

import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO;
import com.github.paicoding.forum.core.cache.RedisClient;
import com.github.paicoding.forum.service.user.repository.entity.UserDO;
import com.github.paicoding.forum.service.user.service.AuthorBlockListService;
import com.github.paicoding.forum.service.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @ClassName AuthorBlockListServiceImpl
 * @Description
 * @Author xqr
 * @Date 2025/4/20 19:32
 */

@Service
public class AuthorBlockListServiceImpl implements AuthorBlockListService {

    @Autowired
    private UserService userService;

    /**
     * 实用 redis - set 来存储允许直接发文章的白名单
     */
    private static final String AUTHOR_BLOCK_PRE = ":auth_block";

    private static final int SECONDS_IN_DAY = 24 * 60 * 60;

    @Override
    public boolean authorInBlockList(Long authorId) {
        String redisCacheKey = AUTHOR_BLOCK_PRE;
        Double score = RedisClient.zScore(redisCacheKey, authorId.toString());
        boolean result = (score != null && score > System.currentTimeMillis());
        return result;
    }

    @Override
    public List<BaseUserInfoDTO> queryAllBlockListAuthors() {
        String redisCacheKey = AUTHOR_BLOCK_PRE;
        Set<Long> users = RedisClient.zGetMembersByScoreGreaterThan(redisCacheKey,(double)System.currentTimeMillis(), Long.class);
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyList();
        }
        List<BaseUserInfoDTO> userInfos = userService.batchQueryBasicUserInfo(users);
        for (BaseUserInfoDTO userInfo : userInfos) {
            Double blockToTime = RedisClient.zScore(redisCacheKey, userInfo.getUserId().toString());
            // 将科学计数法转化为真实数值
            BigDecimal blockTime = new BigDecimal(blockToTime);
            // 将时间戳格式化
            Long timestamp = blockTime.longValue();
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            String formatted = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            userInfo.setBlockTime(formatted);
        }
        return userInfos;
    }

    @Override
    public void addAuthor2BlockList(Long userId, Integer blockDays) {
        int expireTimeInSecond = SECONDS_IN_DAY * blockDays;
        long expireTime = expireTimeInSecond * 1000;

        String redisCacheKey = AUTHOR_BLOCK_PRE;
        RedisClient.zAdd(redisCacheKey, userId.toString(), System.currentTimeMillis() + expireTime);
    }

    @Override
    public void removeAuthorFromBlockList(Long userId) {
        RedisClient.zRemoveMember(AUTHOR_BLOCK_PRE, userId);
    }

    @Override
    public String getUserBlockTime(Long userId) {
        Double blockToTime = RedisClient.zScore(AUTHOR_BLOCK_PRE, userId.toString());
        // 将科学计数法转化为真实数值
        BigDecimal blockTime = new BigDecimal(blockToTime);
        // 将时间戳格式化
        Long timestamp = blockTime.longValue();
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        String formatted = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return formatted;
    }
}
