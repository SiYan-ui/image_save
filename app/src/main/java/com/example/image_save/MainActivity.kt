package com.example.image_save

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(primary = Color(0xFF9A6848), background = Color(0xFFFFF8F1))) {
                val controller = remember { HomeController(applicationContext) }
                HomeScreen(controller)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(ReminderReceiver.NOTIFICATION_CHANNEL, "养育事件", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}

class HomeController(context: Context) {
    private val repository = CareRepository(PreferencesStateStore(context))
    private val scheduler = ReminderScheduler(context)
    var state by mutableStateOf(repository.snapshot())
        private set

    fun refresh() {
        state = repository.reconcile(System.currentTimeMillis(), autoConvertDueEvents = false)
        scheduler.schedule(state)
    }

    fun create(type: CareType, name: String) {
        state = repository.createProfile(type, name, System.currentTimeMillis())
        scheduler.schedule(state)
    }

    fun confirm(eventId: String) {
        state = repository.confirmDirect(eventId, System.currentTimeMillis())
        scheduler.schedule(state)
    }

    fun repay(debtId: String, amountCents: Long): String? = runCatching {
        state = repository.repay(debtId, amountCents, System.currentTimeMillis())
        null
    }.getOrElse { it.message ?: "偿还失败" }
}

@Composable
fun HomeScreen(controller: HomeController) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable { mutableStateOf<CareType?>(null) }
    var showBills by rememberSaveable { mutableStateOf(false) }
    var selectedDebt by remember { mutableStateOf<Debt?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) controller.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (controller.state.profile == null) {
        EmptyHome(onAdd = { showPicker = true })
    } else {
        MainHome(
            state = controller.state,
            onBill = { showBills = true },
            onDebt = { selectedDebt = it },
            onConfirm = controller::confirm
        )
    }

    if (showPicker) ObjectPickerDialog(onDismiss = { showPicker = false }, onSelected = { selectedType = it; showPicker = false })
    selectedType?.let { type -> NameDialog(type, onDismiss = { selectedType = null }, onCreate = { name -> controller.create(type, name); selectedType = null }) }
    if (showBills) BillDialog(controller.state, onDismiss = { showBills = false }, onRepay = { selectedDebt = it })
    selectedDebt?.let { debt -> RepayDialog(debt, onDismiss = { selectedDebt = null }, onConfirm = { amount -> controller.repay(debt.id, amount); selectedDebt = null }) }
}

@Composable
private fun EmptyHome(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.livingroom), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.56f)))
        Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("给未来的陪伴，存下一点准备", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("开始养育一个虚拟的小伙伴，让每次存钱都有一个理由")
            Spacer(Modifier.height(28.dp))
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "创建养育对象") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainHome(state: AppState, onBill: () -> Unit, onDebt: (Debt) -> Unit, onConfirm: (String) -> Unit) {
    val profile = state.profile ?: return
    val configs = ProductCatalog.forType(profile.type)
    Scaffold(topBar = {
        TopAppBar(title = { Text(profile.name) }, actions = {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 12.dp)) {
                Text("累计存钱", style = MaterialTheme.typography.labelSmall)
                Text(formatMoney(state.totalSavedCents), fontWeight = FontWeight.Bold)
                Text("借贷 ${formatMoney(state.outstandingDebtCents)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { state.debts.firstOrNull()?.let(onDebt) })
            }
            IconButton(onClick = onBill) { Icon(Icons.Default.ReceiptLong, contentDescription = "账单") }
        })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Image(painterResource(profile.type.background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.64f)))
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(avatar(profile.type), style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.size(16.dp))
                            Column { Text(profile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("今天也在被好好照顾") }
                        }
                    }
                }
                item { Text("消耗品状态", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(state.inventories) { inventory ->
                    val config = configs.first { it.id == inventory.itemId }
                    val event = state.events.firstOrNull { it.itemId == inventory.itemId }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(config.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("库存 ${inventory.quantity} ${config.unit} · 预计 ${formatDate(inventory.depletedAt)} 用完")
                            if (event != null) {
                                Spacer(Modifier.height(8.dp))
                                Text("需要存入 ${formatMoney(event.amountCents)} 购买 1${config.unit}", color = MaterialTheme.colorScheme.primary)
                                Button(onClick = { onConfirm(event.id) }, modifier = Modifier.padding(top = 8.dp)) { Text("已存钱") }
                            }
                        }
                    }
                }
                item { Text("消耗品会按真实时间消耗；离开 App 时也会继续计算。", color = Color.DarkGray) }
            }
        }
    }
}

@Composable
private fun ObjectPickerDialog(onDismiss: () -> Unit, onSelected: (CareType) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择养育对象") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CareType.values().forEach { type ->
                OutlinedButton(onClick = { onSelected(type) }, modifier = Modifier.fillMaxWidth()) { Text("${avatar(type)}  ${type.label}") }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun NameDialog(type: CareType, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable(type) { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("给${type.label}取个名字") }, text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("名称") }) }, confirmButton = { Button(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("开始养育") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun BillDialog(state: AppState, onDismiss: () -> Unit, onRepay: (Debt) -> Unit) {
    val debtById = state.debts.associateBy { it.id }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("账单", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(420.dp)) {
                    items(state.records.sortedByDescending { it.createdAt }) { record ->
                        val debt = record.debtId?.let(debtById::get)
                        Column(Modifier.fillMaxWidth().background(Color(0xFFF8F3EE), RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Text("${formatDate(record.createdAt)}  ${record.itemName}", fontWeight = FontWeight.Bold)
                            Text("${record.type.label}  ${formatMoney(record.amountCents)}")
                            if (debt != null && record.type == RecordType.VIRTUAL_DEBT) {
                                Text("已偿还 ${formatMoney(debt.repaidAmountCents)} / ${formatMoney(debt.originalAmountCents)}")
                                if (debt.remainingAmountCents > 0) TextButton(onClick = { onRepay(debt) }) { Text("偿还") }
                            }
                        }
                    }
                    if (state.records.isEmpty()) item { Text("还没有账单") }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("关闭") }
            }
        }
    }
}

@Composable
private fun RepayDialog(debt: Debt, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var amount by rememberSaveable(debt.id) { mutableStateOf("") }
    val cents = amount.toCentsOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("偿还借贷") }, text = {
        Column { Text("剩余借贷 ${formatMoney(debt.remainingAmountCents)}"); Spacer(Modifier.height(8.dp)); OutlinedTextField(amount, { amount = it }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), label = { Text("本次偿还金额") }) }
    }, confirmButton = { Button(onClick = { cents?.let(onConfirm) }, enabled = cents != null && cents in 1..debt.remainingAmountCents) { Text("确认偿还") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

private fun avatar(type: CareType): String = when (type) { CareType.BABY -> "👶"; CareType.CAT -> "🐱"; CareType.DOG -> "🐶" }
private fun formatMoney(cents: Long): String = String.format(Locale.CHINA, "¥%.2f", cents / 100.0)
private fun formatDate(time: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(time))
private fun String.toCentsOrNull(): Long? = trim().toBigDecimalOrNull()?.let { runCatching { it.movePointRight(2).longValueExact() }.getOrNull() }
