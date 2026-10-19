package com.hims.web.controller;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.common.core.domain.AjaxResult;
import com.hims.web.service.KnowledgeRagClient;
import com.hims.web.service.KnowledgeRagClient.RagResponse;

/** 知识库问答代理。 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController
{
    private final KnowledgeRagClient ragClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeController(KnowledgeRagClient ragClient)
    {
        this.ragClient = ragClient;
    }

    @PostMapping("/ask")
    public AjaxResult ask(@RequestBody Map<String, Object> request)
    {
        try
        {
            RagResponse response = ragClient.post(request);
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(),
                    new TypeReference<Map<String, Object>>() { });
            if (response.getStatus() >= 200 && response.getStatus() < 300)
            {
                return AjaxResult.success(toPublicResponse(responseBody));
            }
            return toErrorResponse(response.getStatus());
        }
        catch (SocketTimeoutException exception)
        {
            return AjaxResult.error("知识库服务响应超时，请稍后重试");
        }
        catch (ConnectException exception)
        {
            return AjaxResult.error("知识库服务暂不可用");
        }
        catch (Exception exception)
        {
            return AjaxResult.error("知识库服务响应异常");
        }
    }

    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false, defaultValue = "上传文档") String category,
            @RequestParam(value = "drug_name", required = false) String drugName,
            @RequestParam(value = "generic_name", required = false) String genericName)
    {
        if (file == null || file.isEmpty())
        {
            return AjaxResult.error(400, "请选择要上传的文档");
        }
        try
        {
            RagResponse response = ragClient.upload(file.getBytes(), file.getOriginalFilename(), category,
                    drugName, genericName);
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(),
                    new TypeReference<Map<String, Object>>() { });
            if (response.getStatus() >= 200 && response.getStatus() < 300)
            {
                return AjaxResult.success(responseBody);
            }
            return toErrorResponse(response.getStatus());
        }
        catch (SocketTimeoutException exception)
        {
            return AjaxResult.error("文档处理超时，请稍后重试");
        }
        catch (ConnectException exception)
        {
            return AjaxResult.error("知识库服务暂不可用");
        }
        catch (Exception exception)
        {
            return AjaxResult.error("文档上传处理异常");
        }
    }

    private AjaxResult toErrorResponse(int status)
    {
        if (status == 422)
        {
            return AjaxResult.error(400, "知识库请求参数不合法");
        }
        if (status == 401 || status == 403)
        {
            return AjaxResult.error("知识库内部令牌校验失败");
        }
        if (status == 503)
        {
            return AjaxResult.error("知识库服务暂不可用");
        }
        return AjaxResult.error("知识库服务返回错误");
    }

    private Map<String, Object> toPublicResponse(Map<String, Object> response)
    {
        Map<String, Object> projected = new LinkedHashMap<>();
        projected.put("answer", response.get("answer"));
        projected.put("domain", response.get("domain"));
        projected.put("status", response.get("status"));
        projected.put("sources", response.get("sources"));
        projected.put("safety", response.get("safety"));
        projected.put("requestId", response.get("requestId"));
        return projected;
    }
}
