package com.easyhome.senior.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easyhome.senior.ui.common.repeatingPress
import com.easyhome.senior.ui.common.seniorTap
import com.easyhome.senior.ui.theme.SeniorTheme

enum class KeyboardMode { LETTERS, NUMBERS }

private data class KeySpec(val text: String, val weight: Float = 1f)

/** Ordine alfabetico puro, sempre maiuscolo. */
private val LETTER_ROWS: List<List<KeySpec>> = listOf("ABCDEF", "GHIJKL", "MNOPQR", "STUVWXYZ")
    .map { row -> row.map { KeySpec(it.toString()) } }

/** Tastierino numerico gigante + punteggiatura base. */
private val NUMBER_ROWS: List<List<KeySpec>> = listOf(
    listOf(KeySpec("1"), KeySpec("2"), KeySpec("3"), KeySpec(".")),
    listOf(KeySpec("4"), KeySpec("5"), KeySpec("6"), KeySpec(",")),
    listOf(KeySpec("7"), KeySpec("8"), KeySpec("9"), KeySpec("?")),
    listOf(KeySpec("0", weight = 3f), KeySpec("!")),
)

private val ROW_HEIGHT: Dp = 62.dp
private val ACTION_ROW_HEIGHT: Dp = 60.dp

@Composable
fun AbcdKeyboard(
    mode: KeyboardMode,
    onText: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SeniorTheme.colors
    val rows = if (mode == KeyboardMode.LETTERS) LETTER_ROWS else NUMBER_ROWS
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.keyboardBackground)
            .navigationBarsPadding()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT),
            ) {
                row.forEach { key ->
                    KeyButton(
                        label = key.text,
                        weight = key.weight,
                        background = colors.keyBackground,
                        foreground = colors.onKey,
                        fontSize = if (mode == KeyboardMode.LETTERS) 28.sp else 32.sp,
                        description = key.text,
                        onPress = { onText(key.text) },
                    )
                }
            }
        }
        // Riga azioni: CANCELLA (rosso) e INVIO (verde) ben separati dalle lettere.
        Row(
            Modifier
                .fillMaxWidth()
                .height(ACTION_ROW_HEIGHT),
        ) {
            DeleteKey(onDelete = onDelete)
            KeyButton(
                label = "INVIO",
                weight = 1f,
                background = colors.success,
                foreground = colors.onSuccess,
                fontSize = 24.sp,
                description = "Invio",
                onPress = onEnter,
            )
        }
        // Riga inferiore: SPAZIO occupa il 75% della larghezza.
        Row(
            Modifier
                .fillMaxWidth()
                .height(ACTION_ROW_HEIGHT),
        ) {
            KeyButton(
                label = if (mode == KeyboardMode.LETTERS) "123" else "ABC",
                weight = 1f,
                background = colors.keyBackground,
                foreground = colors.onKey,
                fontSize = 22.sp,
                description = if (mode == KeyboardMode.LETTERS) "Numeri" else "Lettere",
                onPress = onToggleMode,
            )
            KeyButton(
                label = "SPAZIO",
                weight = 3f,
                background = colors.keyBackground,
                foreground = colors.onKey,
                fontSize = 22.sp,
                description = "Spazio",
                onPress = { onText(" ") },
            )
        }
    }
}

@Composable
private fun RowScope.KeyButton(
    label: String,
    weight: Float,
    background: Color,
    foreground: Color,
    fontSize: TextUnit,
    description: String,
    onPress: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val colors = SeniorTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .padding(3.dp)
            .clip(shape)
            // Tasto premuto = colori invertiti: conferma visiva inequivocabile.
            .background(if (pressed) foreground else background)
            .border(2.dp, colors.border, shape)
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .seniorTap(
                slop = 8.dp,
                debounceMs = 120L,
                onPressedChange = { pressed = it },
                onTap = onPress,
            ),
    ) {
        Text(
            text = label,
            color = if (pressed) background else foreground,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.DeleteKey(onDelete: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val colors = SeniorTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(3.dp)
            .clip(shape)
            .background(if (pressed) colors.onDanger else colors.danger)
            .border(2.dp, colors.border, shape)
            .semantics {
                role = Role.Button
                contentDescription = "Cancella"
            }
            // Pressione continua = cancellazione ripetuta.
            .repeatingPress(onPressedChange = { pressed = it }, onAction = onDelete),
    ) {
        Text(
            text = "CANCELLA",
            color = if (pressed) colors.danger else colors.onDanger,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}
