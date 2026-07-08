package xyz.malefic.kutint

import com.varabyte.kobweb.compose.ui.graphics.Color
import org.jetbrains.compose.web.css.CSSColorValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Represents the color space of a [KutintColor].
 */
sealed class ColorSpace

/**
 * Represents the [RGB] color space.
 */
object RGBColorSpace : ColorSpace()

/**
 * Represents the [HSL] color space.
 */
object HSLColorSpace : ColorSpace()

/**
 * Base class for all color types supported by Kutint. See [RGB] and [HSL] for specific implementations.
 *
 * Includes a variety of functions for working with colors in Kobweb and Compose HTML.
 */
sealed class KutintColor<T : ColorSpace> : CSSColorValue {
    /**
     * The alpha channel of the color, shared across color spaces.
     */
    abstract val alpha: Float

    /**
     * An [RGB] representation of the current color.
     */
    abstract val rgb: RGB

    /**
     * An [HSL] representation of the current color.
     */
    abstract val hsl: HSL

    /**
     * A [Color] representation of the current color.
     */
    abstract val color: Color

    /**
     * Tints the color, using a linear interpolation between the original color and white in the RGB color space, by a given amount.
     *
     * @param amount The amount to tint the color `0 to 1`.
     *
     * @return The tinted color as an [RGB].
     */
    infix fun tint(amount: Float = 0.2f): RGB =
        withRGB { r, g, b ->
            val clipped = amount.coerceIn(0f, 1f)
            RGB(
                r = r + (255f - r) * clipped,
                g = g + (255f - g) * clipped,
                b = b + (255f - b) * clipped,
                alpha = alpha,
            )
        }

    /**
     * Shades the color, using a linear interpolation between the original color and black in the RGB color space, by a given amount.
     *
     * @param amount The amount to shade the color `0 to 1`.
     *
     * @return The shaded color as an [RGB].
     */
    infix fun shade(amount: Float = 0.2f): RGB =
        withRGB { r, g, b ->
            val clipped = amount.coerceIn(0f, 1f)
            RGB(
                r = r - r * clipped,
                g = g - g * clipped,
                b = b - b * clipped,
                alpha = alpha,
            )
        }

    /**
     * Inverts the color by subtracting each channel from 255 in the RGB color space.
     *
     * @return The inverted color as an [RGB].
     */
    fun invert(): RGB =
        withRGB { r, g, b ->
            RGB(
                r = 255f - r,
                g = 255f - g,
                b = 255f - b,
                alpha = alpha,
            )
        }

    /**
     * Converts the color to grayscale using the W3C formula for relative luminance through the RGB color space.
     *
     * @return The grayscale color as an [RGB].
     */
    fun grayscale(): RGB =
        withRGB { r, g, b ->
            val gray = (0.2126f * r + 0.7152f * g + 0.0722f * b)
            RGB(gray, gray, gray, alpha)
        }

    /**
     * Saturates the color, through the HSL color space, by a given amount.
     *
     * @param amount The amount to saturate the color `0 to 1`.
     *
     * @return The saturated color as an [HSL].
     */
    infix fun saturate(amount: Float = 0.2f): HSL =
        withHSL { h, s, l ->
            val newSaturation = (s + amount * 100f).coerceIn(0f, 100f)
            HSL(h = h, s = newSaturation, l = l, alpha = alpha)
        }

    /**
     * Desaturates the color, through the HSL color space, by a given amount.
     *
     * @param amount The amount to desaturate the color `0 to 1`.
     *
     * @return The desaturated color as an [HSL].
     */
    infix fun desaturate(amount: Float = 0.2f): HSL = saturate(-amount)

    /**
     * Lightens the color, through the HSL color space, by a given amount.
     *
     * @param amount The amount to lighten the color `0 to 1`.
     *
     * @return The lightened color as an [HSL].
     */
    infix fun lighten(amount: Float = 0.2f): HSL =
        withHSL { h, s, l ->
            val newLightness = (l + amount * 100f).coerceIn(0f, 100f)
            HSL(h = h, s = s, l = newLightness, alpha = alpha)
        }

    /**
     * Darkens the color, through the HSL color space, by a given amount.
     *
     * @param amount The amount to darken the color `0 to 1`.
     *
     * @return The darkened color as an [HSL].
     */
    infix fun darken(amount: Float = 0.2f): HSL = lighten(-amount)

    /**
     * Rotates the hue in the HSL color space by a given number of degrees.
     *
     * @param degrees The number of degrees to rotate the hue `0 to 360`.
     *
     * @return The rotated color as an [HSL].
     */
    infix fun hueRotate(degrees: Int = 30): HSL =
        withHSL { h, s, l ->
            val newHue = (h + degrees).mod(360f)
            HSL(h = newHue, s = s, l = l, alpha = alpha)
        }

    /**
     * Gets the complementary color in the HSL color space.
     *
     * @return The complementary color as an [HSL].
     */
    fun complement(): HSL = hueRotate(180)

    /**
     * Blends two colors together using a linear interpolation between them in the RGB color space.
     *
     * @param other The other color to blend with.
     * @param amount The amount to blend the colors `0 to 1`.
     *
     * @return The blended color as an [RGB].
     */
    fun blend(
        other: KutintColor<*>,
        amount: Float = 0.5f,
    ): RGB =
        withRGB { r1, g1, b1 ->
            other.withRGB { r2, g2, b2 ->
                val clipped = amount.coerceIn(0f, 1f)
                RGB(
                    r = r1 + (r2 - r1) * clipped,
                    g = g1 + (g2 - g1) * clipped,
                    b = b1 + (b2 - b1) * clipped,
                    alpha = alpha + (other.alpha - alpha) * clipped,
                )
            }
        }

    /**
     * Calculates the luminance of the color in the [RGB] color space.
     *
     * @return The luminance of the color.
     */
    fun luminance(): Double =
        withRGB { r, g, b ->
            fun adjust(c: Float): Double {
                val s = c / 255.0
                return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
            }
            0.2126 * adjust(r) + 0.7152 * adjust(g) + 0.0722 * adjust(b)
        }

    /**
     * Utility function for switching to the RGB color space and performing an operation on the channels.
     *
     * @param func The function to apply to the RGB channels.
     *
     * @return The result of the function.
     */
    inline fun <T> withRGB(func: (r: Float, g: Float, b: Float) -> T): T = this.rgb.let { func(it.r, it.g, it.b) }

    /**
     * Utility function for switching the HSL color space and performing an operation on the channels.
     *
     * @param func The function to apply to the HSL channels.
     *
     * @return The result of the function.
     */
    inline fun <T> withHSL(func: (h: Float, s: Float, l: Float) -> T): T = this.hsl.let { func(it.h, it.s, it.l) }

    /**
     * Creates a new color with the given alpha value.
     *
     * @param newAlpha The new alpha value `0 to 1`.
     *
     * @return A new color with the given alpha value.
     */
    abstract infix fun withAlpha(newAlpha: Float): KutintColor<T>

    /**
     * Converts the color to a hex string.
     *
     * @param includeAlpha Whether to include the alpha channel in the hex string.
     *
     * @return A hex string representation of the color.
     */
    fun toHex(includeAlpha: Boolean = alpha < 1f): String =
        withRGB { r, g, b ->
            val rHex =
                r
                    .roundToInt()
                    .coerceIn(0, 255)
                    .toString(16)
                    .padStart(2, '0')
            val gHex =
                g
                    .roundToInt()
                    .coerceIn(0, 255)
                    .toString(16)
                    .padStart(2, '0')
            val bHex =
                b
                    .roundToInt()
                    .coerceIn(0, 255)
                    .toString(16)
                    .padStart(2, '0')
            val aHex =
                if (includeAlpha) {
                    (alpha * 255)
                        .roundToInt()
                        .coerceIn(0, 255)
                        .toString(16)
                        .padStart(2, '0')
                } else {
                    ""
                }
            "#$rHex$gHex$bHex$aHex".uppercase()
        }
}

/**
 * Represents a color in the RGB color space.
 *
 * @property r Red channel `0-255`
 * @property g Green channel `0-255`
 * @property b Blue channel `0-255`
 * @property alpha Alpha channel `0-1`
 */
data class RGB(
    val r: Float,
    val g: Float,
    val b: Float,
    override val alpha: Float = 1f,
) : KutintColor<RGBColorSpace>() {
    init {
        require(r in 0f..255f) { "Red channel (r) must be between 0 and 255, got $r" }
        require(g in 0f..255f) { "Green channel (g) must be between 0 and 255, got $g" }
        require(b in 0f..255f) { "Blue channel (b) must be between 0 and 255, got $b" }
        require(alpha in 0f..1f) { "Alpha channel (alpha) must be between 0f and 1f, got $alpha" }
    }

    override val color = Color.rgba(r, g, b, alpha)

    override val rgb: RGB get() = this

    override val hsl: HSL by lazy {
        val rNorm = r / 255f
        val gNorm = g / 255f
        val bNorm = b / 255f

        val maxChannel = max(rNorm, max(gNorm, bNorm))
        val minChannel = min(rNorm, min(gNorm, bNorm))
        val delta = maxChannel - minChannel
        val lightness = (maxChannel + minChannel) / 2

        if (delta == 0f) {
            return@lazy HSL(0f, 0f, lightness * 100f, alpha)
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

        HSL(
            h = hue % 360f,
            s = saturation * 100f,
            l = lightness * 100f,
            alpha = alpha,
        )
    }

    override fun withAlpha(newAlpha: Float): RGB = RGB(r, g, b, newAlpha.coerceIn(0f, 1f))

    override fun toString(): String = "rgba($r, $g, $b, $alpha)"
}

/**
 * Represents a color in the HSL color space.
 *
 * @property h Hue channel `0-360`
 * @property s Saturation channel `0-100`
 * @property l Lightness channel `0-100`
 * @property alpha Alpha channel `0-1`
 */
data class HSL(
    val h: Float,
    val s: Float,
    val l: Float,
    override val alpha: Float = 1f,
) : KutintColor<HSLColorSpace>() {
    init {
        require(h in 0f..360f) { "Hue channel (h) must be between 0f and 360f, got $h" }
        require(s in 0f..100f) { "Saturation channel (s) must be between 0f and 100f, got $s" }
        require(l in 0f..100f) { "Lightness channel (l) must be between 0f and 100f, got $l" }
        require(alpha in 0f..1f) { "Alpha channel (alpha) must be between 0f and 1f, got $alpha" }
    }

    override val color = Color.hsla(h, s, l, alpha)

    override val hsl: HSL get() = this

    override val rgb: RGB by lazy {
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

        RGB(
            r = (rPrime + m) * 255,
            g = (gPrime + m) * 255,
            b = (bPrime + m) * 255,
            alpha = alpha,
        )
    }

    override fun withAlpha(newAlpha: Float): HSL = HSL(h, s, l, newAlpha.coerceIn(0f, 1f))

    override fun toString(): String = "hsla($h, $s%, $l%, $alpha)"
}

/**
 * Utility function for creating an [RGB] color from a hex string.
 *
 * @param hex The hex string to parse.
 *
 * @return The parsed [RGB] color.
 */
fun parseHex(hex: String): RGB {
    val cleanHex = hex.removePrefix("#").uppercase()
    val parsedHex =
        when (cleanHex.length) {
            3 -> cleanHex.map { "$it$it" }.joinToString("")
            4 -> cleanHex.map { "$it$it" }.joinToString("")
            6, 8 -> cleanHex
            else -> throw IllegalArgumentException("Hex color must be 3, 4, 6, or 8 digits, got $hex")
        }

    val r = parsedHex.substring(0, 2).toInt(16).toFloat()
    val g = parsedHex.substring(2, 4).toInt(16).toFloat()
    val b = parsedHex.substring(4, 6).toInt(16).toFloat()
    val alpha =
        if (parsedHex.length == 8) {
            parsedHex.substring(6, 8).toInt(16) / 255f
        } else {
            1f
        }

    return RGB(r, g, b, alpha)
}

/**
 * Utility function for creating an [RGB] color from an Int (e.g., 0xFF0000).
 *
 * @param color The Int to parse.
 * @param hasAlpha Whether the Int has an alpha channel.
 *
 * @return The parsed [RGB] color.
 */
fun parseHex(
    color: Int,
    hasAlpha: Boolean = false,
): RGB =
    if (hasAlpha) {
        val a = (color shr 24 and 0xFF) / 255f
        val r = (color shr 16 and 0xFF).toFloat()
        val g = (color shr 8 and 0xFF).toFloat()
        val b = (color and 0xFF).toFloat()
        RGB(r, g, b, a)
    } else {
        val r = (color shr 16 and 0xFF).toFloat()
        val g = (color shr 8 and 0xFF).toFloat()
        val b = (color and 0xFF).toFloat()
        RGB(r, g, b, 1f)
    }

/**
 * Utility function for creating an [RGB] color from RGB values.
 *
 * @param r Red channel `0-255`
 * @param g Green channel `0-255`
 * @param b Blue channel `0-255`
 * @param alpha Alpha channel `0-1`
 *
 * @return The parsed [RGB] color.
 */
fun rgba(
    r: Number,
    g: Number,
    b: Number,
    alpha: Number = 1f,
): RGB = RGB(r.toFloat(), g.toFloat(), b.toFloat(), alpha.toFloat())

/**
 * Utility function for creating an [HSL] color from HSL values.
 *
 * @param h Hue channel `0-360`
 * @param s Saturation channel `0-100`
 * @param l Lightness channel `0-100`
 * @param alpha Alpha channel `0-1`
 *
 * @return The parsed [HSL] color.
 */
fun hsla(
    h: Number,
    s: Number,
    l: Number,
    alpha: Number = 1f,
): HSL = HSL(h.toFloat(), s.toFloat(), l.toFloat(), alpha.toFloat())
