package eu.pb4.placeholders.api.node.parent;

import com.mojang.serialization.DynamicOps;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.minecraft.component.ComponentChanges;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;

public final class HoverNode<T, H> extends SimpleStylingNode {
    private final Action<T, H> action;
    private final T value;

    public HoverNode(TextNode[] children, Action<T, H> action, T value) {
        super(children);
        this.action = action;
        this.value = value;
    }

    @Override
    protected Style style(ParserContext context) {
        if (this.action == Action.TEXT) {
            return Style.EMPTY.withHoverEvent(new HoverEvent((HoverEvent.Action<Object>) this.action.vanillaType(), ((TextNode) this.value).toText(context, true)));
        } else if (this.action == Action.ENTITY) {
            return Style.EMPTY.withHoverEvent(new HoverEvent((HoverEvent.Action<Object>) this.action.vanillaType(), ((EntityNodeContent) this.value).toVanilla(context)));
        } else if (this.action == Action.LAZY_ITEM_STACK) {
            RegistryWrapper.WrapperLookup wrapper;
            if (context.contains(ParserContext.Key.WRAPPER_LOOKUP)) {
                wrapper = context.getOrThrow(ParserContext.Key.WRAPPER_LOOKUP);
            } else if (context.contains(PlaceholderContext.KEY)) {
                wrapper = context.getOrThrow(PlaceholderContext.KEY).server().getRegistryManager();
            } else {
                return Style.EMPTY;
            }

            return Style.EMPTY.withHoverEvent(new HoverEvent.ShowItem(((LazyItemStackNodeContent) this.value).toVanilla(wrapper)));
        } else {
            return Style.EMPTY.withHoverEvent(new HoverEvent((HoverEvent.Action<Object>) this.action.vanillaType(), this.value));
        }

    }

    @Override
    public ParentTextNode copyWith(TextNode[] children) {
        return new HoverNode(children, this.action, this.value);
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children, NodeParser parser) {
        if (this.action == Action.TEXT) {
            return new HoverNode(children, Action.TEXT, parser.parseNode((TextNode) this.value));
        } else if (this.action == Action.ENTITY && ((EntityNodeContent) this.value).name != null) {
            var val = ((EntityNodeContent) this.value);
            return new HoverNode(children, Action.ENTITY, new EntityNodeContent(val.entityType, val.uuid, parser.parseNode(val.name)));
        }
        return this.copyWith(children);
    }

    public Action<T, H> action() {
        return this.action;
    }

    public T value() {
        return this.value;
    }

    @Override
    public String toString() {
        return "HoverNode{" +
                "value=" + value +
                ", children=" + Arrays.toString(children) +
                '}';
    }

    @Override
    public boolean isDynamicNoChildren() {
        return (this.action == Action.TEXT && ((TextNode) this.value).isDynamic()) || (this.action == Action.ENTITY && ((EntityNodeContent) this.value).name.isDynamic()) || this.action == Action.LAZY_ITEM_STACK;
    }

    public record Action<T, H>(HoverEvent.Action<H> vanillaType) {
        public static final Action<EntityNodeContent, HoverEvent.EntityContent> ENTITY = new Action<>(HoverEvent.Action.SHOW_ENTITY);
        public static final Action<TextNode, Text> TEXT = new Action<>(HoverEvent.Action.SHOW_TEXT);

        public static final Action<HoverEvent.ItemStackContent, HoverEvent.ItemStackContent> ITEM_STACK = new Action<>(HoverEvent.Action.SHOW_ITEM);
        public static final Action<LazyItemStackNodeContent, HoverEvent.ItemStackContent> LAZY_ITEM_STACK = new Action<>(HoverEvent.Action.SHOW_ITEM);
    }

    public record EntityNodeContent(EntityType<?>entityType, UUID uuid, @Nullable TextNode name) {
        public HoverEvent.EntityContent toVanilla(ParserContext context) {
            return new HoverEvent.EntityContent(this.entityType, this.uuid, this.name != null ? this.name.toText(context, true) : null);
        }
    }

    public record LazyItemStackNodeContent<T>(Identifier identifier, int count, DynamicOps<T> ops, T componentMap) {
        public HoverEvent.ItemStackContent toVanilla(RegistryWrapper.WrapperLookup lookup) {
            var stack = new ItemStack(lookup.getOrThrow(RegistryKeys.ITEM).getOrThrow(RegistryKey.of(RegistryKeys.ITEM, identifier)));
            stack.setCount(count);
            stack.applyChanges(ComponentChanges.CODEC.decode(lookup.getOps(ops), componentMap).getOrThrow().getFirst());
            return new HoverEvent.ItemStackContent(stack);
        }
    }
}
