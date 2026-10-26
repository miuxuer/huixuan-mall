package com.huixuan.item.controller;

import com.huixuan.common.domain.PageDTO;
import com.huixuan.item.domain.po.ItemDoc;
import com.huixuan.item.domain.query.ItemPageQuery;
import com.huixuan.item.service.IItemSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final IItemSearchService itemSearchService;

    @Operation(summary = "搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDoc> search(ItemPageQuery query) {
        return itemSearchService.search(query);
    }

    @Operation(summary = "查询搜索过滤项")
    @PostMapping("/filters")
    public Map<String, List<String>> filters(@RequestBody ItemPageQuery query) {
        return itemSearchService.filters(query);
    }
}
