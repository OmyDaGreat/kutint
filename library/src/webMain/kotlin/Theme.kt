package xyz.malefic.kutint

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.CssStyleScopeBase
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import xyz.malefic.kutint.AdaptiveColor.Companion.darkTransform
import xyz.malefic.kutint.AdaptiveColor.Companion.with
import kotlin.reflect.KProperty

/**
 * A color that contains both light and dark variants of a property.
 *
 * @property light The color to use in light mode.
 * @property dark The color to use in dark mode.
 */
data class AdaptiveColor(
    val light: KutintColor<*>,
    val dark: KutintColor<*>,
) {
    /**
     * Color-mode aware accessor for use in [CssStyle] blocks.
     *
     * @return The color to use in the current color mode.
     */
    fun CssStyleScopeBase.value() =
        when (this.colorMode) {
            ColorMode.LIGHT -> light
            ColorMode.DARK -> dark
        }

    /**
     * Color-mode aware accessor for use in [Composable] functions
     *
     * @return The color to use in the current color mode.
     */
    @Composable
    fun value() =
        when (ColorMode.current) {
            ColorMode.LIGHT -> light
            ColorMode.DARK -> dark
        }

    /**
     * Apply a function to both the light and dark variants of the color.
     *
     * @param func The function to apply to the colors.
     *
     * @return A new [AdaptiveColor] with the transformed colors.
     */
    infix fun map(func: (KutintColor<*>) -> KutintColor<*>) = AdaptiveColor(func(light), func(dark))

    companion object {
        /**
         * Create an [AdaptiveColor] from two [KutintColor]s.
         *
         * @param dark The color to use in dark mode.
         *
         * @return An [AdaptiveColor] with the given colors.
         */
        infix fun KutintColor<*>.with(dark: KutintColor<*>) = AdaptiveColor(this, dark)

        /**
         * Create an [AdaptiveColor] from a light variant [KutintColor] and apply a function to create the dark variant.
         *
         * @param func The function to apply to the light variant to create the dark variant.
         *
         * @return An [AdaptiveColor] with the given colors.
         */
        infix fun KutintColor<*>.darkTransform(func: (KutintColor<*>) -> KutintColor<*>) = AdaptiveColor(this, func(this))
    }
}

/**
 * A delegate for defining colors in a [Palette].
 */
@Suppress("ktlint:standard:class-naming")
class ColorDelegate(
    private var name: String?,
    var adaptive: AdaptiveColor,
) {
    operator fun getValue(
        thisRef: Palette,
        property: KProperty<*>,
    ): AdaptiveColor = adaptive

    operator fun setValue(
        thisRef: Palette,
        property: KProperty<*>,
        value: AdaptiveColor,
    ) {
        adaptive = value
        (thisRef as? BasePalette)?.register(name ?: property.name, value)
    }

    operator fun provideDelegate(
        thisRef: Palette,
        property: KProperty<*>,
    ): ColorDelegate {
        if (name == null) name = property.name
        (thisRef as? BasePalette)?.register(name ?: property.name, adaptive)
        return this
    }
}

/**
 * Convenience function for [ColorDelegate].
 */
fun color(
    light: KutintColor<*>,
    dark: KutintColor<*>,
) = ColorDelegate(null, light with dark)

/**
 * Convenience function for [ColorDelegate].
 */
fun color(
    name: String,
    light: KutintColor<*>,
    dark: KutintColor<*>,
) = ColorDelegate(name, light with dark)

/**
 * Convenience function for [ColorDelegate].
 */
fun color(adaptive: AdaptiveColor) = ColorDelegate(null, adaptive)

/**
 * Interface representing a color palette.
 *
 * Implementations can define roles using the [color] delegate.
 */
interface Palette {
    /**
     * A registry of the colors defined in this palette.
     */
    val colors: Map<String, AdaptiveColor>
}

/**
 * Basic implementation of [Palette] that handles color registration.
 */
abstract class BasePalette : Palette {
    private val _colors = mutableMapOf<String, AdaptiveColor>()
    override val colors: Map<String, AdaptiveColor> get() = _colors

    internal fun register(
        name: String,
        adaptive: AdaptiveColor,
    ) {
        _colors[name] = adaptive
    }
}

/**
 * An example palette containing a few Material Design 3 properties that can be generated from a seed color.
 *
 * This class can also be extended to define custom palettes from seed color(s).
 *
 * @property primarySeed The seed color for the primary palette.
 * @property secondarySeed The optional seed color for the secondary palette.
 */
open class MaterialPalette(
    val primarySeed: KutintColor<*>,
    val secondarySeed: KutintColor<*> = primarySeed.hueRotate(30).desaturate(0.2f),
) : BasePalette() {
    var primary by color(primarySeed darkTransform { it.desaturate(0.1f).lighten(0.3f) })
    var onPrimary by color(primary map { it.contrast() })
    var primaryContainer by color(primarySeed.lighten(0.4f) darkTransform { it.darken(0.1f) })
    var onPrimaryContainer by color(primaryContainer map { it.contrast() })

    var secondary by color(secondarySeed darkTransform { it.lighten(0.2f) })
    var onSecondary by color(secondary.map { it.contrast() })
    var secondaryContainer by color(secondarySeed.lighten(0.4f) darkTransform { it.darken(0.1f) })
    var onSecondaryContainer by color(secondaryContainer map { it.contrast() })

    var background by color(parseHex("#FBFAED"), parseHex("#13140D"))
    var onBackground by color(parseHex("#1B1C15"), parseHex("#E4E3D7"))
    var surface by color(parseHex("#FBFAED"), parseHex("#13140D"))
    var onSurface by color(parseHex("#1B1C15"), parseHex("#E4E3D7"))

    var outline by color(primarySeed.desaturate(0.5f).lighten(0.1f) darkTransform { it.lighten(0.1f) })
    var error by color(parseHex("#BA1A1A"), parseHex("#FFB4AB"))
}

/**
 * Utility to choose between black or white as a contrast for the given color.
 *
 * @return The contrast color.
 */
fun KutintColor<*>.contrast() = if (this.luminance() > 0.5) parseHex("#000000") else parseHex("#FFFFFF")
