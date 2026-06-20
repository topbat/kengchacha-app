package com.kengchacha.content;

import com.kengchacha.common.ApiResponse;
import com.kengchacha.common.PageResult;
import com.kengchacha.content.dto.ContentCard;
import com.kengchacha.content.dto.ContentDetail;
import com.kengchacha.content.dto.TagGroup;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /** 避坑头条信息流：五维筛选 + 关键词 + 分页。 */
    @GetMapping("/feed")
    public ApiResponse<PageResult<ContentCard>> feed(
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String psych,
            @RequestParam(required = false) Integer hazard,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(contentService.feed(stage, domain, psych, hazard, q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(contentService.detail(id));
    }

    /** 五维标签字典（筛选条）。 */
    @GetMapping("/tags")
    public ApiResponse<List<TagGroup>> tags() {
        return ApiResponse.ok(contentService.tagGroups());
    }
}
