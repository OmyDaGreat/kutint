package xyz.malefic.kutint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.varabyte.kobweb.compose.css.StyleVariable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.display
import com.varabyte.kobweb.compose.ui.modifiers.setVariable
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.CssStyleScopeBase
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.DisplayStyle
import kotlin.reflect.KProperty

/**
 * A color that contains both light and dark variants of a property.
 *
 * @property light The color to use in light mode.
 * @property dark The color to use in dark mode.
 * @property name The name of the color property, used for CSS variable generation.
 */
@Immutable
data class AdaptiveColor(
    val light: Kutint<*>,
    val dark: Kutint<*>,
    internal val name: String? = null,
) {
    /**
     * The color to use in the current color mode.
     *
     * For use within [Composable] functions.
     */
    val current: Kutint<*>
        @Composable
        get() =
            when (ColorMode.current) {
                ColorMode.LIGHT -> light
                ColorMode.DARK -> dark
            }

    /**
     * The color to use in the current color mode as a CSS variable or literal.
     *
     * For use within [CssStyle] blocks.
     */
    context(scope: CssStyleScopeBase)
    val variable: CSSColorValue
        get() =
            if (name != null) {
                StyleVariable.PropertyValue<CSSColorValue>("kutint-$name").value(null)
            } else {
                if (scope.colorMode.isLight) light else dark
            }

    /**
     * Apply a function to both the light and dark variants of the color.
     *
     * @param func The function to apply to the colors.
     *
     * @return A new [AdaptiveColor] with the transformed colors.
     */
    infix fun map(func: (Kutint<*>) -> Kutint<*>) = AdaptiveColor(func(light), func(dark), name)
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

    private val calc = derivedStateOf { _calc() }

    private var _adaptive by mutableStateOf<AdaptiveColor?>(null)

    val adaptive get() = (_adaptive ?: calc.value).copy(name = name)

    operator fun getValue(
        thisRef: Palette,
        property: KProperty<*>,
    ): AdaptiveColor = adaptive

    operator fun setValue(
        thisRef: Palette,
        property: KProperty<*>,
        value: AdaptiveColor,
    ) {
        _adaptive = value
        (thisRef as? BasePalette)?.register(name ?: property.name, this)
    }

    operator fun setValue(
        thisRef: Palette,
        property: KProperty<*>,
        value: () -> AdaptiveColor,
    ) {
        _calc = value
        _adaptive = null
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

/**
 * A helper to create a type-safe theme accessor for your project.
 *
 * @param P The type of the palette.
 * @property default The default palette to use if none is provided.
 */
open class PaletteDefinition<P : Palette>(
    val default: P,
) {
    /**
     * The [ProvidableCompositionLocal] used to provide the palette to the UI.
     */
    val local = staticCompositionLocalOf { default }

    /**
     * Type-safe access to your palette colors.
     */
    val colors: P @Composable get() = local.current

    /**
     * Accessor for use in static contexts like [CssStyle].
     * Returns the property definitions (which map to CSS variables in a [CssStyleScopeBase] context).
     */
    val static: P get() = default

    /**
     * Wrapper to provide your specific palette to the UI.
     *
     * @param palette The palette to provide.
     * @param content The composable content to wrap.
     */
    @Composable
    fun Provide(
        palette: P,
        content: @Composable () -> Unit,
    ) {
        KutintTheme(palette, local = local, content = content)
    }
}

/**
 * Example theme implementation using [MaterialPalette].
 */
object MaterialTheme : PaletteDefinition<MaterialPalette>(MaterialPalette(parseHex("#6750A4")))

/**
 * The core theme wrapper that provides the palette and mirrors colors to CSS variables.
 *
 * Rather than using this function directly, it's recommended to use [PaletteDefinition.Provide].
 *
 * @param palette The palette to use.
 * @param local The [androidx.compose.runtime.CompositionLocal] to provide the palette through.
 * @param content The composable content to wrap.
 */
@Composable
fun <P : Palette> KutintTheme(
    palette: P,
    local: ProvidableCompositionLocal<P>,
    content: @Composable () -> Unit,
) {
    val colorMode = ColorMode.current

    val rootModifier =
        Modifier
            .display(DisplayStyle.Contents)
            .run {
                palette.colors.entries.fold(this) { acc, (name, color) ->
                    val value = if (colorMode.isLight) color.light else color.dark
                    acc.setVariable(StyleVariable.PropertyValue<CSSColorValue>("kutint-$name"), value)
                }
            }

    Box(rootModifier) {
        CompositionLocalProvider(local provides palette) {
            content()
        }
    }
}
