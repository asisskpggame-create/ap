package com.example.personallifeos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.example.personallifeos.R

data class TopicSvgOption(
    val key: String,
    val labelFa: String,
    val vector: ImageVector,
    val drawableRes: Int? = null
)

object AppSvgIcons {

    val TOPIC_SVG_OPTIONS = listOf(
        TopicSvgOption("SVG_UNIVERSITY", "دانشگاه", Icons.Default.School, R.drawable.ic_svg_topic_university),
        TopicSvgOption("SVG_CINEMA", "فیلم‌برداری", Icons.Default.Movie, R.drawable.ic_svg_topic_cinema),
        TopicSvgOption("SVG_BUSINESS", "کسب‌وکار", Icons.Default.BusinessCenter, R.drawable.ic_svg_topic_business),
        TopicSvgOption("SVG_HOME", "زندگی شخصی", Icons.Default.Home),
        TopicSvgOption("SVG_STUDY", "مطالعه", Icons.Default.Book),
        TopicSvgOption("SVG_TARGET", "اهداف", Icons.Default.TrackChanges, R.drawable.ic_svg_topic_target),
        TopicSvgOption("SVG_ROCKET", "استارتاپ", Icons.Default.RocketLaunch),
        TopicSvgOption("SVG_IDEA", "ایده", Icons.Default.Lightbulb),
        TopicSvgOption("SVG_FITNESS", "ورزش", Icons.Default.FitnessCenter),
        TopicSvgOption("SVG_ART", "هنر و طراحی", Icons.Default.Palette)
    )

    fun resolveTopicVector(iconKeyOrLegacyEmoji: String): ImageVector {
        return when (iconKeyOrLegacyEmoji.trim()) {
            "SVG_UNIVERSITY", "🎓", "university", "school" -> Icons.Default.School
            "SVG_CINEMA", "🎬", "cinema", "movie", "film" -> Icons.Default.Movie
            "SVG_BUSINESS", "💼", "business", "work" -> Icons.Default.BusinessCenter
            "SVG_HOME", "🏠", "home", "personal" -> Icons.Default.Home
            "SVG_STUDY", "📚", "study", "book" -> Icons.Default.Book
            "SVG_TARGET", "🎯", "target", "goal" -> Icons.Default.TrackChanges
            "SVG_ROCKET", "🚀", "rocket" -> Icons.Default.RocketLaunch
            "SVG_IDEA", "💡", "idea" -> Icons.Default.Lightbulb
            "SVG_FITNESS", "🏋️", "fitness" -> Icons.Default.FitnessCenter
            "SVG_ART", "🎨", "art" -> Icons.Default.Palette
            else -> Icons.Default.Flag
        }
    }

    fun resolveTopicDrawableRes(iconKeyOrLegacyEmoji: String): Int? {
        return when (iconKeyOrLegacyEmoji.trim()) {
            "SVG_UNIVERSITY", "🎓" -> R.drawable.ic_svg_topic_university
            "SVG_CINEMA", "🎬" -> R.drawable.ic_svg_topic_cinema
            "SVG_BUSINESS", "💼" -> R.drawable.ic_svg_topic_business
            "SVG_TARGET", "🎯" -> R.drawable.ic_svg_topic_target
            else -> null
        }
    }

    fun resolveQuickPresetVector(keyOrEmoji: String, category: String = ""): ImageVector {
        return when {
            keyOrEmoji.contains("☕") || keyOrEmoji == "SVG_COFFEE" || category.contains("کافه") -> Icons.Default.Coffee
            keyOrEmoji.contains("🚕") || keyOrEmoji == "SVG_TAXI" -> Icons.Default.LocalTaxi
            keyOrEmoji.contains("🍞") || keyOrEmoji == "SVG_SHOP" || category.contains("سوپرمارکت") -> Icons.Default.ShoppingBag
            keyOrEmoji.contains("🍽") || keyOrEmoji == "SVG_FOOD" || category.contains("غذا") -> Icons.Default.Restaurant
            keyOrEmoji.contains("⛽") || keyOrEmoji == "SVG_GAS" -> Icons.Default.LocalGasStation
            category.contains("حمل") -> Icons.Default.DirectionsCar
            else -> Icons.Default.Bolt
        }
    }

    @Composable
    fun TopicSvgIcon(
        iconKey: String,
        tint: Color,
        modifier: Modifier = Modifier,
        contentDescription: String? = null
    ) {
        val drawableRes = resolveTopicDrawableRes(iconKey)
        if (drawableRes != null) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        } else {
            Icon(
                imageVector = resolveTopicVector(iconKey),
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        }
    }
}
