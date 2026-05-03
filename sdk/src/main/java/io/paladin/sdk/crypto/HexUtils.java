package io.paladin.sdk.crypto;

/**
 * Utility class for hexadecimal encoding and decoding.
 *
 * <p>Used internally by the crypto package for converting between
 * byte arrays and hex strings.
 */
public final class HexUtils {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private HexUtils() {}

    /**
     * Encodes a byte array to a lowercase hex string (without "0x" prefix).
     *
     * @param bytes the bytes to encode
     * @return the hex string representation
     */
    public static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Decodes a hex string to a byte array.
     *
     * <p>The input may optionally start with "0x" or "0X", which will be stripped.
     *
     * @param hex the hex string to decode
     * @return the decoded byte array
     * @throws IllegalArgumentException if the string length is odd or contains invalid characters
     */
    public static byte[] fromHex(String hex) {
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length: " + hex.length());
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hex character at position " + (i * 2));
            }
            bytes[i] = (byte) ((hi << 4) | lo);
        }
        return bytes;
    }
}
