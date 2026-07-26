package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class PeepData(
    val id: Int,
    var x: Float,
    var yOffsetFraction: Float, // 0.0f (bottom) to 0.35f (above bottom)
    val heightPx: Float,
    val speed: Float, // px per second
    val direction: Float, // 1f (right) or -1f (left)
    val walkPhaseOffset: Float,
    val variant: Int, // 0..4 character styles
    val alpha: Float,
    val strokeWidthPx: Float
)

@Composable
fun CrowdCanvas(
    modifier: Modifier = Modifier,
    peepCount: Int = 18,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    var frameTimeNanos by remember { mutableLongStateOf(0L) }

    // Animation frame ticker
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        var lastTime = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (lastTime != 0L) {
                    frameTimeNanos = nanos
                }
                lastTime = nanos
            }
        }
    }

    // State for crowd peeps
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    val peeps = remember { mutableStateOf<List<PeepData>>(emptyList()) }

    // Initialize or adjust peeps when canvas size changes
    fun initPeeps(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        val random = Random(42)
        val list = mutableListOf<PeepData>()
        for (i in 0 until peepCount) {
            val dir = if (random.nextBoolean()) 1f else -1f
            val hPx = with(density) { (55 + random.nextInt(35)).dp.toPx() }
            val strokeW = with(density) { (2.5f + random.nextFloat() * 1.5f).dp.toPx() }
            val speed = 40f + random.nextFloat() * 60f
            val x = random.nextFloat() * w
            val yFrac = random.nextFloat() * 0.30f
            val alpha = 0.25f + random.nextFloat() * 0.40f
            val variant = random.nextInt(5)
            val phase = random.nextFloat() * (2f * PI.toFloat())

            list.add(
                PeepData(
                    id = i,
                    x = x,
                    yOffsetFraction = yFrac,
                    heightPx = hPx,
                    speed = speed,
                    direction = dir,
                    walkPhaseOffset = phase,
                    variant = variant,
                    alpha = alpha,
                    strokeWidthPx = strokeW
                )
            )
        }
        // Sort by yOffsetFraction so peeps further back are drawn behind
        list.sortBy { it.yOffsetFraction }
        peeps.value = list
    }

    // Update positions continuously
    val timeSec = frameTimeNanos / 1_000_000_000f

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        if (size.width != canvasWidth || size.height != canvasHeight) {
            canvasWidth = size.width
            canvasHeight = size.height
            initPeeps(canvasWidth, canvasHeight)
        }

        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val currentPeeps = peeps.value
        if (currentPeeps.isEmpty()) return@Canvas

        // Delta calculation for movement
        currentPeeps.forEach { peep ->
            // Move peep
            if (enabled) {
                peep.x += peep.direction * peep.speed * 0.016f // approx 60fps delta
                // Wrap around edges
                if (peep.direction > 0 && peep.x > w + 60f) {
                    peep.x = -60f
                } else if (peep.direction < 0 && peep.x < -60f) {
                    peep.x = w + 60f
                }
            }

            val baseY = h - (peep.yOffsetFraction * h * 0.5f) - 10f
            val cycle = (timeSec * (peep.speed / 15f) + peep.walkPhaseOffset)
            val legSwing = sin(cycle) * 0.45f
            val bobbing = abs(sin(cycle * 2f)) * (peep.heightPx * 0.06f)

            drawPeep(
                peep = peep,
                baseX = peep.x,
                baseY = baseY - bobbing,
                legSwing = legSwing,
                timeSec = timeSec
            )
        }
    }
}

private fun DrawScope.drawPeep(
    peep: PeepData,
    baseX: Float,
    baseY: Float,
    legSwing: Float,
    timeSec: Float
) {
    val h = peep.heightPx
    val w = h * 0.35f
    val color = Color.White.copy(alpha = peep.alpha)
    val strokeWidth = peep.strokeWidthPx
    val dir = peep.direction

    withTransform({
        translate(left = baseX, top = baseY)
        scale(scaleX = dir, scaleY = 1f, pivot = Offset.Zero)
    }) {
        val headRadius = h * 0.12f
        val neckY = -h * 0.72f
        val headCenterY = neckY - headRadius
        val hipY = -h * 0.38f
        val footY = 0f

        // 1. Head
        drawCircle(
            color = color,
            radius = headRadius,
            center = Offset(0f, headCenterY)
        )

        // Variant accessories
        when (peep.variant) {
            1 -> { // Cap / Hat
                drawPath(
                    path = Path().apply {
                        moveTo(-headRadius * 1.2f, headCenterY - headRadius * 0.2f)
                        lineTo(headRadius * 1.5f, headCenterY - headRadius * 0.2f)
                        lineTo(headRadius * 0.8f, headCenterY - headRadius * 0.9f)
                        lineTo(-headRadius * 0.8f, headCenterY - headRadius * 0.9f)
                        close()
                    },
                    color = color
                )
            }
            2 -> { // Headphones
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(-headRadius * 1.2f, headCenterY - headRadius * 1.3f),
                    size = Size(headRadius * 2.4f, headRadius * 2.4f),
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(
                    color = color,
                    radius = headRadius * 0.35f,
                    center = Offset(headRadius * 0.8f, headCenterY)
                )
            }
            3 -> { // Backpack
                drawRoundRect(
                    color = color,
                    topLeft = Offset(-w * 0.7f, neckY + h * 0.05f),
                    size = Size(w * 0.45f, h * 0.28f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }
        }

        // 2. Torso (Body)
        drawLine(
            color = color,
            start = Offset(0f, neckY),
            end = Offset(0f, hipY),
            strokeWidth = strokeWidth * 1.3f,
            cap = StrokeCap.Round
        )

        // 3. Legs
        val leg1Angle = legSwing
        val leg2Angle = -legSwing

        val knee1X = sin(leg1Angle) * (h * 0.2f)
        val knee1Y = hipY + cos(leg1Angle) * (h * 0.2f)
        val foot1X = sin(leg1Angle) * (h * 0.38f)
        val foot1Y = footY

        val knee2X = sin(leg2Angle) * (h * 0.2f)
        val knee2Y = hipY + cos(leg2Angle) * (h * 0.2f)
        val foot2X = sin(leg2Angle) * (h * 0.38f)
        val foot2Y = footY

        // Leg 1 (Back leg)
        drawLine(
            color = color.copy(alpha = color.alpha * 0.8f),
            start = Offset(0f, hipY),
            end = Offset(knee1X, knee1Y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color.copy(alpha = color.alpha * 0.8f),
            start = Offset(knee1X, knee1Y),
            end = Offset(foot1X, foot1Y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Leg 2 (Front leg)
        drawLine(
            color = color,
            start = Offset(0f, hipY),
            end = Offset(knee2X, knee2Y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(knee2X, knee2Y),
            end = Offset(foot2X, foot2Y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 4. Arms (Swinging opposite to legs)
        val arm1Angle = -legSwing * 0.8f
        val arm2Angle = legSwing * 0.8f

        val hand1X = sin(arm1Angle) * (h * 0.25f)
        val hand1Y = neckY + h * 0.08f + cos(arm1Angle) * (h * 0.25f)

        val hand2X = sin(arm2Angle) * (h * 0.25f)
        val hand2Y = neckY + h * 0.08f + cos(arm2Angle) * (h * 0.25f)

        // Arm 1
        drawLine(
            color = color.copy(alpha = color.alpha * 0.8f),
            start = Offset(0f, neckY + h * 0.08f),
            end = Offset(hand1X, hand1Y),
            strokeWidth = strokeWidth * 0.9f,
            cap = StrokeCap.Round
        )

        // Arm 2
        drawLine(
            color = color,
            start = Offset(0f, neckY + h * 0.08f),
            end = Offset(hand2X, hand2Y),
            strokeWidth = strokeWidth * 0.9f,
            cap = StrokeCap.Round
        )
    }
}
