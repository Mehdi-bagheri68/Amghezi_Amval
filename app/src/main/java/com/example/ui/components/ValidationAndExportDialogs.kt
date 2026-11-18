package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AuditFilterTab
import com.example.model.PersianTextUtils
import com.example.ui.ValidationBlockAlert
import com.example.ui.theme.CoralBorder
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralReject
import com.example.ui.theme.CoralText
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldConfirm
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoOnContainer
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.util.ExcelExportPayload

@Composable
fun ValidationBlockDialog(
    alert: ValidationBlockAlert,
    onDismiss: () -> Unit,
    onFilterPendingClicked: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("validation_block_dialog"),
        containerColor = SlateSurface,
        icon = {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = CoralReject,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "تایید نهایی مسدود شد — نواقص ممیزی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CoralText
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "برای دریافت خروجی اکسل نهایی (.xlsx)، تمامی ردیف‌ها باید تعیین وضعیت شده و برای اقلام «تایید شده»، «مغایرت» و «مازاد» عکس دارایی پیوست شده باشد:",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextSecondary
                )

                Surface(
                    color = CoralContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CoralBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (alert.pendingCount > 0) {
                            Text(
                                text = "• ${PersianTextUtils.toPersianDigits(alert.pendingCount)} قلم هنوز «در انتظار بررسی» است.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CoralText
                            )
                        }
                        if (alert.missingPhotoCount > 0) {
                            Text(
                                text = "• ${PersianTextUtils.toPersianDigits(alert.missingPhotoCount)} قلم فاقد «عکس اجباری دارایی» است.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CoralText
                            )
                        }
                        if (alert.incompleteDiscrepancyCount > 0) {
                            Text(
                                text = "• ${PersianTextUtils.toPersianDigits(alert.incompleteDiscrepancyCount)} قلم مغایرت فاقد کد اموال اصلاحی است.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CoralText
                            )
                        }
                    }
                }

                if (alert.problematicDescriptions.isNotEmpty()) {
                    Text(
                        text = "اقلام نیازمند بررسی:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(alert.problematicDescriptions) { idx, desc ->
                            Text(
                                text = "${PersianTextUtils.toPersianDigits(idx + 1)}. $desc",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (alert.pendingCount > 0) {
                Button(
                    onClick = {
                        onFilterPendingClicked()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "نمایش اقلام در انتظار بررسی",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "متوجه شدم",
                    style = MaterialTheme.typography.labelLarge,
                    color = SlateTextSecondary
                )
            }
        }
    )
}

@Composable
fun ExcelExportReadyDialog(
    payload: ExcelExportPayload,
    onSaveToDeviceClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSheetTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .testTag("excel_export_ready_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = SlateSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldConfirm,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "تایید نهایی و تولید فایل اکسل (.xlsx) انجام شد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldText
                        )
                        Text(
                            text = "نام فایل: ${payload.fileName} • شامل ۲ شیت «گزارش نهایی» و «خلاصه وضعیت»",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateTextSecondary
                        )
                    }
                }

                // Sheet selector preview tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSheetTab == 0,
                        onClick = { selectedSheetTab = 0 },
                        label = {
                            Text(
                                text = "شیت ۱: گزارش نهایی (${PersianTextUtils.toPersianDigits(payload.finalReportRows.size - 1)} ردیف)",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    FilterChip(
                        selected = selectedSheetTab == 1,
                        onClick = { selectedSheetTab = 1 },
                        label = {
                            Text(
                                text = "شیت ۲: خلاصه وضعیت",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                // Sheet Data Preview Box
                val activeRows = if (selectedSheetTab == 0) payload.finalReportRows else payload.summarySheetRows
                Surface(
                    color = SlateSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 300.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(activeRows) { rowIdx, row ->
                            val isHeader = rowIdx == 0
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isHeader) IndigoContainer else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                row.forEach { cell ->
                                    Text(
                                        text = PersianTextUtils.toPersianDigits(cell),
                                        style = if (isHeader) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isHeader) IndigoOnContainer else SlateTextPrimary,
                                        modifier = Modifier.width(135.dp)
                                    )
                                }
                            }
                            if (isHeader) {
                                HorizontalDivider(color = SlateBorder)
                            }
                        }
                    }
                }

                // Download & Share Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSaveToDeviceClicked,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("save_xlsx_device_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldConfirm,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "دانلود و ذخیره اکسل (.xlsx)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, payload.shareUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "ارسال فایل اکسل اموال‌گردانی")
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_xlsx_btn"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اشتراک‌گذاری",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "بستن پنجره",
                        style = MaterialTheme.typography.labelLarge,
                        color = SlateTextSecondary
                    )
                }
            }
        }
    }
}
