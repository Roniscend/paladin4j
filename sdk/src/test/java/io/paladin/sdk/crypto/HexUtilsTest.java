package io.paladin.sdk.crypto;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class HexUtilsTest {

    @Test
    @DisplayName("toHex encodes bytes to lowercase hex")
    void testToHex() {
        assertThat(HexUtils.toHex(new byte[]{0x00})).isEqualTo("00");
        assertThat(HexUtils.toHex(new byte[]{(byte) 0xFF})).isEqualTo("ff");
        assertThat(HexUtils.toHex(new byte[]{0x01, 0x23, 0x45})).isEqualTo("012345");
        assertThat(HexUtils.toHex(new byte[]{})).isEqualTo("");
    }

    @Test
    @DisplayName("fromHex decodes hex string to bytes")
    void testFromHex() {
        assertThat(HexUtils.fromHex("00")).isEqualTo(new byte[]{0x00});
        assertThat(HexUtils.fromHex("ff")).isEqualTo(new byte[]{(byte) 0xFF});
        assertThat(HexUtils.fromHex("FF")).isEqualTo(new byte[]{(byte) 0xFF});
        assertThat(HexUtils.fromHex("012345")).isEqualTo(new byte[]{0x01, 0x23, 0x45});
    }

    @Test
    @DisplayName("fromHex strips 0x prefix")
    void testFromHexWithPrefix() {
        assertThat(HexUtils.fromHex("0xFF")).isEqualTo(new byte[]{(byte) 0xFF});
        assertThat(HexUtils.fromHex("0Xff")).isEqualTo(new byte[]{(byte) 0xFF});
    }

    @Test
    @DisplayName("fromHex rejects odd-length strings")
    void testFromHexOddLength() {
        assertThatThrownBy(() -> HexUtils.fromHex("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("even length");
    }

    @Test
    @DisplayName("fromHex rejects invalid hex characters")
    void testFromHexInvalidChars() {
        assertThatThrownBy(() -> HexUtils.fromHex("zz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid hex character");
    }

    @Test
    @DisplayName("roundtrip: toHex(fromHex(x)) == x")
    void testRoundtrip() {
        String hex = "deadbeef01234567890abcdef0";
        assertThat(HexUtils.toHex(HexUtils.fromHex(hex))).isEqualTo(hex);
    }
}
