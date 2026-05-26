package xyz.malefic.kutint

import org.jetbrains.compose.web.css.CSSColorValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

sealed class ColorType

object RGBColorType : ColorType()

object HSLColorType : ColorType()

/**
 * Base sealed class for all Kutint colors.
 * Extends CSSColorValue for seamless integration with Kobweb/Compose Web.
 */
sealed class KutintColor<T : ColorType> : CSSColorValue {
    /**
     * Alpha channel value in the range `0f..1f`, where `0f` is fully transparent and `1f` is fully opaque.
     */
    abstract val alpha: Float

    /**
     * Lighten the color by mixing with white
     * @param amount 0-1, where 0 is no change and 1 is pure white
     */
    fun tint(amount: Float = 0.2f): RGB =
        withRGB { r, g, b ->
            val clipped = amount.coerceIn(0f, 1f)
            RGB(
                r = (r + (255 - r) * clipped).roundToInt(),
                g = (g + (255 - g) * clipped).roundToInt(),
                b = (b + (255 - b) * clipped).roundToInt(),
                alpha = alpha,
            )
        }

    /**
     * Darken the color by mixing with black
     * @param amount 0-1, where 0 is no change and 1 is pure black
     */
    fun dim(amount: Float = 0.2f): RGB =
        withRGB { r, g, b ->
            val clipped = amount.coerceIn(0f, 1f)
            RGB(
                r = (r * (1 - clipped)).roundToInt(),
                g = (g * (1 - clipped)).roundToInt(),
                b = (b * (1 - clipped)).roundToInt(),
                alpha = alpha,
            )
        }

    /**
     * Invert RGB values
     */
    fun invert(): RGB =
        withRGB { r, g, b ->
            RGB(
                r = 255 - r,
                g = 255 - g,
                b = 255 - b,
                alpha = alpha,
            )
        }

    /**
     * Convert to grayscale using luminosity formula
     */
    fun grayscale(): RGB =
        withRGB { r, g, b ->
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt()
            RGB(gray, gray, gray, alpha)
        }

    /**
     * Increase color saturation
     * @param amount 0-1, where 0 is no change
     */
    fun saturate(amount: Float = 0.2f): HSL =
        withHSL { h, s, l ->
            val newSaturation = (s + amount * 100f).coerceIn(0f, 100f)
            HSL(h = h, s = newSaturation, l = l, alpha = alpha)
        }

    /**
     * Decrease color saturation
     * @param amount 0-1, where 0 is no change
     */
    fun desaturate(amount: Float = 0.2f): HSL = saturate(-amount)

    /**
     * Rotate the hue
     * @param degrees -360 to 360
     */
    fun hueRotate(degrees: Int = 30): HSL =
        withHSL { h, s, l ->
            val newHue = ((h + degrees) % 360 + 360) % 360 // Normalize to 0-360
            HSL(h = newHue, s = s, l = l, alpha = alpha)
        }

    /**
     * Get the complementary color (hue rotated 180 degrees)
     */
    fun complement(): HSL = hueRotate(180)

    /**
     * Blend two colors together
     * @param other The color to blend with
     * @param amount 0-1, where 0 is this color and 1 is the other color
     */
    fun blend(
        other: KutintColor<*>,
        amount: Float = 0.5f,
    ): RGB =
        withRGB { r1, g1, b1 ->
            other.withRGB { r2, g2, b2 ->
                val clipped = amount.coerceIn(0f, 1f)
                RGB(
                    r = (r1 + (r2 - r1) * clipped).roundToInt(),
                    g = (g1 + (g2 - g1) * clipped).roundToInt(),
                    b = (b1 + (b2 - b1) * clipped).roundToInt(),
                    alpha = alpha + (other.alpha - alpha) * clipped,
                )
            }
        }

    /**
     * Helper function to convert to RGB and apply a transformation function
     */
    fun <T> withRGB(func: (r: Int, g: Int, b: Int) -> T): T = this.toRGB().let { func(it.r, it.g, it.b) }

    /**
     * Helper function to convert to HSL and apply a transformation function
     */
    fun <T> withHSL(func: (h: Float, s: Float, l: Float) -> T): T = this.toHSL().let { func(it.h, it.s, it.l) }

    fun withAlpha(newAlpha: Float): KutintColor<*> =
        when (this) {
            is RGB -> RGB(r, g, b, newAlpha.coerceIn(0f, 1f))
            is HSL -> HSL(h, s, l, newAlpha.coerceIn(0f, 1f))
        }

    /**
     * Converts a given [KutintColor] into an [HSL] color space
     */
    fun toHSL(): HSL {
        when (this) {
            is HSL -> {
                return this
            }

            is RGB -> {
                val rNorm = r / 255f
                val gNorm = g / 255f
                val bNorm = b / 255f

                val maxChannel = max(rNorm, max(gNorm, bNorm))
                val minChannel = min(rNorm, min(gNorm, bNorm))
                val delta = maxChannel - minChannel
                val lightness = (maxChannel + minChannel) / 2

                if (delta == 0f) {
                    return HSL(0f, 0f, lightness * 100f, alpha)
                }

                val saturation =
                    if (lightness < 0.5) {
                        delta / (maxChannel + minChannel)
                    } else {
                        delta / (2 - maxChannel - minChannel)
                    }

                val hue =
                    when (maxChannel) {
                        rNorm -> ((gNorm - bNorm) / delta + if (gNorm < bNorm) 6f else 0f) * 60f
                        gNorm -> ((bNorm - rNorm) / delta + 2f) * 60f
                        else -> ((rNorm - gNorm) / delta + 4f) * 60f
                    }

                return HSL(
                    h = hue % 360f,
                    s = saturation * 100f,
                    l = lightness * 100f,
                    alpha = alpha,
                )
            }
        }
    }

    /**
     * Converts a given [KutintColor] into an [RGB] color space
     */
    fun toRGB(): RGB {
        when (this) {
            is RGB -> {
                return this
            }

            is HSL -> {
                val sNorm = s / 100f
                val lNorm = l / 100f
                val c = (1f - abs(2f * lNorm - 1f)) * sNorm
                val x = c * (1f - abs((h / 60f) % 2f - 1f))
                val m = lNorm - c / 2f

                val (rPrime, gPrime, bPrime) =
                    when {
                        h < 60f -> Triple(c, x, 0f)
                        h < 120f -> Triple(x, c, 0f)
                        h < 180f -> Triple(0f, c, x)
                        h < 240f -> Triple(0f, x, c)
                        h < 300f -> Triple(x, 0f, c)
                        else -> Triple(c, 0f, x)
                    }

                return RGB(
                    r = ((rPrime + m) * 255).roundToInt(),
                    g = ((gPrime + m) * 255).roundToInt(),
                    b = ((bPrime + m) * 255).roundToInt(),
                    alpha = alpha,
                )
            }
        }
    }
}

/**
 * Represents an RGB color.
 *
 * @property r Red channel value in the range `0..255`.
 * @property g Green channel value in the range `0..255`.
 * @property b Blue channel value in the range `0..255`.
 * @property alpha Opacity value in the range `0f..1f` (default `1f`).
 */
class RGB(
    val r: Int,
    val g: Int,
    val b: Int,
    override val alpha: Float = 1f,
) : KutintColor<RGBColorType>() {
    init {
        require(r in 0..255) { "Red channel (r) must be between 0 and 255, got $r" }
        require(g in 0..255) { "Green channel (g) must be between 0 and 255, got $g" }
        require(b in 0..255) { "Blue channel (b) must be between 0 and 255, got $b" }
        require(alpha in 0f..1f) { "Alpha channel (alpha) must be between 0f and 1f, got $alpha" }
    }

    /**
     * Returns a CSS-compatible `rgba(r, g, b, a)` string representation.
     */
    override fun toString(): String = "rgba($r, $g, $b, $alpha)"
}

/**
 * Represents an HSL color.
 *
 * @property h Hue in degrees (`0f..360f`).
 * @property s Saturation percentage (`0f..100f`).
 * @property l Lightness percentage (`0f..100f`).
 * @property alpha Opacity value in the range `0f..1f` (default `1f`).
 */
class HSL(
    val h: Float,
    val s: Float,
    val l: Float,
    override val alpha: Float = 1f,
) : KutintColor<HSLColorType>() {
    init {
        require(h in 0f..360f) { "Hue channel (h) must be between 0f and 360f, got $h" }
        require(s in 0f..100f) { "Saturation channel (s) must be between 0f and 100f, got $s" }
        require(l in 0f..100f) { "Lightness channel (l) must be between 0f and 100f, got $l" }
        require(alpha in 0f..1f) { "Alpha channel (alpha) must be between 0f and 1f, got $alpha" }
    }

    /**
     * Returns a CSS-compatible `hsla(h, s%, l%, a)` string representation.
     */
    override fun toString(): String = "hsla($h, $s%, $l%, $alpha)"
}

/**
 * Parse a hex color string (#RRGGBB or #RRGGBBAA) into [RGB]
 */
fun parseHex(hex: String): RGB {
    val cleanHex = hex.removePrefix("#").uppercase()
    require(cleanHex.length == 6 || cleanHex.length == 8) {
        "Hex color must be #RRGGBB or #RRGGBBAA, got $hex"
    }

    val r = cleanHex.substring(0, 2).toInt(16)
    val g = cleanHex.substring(2, 4).toInt(16)
    val b = cleanHex.substring(4, 6).toInt(16)
    val alpha =
        if (cleanHex.length == 8) {
            cleanHex.substring(6, 8).toInt(16) / 255f
        } else {
            1f
        }

    return RGB(r, g, b, alpha)
}

/**
 * Convenience function to create an [RGB] color
 */
fun rgb(
    r: Int,
    g: Int,
    b: Int,
    alpha: Float = 1f,
): RGB = RGB(r, g, b, alpha)

/**
 * Convenience function to create an [HSL] color
 */
fun hsl(
    h: Float,
    s: Float,
    l: Float,
    alpha: Float = 1f,
): HSL = HSL(h, s, l, alpha)
