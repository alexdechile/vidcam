package com.vidcam.app.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidcam.app.model.Caption
import com.vidcam.app.model.CaptionMath
import com.vidcam.app.model.CaptionStyle

/**
 * Subtítulo en la vista previa. Usa las mismas fracciones del alto del lienzo
 * que la exportación, así el texto queda donde y del tamaño que sale en el MP4.
 */
@Composable
fun CaptionView(
    caption: Caption,
    style: CaptionStyle,
    containerWidth: Float,
    containerHeight: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sizePx = style.sizeFraction * containerHeight
    val fontSize = with(density) { sizePx.toSp() }
    val maxWidth = with(density) { (containerWidth * CaptionMaxWidth).toDp() }
    val horizontalPadding = with(density) { (sizePx * 0.38f).toDp() }
    val verticalPadding = with(density) { (sizePx * 0.22f).toDp() }
    val corner = with(density) { (sizePx * 0.28f).toDp() }
    val background = Color(style.backgroundArgb)
    var blockHeight by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = maxWidth)
                .onSizeChanged { blockHeight = it.height }
                .graphicsLayer {
                    translationY = style.yFraction * containerHeight - blockHeight / 2f
                }
                .then(
                    if (background.alpha > 0f) {
                        Modifier.background(background, RoundedCornerShape(corner))
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        ) {
            Text(
                text = CaptionMath.displayText(caption, style),
                color = Color(style.colorArgb),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = fontSize * 1.18f,
            )
        }
    }
}

/** Mismo ancho máximo que `CaptionBitmaps.MAX_WIDTH_FRACTION`. */
private const val CaptionMaxWidth = 0.86f
