package ru.sansara.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import ru.sansara.app.ui.theme.SansaraFontFamily

/**
 * Оформление надписей на картинке экрана «Добро пожаловать»: шрифт, размер и место каждой надписи.
 * Место — одна из 9 точек карточки (0 — слева вверху … 8 — справа внизу).
 * Хранится в настройках телефона одной строкой JSON, база данных не меняется.
 */
data class ProtoPromoStyle(
    val titleFont: String = "manrope",
    val titleSize: Int = 22,
    val titlePos: Int = 3,
    val textFont: String = "manrope",
    val textSize: Int = 15,
    val textPos: Int = 3
) {
    fun toJson(): String = org.json.JSONObject()
        .put("titleFont", titleFont).put("titleSize", titleSize).put("titlePos", titlePos)
        .put("textFont", textFont).put("textSize", textSize).put("textPos", textPos).toString()

    companion object {
        const val PREF_KEY = "welcome_promo_style"
        val TITLE_SIZES = 14..40
        val TEXT_SIZES = 11..26

        fun fromJson(raw: String?): ProtoPromoStyle {
            if (raw.isNullOrBlank()) return ProtoPromoStyle()
            return runCatching {
                val o = org.json.JSONObject(raw)
                val d = ProtoPromoStyle()
                ProtoPromoStyle(
                    titleFont = o.optString("titleFont", d.titleFont).takeIf { k -> protoPromoFonts.any { it.key == k } } ?: d.titleFont,
                    titleSize = o.optInt("titleSize", d.titleSize).coerceIn(TITLE_SIZES),
                    titlePos = o.optInt("titlePos", d.titlePos).coerceIn(0, 8),
                    textFont = o.optString("textFont", d.textFont).takeIf { k -> protoPromoFonts.any { it.key == k } } ?: d.textFont,
                    textSize = o.optInt("textSize", d.textSize).coerceIn(TEXT_SIZES),
                    textPos = o.optInt("textPos", d.textPos).coerceIn(0, 8)
                )
            }.getOrDefault(ProtoPromoStyle())
        }
    }
}

internal data class ProtoPromoFont(val key: String, val label: String, val family: FontFamily, val weight: FontWeight = FontWeight.Normal, val tracking: Float = 0.12f)

/** Шрифты с кириллицей (лицензия SIL OFL — свободное коммерческое использование). */
internal val protoPromoFonts = listOf(
    ProtoPromoFont("manrope", "Manrope", SansaraFontFamily, FontWeight.Medium),
    ProtoPromoFont("montserrat", "Montserrat", FontFamily(Font(R.font.proto_font_montserrat))),
    ProtoPromoFont("cormorant", "Cormorant", FontFamily(Font(R.font.proto_font_cormorant)), tracking = 0.06f),
    ProtoPromoFont("playfair", "Playfair", FontFamily(Font(R.font.proto_font_playfair)), tracking = 0.04f),
    ProtoPromoFont("marck", "Рукописный", FontFamily(Font(R.font.proto_font_marck)), tracking = 0f)
)

internal fun protoPromoFont(key: String) = protoPromoFonts.firstOrNull { it.key == key } ?: protoPromoFonts.first()

private fun posAlignment(pos: Int): Alignment = when (pos.coerceIn(0, 8)) {
    0 -> Alignment.TopStart; 1 -> Alignment.TopCenter; 2 -> Alignment.TopEnd
    3 -> Alignment.CenterStart; 4 -> Alignment.Center; 5 -> Alignment.CenterEnd
    6 -> Alignment.BottomStart; 7 -> Alignment.BottomCenter; else -> Alignment.BottomEnd
}

private fun posHorizontal(pos: Int): Alignment.Horizontal = when (pos.coerceIn(0, 8) % 3) { 0 -> Alignment.Start; 1 -> Alignment.CenterHorizontally; else -> Alignment.End }
private fun posTextAlign(pos: Int): TextAlign = when (pos.coerceIn(0, 8) % 3) { 0 -> TextAlign.Start; 1 -> TextAlign.Center; else -> TextAlign.End }

/** Надписи поверх картинки. Если обе надписи в одном месте — заголовок сверху, текст под ним. */
@Composable
internal fun BoxScope.ProtoPromoOverlay(title: String, text: String, style: ProtoPromoStyle, padding: androidx.compose.ui.unit.Dp = 24.dp) {
    val shownTitle = title.ifBlank { "УВАЖЕНИЕ\nВ КАЖДОЙ\nДЕТАЛИ" }
    val positions = buildList { add(style.titlePos); if (text.isNotBlank() && style.textPos != style.titlePos) add(style.textPos) }
    positions.forEach { pos ->
        Column(Modifier.align(posAlignment(pos)).padding(padding), horizontalAlignment = posHorizontal(pos)) {
            if (pos == style.titlePos) {
                val f = protoPromoFont(style.titleFont)
                Text(shownTitle, color = ProtoGoldSoft, fontFamily = f.family, fontWeight = f.weight, fontSize = style.titleSize.sp,
                    lineHeight = (style.titleSize * 1.42f).sp, letterSpacing = (style.titleSize * f.tracking).sp, textAlign = posTextAlign(pos))
            }
            if (text.isNotBlank() && pos == style.textPos) {
                val f = protoPromoFont(style.textFont)
                Text(text, color = ProtoText, fontFamily = f.family, fontWeight = if (f.key == "manrope") FontWeight.Normal else f.weight, fontSize = style.textSize.sp,
                    lineHeight = (style.textSize * 1.4f).sp, textAlign = posTextAlign(pos),
                    modifier = Modifier.padding(top = if (pos == style.titlePos) 12.dp else 0.dp))
            }
        }
    }
}

/** Настройка одной надписи: шрифт, размер, место. */
@Composable
internal fun ProtoPromoTextStyleEditor(
    caption: String,
    font: String, size: Int, pos: Int, sizes: IntRange,
    onFont: (String) -> Unit, onSize: (Int) -> Unit, onPos: (Int) -> Unit
) {
    Text(caption, color = ProtoText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
    Text("Шрифт", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        protoPromoFonts.forEach { f ->
            val active = f.key == font
            Box(Modifier.clip(RoundedCornerShape(18.dp)).background(if (active) ProtoGold else Color.Transparent)
                .border(1.dp, if (active) ProtoGold else ProtoBorder, RoundedCornerShape(18.dp))
                .clickable { onFont(f.key) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(f.label, color = if (active) Color.Black else ProtoText, fontFamily = f.family, fontSize = 15.sp, maxLines = 1)
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Размер", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.width(64.dp))
        Slider(value = size.toFloat(), onValueChange = { onSize(it.toInt().coerceIn(sizes)) }, valueRange = sizes.first.toFloat()..sizes.last.toFloat(),
            steps = sizes.last - sizes.first - 1, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = ProtoGold, activeTrackColor = ProtoGold, inactiveTrackColor = ProtoBorder, activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent))
        Text("$size", color = ProtoText, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(32.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Место на картинке", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        ProtoPositionGrid(pos, onPos, caption)
    }
}

/** Сетка 3×3: где на картинке стоит надпись. */
@Composable
internal fun ProtoPositionGrid(selected: Int, onSelect: (Int) -> Unit, caption: String = "") {
    Column(Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, ProtoBorder, RoundedCornerShape(10.dp)).background(ProtoPanel2).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0..2) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (c in 0..2) {
                val i = r * 3 + c
                val active = i == selected
                Box(Modifier.size(width = 34.dp, height = 26.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (active) ProtoGold else ProtoPanel).clickable { onSelect(i) }
                    .semantics { contentDescription = "$caption: место " + (i + 1) },
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 14.dp, height = 3.dp).background(if (active) Color.Black else ProtoMuted, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}
