package com.example.image_save

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(state: AppState) {
        if (state.profile == null) return
        state.inventories.forEach { inventory ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_ITEM_ID, inventory.itemId)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                inventory.itemId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, inventory.depletedAt, pending)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, inventory.depletedAt, pending)
            }
        }
    }

    companion object { const val EXTRA_ITEM_ID = "item_id" }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(ReminderScheduler.EXTRA_ITEM_ID) ?: return
        val repository = CareRepository(PreferencesStateStore(context.applicationContext))
        val before = repository.snapshot()
        val beforeCount = before.records.size
        val after = repository.reconcile(System.currentTimeMillis(), autoConvertDueEvents = true)
        ReminderScheduler(context.applicationContext).schedule(after)
        val created = after.records.drop(beforeCount).firstOrNull { it.type == RecordType.VIRTUAL_DEBT && it.itemId == itemId }
        val config = ProductCatalog.get(itemId)
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${before.profile?.name ?: "养育对象"}的${config.name}用完了")
            .setContentText("已自动记为虚拟借贷，打开 App 查看详情")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        if (created != null && (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED)) {
            NotificationManagerCompat.from(context).notify(itemId.hashCode(), notification)
        }
    }

    companion object { const val NOTIFICATION_CHANNEL = "care_events" }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = CareRepository(PreferencesStateStore(context.applicationContext))
            val state = repository.reconcile(System.currentTimeMillis(), autoConvertDueEvents = true)
            ReminderScheduler(context.applicationContext).schedule(state)
        }
    }
}
