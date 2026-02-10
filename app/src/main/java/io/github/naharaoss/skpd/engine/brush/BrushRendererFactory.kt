package io.github.naharaoss.skpd.engine.brush

import kotlin.reflect.KClass

data class BrushRendererFactory(
    val clazz: KClass<out Brush>,
    val factory: () -> BrushRenderer<out Brush>
)

inline fun <reified T : Brush> factoryOf(noinline factory: () -> BrushRenderer<T>): BrushRendererFactory {
    return BrushRendererFactory(T::class, factory)
}

fun factoryMapOf(vararg factories: BrushRendererFactory): Map<KClass<out Brush>, BrushRendererFactory> {
    val map = mutableMapOf<KClass<out Brush>, BrushRendererFactory>()
    for (factory in factories) map[factory.clazz] = factory
    return map
}