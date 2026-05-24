package com.zode.api.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoump
 * @date 2026/4/13
 * @purpose
 */
@Component
public class HttpClientRedirect {

    // 方法1: 自动处理重定向
    public String fetchWithAutoRedirect(String url) throws Exception {
        // 创建支持重定向的 HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setRedirectStrategy(new LaxRedirectStrategy()) // 宽松的重定向策略
                .build();

        HttpGet request = new HttpGet(url);
        // 设置请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .setRedirectsEnabled(true)  // 启用重定向
                .setMaxRedirects(10)         // 最大重定向次数
                .build();
        request.setConfig(requestConfig);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            // 获取最终重定向后的 URL
            String finalUrl = response.getLastHeader("Location") != null
                    ? response.getLastHeader("Location").getValue()
                    : url;
            System.out.println("最终URL: " + finalUrl);

            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, "UTF-8");
        } finally {
            httpClient.close();
        }
    }

    // 方法2: 自定义重定向处理器（获取重定向链）
    public RedirectChainResult fetchWithRedirectChain(String url) throws Exception {
        List<String> redirectChain = new ArrayList<>();
        redirectChain.add(url);

        // 自定义重定向策略
        RedirectStrategy customStrategy = new RedirectStrategy() {
            @Override
            public boolean isRedirected(HttpRequest request, HttpResponse response,
                                        HttpContext context) {
                int statusCode = response.getStatusLine().getStatusCode();
                return statusCode == 301 || statusCode == 302 ||
                        statusCode == 303 || statusCode == 307 || statusCode == 308;
            }

            @Override
            public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response,
                                              HttpContext context) throws ProtocolException {
                // 记录重定向URL
                String redirectUrl = response.getFirstHeader("Location").getValue();
                redirectChain.add(redirectUrl);
                return new HttpGet(redirectUrl);
            }
        };

        CloseableHttpClient httpClient = HttpClients.custom()
                .setRedirectStrategy(customStrategy)
                .build();

        HttpGet request = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String content = EntityUtils.toString(entity, "UTF-8");
            return new RedirectChainResult(redirectChain, content);
        } finally {
            httpClient.close();
        }
    }

}
