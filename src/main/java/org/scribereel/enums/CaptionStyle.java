package org.scribereel.enums;

import lombok.Getter;

/**
 * Each style maps to its own .ass Style line (font, size, color, outline)
 * plus the word-chunking behavior shown for it in the UI.
 * ASS colors are &HAABBGGRR - alpha + BGR, reversed from normal hex RGB.
 */
public enum CaptionStyle {

    PUNCH("Arial", 76, "&H00FFFFFF", "&H00000000", 3, 1),
    CLASSIC("Arial", 58, "&H00FFFFFF", "&H00000000", 3, 3),
    NEON("Arial Black", 70, "&H00B7E76E", "&H00000000", 2, 1),
    BOLD("Arial Black", 78, "&H004DE1FF", "&H00001A1A", 4, 1),
    MINIMAL("Arial", 46, "&H00D3CDC9", "&H00000000", 1, 3);

    private final String fontName;
    private final int fontSize;
    private final String primaryColor;
    private final String outlineColor;
    private final int outlineWidth;
    @Getter
    private final int wordsPerChunk;

    CaptionStyle(String fontName, int fontSize, String primaryColor,
                 String outlineColor, int outlineWidth, int wordsPerChunk) {
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.primaryColor = primaryColor;
        this.outlineColor = outlineColor;
        this.outlineWidth = outlineWidth;
        this.wordsPerChunk = wordsPerChunk;
    }

    public String toAssStyleLine() {
        return "Style: Default," + fontName + "," + fontSize + "," + primaryColor + ","
                + outlineColor + ",1,1," + outlineWidth + ",2,120";
    }

}