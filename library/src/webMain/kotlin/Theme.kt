package xyz.malefic.kutint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.CssStyleScopeBase
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import kotlin.reflect.KProperty

/**
 * A color that contains both light and dark variants of a property.
 *
 * @property light The color to use in light mode.
 * @property dark The color to use in dark mode.
 */
@Immutable
data class AdaptiveColor(
    val light: Kutint<*>,
    val dark: Kutint<*>,
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
    infix fun map(func: (Kutint<*>) -> Kutint<*>) = AdaptiveColor(func(light), func(dark))
}

/**
 * Create an [AdaptiveColor] from two [Kutint]s.
 *
 * @param dark The color to use in dark mode.
 *
 * @return An [AdaptiveColor] with the given colors.
 */
infix fun Kutint<*>.with(dark: Kutint<*>) = AdaptiveColor(this, dark)

/**
 * Create an [AdaptiveColor] from a light variant [Kutint] and apply a function to create the dark variant.
 *
 * @param func The function to apply to the light variant to create the dark variant.
 *
 * @return An [AdaptiveColor] with the given colors.
 */
infix fun Kutint<*>.darkTransform(func: (Kutint<*>) -> Kutint<*>) = AdaptiveColor(this, func(this))

/**
 * A delegate for defining colors in a [Palette].
 */
@Stable
class ColorDelegate(
    private var name: String?,
    initial: () -> AdaptiveColor,
) {
    @Suppress("ktlint:standard:backing-property-naming")
    private var _calc by mutableStateOf(initial)

    @Suppress("ktlint:standard:backing-property-naming")
    private val _derived = derivedStateOf { _calc() }

    @Suppress("ktlint:standard:backing-property-naming")
    private var _override by mutableStateOf<AdaptiveColor?>(null)

    val adaptive get() = _override ?: _derived.value

    operator fun getValue(
        thisRef: Palette,
        property: KProperty<*>,
    ): AdaptiveColor = adaptive

    operator fun setValue(
        thisRef: Palette,
        property: KProperty<*>,
        value: AdaptiveColor,
    ) {
        _override = value
        (thisRef as? BasePalette)?.register(name ?: property.name, this)
    }

    operator fun setValue(
        thisRef: Palette,
        property: KProperty<*>,
        value: () -> AdaptiveColor,
    ) {
        _calc = value
        _override = null
        (thisRef as? BasePalette)?.register(name ?: property.name, this)
    }

    operator fun provideDelegate(
        thisRef: Palette,
        property: KProperty<*>,
    ): ColorDelegate {
        if (name == null) name = property.name
        (thisRef as? BasePalette)?.register(name ?: property.name, this)
        return this
    }
}

/**
 * Sets one color to be for both light and dark modes.
 */
fun color(
    color: Kutint<*>,
    name: String? = null,
) = ColorDelegate(name) { color with color }

/**
 * Convenience function for [ColorDelegate].
 */
fun color(
    light: Kutint<*>,
    dark: Kutint<*>,
    name: String? = null,
) = ColorDelegate(name) { light with dark }

/**
 * Convenience function for [ColorDelegate].
 */
fun color(
    initial: AdaptiveColor,
    name: String? = null,
) = ColorDelegate(name) { initial }

/**
 * Convenience function for [ColorDelegate].
 */
fun color(
    name: String? = null,
    calc: () -> AdaptiveColor,
) = ColorDelegate(name, calc)

/**
 * Interface representing a color palette. Implementations can define roles using the [color] delegate.
 *
 * This interface should only be used directly for custom implementations of the [colors] property.
 */
@Stable
interface Palette {
    /**
     * A registry of the colors defined in this palette.
     */
    val colors: Map<String, AdaptiveColor>
}

/**
 * Basic implementation of [Palette] that handles color registration through the [colors] map automatically.
 */
@Stable
abstract class BasePalette : Palette {
    private val _colors = mutableStateMapOf<String, ColorDelegate>()
    override val colors: Map<String, AdaptiveColor> by derivedStateOf { _colors.mapValues { it.value.adaptive } }

    internal fun register(
        name: String,
        delegate: ColorDelegate,
    ) {
        _colors[name] = delegate
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
    val primarySeed: Kutint<*>,
    val secondarySeed: Kutint<*> = primarySeed.hueRotate(30).desaturate(0.2f),
) : BasePalette() {
    var primary by color(primarySeed darkTransform { it.desaturate(0.1f).lighten(0.3f) })
    var onPrimary by color { primary map { it.contrast() } }
    var primaryContainer by color(primarySeed.lighten(0.4f) darkTransform { it.darken(0.1f) })
    var onPrimaryContainer by color { primaryContainer map { it.contrast() } }

    var secondary by color(secondarySeed darkTransform { it.lighten(0.2f) })
    var onSecondary by color { secondary.map { it.contrast() } }
    var secondaryContainer by color(secondarySeed.lighten(0.4f) darkTransform { it.darken(0.1f) })
    var onSecondaryContainer by color { secondaryContainer map { it.contrast() } }

    var background by color(parseHex("#FBFAED"), parseHex("#13140D"))
    var onBackground by color(parseHex("#1B1C15"), parseHex("#E4E3D7"))
    var surface by color(parseHex("#FBFAED"), parseHex("#13140D"))
    var onSurface by color(parseHex("#1B1C15"), parseHex("#E4E3D7"))

    var outline by color { primarySeed.desaturate(0.5f).lighten(0.1f) darkTransform { it.lighten(0.1f) } }
    var error by color(parseHex("#BA1A1A"), parseHex("#FFB4AB"))
}
