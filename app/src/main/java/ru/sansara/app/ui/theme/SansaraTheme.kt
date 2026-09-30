package ru.sansara.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.sansara.app.R

val Bg=Color(0xFF0A0A09)
val Panel=Color(0xFF171613)
val Panel2=Color(0xFF211E19)
val Gold=Color(0xFFD8B07C)
val GoldSoft=Color(0xFFE7C59C)
val TextPrimary=Color(0xFFF5F0E8)
val TextSecondary=Color(0xFF9D9992)
val Success=Color(0xFF4BE087)
val Error=Color(0xFFFF746C)
val Warning=Color(0xFFFFB15A)
val Border=Color(0xFF4B4033)
val Text=TextPrimary
val Muted=TextSecondary
val Green=Success
val Red=Error

val ManropeFamily=FontFamily(
    Font(R.font.manrope_variable,FontWeight.Normal),
    Font(R.font.manrope_variable,FontWeight.Medium),
    Font(R.font.manrope_variable,FontWeight.SemiBold),
    Font(R.font.manrope_variable,FontWeight.Bold),
    Font(R.font.manrope_variable,FontWeight.ExtraBold)
)

val SansaraTypography=Typography(
    displayLarge=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.Bold,fontSize=34.sp,lineHeight=42.sp),
    headlineLarge=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.Bold,fontSize=30.sp,lineHeight=38.sp),
    headlineMedium=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.Bold,fontSize=24.sp,lineHeight=31.sp),
    titleLarge=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.SemiBold,fontSize=20.sp,lineHeight=27.sp),
    titleMedium=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=22.sp),
    bodyLarge=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.Normal,fontSize=16.sp,lineHeight=23.sp),
    bodyMedium=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.Normal,fontSize=14.sp,lineHeight=20.sp),
    bodySmall=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.Normal,fontSize=12.sp,lineHeight=17.sp),
    labelLarge=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.SemiBold,fontSize=15.sp,lineHeight=20.sp),
    labelMedium=TextStyle(fontFamily=ManropeFamily,fontWeight=FontWeight.Medium,fontSize=11.sp,lineHeight=15.sp)
)
val SansaraShapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(32.dp))
private val Scheme=darkColorScheme(primary=Gold,onPrimary=Color.Black,background=Bg,onBackground=TextPrimary,surface=Panel,onSurface=TextPrimary,surfaceVariant=Panel2,onSurfaceVariant=TextSecondary,secondary=GoldSoft,error=Error)
@Composable fun SansaraTheme(content:@Composable ()->Unit){MaterialTheme(colorScheme=Scheme,typography=SansaraTypography,shapes=SansaraShapes,content=content)}
