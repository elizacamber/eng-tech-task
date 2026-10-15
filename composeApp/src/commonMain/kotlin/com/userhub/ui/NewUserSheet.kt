package com.userhub.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.userhub.domain.model.DataError
import com.userhub.presentation.AddUserState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewUserSheet(
    state: AddUserState,
    onDismiss: () -> Unit,
    onInputChanged: () -> Unit,
    onSubmit: (name: String, email: String) -> Unit
) {
    // saveable so typed values survive rotation
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    val nameValid = name.trim().length >= 2
    val emailValid = email.contains("@") && email.substringAfter("@").contains(".")

    // 422 errors from the server, shown under the field they belong to
    val fieldErrors = (state.error as? DataError.InvalidFields)?.messages.orEmpty()
    val nameError = when {
        name.isNotEmpty() && !nameValid -> "Name must be at least 2 characters"
        else -> fieldErrors["name"]?.let { "Name $it" }
    }
    val emailError = when {
        email.isNotEmpty() && !emailValid -> "Enter a valid email address"
        else -> fieldErrors["email"]?.let { "Email $it" }
    }
    val generalError = state.error?.let(::generalMessage)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text("Add user", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onInputChanged()
                },
                label = { Text("Full name") },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    // editing clears a server error such as "has already been taken"
                    onInputChanged()
                },
                label = { Text("Email") },
                isError = emailError != null,
                supportingText = { emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (generalError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = generalError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSubmit(name.trim(), email.trim()) },
                enabled = nameValid && emailValid && !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSubmitting) "Adding…" else "Add user")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// Errors that don't belong to a single field. Field errors are shown on the fields.
private fun generalMessage(error: DataError): String? = when (error) {
    DataError.Offline -> "No internet connection. Please check your network and try again."
    DataError.Unauthorized -> "The app isn't authorised to add users. The API token may be invalid."
    is DataError.InvalidFields -> null
    is DataError.Server -> "Couldn't create user (${error.status}). Please try again."
    DataError.UnreadableResponse -> "User was created, but the response couldn't be read. Pull to refresh."
}
