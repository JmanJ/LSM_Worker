package Utilities;


import java.awt.*;

public class ColorUtilities {

    public static byte[] getColorPalette(Color color, int PaletteSize) {
        byte[] cmap = new byte[PaletteSize * 3];
        byte r, g, b;
        for (int i = PaletteSize - 1; i > 0; i--) {
            r = (byte) Math.round((color.getRed() / (double) (PaletteSize - 1)) * i);
            g = (byte) Math.round((color.getGreen() / (double) (PaletteSize - 1)) * i);
            b = (byte) Math.round((color.getBlue() / (double) (PaletteSize - 1)) * i);
            cmap[i*3] = r;
            cmap[i*3 + 1] = g;
            cmap[i*3 + 2] = b;
        }
        return cmap;
    }

    public static int getNormalizedColorValue(int value, double maxValue) {
        if (maxValue == 0)
            return 0;
        int colorValue = (int) ((value / maxValue) * 255);
        if (colorValue > 255)
            colorValue = 255;
        return colorValue;
    }
}

