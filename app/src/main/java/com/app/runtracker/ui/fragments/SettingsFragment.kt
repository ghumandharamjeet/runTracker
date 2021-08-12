package com.app.runtracker.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.app.runtracker.R
import com.app.runtracker.others.RunningConstants
import com.app.runtracker.others.RunningConstants.KEY_FIRST_TIME
import com.app.runtracker.others.RunningConstants.KEY_NAME
import com.app.runtracker.others.RunningConstants.KEY_WEIGHT
import com.app.runtracker.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_setup.*
import kotlinx.android.synthetic.main.fragment_setup.etName
import kotlinx.android.synthetic.main.fragment_setup.etWeight
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        populateNameAndWeight()

        btnApplyChanges.setOnClickListener{
            val enteredWeight = etWeight.text.toString()
            val enteredName = etName.text.toString()

            if (enteredName.isEmpty() || enteredWeight.isEmpty()) {
                Snackbar.make(requireView(), "Please enter both weight and name", Snackbar.LENGTH_SHORT).show()
            } else {
                sharedPreferences.edit().putFloat(KEY_WEIGHT, enteredWeight.toFloat())
                        .putString(KEY_NAME, enteredName).apply()

                Snackbar.make(requireView(), "Applied Changes Successfully!", Snackbar.LENGTH_SHORT).show()

                var navOptions = NavOptions.Builder().setPopUpTo(R.id.runFragment, true).build()
                findNavController().navigate(R.id.action_settings_fragment_to_run_fragment, savedInstanceState, navOptions)
            }
        }
    }

    private fun populateNameAndWeight() {

        etName.setText(sharedPreferences.getString(KEY_NAME, "") ?: "")
        etWeight.setText(sharedPreferences.getFloat(KEY_WEIGHT, 1f).toString())
    }
}