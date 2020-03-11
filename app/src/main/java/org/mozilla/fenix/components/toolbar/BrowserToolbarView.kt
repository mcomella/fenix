/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.browser_toolbar_popup_window.view.copy
import kotlinx.android.synthetic.main.browser_toolbar_popup_window.view.paste
import kotlinx.android.synthetic.main.browser_toolbar_popup_window.view.paste_and_go
import kotlinx.android.synthetic.main.component_browser_top_toolbar.view.app_bar
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.session.Session
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.support.ktx.android.util.dpToFloat
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.customtabs.CustomTabToolbarIntegration
import org.mozilla.fenix.customtabs.CustomTabToolbarMenu
import org.mozilla.fenix.ext.bookmarkStorage
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.search.toolbar.setScrollFlagsForTopToolbar
import org.mozilla.fenix.theme.ThemeManager
import androidx.asynclayoutinflater.view.AsyncLayoutInflater

interface BrowserToolbarViewInteractor {
    fun onBrowserToolbarPaste(text: String)
    fun onBrowserToolbarPasteAndGo(text: String)
    fun onBrowserToolbarClicked()
    fun onBrowserToolbarMenuItemTapped(item: ToolbarMenu.Item)
    fun onTabCounterClicked()
    fun onBrowserMenuDismissed(lowPrioHighlightItems: List<ToolbarMenu.Item>)
}

class BrowserToolbarView(
    private val container: ViewGroup,
    private val shouldUseBottomToolbar: Boolean,
    private val interactor: BrowserToolbarViewInteractor,
    private val customTabSession: Session?,
    private val lifecycleOwner: LifecycleOwner
) : LayoutContainer {

    override val containerView: View?
        get() = container

    private val settings = container.context.settings()

    lateinit var toolbarIntegration: ToolbarIntegration

    lateinit var view: BrowserToolbar

    lateinit var layout: View

    @LayoutRes
    private val toolbarLayout = when {
        settings.shouldUseBottomToolbar -> R.layout.component_bottom_browser_toolbar
        else -> R.layout.component_browser_top_toolbar
    }

    @Suppress("ComplexMethod", "LongMethod")
    private fun setupView() {
        val isCustomTabSession = customTabSession != null

        view.display.setOnUrlLongClickListener {
            onUrlLongClick(view, isCustomTabSession)
        }

        with(container.context) {
            val sessionManager = components.core.sessionManager

            view.apply {
                setScrollFlagsForTopToolbar()

                elevation = TOOLBAR_ELEVATION.dpToFloat(resources.displayMetrics)

                if (!isCustomTabSession) {
                    display.setUrlBackground(getDrawable(R.drawable.search_url_background))
                }

                display.onUrlClicked = {
                    interactor.onBrowserToolbarClicked()
                    false
                }

                display.progressGravity = if (shouldUseBottomToolbar) {
                    DisplayToolbar.Gravity.TOP
                } else {
                    DisplayToolbar.Gravity.BOTTOM
                }

                val primaryTextColor = ContextCompat.getColor(
                    container.context,
                    ThemeManager.resolveAttribute(R.attr.primaryText, container.context)
                )
                val secondaryTextColor = ContextCompat.getColor(
                    container.context,
                    ThemeManager.resolveAttribute(R.attr.secondaryText, container.context)
                )
                val separatorColor = ContextCompat.getColor(
                    container.context,
                    ThemeManager.resolveAttribute(R.attr.toolbarDivider, container.context)
                )

                display.colors = display.colors.copy(
                    text = primaryTextColor,
                    securityIconSecure = primaryTextColor,
                    securityIconInsecure = primaryTextColor,
                    menu = primaryTextColor,
                    hint = secondaryTextColor,
                    separator = separatorColor,
                    trackingProtection = primaryTextColor
                )

                display.hint = context.getString(R.string.search_hint)
            }

            val menuToolbar: ToolbarMenu
            if (isCustomTabSession) {
                menuToolbar = CustomTabToolbarMenu(
                    this,
                    sessionManager,
                    customTabSession?.id,
                    shouldReverseItems = !shouldUseBottomToolbar,
                    onItemTapped = {
                        interactor.onBrowserToolbarMenuItemTapped(it)
                    }
                )
            } else {
                menuToolbar = DefaultToolbarMenu(
                    context = this,
                    hasAccountProblem = components.backgroundServices.accountManager.accountNeedsReauth(),
                    shouldReverseItems = !shouldUseBottomToolbar,
                    onItemTapped = { interactor.onBrowserToolbarMenuItemTapped(it) },
                    lifecycleOwner = lifecycleOwner,
                    sessionManager = sessionManager,
                    bookmarksStorage = bookmarkStorage
                )
                view.display.setMenuDismissAction {
                    interactor.onBrowserMenuDismissed(menuToolbar.getLowPrioHighlightItems())
                    view.invalidateActions()
                }
            }

            toolbarIntegration = if (customTabSession != null) {
                CustomTabToolbarIntegration(
                    this,
                    view,
                    menuToolbar,
                    customTabSession.id,
                    isPrivate = customTabSession.private
                )
            } else {
                DefaultToolbarIntegration(
                    this,
                    view,
                    menuToolbar,
                    ShippedDomainsProvider().also { it.initialize(this) },
                    components.core.historyStorage,
                    components.core.sessionManager,
                    sessionId = null,
                    isPrivate = sessionManager.selectedSession?.private ?: false,
                    interactor = interactor,
                    engine = components.core.engine
                )
            }
        }
    }

    private fun onUrlLongClick(toolbarView: View, isCustomTabSession: Boolean): Boolean {
        val clipboard = toolbarView.context.components.clipboardHandler
        val customView = LayoutInflater.from(toolbarView.context)
            .inflate(R.layout.browser_toolbar_popup_window, null)
        val popupWindow = PopupWindow(
            customView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            toolbarView.context.resources.getDimensionPixelSize(R.dimen.context_menu_height),
            true
        )

        val selectedSession = container.context.components.core.sessionManager.selectedSession

        popupWindow.elevation =
            toolbarView.context.resources.getDimension(R.dimen.mozac_browser_menu_elevation)

        customView.paste.isVisible = !clipboard.text.isNullOrEmpty() && !isCustomTabSession
        customView.paste_and_go.isVisible =
            !clipboard.text.isNullOrEmpty() && !isCustomTabSession

        customView.copy.setOnClickListener {
            popupWindow.dismiss()
            if (isCustomTabSession) {
                clipboard.text = customTabSession?.url
            } else {
                clipboard.text = selectedSession?.url
            }

            FenixSnackbar.make(toolbarView, Snackbar.LENGTH_SHORT)
                .setText(toolbarView.context.getString(R.string.browser_toolbar_url_copied_to_clipboard_snackbar))
                .show()
        }

        customView.paste.setOnClickListener {
            popupWindow.dismiss()
            interactor.onBrowserToolbarPaste(clipboard.text!!)
        }

        customView.paste_and_go.setOnClickListener {
            popupWindow.dismiss()
            interactor.onBrowserToolbarPasteAndGo(clipboard.text!!)
        }

        popupWindow.showAsDropDown(
            toolbarView,
            toolbarView.context.resources.getDimensionPixelSize(R.dimen.context_menu_x_offset),
            0,
            Gravity.START
        )

        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun update(state: BrowserFragmentState) {
        // Intentionally leaving this as a stub for now since we don't actually want to update currently
    }

    fun expand() {
        if (!settings.shouldUseBottomToolbar) {
            view.app_bar?.setExpanded(true)
        }
    }

    /**
     * Performs all actions that refer to the BrowserToolbarView that don't require user action.
     * They are set in the callback because we need to ensure phones execute the code before
     * we are done inflating.
     */
    fun loadAsync(action: View.() -> Unit) {
        AsyncLayoutInflater(container.context).inflate(toolbarLayout, container) { finalView, _, parent ->
            view = finalView.findViewById((R.id.toolbar))
            with(parent!!) {
                addView(finalView)
                setupView()
                action()
            }
        }
    }

    companion object {
        private const val TOOLBAR_ELEVATION = 16
    }
}
