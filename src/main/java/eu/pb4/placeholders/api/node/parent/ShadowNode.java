package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;

import java.util.Arrays;

public final class ShadowNode extends SimpleStylingNode {
    private final int color;

    public ShadowNode(TextNode[] children, int color) {
        super(children);
        this.color = color;
    }

    @Override
    protected Style style(ParserContext context) {
        return Style.EMPTY.withShadowColor(this.color);
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children) {
        return new ShadowNode(children, this.color);
    }

    @Override
    public String toString() {
        return "ShadowNode{" +
                "color=" + color +
                ", children=" + Arrays.toString(children) +
                '}';
    }
}
