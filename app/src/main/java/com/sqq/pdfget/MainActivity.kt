package com.sqq.pdfget

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sqq.pdfget.ui.theme.PDFGetTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    // 添加一个webView全局变量便于访问
    private var mainWebView: WebView? = null
    
    // 书签数据类
    data class Bookmark(val title: String, val url: String)

    // 加载和保存书签
    private fun loadBookmarks(context: Context): List<Bookmark> {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val bookmarksJson = prefs.getString("bookmarks", "[]") ?: "[]"
        
        return try {
            // 使用自定义方法解析JSON
            parseBookmarksJson(bookmarksJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseBookmarksJson(json: String): List<Bookmark> {
        if (json == "[]") return emptyList()
        
        val result = mutableListOf<Bookmark>()
        // 非常简单的解析方法，仅用于临时替代
        val entries = json.trim('[', ']').split("},{")
        
        for (entry in entries) {
            val cleaned = entry.replace("{", "").replace("}", "")
            val parts = cleaned.split(",")
            if (parts.size >= 2) {
                val title = parts[0].substringAfter("title\":\"").substringBefore("\"")
                val url = parts[1].substringAfter("url\":\"").substringBefore("\"")
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    result.add(Bookmark(title, url))
                }
            }
        }
        
        return result
    }

    private fun saveBookmarks(context: Context, bookmarks: List<Bookmark>) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val bookmarksJson = bookmarksToJson(bookmarks)
        prefs.edit().putString("bookmarks", bookmarksJson).apply()
    }

    private fun bookmarksToJson(bookmarks: List<Bookmark>): String {
        if (bookmarks.isEmpty()) return "[]"
        
        val sb = StringBuilder("[")
        bookmarks.forEachIndexed { index, bookmark ->
            sb.append("{\"title\":\"${bookmark.title}\",\"url\":\"${bookmark.url}\"}")
            if (index < bookmarks.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    // 加载和保存主页
    private fun loadHomepage(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("homepage", "https://www.baidu.com") ?: "https://www.baidu.com"
    }

    private fun saveHomepage(context: Context, url: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("homepage", url).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 添加返回键处理
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 检查WebView是否可以返回
                if (mainWebView?.canGoBack() == true) {
                    // WebView有历史记录，返回上一页
                    mainWebView?.goBack()
                } else {
                    // 没有历史记录，正常退出应用
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        enableEdgeToEdge()
        setContent {
            PDFGetTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(), // 添加这个修饰符
                    // 其他属性保持不变
                ) { innerPadding ->
                    PDFSaverApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @SuppressLint("DefaultLocale", "SetJavaScriptEnabled")
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun PDFSaverApp(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        var url by remember { mutableStateOf("https://www.baidu.com") }
        var scale by remember { mutableFloatStateOf(1.0f) }
        var outputDir by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(true) }
        var isWebViewVisible by remember { mutableStateOf(true) }
        var webView by remember { mutableStateOf<WebView?>(null) }
        var webViewUrl by remember { mutableStateOf("https://www.baidu.com") }
        var isWebPageLoaded by remember { mutableStateOf(false) }
        var reloadCounter by remember { mutableIntStateOf(0) }
        var loadingProgress by remember { mutableIntStateOf(0) }
        
        // 添加前进/后退状态跟踪
        var canGoBack by remember { mutableStateOf(false) }
        var canGoForward by remember { mutableStateOf(false) }

        // 添加桌面模式状态跟踪
        var isDesktopMode by remember { mutableStateOf(false) }
        
        // 添加收藏夹和主页相关状态
        var showBookmarks by remember { mutableStateOf(false) }
        var bookmarks by remember { mutableStateOf(loadBookmarks(context)) }
        var homepage by remember { mutableStateOf(loadHomepage(context)) }
        var showHomepageDialog by remember { mutableStateOf(false) }
        var homepageInput by remember { mutableStateOf("") }

        // 处理存储权限
        val writePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // 文件夹选择器
        val directoryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let {
                outputDir = uri.toString()
                // 获取持久性权限
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }

        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 使用remember仅在相关状态变化时重组导航按钮
            val navButtonsState = remember(isWebViewVisible, canGoBack, canGoForward, isDesktopMode) {
                // 返回一个"状态包"，包含所有导航事件处理器
                object {
                    val onBackClick: () -> Unit = {
                        webView?.goBack()
                        Handler(Looper.getMainLooper()).postDelayed({
                            canGoBack = webView?.canGoBack() ?: false
                            canGoForward = webView?.canGoForward() ?: false
                        }, 100)
                    }
                    
                    val onForwardClick: () -> Unit = {
                        webView?.goForward()
                        Handler(Looper.getMainLooper()).postDelayed({
                            canGoBack = webView?.canGoBack() ?: false
                            canGoForward = webView?.canGoForward() ?: false
                        }, 100)
                    }
                    
                    val onRefreshClick: () -> Unit = {
                        webView?.reload()
                        isLoading = true
                    }
                    
                    val onModeToggle: () -> Unit = {
                        isDesktopMode = !isDesktopMode

                        if (isWebViewVisible) {
                            // 当已加载网页且模式改变时，需要重新加载
                            webView?.apply {
                                clearCache(true)
                                clearHistory()
                            }

                            // 重置WebView来应用新的User-Agent
                            webView?.destroy()
                            webView = null
                            isWebViewVisible = false
                            isWebPageLoaded = false
                            reloadCounter++

                            // 延迟重建WebView
                            Handler(Looper.getMainLooper()).postDelayed({
                                isWebViewVisible = true
                                webViewUrl = url
                                isLoading = true
                            }, 100)
                        }
                    }
                    
                    val onHomeClick: () -> Unit = {
                        // 点击主页按钮，跳转到主页
                        if (isWebViewVisible && homepage.isNotBlank()) {
                            webView?.loadUrl(homepage)
                            url = homepage
                            webViewUrl = homepage
                            isLoading = true
                        }
                    }
                    
                    val onHomeLongClick: () -> Unit = {
                        // 长按主页按钮，显示设置主页对话框
                        homepageInput = homepage
                        showHomepageDialog = true
                    }
                    
                    val onBookmarkClick: () -> Unit = {
                        // 点击收藏夹按钮，显示/隐藏收藏夹列表
                        showBookmarks = !showBookmarks
                    }
                }
            }

            NavigationButtons(
                isWebViewVisible = isWebViewVisible,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isDesktopMode = isDesktopMode,
                onBackClick = navButtonsState.onBackClick,
                onForwardClick = navButtonsState.onForwardClick,
                onRefreshClick = navButtonsState.onRefreshClick,
                onModeToggle = navButtonsState.onModeToggle,
                onHomeClick = navButtonsState.onHomeClick,
                onHomeLongClick = navButtonsState.onHomeLongClick,
                onBookmarkClick = navButtonsState.onBookmarkClick
            )
            
            // 添加收藏夹下拉菜单
            Box(modifier = Modifier.fillMaxWidth()) {
                DropdownMenu(
                    expanded = showBookmarks,
                    onDismissRequest = { showBookmarks = false },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    // 显示收藏夹列表
                    bookmarks.forEachIndexed { _, bookmark ->
                        DropdownMenuItem(
                            text = { Text(bookmark.title) },
                            onClick = {
                                // 点击收藏夹项跳转到相应网址
                                webView?.loadUrl(bookmark.url)
                                url = bookmark.url
                                webViewUrl = bookmark.url
                                isLoading = true
                                showBookmarks = false
                            }
                        )
                    }

                    HorizontalDivider()
                    
                    // 添加收藏功能
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("添加当前页面到收藏夹") 
                            }
                        },
                        onClick = {
                            if (isWebPageLoaded && webViewUrl.isNotBlank()) {
                                // 使用URL最后部分作为标题
                                val urlParts = webViewUrl.split("/")
                                val title = if (urlParts.size > 2) urlParts.last().ifEmpty { webViewUrl } else webViewUrl
                                
                                val updatedBookmarks = bookmarks + Bookmark(title, webViewUrl)
                                bookmarks = updatedBookmarks
                                saveBookmarks(context, updatedBookmarks)
                                
                                Toast.makeText(context, "已添加到收藏夹", Toast.LENGTH_SHORT).show()
                                showBookmarks = false
                            }
                        }
                    )
                }
            }
            
            // WebView显示加载的网页，放在顶部并使用weight让它占据大部分空间
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // 使用权重让WebView占据剩余所有空间
            ) {
                if (isWebViewVisible) {
                    // WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    setSupportZoom(true)
                                    
                                    // 添加这些设置确保不使用缓存
                                    cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                                    domStorageEnabled = true // 启用DOM存储以确保页面完整加载
                                    
                                    // 根据模式设置 User-Agent
                                    userAgentString = if (isDesktopMode) {
                                        // 桌面模式 User-Agent
                                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (HTML, like Gecko) Chrome/99.0.9999.99 Safari/537.36"
                                    } else {
                                        // 移动模式 User-Agent - 使用默认值或指定移动设备 UA
                                        null // 使用默认值
                                    }
                                    
                                    // 其他设置保持不变 - 移到settings.apply块内
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    
                                    // 确保缩放功能可用 - 移到settings.apply块内
                                    builtInZoomControls = true    // 允许内置缩放控件
                                    displayZoomControls = false   // 隐藏默认缩放控件
                                }  // settings.apply 结束
                                
                                setInitialScale((scale * 100).toInt())
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, webUrl: String?) {  // 修改参数名为webUrl
                                        super.onPageFinished(view, webUrl)
                                        isLoading = false
                                        isWebPageLoaded = true
                                        
                                        // 确保缩放设置在页面加载后应用
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            view?.setInitialScale((scale * 100).toInt())
                                        }, 300) // 延迟一点应用缩放，确保页面完全渲染
                                        
                                        // 更新导航状态
                                        canGoBack = view?.canGoBack() ?: false
                                        canGoForward = view?.canGoForward() ?: false
                                        
                                        // 更新地址栏
                                        webUrl?.let {
                                            webViewUrl = it
                                            // 现在可以正确访问外部的url变量
                                            url = it
                                        }
                                    }
                                    
                                    // 添加URL监听以捕获导航变化
                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(view: WebView?, webUrl: String?): Boolean {
                                        if (webUrl == null) return false
                                        
                                        try {
                                            // 检查是否是自定义协议
                                            if (!webUrl.startsWith("http://") && !webUrl.startsWith("https://")) {
                                                // 对于baiduboxapp协议特殊处理
                                                if (webUrl.startsWith("baiduboxapp://")) {
                                                    Toast.makeText(context, "此链接需要在百度App中打开", Toast.LENGTH_SHORT).show()
                                                    return true
                                                }
                                                
                                                // 尝试用系统应用打开其他自定义协议
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    context.startActivity(intent)
                                                    return true
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "无法打开此链接: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    return true
                                                }
                                            }
                                            
                                            // 更新地址栏
                                            webViewUrl = webUrl
                                            url = webUrl
                                            
                                            // 让WebView继续加载http和https链接
                                            return false
                                        } catch (e: Exception) {
                                            return false
                                        }
                                    }

                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        super.onReceivedError(view, request, error)
                                        // 仅处理主页面错误，忽略资源加载错误
                                        if (request?.isForMainFrame == true) {
                                            isLoading = false
                                            isWebPageLoaded = false
                                            
                                            // 安全地获取错误描述
                                            val errorDescription =
                                                error?.description?.toString() ?: "未知错误"

                                            Toast.makeText(context, "加载失败: $errorDescription", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadingProgress = newProgress
                                    }
                                }
                                
                                // 确保第一次加载是全新的
                                mainWebView = this
                                webView = this
                                if (webViewUrl.isNotEmpty()) {
                                    loadUrl(webViewUrl)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            // 只更新缩放，不重新加载
                            view.setInitialScale((scale * 100).toInt())
                        }
                    )
                    
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            CircularProgressIndicator()
                            Text("加载中: $loadingProgress%")
                        }
                    }
                }
            }

            // 控制面板
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 网址输入行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("输入网址") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    // 创建圆形按钮
                    FloatingActionButton(
                        onClick = {
                            if (url.isNotBlank()) {
                                // 确保完全清除旧WebView
                                webView?.apply {
                                    clearHistory()
                                    clearCache(true)
                                    clearFormData()
                                }
                                webView?.destroy()
                                webView = null
                                isWebViewVisible = false
                                isWebPageLoaded = false
                                reloadCounter++
                                
                                // 延迟重建WebView
                                Handler(Looper.getMainLooper()).postDelayed({
                                    isWebViewVisible = true
                                    webViewUrl = url
                                    isLoading = true
                                }, 100)
                            }
                        },
                        modifier = Modifier.padding(top = 6.dp),  // 添加顶部内边距让按钮下移一点
                        containerColor = Color(0xFF4B964B), // 按钮颜色
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "加载网页",
                            tint = Color.White // 白色图标
                        )
                    }
                }

                // 缩放设置、输出目录和保存PDF按钮合并到一行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 缩放部分 - 增加权重
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(0.40f)
                    ) {
                        // 减号按钮 - 设置固定尺寸
                        IconButton(
                            onClick = {
                                val newScale = maxOf(0.5f, scale - 0.1f)
                                scale = newScale
                                webView?.setInitialScale((scale * 100).toInt())
                            },
                            modifier = Modifier
                                .size(40.dp)  // 设置固定大小
                                .padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "减小缩放",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(24.dp)  // 设置图标固定大小
                            )
                        }
                        
                        // 缩放百分比文本
                        Text(
                            text = "${(scale * 100).toInt()}%", 
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        // 加号按钮 - 使用与减号按钮相同的尺寸
                        IconButton(
                            onClick = {
                                val newScale = minOf(2.0f, scale + 0.1f)
                                scale = newScale
                                webView?.setInitialScale((scale * 100).toInt())
                            },
                            modifier = Modifier
                                .size(40.dp)  // 设置与减号按钮相同的固定大小
                                .padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "增加缩放",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(24.dp)  // 设置图标固定大小
                            )
                        }
                    }
                    
                    // 目录选择部分
                    Button(
                        onClick = { 
                            if (!writePermission.status.isGranted) {
                                writePermission.launchPermissionRequest()
                            } else {
                                directoryLauncher.launch(null)
                            }
                        },
                        modifier = Modifier.weight(0.30f)
                    ) {
                        Text(if (outputDir.isEmpty()) "选择目录" else "已选择")
                    }
                    
                    // 保存为PDF按钮
                    Button(
                        onClick = { 
                            if (webView != null && isWebPageLoaded && outputDir.isNotEmpty()) {
                                createWebPrintJob(context, webView)
                            } else {
                                Toast.makeText(context, "请先加载网页并选择输出目录", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(0.30f),
                        enabled = isWebPageLoaded && outputDir.isNotEmpty()
                    ) {
                        Text("保存PDF")
                    }
                }
            }
        }
        
        // 添加设置主页对话框
        if (showHomepageDialog) {
            AlertDialog(
                onDismissRequest = { showHomepageDialog = false },
                title = { Text("设置主页") },
                text = {
                    OutlinedTextField(
                        value = homepageInput,
                        onValueChange = { homepageInput = it },
                        label = { Text("输入网址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (homepageInput.isNotBlank()) {
                                homepage = homepageInput
                                saveHomepage(context, homepageInput)
                                showHomepageDialog = false
                                Toast.makeText(context, "主页已设置", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showHomepageDialog = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }

    // 创建打印任务将WebView内容保存为PDF
    private fun createWebPrintJob(context: Context, webView: WebView?) {
        if (webView == null) return

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "PDFGet_${SimpleDateFormat("yyyyMMdd_msys", Locale.getDefault()).format(Date())}"

        val adapter = webView.createPrintDocumentAdapter(jobName)
        
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        try {
            printManager.print(jobName, adapter, attributes)
            
            Toast.makeText(context, "正在创建PDF...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e("PDFGet", "创建PDF失败", e)
            Toast.makeText(context, "创建PDF失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 提取导航按钮为单独的可组合函数
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun NavigationButtons(
        isWebViewVisible: Boolean,
        canGoBack: Boolean,
        canGoForward: Boolean,
        isDesktopMode: Boolean,
        onBackClick: () -> Unit,
        onForwardClick: () -> Unit,
        onRefreshClick: () -> Unit,
        onModeToggle: () -> Unit,
        onHomeClick: () -> Unit,
        onHomeLongClick: () -> Unit,
        onBookmarkClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主页按钮 - 统一大小为40dp
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .combinedClickable(
                        onClick = onHomeClick,
                        onLongClick = onHomeLongClick
                    )
                    .padding(8.dp), // 减小内边距，让图标看起来与其他按钮一致
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "主页",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // 收藏夹按钮 - 统一大小为40dp
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "收藏夹",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // 后退按钮 - 统一大小为40dp
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBackClick,
                    enabled = isWebViewVisible && canGoBack,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "后退",
                        tint = if (isWebViewVisible && canGoBack) Color(0xFF2196F3) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // 前进按钮 - 统一大小为40dp
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onForwardClick,
                    enabled = isWebViewVisible && canGoForward,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "前进",
                        tint = if (isWebViewVisible && canGoForward) Color(0xFF2196F3) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // 刷新按钮 - 统一大小为40dp
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onRefreshClick,
                    enabled = isWebViewVisible,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = if (isWebViewVisible) Color(0xFF2196F3) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // 桌面/移动模式切换按钮 - 统一大小为40dp
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onModeToggle,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isDesktopMode) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = "电脑版",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "手机版",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}