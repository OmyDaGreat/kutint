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
     * Lighten a color by mixing with white (increase brightness).
     *
     * @param amount Tinting intensity in the range `0f..1f`. Default is `0.2f`.
     *   - `0f` = no change
     *   - `0.5f` = 50% towards white
     *   - `1f` = pure white
     *
     * @return A new [RGB] color lightened by the specified amount, preserving alpha.
     *
     * Example: `rgba(100, 100, 100).tint(0.3f)` produces a lighter shade of gray.
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
     * Darken a color by mixing with black (decrease brightness).
     *
     * @param amount Dimming intensity in the range `0f..1f`. Default is `0.2f`.
     *   - `0f` = no change
     *   - `0.5f` = 50% towards black
     *   - `1f` = pure black
     *
     * @return A new [RGB] color darkened by the specified amount, preserving alpha.
     *
     * Example: `rgba(200, 200, 200).dim(0.3f)` produces a darker shade of gray.
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
     * Invert a color to obtain its complementary color (RGB inverse). Note that [complement] should be used for the [HSL] inverse, as it's slightly more accurate in its usage of [Float] for its parameters.
     *
     * Performs a simple RGB inversion: each channel is transformed as `255 - channel`.
     *
     * @return A new [RGB] color with inverted values, preserving alpha.
     *
     * Example: `rgba(255, 0, 0)` (red) inverts to `rgba(0, 255, 255)` (cyan).
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
     * Convert to grayscale using the luminosity formula.
     *
     * Applies the standard human eye luminosity weights (0.299R + 0.587G + 0.114B) to produce
     * a perceptually accurate grayscale representation. Custom weights can be provided for creative effects.
     *
     * @param rWeight Weight for the red channel (default 0.299).
     * @param gWeight Weight for the green channel (default 0.587).
     * @param bWeight Weight for the blue channel (default 0.114).
     *
     * @return A new grayscale [RGB] color where R=G=B, preserving alpha.
     *
     * Example: `rgba(255, 0, 0)` (red) converts to approximately `rgba(76, 76, 76)`.
     */
    fun grayscale(
        rWeight: Double = 0.299,
        gWeight: Double = 0.587,
        bWeight: Double = 0.114,
    ): RGB =
        withRGB { r, g, b ->
            val gray = (rWeight * r + gWeight * g + bWeight * b).roundToInt()
            RGB(gray, gray, gray, alpha)
        }

    /**
     * Increase color saturation (make colors more vivid).
     *
     * Adjusts the saturation component in HSL color space, increasing color intensity.
     *
     * @param amount Saturation boost in the range `0f..1f`. Default is `0.2f`.
     *   - `0f` = no change
     *   - `0.5f` = 50% increase in saturation
     *   - Values clipped to HSL bounds (0-100%).
     *
     * @return A new [HSL] color with increased saturation, preserving alpha.
     *
     * Example: `rgba(200, 100, 100).saturate(0.3f)` produces a more vibrant red.
     */
    fun saturate(amount: Float = 0.2f): HSL =
        withHSL { h, s, l ->
            val newSaturation = (s + amount * 100f).coerceIn(0f, 100f)
            HSL(h = h, s = newSaturation, l = l, alpha = alpha)
        }

    /**
     * Decrease color saturation (make colors more muted/grayscale).
     *
     * Adjusts the saturation component in HSL color space, reducing color intensity.
     *
     * @param amount Desaturation intensity in the range `0f..1f`. Default is `0.2f`.
     *   - `0f` = no change
     *   - `0.5f` = 50% decrease in saturation
     *   - `1f` = complete desaturation (grayscale)
     *
     * @return A new [HSL] color with decreased saturation, preserving alpha.
     *
     * Example: `rgba(255, 0, 0).desaturate(0.5f)` produces a muted, brownish tone.
     */
    fun desaturate(amount: Float = 0.2f): HSL = saturate(-amount)

    /**
     * Rotate the hue around the color wheel.
     *
     * Adjusts the hue component in HSL color space, shifting the color through the spectrum.
     *
     * @param degrees Hue rotation in degrees. Default is `30`.
     *   - Positive values rotate clockwise through the color wheel
     *   - Negative values rotate counter-clockwise
     *   - Range wraps around at 360 degrees
     *
     * @return A new [HSL] color with rotated hue, preserving saturation/lightness and alpha.
     *
     * Example: `rgba(255, 0, 0).hueRotate(120)` produces green; `hueRotate(240)` produces blue.
     */
    fun hueRotate(degrees: Int = 30): HSL =
        withHSL { h, s, l ->
            val newHue = ((h + degrees) % 360 + 360) % 360 // Normalize to 0-360
            HSL(h = newHue, s = s, l = l, alpha = alpha)
        }

    /**
     * Get the complementary color (opposite on the color wheel) using an [HSL] color space.
     *
     * Rotates the hue by 180 degrees to obtain the contrasting color.
     *
     * @return A new [HSL] color with hue rotated 180 degrees, preserving saturation/lightness and alpha.
     *
     * Example: `rgba(255, 0, 0)` (red) has complement `rgba(0, 255, 255)` (cyan).
     *
     * @see hueRotate
     */
    fun complement(): HSL = hueRotate(180)

    /**
     * Blend two colors together using linear interpolation.
     *
     * Smoothly transitions between this color and another using a weighted average.
     *
     * @param other The target color to blend with.
     * @param amount Blend proportion in the range `0f..1f`. Default is `0.5f`.
     *   - `0f` = 100% this color
     *   - `0.5f` = equal mix of both colors
     *   - `1f` = 100% other color
     *
     * @return A new [RGB] color blended between the two, with alpha also interpolated.
     *
     * Example: `rgba(255, 0, 0).blend(rgba(0, 0, 255), 0.5f)` produces purple.
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
     * Helper function to convert to RGB and apply a transformation function.
     *
     * Converts this color to RGB format and passes the individual components to the provided lambda,
     * automatically handling HSL-to-RGB conversion if needed.
     *
     * @param func A lambda receiving (r: Int, g: Int, b: Int) parameters.
     * @return The result of the transformation function.
     *
     * Example: `color.withRGB { r, g, b -> RGB(r+10, g+10, b+10) }`
     */
    fun <T> withRGB(func: (r: Int, g: Int, b: Int) -> T): T = this.toRGB().let { func(it.r, it.g, it.b) }

    /**
     * Helper function to convert to HSL and apply a transformation function.
     *
     * Converts this color to HSL format and passes the individual components to the provided lambda,
     * automatically handling RGB-to-HSL conversion if needed.
     *
     * @param func A lambda receiving (h: Float, s: Float, l: Float) parameters.
     * @return The result of the transformation function.
     *
     * Example: `color.withHSL { h, s, l -> HSL(h+30, s, l) }`
     */
    fun <T> withHSL(func: (h: Float, s: Float, l: Float) -> T): T = this.toHSL().let { func(it.h, it.s, it.l) }

    /**
     * Create a new color with adjusted alpha (opacity).
     *
     * Returns a new instance of the same color type with the provided alpha value.
     * Works with both [RGB] and [HSL] colors.
     *
     * @param newAlpha New opacity value in the range `0f..1f`.
     *   - `0f` = fully transparent
     *   - `0.5f` = semi-transparent
     *   - `1f` = fully opaque (default)
     *
     * @return A new color with updated alpha, clamped to valid range.
     *
     * Example: `rgba(255, 0, 0, 1f).withAlpha(0.5f)` produces a semi-transparent red.
     */
    fun withAlpha(newAlpha: Float): KutintColor<*> =
        when (this) {
            is RGB -> RGB(r, g, b, newAlpha.coerceIn(0f, 1f))
            is HSL -> HSL(h, s, l, newAlpha.coerceIn(0f, 1f))
        }

    /**
     * Converts a given [KutintColor] into an [HSL] color space representation.
     *
     * Handles both RGB-to-HSL conversion (using precise algorithms) and caching for HSL colors.
     * HSL (Hue, Saturation, Lightness) is useful for hue-based manipulations like color rotation.
     *
     * @return An [HSL] representation of this color, preserving alpha.
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
     * Converts a given [KutintColor] into an [RGB] color space representation.
     *
     * Handles both HSL-to-RGB conversion (using precise algorithms) and caching for RGB colors.
     * RGB (Red, Green, Blue) is the standard format for most web applications and CSS.
     *
     * @return An [RGB] representation of this color, preserving alpha.
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
 * Represents an RGB color in the standard Red-Green-Blue color space.
 *
 * RGB colors are commonly used for web development and CSS styling.
 * All manipulation functions are available through the [KutintColor] base class.
 *
 * @property r Red channel value in the range `0..255`.
 * @property g Green channel value in the range `0..255`.
 * @property b Blue channel value in the range `0..255`.
 * @property alpha Opacity value in the range `0f..1f` (default `1f`).
 *
 * @see HSL for hue-based color manipulations
 * @see parseHex for parsing hex color strings
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
 * Represents an HSL color in the Hue-Saturation-Lightness color space.
 *
 * HSL colors are intuitive for color manipulations like rotation, saturation adjustment,
 * and lightness changes. All manipulation functions are available through the [KutintColor] base class.
 *
 * @property h Hue angle in degrees (`0f..360f`). Represents position on the color wheel.
 * @property s Saturation percentage (`0f..100f`). Represents color intensity (0% = gray, 100% = vivid).
 * @property l Lightness percentage (`0f..100f`). Represents brightness (0% = black, 100% = white).
 * @property alpha Opacity value in the range `0f..1f` (default `1f`).
 *
 * @see RGB for standard web color representation
 * @see hsla for convenient HSL color creation
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
 * Parse a hex color string into an [RGB] color.
 *
 * Supports both 6-digit (#RRGGBB) and 8-digit (#RRGGBBAA) hex formats.
 * The `#` prefix is optional (automatically stripped if present).
 *
 * @param hex A hex color string in the format `#RRGGBB` or `#RRGGBBAA` (# optional).
 * @return An [RGB] color parsed from the hex string.
 * @throws IllegalArgumentException if the hex string format is invalid.
 *
 * Example:
 * ```
 * parseHex("#FF0000")      // Red (1f alpha)
 * parseHex("#00FF0080")    // Green (0.5f alpha)
 * parseHex("0000FF")       // Blue (no prefix)
 * ```
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
 * Convenience function to create an [RGB] color.
 *
 * @param r Red channel value (`0-255`).
 * @param g Green channel value (`0-255`).
 * @param b Blue channel value (`0-255`).
 * @param alpha Opacity value (`0f-1f`, default `1f`).
 * @return A new [RGB] color instance with the specified values.
 * @throws IllegalArgumentException if values are out of valid ranges.
 *
 * Example: `rgba(255, 128, 64, 0.5f)`
 */
fun rgba(
    r: Int,
    g: Int,
    b: Int,
    alpha: Float = 1f,
): RGB = RGB(r, g, b, alpha)

/**
 * Convenience function to create an [HSL] color.
 *
 * @param h Hue angle in degrees (`0f-360f`).
 * @param s Saturation percentage (`0f-100f`).
 * @param l Lightness percentage (`0f-100f`).
 * @param alpha Opacity value (`0f-1f`, default `1f`).
 * @return A new [HSL] color instance with the specified values.
 * @throws IllegalArgumentException if values are out of valid ranges.
 *
 * Example: `hsla(180f, 50f, 50f, 1f)`
 */
fun hsla(
    h: Float,
    s: Float,
    l: Float,
    alpha: Float = 1f,
): HSL = HSL(h, s, l, alpha)
