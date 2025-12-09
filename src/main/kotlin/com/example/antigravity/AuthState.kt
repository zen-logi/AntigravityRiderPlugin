package com.example.antigravity

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.example.antigravity.AuthState",
    storages = [Storage("AntigravityAuthState.xml")],
)
class AuthState : PersistentStateComponent<AuthState> {
    var accessToken: String = ""

    override fun getState(): AuthState {
        return this
    }

    override fun loadState(state: AuthState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: AuthState
            get() = ApplicationManager.getApplication().getService(AuthState::class.java)
    }
}
