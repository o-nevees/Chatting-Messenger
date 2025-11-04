package com.chatting.ui.activitys

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.chatting.ui.theme.MyComposeApplicationTheme
import com.chatting.ui.utils.CountryList
import com.chatting.ui.utils.SecurePrefs
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

class PhoneRegistrationActivity : ComponentActivity() {

    private lateinit var phoneNumberUtil: PhoneNumberUtil
    private val viewModel: PhoneRegistrationViewModel by viewModels { PhoneRegistrationViewModelFactory(phoneNumberUtil) }

    private val countrySelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val countryEmoji = data?.getStringExtra("country_emoji") ?: ""
            val countryName = data?.getStringExtra("country_name") ?: ""
            val countryCode = data?.getStringExtra("country_code") ?: ""

            if (countryName.isNotEmpty() && countryCode.isNotEmpty()) {
                viewModel.onCountrySelected(" $countryEmoji $countryName", countryCode)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permission", "READ_PHONE_NUMBERS granted.")
        } else {
            Log.d("Permission", "READ_PHONE_NUMBERS denied.")
        }
        // A inicialização ocorre independentemente da permissão (com ou sem o número)
        initializePhoneNumberAndCountry()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phoneNumberUtil = PhoneNumberUtil.getInstance()

        if (savedInstanceState == null) {
            requestPhoneNumberPermission()
        }

        setContent {
            MyComposeApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()

                PhoneRegistrationScreen(
                    uiState = uiState,
                    onCountryClick = {
                        val intent = Intent(this, CountrySelectionActivity::class.java)
                        countrySelectionLauncher.launch(intent)
                    },
                    onLocalPhoneNumberChange = { viewModel.onLocalPhoneNumberChange(it) },
                    onStartClick = {
                        startVerificationActivity(uiState.fullPhoneNumber)
                    }
                )
            }
        }
    }

    private fun requestPhoneNumberPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializePhoneNumberAndCountry()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_NUMBERS)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializePhoneNumberAndCountry() {
        val deviceRegion = Locale.getDefault().country.uppercase(Locale.ROOT)
        var localNumber = ""
        var country: com.chatting.ui.model.Country? = null

        // Tenta obter o número de telefone se a permissão foi concedida
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val fullPhoneNumber = telephonyManager.line1Number
                if (!fullPhoneNumber.isNullOrBlank()) {
                    // Ponto CRÍTICO da correção: Usa a região do dispositivo como "dica" para a biblioteca
                    // de parsing, o que a ajuda a entender números em formato local.
                    val parsedNumber = phoneNumberUtil.parse(fullPhoneNumber, deviceRegion)
                    val countryCodeFromNumber = parsedNumber.countryCode

                    // Encontra o país usando o código de país extraído do número (mais confiável)
                    country = CountryList.countries.firstOrNull {
                        it.code.removePrefix("+") == countryCodeFromNumber.toString()
                    }
                    localNumber = parsedNumber.nationalNumber.toString()
                }
            } catch (e: Exception) {
                Log.e("Telephony", "Não foi possível obter ou analisar o número de telefone: ${e.message}")
            }
        }

        // Se o país não pôde ser determinado a partir do número, usa a região do dispositivo como fallback
        if (country == null) {
            val countryCodeFromRegion = phoneNumberUtil.getCountryCodeForRegion(deviceRegion)
            country = CountryList.countries.firstOrNull {
                it.code.removePrefix("+") == countryCodeFromRegion.toString()
            }
        }

        // Finalmente, atualiza o ViewModel com a informação que foi encontrada
        country?.let {
            viewModel.setInitialData(" ${it.emoji} ${it.name}", it.code, localNumber)
        }
    }

    private fun startVerificationActivity(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            return
        }
        SecurePrefs.putString("my_number", phoneNumber)
        startActivity(Intent(this, VerifyNumberActivity::class.java))
    }
}