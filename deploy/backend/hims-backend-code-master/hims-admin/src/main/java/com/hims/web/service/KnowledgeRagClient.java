package com.hims.web.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/** FastAPI 知识库服务客户端。 */
@Component
public class KnowledgeRagClient
{
    @Value("${knowledge.rag.url:http://127.0.0.1:8000/api/ask}")
    private String ragUrl;
    @Value("${knowledge.rag.internal-token:}")
    private String internalToken;
    @Value("${knowledge.rag.connect-timeout-ms:2000}")
    private int connectTimeoutMs;
    @Value("${knowledge.rag.read-timeout-ms:90000}")
    private int readTimeoutMs;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagResponse post(Map<String, Object> request) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) new URL(ragUrl).openConnection();
        try
        {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            if (internalToken != null && !internalToken.trim().isEmpty())
            {
                connection.setRequestProperty("X-Internal-Token", internalToken);
            }
            connection.getOutputStream().write(objectMapper.writeValueAsBytes(request));
            int status = connection.getResponseCode();
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new RagResponse(status, readBody(input));
        }
        finally
        {
            connection.disconnect();
        }
    }

    public RagResponse upload(byte[] fileBytes, String fileName, String category,
            String drugName, String genericName) throws IOException
    {
        String uploadUrl = ragUrl.replaceAll("/api/ask$", "/api/upload");
        String boundary = "----SmartCare" + UUID.randomUUID().toString().replace("-", "");
        HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
        try
        {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            if (internalToken != null && !internalToken.trim().isEmpty())
            {
                connection.setRequestProperty("X-Internal-Token", internalToken);
            }
            try (OutputStream output = connection.getOutputStream())
            {
                writeField(output, boundary, "category", category);
                writeField(output, boundary, "drug_name", drugName);
                writeField(output, boundary, "generic_name", genericName);
                output.write(("--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\""
                        + fileName + "\"\r\n"
                        + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(fileBytes);
                output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new RagResponse(status, readBody(input));
        }
        finally
        {
            connection.disconnect();
        }
    }

    private void writeField(OutputStream output, String boundary, String name, String value) throws IOException
    {
        if (value == null || value.trim().isEmpty())
        {
            return;
        }
        output.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private String readBody(InputStream input) throws IOException
    {
        if (input == null)
        {
            return "{}";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                body.append(line);
            }
        }
        return body.toString();
    }

    public static class RagResponse
    {
        private final int status;
        private final String body;

        public RagResponse(int status, String body)
        {
            this.status = status;
            this.body = body;
        }

        public int getStatus() { return status; }
        public String getBody() { return body; }
    }
}
