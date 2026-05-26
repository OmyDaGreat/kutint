package xyz.malefic.kutint

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebTest {
    @Test
    fun `test RGB color creation`() {
        val color = rgba(255, 128, 64)
        assertEquals(255, color.r)
        assertEquals(128, color.g)
        assertEquals(64, color.b)
        assertEquals(1f, color.alpha)
    }

    @Test
    fun `test RGB toString renders correct RGBA`() {
        val color = rgba(255, 128, 64, 0.5f)
        assertEquals("rgba(255, 128, 64, 0.5)", color.toString())
    }

    @Test
    fun `test HSL color creation`() {
        val color = hsla(180f, 50f, 50f)
        assertEquals(180f, color.h)
        assertEquals(50f, color.s)
        assertEquals(50f, color.l)
    }

    @Test
    fun `test RGB to HSL conversion`() {
        val rgb = rgba(255, 0, 0) // Pure red
        val hsl = rgb.toHSL()
        assertEquals(0f, hsl.h)
        assertEquals(100f, hsl.s)
        assertEquals(50f, hsl.l)
    }

    @Test
    fun `test HSL to RGB conversion`() {
        val hsl = hsla(0f, 100f, 50f) // Pure red
        val rgb = hsl.toRGB()
        assertEquals(255, rgb.r)
        assertEquals(0, rgb.g)
        assertEquals(0, rgb.b)
    }

    @Test
    fun `test tint lightens color`() {
        val color = rgba(100, 100, 100)
        val tinted = color.tint(0.5f)
        assertTrue(tinted.r > color.r)
        assertTrue(tinted.g > color.g)
        assertTrue(tinted.b > color.b)
    }

    @Test
    fun `test dim darkens color`() {
        val color = rgba(200, 200, 200)
        val dimmed = color.dim(0.5f)
        assertTrue(dimmed.r < color.r)
        assertTrue(dimmed.g < color.g)
        assertTrue(dimmed.b < color.b)
    }

    @Test
    fun `test withAlpha adjusts opacity`() {
        val color = rgba(255, 128, 64, 1f)
        val transparent = color.withAlpha(0.5f)
        assertEquals(0.5f, transparent.alpha)
    }

    @Test
    fun `test invert inverts RGB values`() {
        val color = rgba(100, 150, 200)
        val inverted = color.invert()
        assertEquals(155, inverted.r)
        assertEquals(105, inverted.g)
        assertEquals(55, inverted.b)
    }

    @Test
    fun `test parseHex parses hex colors`() {
        val color = parseHex("#FF8040")
        assertEquals(255, color.r)
        assertEquals(128, color.g)
        assertEquals(64, color.b)
    }

    @Test
    fun `test hueRotate rotates hue`() {
        val color = hsla(0f, 100f, 50f)
        val rotated = color.hueRotate(120)
        assertEquals(120f, rotated.h)
    }

    @Test
    fun `test complement gets opposite hue`() {
        val color = hsla(0f, 100f, 50f)
        val comp = color.complement()
        assertEquals(180f, comp.h)
    }

    @Test
    fun `test blend mixes colors`() {
        val color1 = rgba(100, 100, 100)
        val color2 = rgba(200, 200, 200)
        val blended = color1.blend(color2, 0.5f)
        assertEquals(150, blended.r)
        assertEquals(150, blended.g)
        assertEquals(150, blended.b)
    }

    @Test
    fun `test grayscale desaturates`() {
        val color = rgba(255, 0, 0)
        val gray = color.grayscale()
        assertEquals(gray.r, gray.g)
        assertEquals(gray.g, gray.b)
    }
}
