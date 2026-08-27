package dev1503.browseevo.ui.widgets

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import dev1503.browseevo.EvoWebViewTab
import dev1503.browseevo.Utils
import dev1503.browseevo.data.HistoryManager
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class EvoWebViewWrapper(
    val activity: Activity,
) {
    val geckoView: GeckoView = GeckoView(activity)

    // 最近一次触摸/鼠标事件的屏幕坐标，用于在长按/右键弹出菜单时定位到真实指针位置。
    var lastPointerX: Int = 0
        private set
    var lastPointerY: Int = 0
        private set

    val formalTabs: MutableList<EvoWebViewTab> = mutableListOf()

    val geckoRuntime: GeckoRuntime by lazy { initRuntime(activity) }


    companion object {
        @Volatile
        private var runtimeInstance: GeckoRuntime? = null

        @Volatile
        var userAgentOverride: String? = null
            private set

        private var mobileUserAgent: String? = null
        private var desktopUserAgent: String? = null

        fun resetRuntime() {
            synchronized(this) {
                runtimeInstance = null
            }
        }

        fun rebuildUserAgent() {
            val mobile = mobileUserAgent ?: return
            val pcMode = Utils.isPcMode()
            userAgentOverride = if (pcMode) (desktopUserAgent ?: mobile) else mobile
        }

        private fun initRuntime(activity: Activity): GeckoRuntime {
            if (runtimeInstance != null) return runtimeInstance!!
            synchronized(this) {
                if (runtimeInstance != null) return runtimeInstance!!
                val settings = GeckoRuntimeSettings.Builder()
                    .preferredColorScheme(Utils.getPreferredColorScheme())
                    .build()
                val runtime = GeckoRuntime.create(activity, settings)
                val defaultUa = GeckoSession.getDefaultUserAgent()
                if (!defaultUa.isNullOrEmpty()) {
                    val appVersion = try {
                        @Suppress("DEPRECATION")
                        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                    val ua = defaultUa.replaceFirst(
                        Regex("""Android [\d.]+;"""),
                        "Android ${Build.VERSION.RELEASE};"
                    )
                    mobileUserAgent = "$ua BrowseEvo/$appVersion"
                    desktopUserAgent = mobileUserAgent!!.replaceFirst(
                        Regex("""Android [\d.]+;"""),
                        "Windows NT 10.0;"
                    ).replace(
                        Regex("""\bMobile\b"""),
                        "x64"
                    )
                    userAgentOverride = if (Utils.isPcMode()) desktopUserAgent else mobileUserAgent
                }
                runtimeInstance = runtime
                return runtime
            }
        }
    }

    private val historyManager = HistoryManager(activity)

    private var activeTabIndex: Int = -1

    val activeTab: EvoWebViewTab?
        get() = formalTabs.getOrNull(activeTabIndex)

    val view: View
        get() = geckoView

    var onTabChanged: ((index: Int, tab: EvoWebViewTab?) -> Unit)? = null
    var onTabCreated: (() -> Unit)? = null
    var onTitleChanged: ((String?) -> Unit)? = null
    var onNavigationStateChanged: (() -> Unit)? = null
    var onPageStarted: ((String?) -> Unit)? = null
    var onPageStopped: ((Boolean) -> Unit)? = null
    var onProgressChanged: ((Int) -> Unit)? = null
    var onLoadingChanged: ((Boolean) -> Unit)? = null
    var onExternalSchemeRequested: ((String) -> Unit)? = null
    var onNavigateRequested: ((String) -> Unit)? = null
    var onDownloadRequested: ((url: String, filename: String?, contentLength: Long) -> Unit)? = null
    var onContextMenu: ((screenX: Int, screenY: Int, element: GeckoSession.ContentDelegate.ContextElement) -> Unit)? = null

    init {
        geckoView.apply {
            // 触摸：记录按下手指的真实屏幕坐标；返回 false 让 GeckoView 继续正常处理触摸。
            setOnTouchListener { _, event ->
                lastPointerX = event.rawX.toInt()
                lastPointerY = event.rawY.toInt()
                false
            }
            // 鼠标悬停：记录光标位置。
            setOnHoverListener { _, event ->
                lastPointerX = event.rawX.toInt()
                lastPointerY = event.rawY.toInt()
                false
            }
            // 鼠标按键（右键等）：记录按下位置。
            setOnGenericMotionListener { _, event ->
                lastPointerX = event.rawX.toInt()
                lastPointerY = event.rawY.toInt()
                false
            }
        }
    }

    private fun reportActiveLoadingState() {
        onLoadingChanged?.invoke(activeTab?.isCurrentSessionLoading == true)
    }

    fun getTabCount(): Int = formalTabs.size

    fun getActiveTabIndex(): Int = activeTabIndex

    fun createTab(insertIndex: Int = formalTabs.size): EvoWebViewTab {
        val tab = EvoWebViewTab(activity, geckoRuntime, historyManager)
        tab.onNewTabRequested = { session ->
            val sourceIndex = formalTabs.indexOf(tab).coerceAtLeast(0)
            val newTab = createTab(sourceIndex + 1)
            newTab.adoptSession(session)
            newTab.pushSession(session)
            geckoView.setSession(session)
        }
        tab.onTitleChanged = { title -> if (tab === activeTab) onTitleChanged?.invoke(title) }
        tab.onContextMenu = { screenX, screenY, element ->
            if (tab === activeTab) onContextMenu?.invoke(screenX, screenY, element)
        }
        tab.onNavigationStateChanged = { if (tab === activeTab) onNavigationStateChanged?.invoke() }
        tab.onNavigationRequested = { session, uri ->
            geckoView.setSession(session)
            session.loadUri(uri)
        }
        tab.onPageStarted = { url -> if (tab === activeTab) onPageStarted?.invoke(url) }
        tab.onPageStopped = { success -> if (tab === activeTab) onPageStopped?.invoke(success) }
        tab.onProgressChanged = { progress -> if (tab === activeTab) onProgressChanged?.invoke(progress) }
        tab.onLoadingChanged = { loading -> if (tab === activeTab) onLoadingChanged?.invoke(loading) }
        tab.onExternalSchemeRequested = { uri -> if (tab === activeTab) onExternalSchemeRequested?.invoke(uri) }
        tab.onNavigateRequested = { value -> if (tab === activeTab) onNavigateRequested?.invoke(value) }
        tab.onDownloadRequested = { url, filename, length ->
            if (tab === activeTab) onDownloadRequested?.invoke(url, filename, length)
        }
        formalTabs.add(insertIndex.coerceIn(0, formalTabs.size), tab)
        activeTabIndex = formalTabs.indexOf(tab)
        reportActiveLoadingState()
        onTabChanged?.invoke(activeTabIndex, tab)
        onTabCreated?.invoke()
        return tab
    }

    fun switchToTab(index: Int) {
        val tab = formalTabs.getOrNull(index) ?: return
        activeTabIndex = index
        val session = tab.currentSession
        if (session != null) {
            geckoView.setSession(session)
        }
        reportActiveLoadingState()
        onTabChanged?.invoke(index, tab)
    }

    fun closeTab(index: Int) {
        val tab = formalTabs.getOrNull(index) ?: return
        tab.getAllSessions().forEach { session ->
            session.close()
        }
        tab.close()
        formalTabs.removeAt(index)

        if (formalTabs.isEmpty()) {
            activeTabIndex = -1
        } else if (activeTabIndex >= formalTabs.size) {
            activeTabIndex = formalTabs.lastIndex
        } else if (index <= activeTabIndex) {
            activeTabIndex = (activeTabIndex - 1).coerceAtLeast(0)
        }
        val currentSession = activeTab?.currentSession
        if (currentSession != null) {
            geckoView.setSession(currentSession)
        }
        if (formalTabs.isEmpty()) {
            createTab()
            loadBuiltInPage()
        }
        reportActiveLoadingState()
    }

    fun goToUrl(url: String) {
        val tab = activeTab ?: createTab()
        val newSession = tab.createSession()
        tab.markPending(newSession)
        tab.pushSession(newSession)
        geckoView.setSession(newSession)
        newSession.loadUri(url)
    }

    fun loadUrl(url: String) {
        val tab = activeTab ?: createTab()
        tab.loadUrl(url)
        val session = tab.currentSession ?: return
        geckoView.setSession(session)
    }

    fun goBack(): Boolean {
        val tab = activeTab ?: return false
        val current = tab.currentSession ?: return false
        val previousSession = tab.goBack() ?: return false
        if (previousSession != current) {
            geckoView.setSession(previousSession)
        }
        return true
    }

    fun canGoBack(): Boolean = activeTab?.canGoBack == true

    fun goForward(): Boolean {
        val tab = activeTab ?: return false
        val current = tab.currentSession ?: return false
        val nextSession = tab.goForward() ?: return false
        if (nextSession != current) {
            geckoView.setSession(nextSession)
        }
        return true
    }

    fun canGoForward(): Boolean = activeTab?.canGoForward == true

    fun closeActiveTab() {
        val index = activeTabIndex
        if (index < 0) return
        closeTab(index)
        onTabChanged?.invoke(activeTabIndex, activeTab)
    }

    fun reload() {
        activeTab?.reload()
    }

    fun updateAllSessionsUserAgent() {
        for (tab in formalTabs) {
            tab.updateAllSessionsUserAgent()
        }
    }

    val isLoading: Boolean
        get() = activeTab?.isCurrentSessionLoading == true

    fun stopLoading() {
        activeTab?.stopLoading()
    }

    fun capturePixels(): GeckoResult<Bitmap>? = geckoView.capturePixels()

    fun loadBuiltInPage() {
        loadUrl("evo://index")
    }
}
