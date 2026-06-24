package com.gsmv.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 百度地图天气 API 集成。
 * 文档: https://lbsyun.baidu.com/index.php?title=webapi/weather
 * 端点: https://api.map.baidu.com/weather/v1/
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final ObjectMapper objectMapper;
    private final String baiduAk;
    private final RestClient restClient;

    public WeatherService(
            ObjectMapper objectMapper,
            @Value("${gsmv.baidu.map-ak:}") String baiduAk
    ) {
        this.objectMapper = objectMapper;
        this.baiduAk = baiduAk;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.map.baidu.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(baiduAk);
    }

    /**
     * 根据城市名获取实时天气。
     * 返回格式化的天气信息字符串，供 AI prompt 使用。
     */
    public String fetchWeatherByCity(String city) {
        if (!isConfigured()) {
            return null;
        }
        if (!StringUtils.hasText(city)) {
            return null;
        }

        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weather/v1/")
                            .queryParam("district", city)
                            .queryParam("data_type", "all")
                            .queryParam("ak", baiduAk)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            int status = root.path("status").asInt(-1);
            if (status != 0) {
                log.warn("Baidu weather API returned status={}, message={}", status, root.path("message").asText());
                return null;
            }

            return formatWeatherResult(root, city);
        } catch (Exception e) {
            log.warn("Failed to fetch weather for city={}: {}", city, e.getMessage());
            return null;
        }
    }

    private String formatWeatherResult(JsonNode root, String city) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(city).append("实时天气信息】\n");

        JsonNode result = root.path("result");
        JsonNode forecasts = result.path("forecasts");
        if (forecasts.isArray() && !forecasts.isEmpty()) {
            JsonNode today = forecasts.get(0);
            sb.append("日期: ").append(today.path("date").asText("未知")).append("\n");
            sb.append("白天天气: ").append(today.path("text_day").asText("未知")).append("\n");
            sb.append("夜间天气: ").append(today.path("text_night").asText("未知")).append("\n");
            sb.append("最高温度: ").append(today.path("high").asText("未知")).append("℃\n");
            sb.append("最低温度: ").append(today.path("low").asText("未知")).append("℃\n");
            sb.append("白天风向风力: ").append(today.path("wind_day").asText("未知"))
              .append(" ").append(today.path("wc_day").asText("")).append("\n");
            sb.append("夜间风向风力: ").append(today.path("wind_night").asText("未知"))
              .append(" ").append(today.path("wc_night").asText("")).append("\n");

            // 检查是否有实时数据
            JsonNode now = result.path("now");
            if (!now.isMissingNode()) {
                sb.append("当前温度: ").append(now.path("temp").asText("未知")).append("℃\n");
                sb.append("当前天气: ").append(now.path("text").asText("未知")).append("\n");
                sb.append("当前风向风力: ").append(now.path("wind_dir").asText("未知"))
                  .append(" ").append(now.path("wind_class").asText("")).append("\n");
                sb.append("相对湿度: ").append(now.path("rh").asText("未知")).append("%\n");
            }
        }

        // 未来几天预报
        if (forecasts.isArray() && forecasts.size() > 1) {
            sb.append("\n未来预报:\n");
            for (int i = 1; i < forecasts.size() && i <= 3; i++) {
                JsonNode f = forecasts.get(i);
                sb.append("  ").append(f.path("date").asText("未知"))
                  .append(": ").append(f.path("text_day").asText(""))
                  .append("/").append(f.path("text_night").asText(""))
                  .append(" ").append(f.path("high").asText("")).append("-").append(f.path("low").asText("")).append("℃\n");
            }
        }

        return sb.toString();
    }
}
