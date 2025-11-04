package com.chatting.ui.activitys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.chatting.domain.AccountManager
import com.service.api.ApiService
import com.chatting.ui.MyApp
import com.chatting.ui.screens.ProfileScreen
import com.chatting.ui.theme.MyComposeApplicationTheme
import com.chatting.ui.viewmodel.ProfileViewModel
import com.chatting.ui.viewmodel.ProfileViewModelFactory

class ProfileActivity : ComponentActivity() {

    private val accountManager: AccountManager by lazy { (application as MyApp).accountManager }

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(application, accountManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyComposeApplicationTheme {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateUp = { finish() }
                )
            }
        }
    }
}