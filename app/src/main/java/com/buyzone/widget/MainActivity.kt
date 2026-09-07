package com.buyzone.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.buyzone.widget.data.TokenStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WelcomeScreen(TokenStore(this)) }
    }
}

@Composable
private fun WelcomeScreen(tokenStore: TokenStore) {
    var token by remember { mutableStateOf(tokenStore.read().orEmpty()) }
    var saved by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Buy Zone Widget", style = MaterialTheme.typography.headlineMedium)
        Text("Add a widget from your home screen, then configure its ticker and strategy.")
        OutlinedTextField(
            value = token,
            onValueChange = { token = it; saved = false },
            label = { Text("Tiingo API token") }
        )
        Button(onClick = { tokenStore.save(token); saved = true }) { Text("Save token") }
        if (saved) Text("Token saved securely on this device.")
    }
}
