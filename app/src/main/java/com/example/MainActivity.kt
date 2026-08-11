package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.UserRepository
import com.example.ui.navigation.FoundryAppNavigation
import com.example.ui.theme.FoundryTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val userRepository = UserRepository(applicationContext)
    val chatRepository = com.example.data.ChatRepository(applicationContext)

    setContent {
      val activeUser by userRepository.activeUserFlow.collectAsState(initial = null)
      val themePreference = activeUser?.appearancePreference ?: "DARK"

      FoundryTheme(themePreference = themePreference) {
        Surface(modifier = Modifier.fillMaxSize()) {
          FoundryAppNavigation(
            userRepository = userRepository,
            chatRepository = chatRepository
          )
        }
      }
    }
  }
}

