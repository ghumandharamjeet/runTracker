package com.app.runtracker.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.app.runtracker.R
import com.app.runtracker.others.RunningConstants.KEY_FIRST_TIME
import com.app.runtracker.others.RunningConstants.KEY_NAME
import com.app.runtracker.others.RunningConstants.KEY_WEIGHT
import com.app.runtracker.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_setup.*
import javax.inject.Inject

@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var name: String

    @set:Inject
    var firstTime = false

    @set:Inject
    var weight = 0f

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(!firstTime){
            var navOptions = NavOptions.Builder().setPopUpTo(R.id.setupFragment, true).build()
            findNavController().navigate(R.id.action_setupFragment_to_runFragment, savedInstanceState ,navOptions)
        }

        tvContinue.setOnClickListener{

            val enteredWeight = etWeight.text.toString()
            val enteredName = etName.text.toString()

            if(enteredName.isEmpty() || enteredWeight.isEmpty()){
                Snackbar.make(requireView(), "Please enter both weight and name", Snackbar.LENGTH_SHORT).show()
            }
            else
            {
                sharedPreferences.edit().putFloat(KEY_WEIGHT, enteredWeight.toFloat())
                .putString(KEY_NAME, enteredName)
                .putBoolean(KEY_FIRST_TIME, false).apply()


                findNavController().navigate(R.id.action_setupFragment_to_runFragment)
            }
        }
    }
}