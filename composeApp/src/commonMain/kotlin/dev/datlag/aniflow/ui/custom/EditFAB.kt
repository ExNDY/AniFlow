package dev.datlag.aniflow.ui.custom

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import dev.datlag.aniflow.SharedRes
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun EditFAB(
    displayAdd: Boolean = false,
    bsAvailable: Boolean = false,
    onBS: () -> Unit,
    onRate: () -> Unit,
    onProgress: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        var showOtherFABs by remember { mutableStateOf(false) }

        AnimatedVisibility(
            visible = showOtherFABs,
            enter = scaleIn(
                animationSpec = bouncySpring(),
                transformOrigin = TransformOrigin(1F, 0.5F)
            ) + fadeIn(
                animationSpec = bouncySpring()
            ),
            exit = scaleOut(
                transformOrigin = TransformOrigin(1F, 0.5F)
            ) + fadeOut(
                animationSpec = bouncySpring()
            )
        ) {
            LabelFAB(
                label = "Progress",
                onClick = {
                    showOtherFABs = false
                    onProgress()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null
                )
            }
        }
        AnimatedVisibility(
            visible = showOtherFABs,
            enter = scaleIn(
                animationSpec = bouncySpring(),
                transformOrigin = TransformOrigin(1F, 0.5F)
            ) + fadeIn(
                animationSpec = bouncySpring()
            ),
            exit = scaleOut(
                transformOrigin = TransformOrigin(1F, 0.5F)
            ) + fadeOut(
                animationSpec = bouncySpring()
            )
        ) {
            LabelFAB(
                label = "Rating",
                onClick = {
                    showOtherFABs = false
                    onRate()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null
                )
            }
        }
        AnimatedVisibility(
            visible = showOtherFABs && bsAvailable,
            enter = scaleIn(
                animationSpec = bouncySpring(),
                transformOrigin = TransformOrigin(1F, 0.5F)
            ) + fadeIn(
                animationSpec = bouncySpring()
            ),
            exit = scaleOut(
                transformOrigin = TransformOrigin(1F, 0.5F)
            ) + fadeOut(
                animationSpec = bouncySpring()
            )
        ) {
            LabelFAB(
                label = stringResource(SharedRes.strings.bs),
                onClick = {
                    showOtherFABs = false
                    onBS()
                }
            ) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(SharedRes.images.bs),
                    contentDescription = stringResource(SharedRes.strings.bs),
                    colorFilter = ColorFilter.tint(LocalContentColor.current)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                showOtherFABs = !showOtherFABs
            }
        ) {
            val icon = if (displayAdd) {
                Icons.Default.Add
            } else {
                Icons.Default.Edit
            }

            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun LabelFAB(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onClick,
            tonalElevation = 8.dp,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                modifier = Modifier.padding(4.dp),
                text = label,
                maxLines = 1
            )
        }

        SmallFloatingActionButton(
            modifier = Modifier.padding(end = 4.dp),
            onClick = onClick
        ) {
            icon()
        }
    }
}

private fun <T> bouncySpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)