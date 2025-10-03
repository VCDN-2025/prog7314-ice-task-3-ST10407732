package com.example.weatherapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.util.Log


class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    // Biometric variables
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        setupBiometric()

        binding.textView.setOnClickListener {
            // Navigate to SignUp screen
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        // After manual login, you might want to enable biometric here
                        enableBiometricForUser()

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, it.exception?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("BiometricAuth", "onStart called")

        if (firebaseAuth.currentUser != null) {
            Log.d("BiometricAuth", "User is logged in")

            val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val isBiometricEnabled = sharedPref.getBoolean("biometric_enabled", false)
            Log.d("BiometricAuth", "Biometric enabled: $isBiometricEnabled")

            val biometricManager = BiometricManager.from(this)
            val canAuthenticate = biometricManager.canAuthenticate()
            Log.d("BiometricAuth", "Can authenticate result: $canAuthenticate")

            if (isBiometricEnabled && canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                Log.d("BiometricAuth", "Starting biometric authentication")
                biometricPrompt.authenticate(promptInfo)
            } else {
                Log.d("BiometricAuth", "Biometric not enabled or unavailable, going to MainActivity")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        } else {
            Log.d("BiometricAuth", "User not logged in, no action")
        }
    }


    private fun setupBiometric() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    Log.e("BiometricAuth", "Error ($errorCode): $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    Log.d("BiometricAuth", "Authentication succeeded")
                    val intent = Intent(this@SignInActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                    Log.d("BiometricAuth", "Authentication failed")
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use password instead")
            .build()
    }


    private fun enableBiometricForUser() {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("biometric_enabled", true)
            apply()
        }
        Log.d("BiometricAuth", "Biometric enabled flag set to true")
    }

}