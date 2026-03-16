package gg.magic.academy.api;

import net.kyori.adventure.text.format.TextColor;

public enum Rarity {
    COMMON(TextColor.color(0xAAAAAA)),
    RARE(TextColor.color(0x5555FF)),
    EPIC(TextColor.color(0xAA00AA)),
    LEGENDARY(TextColor.color(0xFFAA00));

    private final TextColor color;

    Rarity(TextColor color) {
        this.color = color;
    }

    public TextColor color() {
        return color;
    }
}
