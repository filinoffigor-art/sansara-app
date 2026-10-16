package ru.sansara.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Запись истории рассылок. */
data class ProtoBroadcast(val date: String, val audience: String, val title: String, val text: String, val count: Int)

/** Рассылка администратора: всем клиентам, агентам или торговым организациям. */
@Composable
internal fun ProtoBroadcastDialog(
    clients: List<ProtoClient>,
    history: List<ProtoBroadcast>,
    onDismiss: () -> Unit,
    onSend: (audience: String, title: String, text: String, recipients: List<ProtoClient>) -> Unit
) {
    var audience by remember { mutableStateOf("Все клиенты") }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val recipients = clients.filter { c ->
        c.status != "Приостановлен" && when (audience) {
            "Агенты" -> c.clientType.contains("Агент", true)
            "Торговые организации" -> !c.clientType.contains("Агент", true)
            else -> true
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false }, containerColor = ProtoPanel,
        title = { Text("Отправить рассылку?", color = ProtoText) },
        text = { Text("«" + title.trim() + "» получат " + protoPlural(recipients.size, "клиент", "клиента", "клиентов") + " (" + audience.lowercase() + ").", color = ProtoMuted) },
        confirmButton = { TextButton(onClick = { confirm = false; onSend(audience, title.trim(), text.trim(), recipients); title = ""; text = "" }) { Text("Отправить", color = ProtoGold) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Отмена", color = ProtoMuted) } }
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = ProtoPanel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f)) {
            Column(Modifier.padding(16.dp)) {
                Text("Рассылка", color = ProtoText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Сообщение придёт клиентам в уведомления приложения и всплывёт на экране телефона.", color = ProtoMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { ProtoChipRow(listOf("Все клиенты", "Агенты", "Торговые организации"), audience) { audience = it } }
                    item { Text("Получателей: " + recipients.size + " (приостановленные не получают)", color = ProtoGoldSoft, fontSize = 13.sp) }
                    item { OutlinedTextField(title, { title = it.take(80) }, label = { Text("Заголовок") }, singleLine = true, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(text, { text = it.take(1000) }, label = { Text("Текст сообщения") }, minLines = 4, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth()) }
                    item { ProtoPrimaryButton("Отправить " + protoPlural(recipients.size, "клиенту", "клиентам", "клиентам"), { confirm = true }, enabled = title.isNotBlank() && text.isNotBlank() && recipients.isNotEmpty()) }
                    item { Text("Без сервера рассылка доходит только до клиентов на этом телефоне; после подключения сервера — на все телефоны.", color = ProtoOrange, fontSize = 12.sp) }
                    if (history.isNotEmpty()) item { Text("История рассылок", color = ProtoText, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp)) }
                    items(history) { b ->
                        ProtoSectionCard {
                            Text(b.title, color = ProtoText, fontWeight = FontWeight.SemiBold)
                            Text(b.date + " · " + b.audience + " · " + b.count + " получ.", color = ProtoMuted, fontSize = 12.sp)
                            Text(b.text, color = ProtoText, fontSize = 13.sp, maxLines = 3, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Закрыть", color = ProtoMuted) }
            }
        }
    }
}

/** Администратор: опубликовать файл новой версии для клиентов и сотрудников. */
@Composable
internal fun ProtoPublishUpdateDialog(current: SansaraUpdateInfo?, onDismiss: () -> Unit, onPublish: (Uri, String) -> Unit, onUnpublish: () -> Unit) {
    var notes by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) picked = uri }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = ProtoPanel,
        title = { Text("Обновление приложения", color = ProtoText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Установлена: " + BuildConfig.VERSION_NAME, color = ProtoMuted, fontSize = 13.sp)
                if (current != null) Text("Опубликована: " + current.versionName + " · " + current.publishedAt, color = ProtoGoldSoft, fontSize = 13.sp)
                OutlinedButton(onClick = { pick.launch("application/vnd.android.package-archive") }, border = BorderStroke(1.dp, ProtoGold), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.UploadFile, null, tint = ProtoGold); Spacer(Modifier.width(6.dp)); Text(if (picked == null) "Выбрать файл APK" else "Файл выбран — заменить", color = ProtoGold)
                }
                OutlinedTextField(notes, { notes = it.take(500) }, label = { Text("Что нового (увидят клиенты)") }, minLines = 3, colors = protoFieldColors(), modifier = Modifier.fillMaxWidth())
                Text("Клиенты и сотрудники получат уведомление и пункт «Обновление». Android попросит один раз разрешить установку из SANSARA и каждый раз подтвердить «Установить».", color = ProtoMuted, fontSize = 12.sp)
                if (current != null) TextButton(onClick = onUnpublish) { Text("Снять публикацию", color = ProtoRed) }
            }
        },
        confirmButton = { TextButton(enabled = picked != null, onClick = { picked?.let { onPublish(it, notes) } }) { Text("Опубликовать", color = ProtoGold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = ProtoMuted) } }
    )
}

/** Клиент или сотрудник: установить опубликованное обновление. */
@Composable
internal fun ProtoUpdateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val update = remember { SansaraUpdates.available(context) }
    var permissionAsked by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = ProtoPanel,
        title = { Text(if (update != null) "Доступна версия " + update.versionName else "Обновлений нет", color = ProtoText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("У вас версия " + BuildConfig.VERSION_NAME, color = ProtoMuted, fontSize = 13.sp)
                if (update != null) {
                    if (update.notes.isNotBlank()) Text("Что нового:\n" + update.notes, color = ProtoText, fontSize = 14.sp)
                    if (!SansaraUpdates.canInstall(context)) Text("Сначала разрешите SANSARA устанавливать обновления: нажмите «Разрешить», включите переключатель и вернитесь в приложение. Это нужно один раз.", color = ProtoOrange, fontSize = 13.sp)
                    Text("Все ваши данные сохранятся.", color = ProtoGoldSoft, fontSize = 13.sp)
                } else Text("Вы используете последнюю версию.", color = ProtoText)
            }
        },
        confirmButton = {
            if (update != null) {
                if (!SansaraUpdates.canInstall(context) && !permissionAsked) TextButton(onClick = { permissionAsked = true; SansaraUpdates.openInstallPermission(context) }) { Text("Разрешить", color = ProtoGold) }
                else TextButton(onClick = {
                    if (!SansaraUpdates.canInstall(context)) SansaraUpdates.openInstallPermission(context)
                    else if (!SansaraUpdates.install(context)) android.widget.Toast.makeText(context, "Не удалось открыть установку", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Установить", color = ProtoGold, fontWeight = FontWeight.Bold) }
            } else TextButton(onClick = onDismiss) { Text("Понятно", color = ProtoGold) }
        },
        dismissButton = { if (update != null) TextButton(onClick = onDismiss) { Text("Позже", color = ProtoMuted) } }
    )
}
