package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.impl.GeneralUtils;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

import java.util.Arrays;

public final class ColorBasedShadowNode extends ParentNode {
    private final float scale;

    public ColorBasedShadowNode(TextNode[] children) {
        this(children, 0.25f);
    }

    public ColorBasedShadowNode(TextNode[] children, float scale) {
        super(children);
        this.scale = scale;
    }

    @Override
    protected Text applyFormatting(MutableText out, ParserContext context) {
        var defaultColor = ColorHelper.scaleRgb(context.getOrElse(ParserContext.Key.DEFAULT_TEXT_COLOR, 0xFFFFFF), this.scale) | 0xFF000000;

        return GeneralUtils.cloneTransformText(out, text -> {
                var color = text.getStyle().getColor();
                return text.setStyle(text.getStyle().withShadowColor(color != null ? ColorHelper.scaleRgb(color.getRgb(), this.scale) | 0xFF000000 : defaultColor));
            }, text -> text == out || text.getStyle().getShadowColor() == null && text.getStyle().getColor() != null);
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children) {
        return new ColorBasedShadowNode(children, this.scale);
    }

    @Override
    public String toString() {
        return "ColorBasedShadowNode{" +
                "scale=" + scale +
                '}';
    }
}
