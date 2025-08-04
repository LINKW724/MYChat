package chat.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson的自定义适配器，用于正确地序列化和反序列化Java 8的LocalDateTime对象。
 * 这确保了时间戳在后端和前端之间能够以一种标准的、可读的格式（如 "2025-08-02T21:30:00"）进行传输。
 */
public class GsonLocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    // 定义一个标准的ISO 8601格式化器，这是现代Web API中最常用的时间格式
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 将 LocalDateTime 对象序列化（转换）为JSON字符串。
     * @param src 要序列化的LocalDateTime对象
     * @return 一个JsonPrimitive对象，其内容为格式化后的时间字符串
     */
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(formatter.format(src));
    }

    /**
     * 将JSON字符串反序列化（转换）为 LocalDateTime 对象。
     * @param json 要反序列化的JsonElement对象
     * @return 一个解析后的LocalDateTime对象
     * @throws JsonParseException 如果JSON字符串格式不正确
     */
    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return LocalDateTime.parse(json.getAsString(), formatter);
    }
}