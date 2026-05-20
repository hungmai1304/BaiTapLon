package com.auction.common.utils;


import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.format(formatter));
    }

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // TRƯỜNG HỢP 1: Server gửi về dạng String chuẩn ISO (Ví dụ: "2026-05-20T11:27:00")
        if (json.isJsonPrimitive()) {
            return LocalDateTime.parse(json.getAsString(), formatter);
        }

        // TRƯỜNG HỢP 2: Server chạy JDK cũ, dùng Reflection biến LocalDateTime thành Object có cấu trúc {date:..., time:...}
        else if (json.isJsonObject()) {
            try {
                JsonObject obj = json.getAsJsonObject();
                JsonObject dateObj = obj.getAsJsonObject("date");
                JsonObject timeObj = obj.getAsJsonObject("time");

                int year = dateObj.has("year") ? dateObj.get("year").getAsInt() : 1970;
                int month = dateObj.has("month") ? dateObj.get("month").getAsInt() : 1;
                int day = dateObj.has("day") ? dateObj.get("day").getAsInt() : 1;

                int hour = timeObj.has("hour") ? timeObj.get("hour").getAsInt() : 0;
                int minute = timeObj.has("minute") ? timeObj.get("minute").getAsInt() : 0;
                int second = timeObj.has("second") ? timeObj.get("second").getAsInt() : 0;
                int nano = timeObj.has("nano") ? timeObj.get("nano").getAsInt() : 0;

                return LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute, second, nano));
            } catch (Exception e) {
                throw new JsonParseException("Không thể bóc tách cấu trúc Object LocalDateTime cũ: " + json, e);
            }
        }
        throw new JsonParseException("Kiểu dữ liệu JSON không hợp lệ đối với LocalDateTime: " + json);
    }
}