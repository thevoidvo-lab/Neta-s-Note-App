package com.example.ui.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.calculatePan
import androidx.compose.ui.input.pointer.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.CanvasBackground
import com.example.data.NoteElement

@Composable
fun CanvasView(
    elements: List<NoteElement>,
    background: CanvasBackground,
    selectedElementId: String?,
    showGrid: Boolean,
    snapToGrid: Boolean,
    canvasPanX: Float,
    canvasPanY: Float,
    canvasZoom: Float,
    onCanvasTransform: (panX: Float, panY: Float, zoom: Float) -> Unit,
    onSelectElement: (String?) -> Unit,
    onEditElementText: (NoteElement) -> Unit,
    onLongPressElement: (NoteElement) -> Unit,
    onMoveElement: (id: String, dx: Float, dy: Float) -> Unit,
    onTransformElement: (id: String, scaleDelta: Float, rotationDelta: Float) -> Unit,
    onGestureEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(elements, snapToGrid) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val contentX = (down.position.x - canvasPanX) / canvasZoom
                    val contentY = (down.position.y - canvasPanY) / canvasZoom
                    val hitElement = elements.any { element ->
                        !element.isHidden &&
                            contentX >= element.x &&
                            contentX <= element.x + element.width * density * element.scale &&
                            contentY >= element.y &&
                            contentY <= element.y + element.height * density * element.scale
                    }

                    if (!hitElement) {
                        var moved = false
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.any { it.pressed }
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            if (pressed && (pan != Offset.Zero || zoom != 1f)) {
                                moved = true
                                onCanvasTransform(pan.x, pan.y, zoom)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        if (!moved) onSelectElement(null)
                    } else {
                        // Do not consume element touches: ElementView owns the drag gesture.
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    }
                }
            }
    ) {
        // Background remains stationary while the content layer pans and zooms.
        CanvasBackgroundLayer(background = background)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = canvasPanX
                    translationY = canvasPanY
                    scaleX = canvasZoom
                    scaleY = canvasZoom
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            if (showGrid) {
                GridOverlay(gridSize = 32.dp.value)
            }

            val sortedElements = elements.sortedBy { it.zIndex }
            for (elem in sortedElements) {
                val isSelected = elem.id == selectedElementId
                ElementView(
                    element = elem,
                    isSelected = isSelected,
                    onSelect = { onSelectElement(elem.id) },
                    onDoubleTap = { onEditElementText(elem) },
                    onLongPress = { onLongPressElement(elem) },
                    onMove = { dx, dy -> onMoveElement(elem.id, dx / canvasZoom, dy / canvasZoom) },
                    onTransform = { scaleDelta, rotDelta ->
                        onTransformElement(elem.id, scaleDelta, rotDelta)
                    },
                    onGestureEnd = onGestureEnd
                )
            }
        }
    }
}

@Composable
fun CanvasBackgroundLayer(background: CanvasBackground) {
    val baseModifier = Modifier
        .fillMaxSize()
        .alpha(background.opacity)
        .then(if (background.blurRadius > 0f) Modifier.blur(background.blurRadius.dp) else Modifier)

    when (background.type) {
        "IMAGE" -> {
            if (background.imageUri.isNotBlank()) {
                AsyncImage(
                    model = background.imageUri,
                    contentDescription = "Canvas Background Image",
                    contentScale = ContentScale.Crop,
                    modifier = baseModifier
                )
            } else {
                Box(modifier = baseModifier.background(parseHexColor(background.colorHex, MaterialTheme.colorScheme.background)))
            }
        }
        "GRADIENT" -> {
            val c1 = parseHexColor(background.colorHex, MaterialTheme.colorScheme.background)
            val c2 = parseHexColor(background.secondaryColorHex, MaterialTheme.colorScheme.surface)
            Box(
                modifier = baseModifier.background(
                    brush = Brush.verticalGradient(colors = listOf(c1, c2))
                )
            )
        }
        else -> {
            Box(modifier = baseModifier.background(parseHexColor(background.colorHex, MaterialTheme.colorScheme.background)))
        }
    }
}

@Composable
fun GridOverlay(gridSize: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

        var x = 0f
        while (x < w) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
            x += gridSize
        }

        var y = 0f
        while (y < h) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            y += gridSize
        }
    }
}
