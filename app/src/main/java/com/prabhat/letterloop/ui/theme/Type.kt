// The app's only typeface: Fredoka (SIL Open Font License), bundled in res/font.
// Screens set their own sizes; this file only makes Fredoka the default.
package com.prabhat.letterloop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.prabhat.letterloop.R

val Fredoka = FontFamily(
    Font(R.font.fredoka_regular, FontWeight.Normal),
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
)

// MaterialTheme uses bodyLarge as the default style for every Text, so setting the font
// here is enough. No lineHeight on purpose: a fixed one would clip the larger titles.
val Typography = Typography(
    bodyLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Normal, fontSize = 15.sp),
)
