package com.betterx.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ small motion helpers

/** Springy press feedback: the element shrinks slightly while a finger is down. */
fun Modifier.pressScale(source: InteractionSource, pressed: Float = 0.96f): Modifier = composed {
    val isPressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "press",
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Pops an element in with a spring, [index] * 70 ms after it first appears. */
fun Modifier.popIn(index: Int = 0): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        a.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 300f))
    }
    graphicsLayer {
        scaleX = a.value
        scaleY = a.value
        alpha = a.value.coerceIn(0f, 1f)
    }
}

/** Counts from 0 up to [target] the first time it is shown, and animates between values after that. */
@Composable
fun animatedCount(target: Int): Int {
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) { t = target }
    val v by animateIntAsState(t, tween(900, easing = FastOutSlowInEasing), label = "count")
    return v
}

// ------------------------------------------------------------------ swipe to confirm

/** "Swipe right to confirm" control from the reference: drag the round handle all the way across. */
@Composable
fun SwipeToConfirm(
    label: String,
    enabled: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val thumbPx = with(density) { 56.dp.toPx() }
    val padPx = with(density) { 4.dp.toPx() }
    var trackW by remember { mutableIntStateOf(0) }
    val maxPx = (trackW - thumbPx - padPx * 2).coerceAtLeast(1f)
    val offset = remember { Animatable(0f) }
    var confirmed by remember { mutableStateOf(false) }
    val progress = (offset.value / maxPx).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .background(BX.Card)
            .onSizeChanged { trackW = it.width },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Green fill that follows the handle.
        Box(
            Modifier
                .fillMaxHeight()
                .width(with(density) { (offset.value + thumbPx + padPx * 2).toDp() })
                .background(BX.Success.copy(alpha = 0.18f + 0.5f * progress)),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { alpha = (1f - progress * 1.6f).coerceIn(0f, 1f) * (if (enabled) 1f else 0.5f) },
        )
        Box(
            Modifier
                .offset { IntOffset((offset.value + padPx).roundToInt(), 0) }
                .size(56.dp)
                .clip(CircleShape)
                .background(if (enabled) BX.Pill else BX.Chip)
                .pointerInput(enabled, maxPx) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offset.value > maxPx * 0.85f) {
                                    offset.animateTo(maxPx, spring(stiffness = 400f))
                                    if (!confirmed) {
                                        confirmed = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onConfirm()
                                    }
                                } else {
                                    offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 300f))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    ) { change, drag ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + drag).coerceIn(0f, maxPx)) }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (progress > 0.9f) Icons.Filled.Check else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Swipe to confirm",
                tint = if (enabled) Color.Black else Color(0xFF777777),
            )
        }
    }
}

// ------------------------------------------------------------------ success screen

/** Full-screen colour flash with a drawn check mark, like the "Sent to Pavlo" screen in the reference. */
@Composable
fun SuccessOverlay(title: String, subtitle: String, onClose: () -> Unit) {
    val reveal = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        check.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = reveal.value
                val s = 1.08f - 0.08f * reveal.value
                scaleX = s
                scaleY = s
            }
            .background(BX.Success)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(64.dp)) {
                val w = size.width
                val h = size.height
                val full = Path().apply {
                    moveTo(w * 0.10f, h * 0.55f)
                    lineTo(w * 0.40f, h * 0.84f)
                    lineTo(w * 0.90f, h * 0.18f)
                }
                val pm = PathMeasure()
                pm.setPath(full, false)
                val part = Path()
                pm.getSegment(0f, pm.length * check.value, part, true)
                drawPath(part, Color.White, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            "Tap anywhere to close",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
        )
    }
}
