package com.github.paicoding.forum.service.user.service;

import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO;

import java.util.List;

/**
 * @ClassName AuthorBlockListService
 * @Description
 * @author xqr
 * @Date 2025/4/20 19:24
 */

public interface AuthorBlockListService {
    /**
     * 判断作者是否在封禁名单
     *
     * @param authorId
     * @return
     */
    boolean authorInBlockList(Long authorId);

    /**
     * 获取所有的封禁用户
     *
     * @return
     */
    List<BaseUserInfoDTO> queryAllBlockListAuthors();

    /**
     * 将用户添加到封禁名单中
     *
     * @param userId
     */
    void addAuthor2BlockList(Long userId, Integer blockDays);

    /**
     * 从封禁名单中移除用户
     *
     * @param userId
     */
    void removeAuthorFromBlockList(Long userId);

    /**
     * 获取用户封禁时间
     *
     * @param userId
     */
    String getUserBlockTime(Long userId);

}
