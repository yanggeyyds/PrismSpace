@file:Suppress("MagicNumber")
package com.yzddmr6.prismspace.prism.compose.component

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Adb
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WbSunny

/**
 * Icon path data retained for compatibility with tests and legacy vector references.
 * Not every key has a matching PrismIcons member.
 */
object PrismIconPaths {
    val D: Map<String, String> = mapOf(
        "home" to "M3 11 12 3l9 8M5 10v10h14V10M9 20v-6h6v6",
        "grid" to "M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z",
        "swap" to "M7 4v13M4 7l3-3 3 3M17 20V7M14 17l3 3 3-3",
        "set" to "M21.23 10.21L21.23 13.79L18.48 13.26L17.47 15.69L19.79 17.26L17.26 19.79L15.69 17.47L13.26 18.48L13.79 21.23L10.21 21.23L10.74 18.48L8.31 17.47L6.74 19.79L4.21 17.26L6.53 15.69L5.52 13.26L2.77 13.79L2.77 10.21L5.52 10.74L6.53 8.31L4.21 6.74L6.74 4.21L8.31 6.53L10.74 5.52L10.21 2.77L13.79 2.77L13.26 5.52L15.69 6.53L17.26 4.21L19.79 6.74L17.47 8.31L18.48 10.74ZM9.00 12.00A3.00 3.00 0 1 0 15.00 12.00A3.00 3.00 0 1 0 9.00 12.00",
        "git" to "M9 19c-4 1.4-4-2-6-2.5M15 21v-3.5c0-1 .1-1.4-.5-2 2.8-.3 5.5-1.4 5.5-6a4.6 4.6 0 0 0-1.3-3.2 4.3 4.3 0 0 0-.1-3.2s-1-.3-3.4 1.3a11.6 11.6 0 0 0-6 0C6.3 1.1 5.3 1.4 5.3 1.4a4.3 4.3 0 0 0-.1 3.2A4.6 4.6 0 0 0 4 7.8c0 4.6 2.7 5.7 5.5 6-.6.6-.6 1.2-.5 2V21",
        "info" to "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18M12 11v5M12 8h.01",
        "alert" to "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18M12 8v5M12 16h.01",
        "shield" to "M12 3 5 6v6c0 4 3 7 7 9 4-2 7-5 7-9V6z",
        "phone" to "M5 4h4l2 5-3 2a12 12 0 0 0 5 5l2-3 5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2",
        "key" to "M4.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0M10.4 10.4 19 19M16 17l2 2M13.8 19.2l2 2",
        "droid" to "M7 9a5 5 0 0 1 10 0v7H7zM4 12h3M17 12h3M9 4 8 2M15 4l1-2",
        "wrench" to "M14.5 5.5a4 4 0 0 1-5 5l-5 5 2 2 5-5a4 4 0 0 0 5-5l-2.5 2.5-2-2 2.5-2.5",
        "power" to "M12 3v9M7 6a8 8 0 1 0 10 0",
        "download" to "M12 3v12M8 11l4 4 4-4M5 21h14",
        "play" to "M7 4v16l13-8z",
        "trash" to "M4 6h16M9 6V4h6v2M6 6l1 14h10l1-14",
        "snow" to "M12 2v20M4 7l16 10M20 7 4 17M12 2 9 5M12 2l3 3M12 22l-3-3M12 22l3-3",
        "sun" to "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8M12 2v2M12 20v2M4 12H2M22 12h-2M5 5 4 4M19 19l1 1M19 5l1-1M5 19l-1 1",
        "file" to "M6 2h8l4 4v16H6zM14 2v4h4",
        "img" to "M4 4h16v16H4zM8 10a2 2 0 1 0 0-4 2 2 0 0 0 0 4M4 16l5-5 4 4 3-3 4 4",
        "add" to "M12 5v14M5 12h14",
        "search" to "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14M20 20l-4-4",
        "dots" to "M12 5h.01M12 12h.01M12 19h.01",
        "copy" to "M9 9h11v11H9zM5 15H4V4h11v1",
        "chev" to "M9 6l6 6-6 6",
        "check" to "M5 12l5 5 9-10",
        "refresh" to "M4 12a8 8 0 0 1 13.7-5.7L20 8M20 4v4h-4",
        "store" to "M4 7h16l-1 13H5zM9 7a3 3 0 0 1 6 0",
        "adb" to "M5 11a7 7 0 0 1 14 0v5H5zM8 11 6 8M16 11l2-3M9 16v3M15 16v3",
        "box" to "M3 7l9-4 9 4-9 4zM3 7v10l9 4M21 7v10l-9 4",
        "close" to "M6 6l12 12M18 6 6 18",
        "files" to "M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
        "swaph" to "M3 8h13M13 5l3 3-3 3M21 16H8M11 13l-3 3 3 3",
        "sort" to "M7 4v16M7 4 4 7M7 4l3 3M14 7h7M14 12h5M14 17h3",
    )
}

/** Material aliases. Keep PrismIconPaths.D intact for test guards. */
object PrismIcons {
    val Home: ImageVector get() = Icons.Outlined.Home
    val Grid: ImageVector get() = Icons.Outlined.GridView
    val Files: ImageVector get() = Icons.Outlined.Folder
    val Settings: ImageVector get() = Icons.Outlined.Settings
    val Gear: ImageVector get() = Settings
    val Search: ImageVector get() = Icons.Outlined.Search
    val Dots: ImageVector get() = Icons.Filled.MoreVert
    val Add: ImageVector get() = Icons.Filled.Add
    val Check: ImageVector get() = Icons.Filled.Check
    val Chev: ImageVector get() = Icons.Filled.KeyboardArrowRight
    val Close: ImageVector get() = Icons.Filled.Close
    val Trash: ImageVector get() = Icons.Outlined.Delete
    val Play: ImageVector get() = Icons.Filled.PlayArrow
    val Snow: ImageVector get() = Icons.Outlined.AcUnit
    val Sun: ImageVector get() = Icons.Outlined.WbSunny
    val Info: ImageVector get() = Icons.Outlined.Info
    val Git: ImageVector get() = Icons.Filled.Code
    val Shield: ImageVector get() = Icons.Outlined.Shield
    val Phone: ImageVector get() = Icons.Outlined.PhoneAndroid
    val Droid: ImageVector get() = Icons.Filled.Android
    val Key: ImageVector get() = Icons.Outlined.VpnKey
    val Wrench: ImageVector get() = Icons.Outlined.Build
    val Power: ImageVector get() = Icons.Outlined.PowerSettingsNew
    val Download: ImageVector get() = Icons.Outlined.FileDownload
    val Refresh: ImageVector get() = Icons.Outlined.Refresh
    val Box: ImageVector get() = Icons.Outlined.Inventory2
    val Store: ImageVector get() = Icons.Outlined.Storefront
    val Adb: ImageVector get() = Icons.Outlined.Adb
    val Img: ImageVector get() = Icons.Outlined.Image
    val File: ImageVector get() = Icons.Outlined.InsertDriveFile
    val FileOpen: ImageVector get() = Icons.Outlined.FileOpen
    val Sort: ImageVector get() = Icons.Outlined.Sort
    val SwapH: ImageVector get() = Icons.Outlined.SwapHoriz
    val Alert: ImageVector get() = Icons.Outlined.ErrorOutline
}
