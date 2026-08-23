package dev1503.browseevo.ui.widgets

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import dev1503.browseevo.EvoWebViewTab
import dev1503.browseevo.data.HistoryManager
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class EvoWebViewWrapper(
    val activity: Activity,
) {
    val geckoView: GeckoView = GeckoView(activity)

    val formalTabs: MutableList<EvoWebViewTab> = mutableListOf()

    val geckoRuntime: GeckoRuntime by lazy { initRuntime(activity) }

    companion object {
        @Volatile
        private var runtimeInstance: GeckoRuntime? = null

        @Volatile
        var userAgentOverride: String? = null
            private set

        private fun initRuntime(activity: Activity): GeckoRuntime {
            if (runtimeInstance != null) return runtimeInstance!!
            synchronized(this) {
                if (runtimeInstance != null) return runtimeInstance!!
                val runtime = GeckoRuntime.create(activity)
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
                        userAgentOverride = "$ua BrowseEvo/$appVersion"
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

    init {
        geckoView.apply {
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
