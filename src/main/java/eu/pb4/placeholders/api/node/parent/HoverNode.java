package eu.pb4.placeholders.api.node.parent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.DynamicOps;
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

public final class HoverNode<T, H> extends SimpleStylingNode {
    public static final Logger LOGGER = LogManager.getLogger("HOVER_NODE");
    private final Action<T, H> action;
    private final T value;

    public HoverNode(TextNode[] children, Action<T, H> action, T value) {
        super(children);
        this.action = action;
        this.value = value;
    }

    public Action<T, H> action() {
        return this.action;
    }

    public T value() {
        return this.value;
    }

    private void dumpStyle(Style style)
    {
        LOGGER.error("dumpStyle:[{}]: [{}]", this.action.toString(), style.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Style style(ParserContext context) {
        Style test;

        if (this.action == Action.TEXT) {
            test = Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(((TextNode) this.value).toText(context, true)));
        } else if (this.action == Action.ENTITY) {
            test = Style.EMPTY.withHoverEvent(new HoverEvent.ShowEntity(((EntityNodeContent) this.value).toVanilla(context)));
        } else if (this.action == Action.ITEM_STACK) {
            RegistryWrapper.WrapperLookup wrapper;
            if (context.contains(ParserContext.Key.WRAPPER_LOOKUP)) {
                LOGGER.error("show_item: hasWrapper");
                wrapper = context.getOrThrow(ParserContext.Key.WRAPPER_LOOKUP);
            } else if (context.contains(PlaceholderContext.KEY)) {
                LOGGER.error("show_item: hasKey");
                wrapper = context.getOrThrow(PlaceholderContext.KEY).server().getRegistryManager();
            } else {
                LOGGER.error("show_item: has none -> empty");
                test = Style.EMPTY;
                this.dumpStyle(test);
                return test;
            }

            test = Style.EMPTY.withHoverEvent(new HoverEvent.ShowItem(((LazyItemStackNodeContent<T>) this.value).toVanilla(wrapper)));
        } else if (this.action == Action.VANILLA_ITEM) {
            test = Style.EMPTY.withHoverEvent(new HoverEvent.ShowItem(((HoverEvent.ShowItem) this.value).item()));
        } else if (this.action == Action.VANILLA_ENTITY) {
            test = Style.EMPTY.withHoverEvent(new HoverEvent.ShowEntity(((HoverEvent.ShowEntity) this.value).entity()));
        } else {
            test = Style.EMPTY;
        }

        this.dumpStyle(test);
        return test;
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children) {
        return new HoverNode<>(children, this.action, this.value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ParentTextNode copyWith(TextNode[] children, NodeParser parser) {
        if (this.value == null) {
            return this.copyWith(children);
        } else if (this.action == Action.TEXT) {
            return new HoverNode<>(children,
                                   Action.TEXT,
                                   parser.parseNode((TextNode) this.value)
            );
        } else if (this.action == Action.ENTITY &&
                  ((EntityNodeContent) this.value).name != null) {
            var val = ((EntityNodeContent) this.value);
            return new HoverNode<>(children,
                                   Action.ENTITY,
                                   new EntityNodeContent(val.entityType, val.uuid, parser.parseNode(val.name))
            );
        } else if (this.action == Action.ITEM_STACK &&
                  ((LazyItemStackNodeContent<T>) this.value).identifier != null) {
            var val = ((LazyItemStackNodeContent<T>) this.value);
            return new HoverNode<>(children,
                                   Action.ITEM_STACK,
                                   new LazyItemStackNodeContent<>(val.identifier, val.count, val.ops, val.componentMap)
            );
        } else if (this.action == Action.VANILLA_ITEM &&
                  ((HoverEvent.ShowItem) this.value).item() != null) {
            var val = ((HoverEvent.ShowItem) this.value).item();
            return new HoverNode<>(children,
                                   Action.VANILLA_ITEM,
                                   new HoverEvent.ShowItem(val)
            );
        } else if (this.action == Action.VANILLA_ENTITY &&
                  ((HoverEvent.ShowEntity) this.value).entity() != null) {
            var val = ((HoverEvent.ShowEntity) this.value).entity();
            return new HoverNode<>(children,
                                   Action.VANILLA_ENTITY,
                                   new HoverEvent.ShowEntity(val)
            );
        } return this.copyWith(children);
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
        return (this.action == Action.TEXT && ((TextNode) this.value).isDynamic()) || (this.action == Action.ENTITY && ((EntityNodeContent) this.value).name.isDynamic()) || this.action == Action.ITEM_STACK;
    }

    public record Action<T, H>(HoverEvent.Action vanillaType) {
        public static final Action<TextNode, HoverEvent.ShowText> TEXT = new Action<>(HoverEvent.Action.SHOW_TEXT);
        public static final Action<EntityNodeContent, HoverEvent.ShowEntity> ENTITY = new Action<>(HoverEvent.Action.SHOW_ENTITY);
        public static final Action<LazyItemStackNodeContent<?>, HoverEvent.ShowItem> ITEM_STACK = new Action<>(HoverEvent.Action.SHOW_ITEM);

        // Vanilla (STF V1)
        public static final Action<HoverEvent.ShowItem, HoverEvent.ShowItem> VANILLA_ITEM = new Action<>(HoverEvent.Action.SHOW_ITEM);
        public static final Action<HoverEvent.ShowEntity, HoverEvent.ShowEntity> VANILLA_ENTITY = new Action<>(HoverEvent.Action.SHOW_ENTITY);

        @Override
        public String toString()
        {
            return "HoverNode$Action{vanillaType={"+vanillaType.name()+"}}";
        }
    }

    public record EntityNodeContent(EntityType<?>entityType, UUID uuid, @Nullable TextNode name) implements HoverEvent {
        // todo CODEC notes
        /*
        public static final MapCodec<EntityContent> CONTENT_CODEC =
                RecordCodecBuilder.mapCodec((instance) ->
                                                    instance.group(
                                                            Registries.ENTITY_TYPE.getCodec().fieldOf("id")
                                                                                  .forGetter((content) -> content.entityType),
                                                            Uuids.STRICT_CODEC.fieldOf("uuid")
                                                                              .forGetter((content) -> content.uuid),
                                                            TextCodecs.CODEC.optionalFieldOf("name")
                                                                            .forGetter((content) -> content.name)
                                                    ).apply(instance, EntityContent::new)
                );
        public static final MapCodec<ShowEntity> ENTITY_CODEC =
                RecordCodecBuilder.mapCodec((instance) ->
                                                    instance.group(
                                                            CONTENT_CODEC
                                                                    .forGetter(ShowEntity::entity)
                                                    ).apply(instance, ShowEntity::new)
                );
         */
        public EntityContent toVanilla(ParserContext context) {
            LOGGER.error("EntityContent:TextNode: [{}]", this.name!= null ? this.name.toText() : "<empty>");
            EntityContent content = new HoverEvent.EntityContent(this.entityType, this.uuid, Optional.ofNullable(this.name != null ? this.name.toText(context, true) : null));
            List<Text> list = content.asTooltip();
            LOGGER.error("EntityContent: type: [{}] // tooltip list: [{}]", content.entityType.getName().getLiteralString(), list.toString());
            LOGGER.error("EntityContent: getTranslatedName: [{}]", Text.translatableWithFallback(content.entityType.getTranslationKey(), "id="+ EntityType.getId(content.entityType).toString()));
            return content;
        }

        @Override
        public Action getAction()
        {
            return Action.SHOW_ENTITY;
        }

        @Override
        public String toString()
        {
            return "HoverNode$EntityNodeContent{id="+
                    EntityType.getId(entityType).toString()
                    + ",uuid=["+
                    uuid.toString()
                    + "],name={" +
                    (name != null ? name.toText().getLiteralString() : "<NULL>")
                    + "}}";
        }
    }

    public record LazyItemStackNodeContent<T>(Identifier identifier, int count, DynamicOps<T> ops, T componentMap) implements HoverEvent {
        // todo CODEC notes
        /*
        public static final MapCodec<ItemStack> MAP_CODEC =
                MapCodec.recursive("ItemStack", (codec) ->
                                           RecordCodecBuilder.mapCodec((instance) ->
                                                                               instance.group(
                                                                                       Item.ENTRY_CODEC.fieldOf("id")
                                                                                                       .forGetter(ItemStack::getRegistryEntry),
                                                                                       Codecs.rangedInt(1, 99).fieldOf("count").orElse(1)
                                                                                                        .forGetter(ItemStack::getCount),
                                                                                       ComponentChanges.CODEC.optionalFieldOf("components", ComponentChanges.EMPTY)
                                                                                                        .forGetter(ItemStack::getComponentChanges)
                                                                               ).apply(instance, ItemStack::new))
                );
        public static final MapCodec<ShowItem> ITEM_CODEC = MAP_CODEC.xmap(ShowItem::new, ShowItem::item);
         */

        public ItemStack toVanilla(RegistryWrapper.WrapperLookup lookup) {
            LOGGER.error("Dump Lazy Stack: id: [{}], count: [{}], comps: [{}]", identifier.toString(), count, componentMap != null ? componentMap.toString() : "<null>");
            var stack = new ItemStack(lookup.getOrThrow(RegistryKeys.ITEM).getOrThrow(RegistryKey.of(RegistryKeys.ITEM, identifier)));
            stack.setCount(count);
            if (componentMap != null) {
                stack.applyChanges(ComponentChanges.CODEC.decode(lookup.getOps(ops), componentMap).getOrThrow().getFirst());
            }
            LOGGER.error("Dump Lazy Stack: [{}]", stack.toNbt(lookup));
            return stack;
        }

        @Override
        public Action getAction()
        {
            return Action.SHOW_ITEM;
        }

        @Override
        public String toString()
        {
            return "HoverNode$LazyItemStackNodeContent{id="
                    +identifier.toString()
                    + ",count="+
                    count
                    + ",ops=["+
                    ops.toString()
                    + "],components={"+
                    componentMap.toString()
                    + "}}";
        }
    }
}
