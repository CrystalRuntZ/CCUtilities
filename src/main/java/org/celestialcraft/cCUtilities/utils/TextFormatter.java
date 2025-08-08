package org.celestialcraft.cCUtilities.utils;

import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFormatter {

    public static String colorize(String input) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(input);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder legacy = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                legacy.append("§").append(c);
            }
            matcher.appendReplacement(buffer, legacy.toString());
        }

        matcher.appendTail(buffer);
        return buffer.toString().replace("&", "§");
    }

    // Replaced placeholder support with basic %player% replacement
    public static String withPlaceholders(String input, Player player) {
        if (player != null) {
            return input.replace("%player%", player.getName());
        }
        return input;
    }

    public static String applyFormatting(String input) {
        String result = input;

        // Gradient
        Pattern gradientPattern = Pattern.compile("<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>");
        Matcher gradientMatcher = gradientPattern.matcher(result);
        while (gradientMatcher.find()) {
            String startHex = gradientMatcher.group(1);
            String endHex = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);
            String gradient = applyGradientToText(text, startHex, endHex);
            result = result.replace(gradientMatcher.group(0), gradient);
        }

        // Rainbow
        Pattern rainbowPattern = Pattern.compile("<rainbow>(.*?)</rainbow>");
        Matcher rainbowMatcher = rainbowPattern.matcher(result);
        while (rainbowMatcher.find()) {
            String rainbow = applyRainbow(rainbowMatcher.group(1));
            result = result.replace(rainbowMatcher.group(0), rainbow);
        }

        result = result.replace("<bold>", "§l").replace("</bold>", "")
                .replace("<italic>", "§o").replace("</italic>", "")
                .replace("<underline>", "§n").replace("</underline>", "")
                .replace("<strikethrough>", "§m").replace("</strikethrough>", "")
                .replace("<obfuscated>", "§k").replace("</obfuscated>", "");

        return result;
    }

    private static String applyGradientToText(String text, String startHex, String endHex) {
        int[] startRGB = hexToRGB(startHex);
        int[] endRGB = hexToRGB(endHex);
        int length = Math.max(1, text.length());

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            double ratio = i / (double) (length - 1);
            int r = interpolate(startRGB[0], endRGB[0], ratio);
            int g = interpolate(startRGB[1], endRGB[1], ratio);
            int b = interpolate(startRGB[2], endRGB[2], ratio);
            result.append(rgbToMCColor(r, g, b)).append(text.charAt(i));
        }

        return result.toString();
    }

    private static String applyRainbow(String text) {
        int[] rainbowColors = new int[]{
                0xFF0000, 0xFF7F00, 0xFFFF00,
                0x00FF00, 0x0000FF, 0x4B0082, 0x9400D3
        };

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int color = rainbowColors[i % rainbowColors.length];
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            result.append(rgbToMCColor(r, g, b)).append(text.charAt(i));
        }

        return result.toString();
    }

    private static int[] hexToRGB(String hex) {
        return new int[]{
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    private static int interpolate(int start, int end, double ratio) {
        return (int) (start + (end - start) * ratio);
    }

    private static String rgbToMCColor(int r, int g, int b) {
        return String.format("§x§%s§%s§%s§%s§%s§%s",
                toHexChar(r >> 4), toHexChar(r & 0xF),
                toHexChar(g >> 4), toHexChar(g & 0xF),
                toHexChar(b >> 4), toHexChar(b & 0xF)
        );
    }

    private static char toHexChar(int val) {
        return "0123456789abcdef".charAt(val & 0xF);
    }
}
