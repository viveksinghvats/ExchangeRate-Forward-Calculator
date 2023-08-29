package forexim.util;

import com.google.gson.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class FEGson {

    // Exclusion strategy to exclude any attribute that has a @DoNotExpose marker annotation.
    private static ExclusionStrategy exclusionStrategy = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return fieldAttributes.getAnnotation(DoNotExpose.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) { return false; }
    };

    /** Gson custom object with our custom exclusion strategy, serializers and deserialiezers. */
    public static Gson gson = new GsonBuilder()
            .setExclusionStrategies(exclusionStrategy)
            .registerTypeAdapter(Instant.class,   new InstantSerializer())
            .registerTypeAdapter(Instant.class,   new InstantDeserializer())
            .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
            .registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
            .registerTypeAdapter(LocalTime.class, new LocalTimeSerializer())
            .registerTypeAdapter(LocalTime.class, new LocalTimeDeserializer())
            .create();

    /** A marker annotation, when set, ignores the attribute for all Gson serialization and deserialization. */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DoNotExpose {}

    private static class InstantSerializer implements JsonSerializer<Instant> {
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? null : new JsonPrimitive(src.toString());
        }
    }

    private static class InstantDeserializer implements JsonDeserializer<Instant> {
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null ? null : Instant.parse(json.getAsJsonPrimitive().getAsString());
        }
    }

    private static class LocalDateSerializer implements JsonSerializer<LocalDate> {
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? null : new JsonPrimitive(src.toString());
        }
    }

    private static class LocalDateDeserializer implements JsonDeserializer<LocalDate> {
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null ? null : LocalDate.parse(json.getAsJsonPrimitive().getAsString());
        }
    }

    private static class LocalTimeSerializer implements JsonSerializer<LocalTime> {
        public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? null : new JsonPrimitive(src.toString());
        }
    }

    private static class LocalTimeDeserializer implements JsonDeserializer<LocalTime> {
        public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null ? null : LocalTime.parse(json.getAsJsonPrimitive().getAsString());
        }
    }
}
