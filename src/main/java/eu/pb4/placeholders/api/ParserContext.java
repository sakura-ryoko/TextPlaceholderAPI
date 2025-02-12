package eu.pb4.placeholders.api;

import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class ParserContext {
    private final Map<Key<?>, Object> map = new HashMap<>();

    private ParserContext() {}

    public static ParserContext of() {
        return new ParserContext();
    }

    public static <T> ParserContext of(Key<T> key, T object) {
        return new ParserContext().with(key, object);
    }

    public <T> ParserContext with(Key<T> key, T object) {
        this.map.put(key, object);
        return this;
    }

    @Nullable
    public <T> T get(Key<T> key) {
        //noinspection unchecked
        return (T) this.map.get(key);
    }

    public <T> T getOrElse(Key<T> key, T defaultValue) {
        //noinspection unchecked
        return (T) this.map.getOrDefault(key, defaultValue);
    }

    public <T> T getOrElse(Key<T> key, Supplier<T> defaultValue) {
        //noinspection unchecked
        var x =  (T) this.map.get(key);
        if (x == null) {
            return defaultValue.get();
        }
        return x;
    }

    public <T> T getOrThrow(Key<T> key) {
        //noinspection unchecked
        return Objects.requireNonNull((T) this.map.get(key));
    };

    public boolean contains(Key<?> key) {
        return this.map.containsKey(key);
    }




    public record Key<T>(String key, @Nullable Class<T> type) {
        public static final Key<Boolean> COMPACT_TEXT = new Key<>("compact_text", Boolean.class);
        public static final Key<RegistryWrapper.WrapperLookup> WRAPPER_LOOKUP = new Key<>("wrapper_lookup", RegistryWrapper.WrapperLookup.class);
        public static final Key<Integer> DEFAULT_TEXT_COLOR = new Key<>("default_text_color", Integer.class);
        public static final Key<Text> TEXT_CONTENT = new Key<>("text_content", Text.class);

        public static <T> Key<T> of(String key, T type) {
            //noinspection unchecked
            return new Key<T>(key, (Class<T>) type.getClass());
        }


        public static <T> Key<T> of(String key) {
            //noinspection unchecked
            return new Key<T>(key, null);
        }
    };
}
