package org.nrnr.neverdies.impl.font;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class FontRenderers {
    public static FontAdapter Verdana;

    public static @NotNull RendererFontAdapter createDefault(float size, String name) throws IOException, FontFormatException {
        return new RendererFontAdapter(Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(FontRenderers.class.getClassLoader().getResourceAsStream("assets/neverdies/font/" + name + ".ttf"))).deriveFont(Font.PLAIN, size / 2f), size / 2f);
    }

    public static RendererFontAdapter createVerdana(float size, int style) {
        return new RendererFontAdapter(new Font("Verdana", style, (int) (size / 2f)), size / 2f);
    }
}
