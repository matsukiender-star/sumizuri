package com.sumizuri.app.ui.theme.spec

import kotlinx.serialization.Serializable

// How much room there is around things, from tight to airy.
@Serializable
enum class LayoutDensity(val scale: Float) { Compact(0.8f), Comfortable(1f), Spacious(1.25f) }

// How the count of unread chapters is drawn on a cover.
@Serializable
enum class CoverBadgeStyle { Tilted, Circle, Pill, Hidden }

// How the bar of how far a title is read is filled: from one accent of the theme to the other, or in the main accent alone.
@Serializable
enum class ProgressStyle { Gradient, Solid }

// What moves behind every page: soft ink washes, bands of light like an aurora, small floating lights, layered waves, a field of dots that ripples, wide brush strokes, or a slow mosaic of the covers of the library.
@Serializable
enum class BackgroundStyle { Ink, Aurora, Orbs, Waves, Dots, Strokes, Covers }

// The colors of the background: the accents of the theme, the main accent alone, a soft gray from the text color, or colors of its own.
@Serializable
enum class BackgroundPalette { Accents, Accent, Gray, Own }

// How much of the page of a title follows its cover: nothing, only the accents, the accents with a light tint of the page, or the whole page deep in the colors of the cover with its glow.
@Serializable
enum class CoverColorMode { Off, Accent, Tinted, Immersive }

// Where the accent of a theme comes from: its own colors, the accent the system was set to, or a color that moves with the time of day.
@Serializable
enum class AccentSource { Theme, System, Clock }

// The proportions of a cover, width over height.
@Serializable
enum class CoverAspect(val ratio: Float) { Tall(2f / 3f), Classic(3f / 4f), Square(1f) }
