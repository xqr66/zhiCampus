package com.github.paicoding.forum.web.admin.rest;

import com.github.paicoding.forum.api.model.vo.ResVo;
import com.github.paicoding.forum.api.model.vo.user.dto.BaseUserInfoDTO;
import com.github.paicoding.forum.core.permission.Permission;
import com.github.paicoding.forum.core.permission.UserRole;
import com.github.paicoding.forum.service.user.service.AuthorBlockListService;
import com.github.paicoding.forum.service.user.service.AuthorWhiteListService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户封禁服务
 *
 * @author xqr
 * @date 2023/4/9
 */
@RestController
@Permission(role = UserRole.ADMIN)
@RequestMapping(path = {"api/admin/author/blocklist"})
public class AuthorBlockListController {
    @Autowired
    private AuthorBlockListService articleBlockListService;

    @GetMapping(path = "get")
    @ApiOperation(value = "封禁列表", notes = "返回封禁列表")
    public ResVo<List<BaseUserInfoDTO>> whiteList() {
        return ResVo.ok(articleBlockListService.queryAllBlockListAuthors());
    }

    @GetMapping(path = "add")
    @ApiOperation(value = "添加封禁用户", notes = "将指定作者加入作者封禁列表")
    @ApiImplicitParam(name = "authorId", value = "传入需要添加封禁的作者UserId", required = true, allowEmptyValue = false, example = "1")
    public ResVo<Boolean> addAuthor(@RequestParam("authorId") Long authorId, @RequestParam("blockDays") Integer blockDays) {
        articleBlockListService.addAuthor2BlockList(authorId, blockDays);
        return ResVo.ok(true);
    }

    @GetMapping(path = "remove")
    @ApiOperation(value = "删除封禁", notes = "将作者从封禁列表删除")
    @ApiImplicitParam(name = "authorId", value = "传入需要删除封禁的作者UserId", required = true, allowEmptyValue = false, example = "1")
    public ResVo<Boolean> rmAuthor(@RequestParam("authorId") Long authorId) {
        articleBlockListService.removeAuthorFromBlockList(authorId);
        return ResVo.ok(true);
    }
}
