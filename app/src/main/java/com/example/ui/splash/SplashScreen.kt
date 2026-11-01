package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GMKBackgroundDark
import com.example.ui.theme.GMKElectricBlue
import com.example.ui.theme.GMKTextDim
import com.example.ui.theme.GMKTextMuted
import com.example.ui.theme.GMKTextWhite
import com.example.ui.theme.GMKVerifiedGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        scale.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        // Target splash timing: ~1.4 seconds for a swift, responsive app start
        delay(1400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F1A2E), // Subtle dark blue cinematic glow in center
                        Color(0xFF070B13),
                        Color(0xFF030509)
                    ),
                    radius = 1200f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("splash_screen_container")
    ) {
        // Center Branding
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // GMK App Icon / Logo with sleek rounded glass container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
            ) {
                // Outer subtle blue glow
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GMKElectricBlue.copy(alpha = glowAlpha * 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_gmk_launcher),
                    contentDescription = "GMK Player Logo",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.dp, GMKElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Brand Title
            Text(
                text = "GMK Player",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GMKTextWhite,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle: P L A Y E R tracking
            Text(
                text = "P  L  A  Y  E  R",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GMKElectricBlue,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Green Shield with Checkmark Badge
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0E1A22),
                border = androidx.compose.foundation.BorderStroke(1.dp, GMKVerifiedGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(GMKVerifiedGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verified",
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Powered by ",
                        fontSize = 12.sp,
                        color = GMKTextMuted
                    )
                    Text(
                        text = "GMK Studios",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GMKTextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Elegant Loading Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(180.dp)
            ) {
                Text(
                    text = "Loading...",
                    fontSize = 12.sp,
                    color = GMKTextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = GMKElectricBlue,
                    trackColor = Color(0xFF131D2D)
                )
            }
        }

        // Bottom Copyright Footer
        Text(
            text = "© 2026 GMK Studios",
            fontSize = 11.sp,
            color = GMKTextDim,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
