package com.example.image_save

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class Role(
    val name: String,
    val imageRes: Int?,
    val emoji: String?
)

sealed class DialogState {
    data object None : DialogState()
    data object SelectRole : DialogState()
    data class Confirm(val role: Role) : DialogState()
    data class Naming(val role: Role) : DialogState()
}

private val roles = listOf(
    Role(name = "男婴", imageRes = R.drawable.baby_m1, emoji = null),
    Role(name = "女婴", imageRes = null, emoji = "👧"),
    Role(name = "猫猫", imageRes = null, emoji = "🐱"),
    Role(name = "狗狗", imageRes = null, emoji = "🐶")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrowingPiggyBankTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
private fun GrowingPiggyBankTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFFC77C48),
            onPrimary = Color.White,
            secondary = Color(0xFF8C6B55),
            background = Color(0xFFFFF9F4),
            surface = Color(0xFFFFF9F4)
        ),
        content = content
    )
}

@Composable
fun HomeScreen() {
    var dialogState: DialogState by remember { mutableStateOf(DialogState.None) }
    var selectedRole by remember { mutableStateOf<Role?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val role = selectedRole
            if (role == null) {
                AddRoleButton(
                    onClick = { dialogState = DialogState.SelectRole },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                SelectedRole(
                    role = role,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    when (val state = dialogState) {
        DialogState.None -> Unit
        DialogState.SelectRole -> SelectRoleDialog(
            onDismiss = { dialogState = DialogState.None },
            onRoleSelected = { role -> dialogState = DialogState.Confirm(role) }
        )
        is DialogState.Confirm -> ConfirmRoleDialog(
            role = state.role,
            onCancel = { dialogState = DialogState.None },
            onConfirm = { dialogState = DialogState.Naming(state.role) }
        )
        is DialogState.Naming -> NamingDialog(
            role = state.role,
            onCancel = { dialogState = DialogState.None },
            onConfirm = { name ->
                selectedRole = state.role.copy(name = name)
                dialogState = DialogState.None
            }
        )
    }
}

@Composable
private fun AddRoleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(150.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .clickable(onClick = onClick)
            .background(Color(0xFFD79568)),
        contentAlignment = Alignment.Center
    ) {
        // Smaller highlight disc creates a soft, warm inner-glow effect.
        Box(
            modifier = Modifier
                .size(138.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xFFE8B28C)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(64.dp)
            ) {
                val stroke = 8.dp.toPx()
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val arm = 24.dp.toPx()
                drawLine(
                    color = Color(0xFF8C573C),
                    start = androidx.compose.ui.geometry.Offset(centerX - arm, centerY),
                    end = androidx.compose.ui.geometry.Offset(centerX + arm, centerY),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF8C573C),
                    start = androidx.compose.ui.geometry.Offset(centerX, centerY - arm),
                    end = androidx.compose.ui.geometry.Offset(centerX, centerY + arm),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun SelectRoleDialog(
    onDismiss: () -> Unit,
    onRoleSelected: (Role) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column {
                Text(
                    text = "请选择你的养育对象",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 20.dp, end = 16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(roles) { role ->
                        RoleOption(
                            role = role,
                            onClick = { onRoleSelected(role) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleOption(role: Role, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = role.name,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        RoleVisual(
            role = role,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFEFE2)),
            emojiSize = 48
        )
    }
}

@Composable
private fun ConfirmRoleDialog(
    role: Role,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            Text(
                text = "确定要养育${role.name}吗",
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun NamingDialog(
    role: Role,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(role) { mutableStateOf(role.name) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "请输入名称") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trimmedName) },
                enabled = trimmedName.isNotEmpty()
            ) {
                Text(text = "确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "取消")
            }
        }
    )
}

@Composable
private fun SelectedRole(role: Role, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = role.name, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        RoleVisual(
            role = role,
            modifier = Modifier.size(180.dp),
            emojiSize = 48
        )
    }
}

@Composable
private fun RoleVisual(
    role: Role,
    modifier: Modifier,
    emojiSize: Int
) {
    val imageRes = role.imageRes
    if (imageRes != null) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = role.name,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = role.emoji.orEmpty(),
                fontSize = emojiSize.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
