package com.app.runtracker.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.runtracker.R
import com.app.runtracker.db.Run
import com.app.runtracker.others.RunningConstants.ACTION_PAUSE_SERVICE
import com.app.runtracker.others.RunningConstants.ACTION_START_OR_RESUME_SERVICE
import com.app.runtracker.others.RunningConstants.ACTION_STOP_SERVICE
import com.app.runtracker.others.TrackingUtility
import com.app.runtracker.services.TrackingService
import com.app.runtracker.services.polyLine
import com.app.runtracker.ui.viewmodels.MainViewModel
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*
import javax.inject.Inject
import kotlin.math.round


@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val viewModel: MainViewModel by viewModels()
    private var map: GoogleMap? = null
    private var launcher: ActivityResultLauncher<String>? = null
    private var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var isTracking = false
    private var pathPoints = mutableListOf<polyLine>()
    private var menu: Menu? = null
    private var runTime: Long = 0L
    private var isRunStarted = false

    @set:Inject
    var weight = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

         launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if(!it)
            {
                openSettings()
            }
        }

        multiplePermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
        ) {

            Log.e("keys", it.keys.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.tracking_menu, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if(runTime > 0L)
            this.menu?.getItem(0)?.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.cancel_run -> showCancelDialog()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestLocationPermissions()

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync{map = it}

        TrackingService.isTracking.value?.let {

            isTracking = it
        }

        subscribeToObservers()

        toggleButtons()

        btnToggleRun.setOnClickListener{
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                createBackgroundPermissionDialog()
            else
            {
                val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var gps_enabled = false

                try {
                    gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

                    if(!gps_enabled){

                        Snackbar.make(requireView(), "Please Turn on GPS services!", Snackbar.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    else{
                        isTracking = !isTracking
                        toggleService()
                    }
                } catch (ex: Exception) {
                    Snackbar.make(requireView(), "Please Turn on GPS services!", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        btnFinishRun.setOnClickListener{
            finishRunAndSaveInDB()
        }

        addAllPolyline()
    }

    private fun showCancelDialog(){

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
                .setTitle(R.string.cancel_run_title)
                .setMessage(R.string.cancel_run_message)
                .setIcon(R.drawable.ic_run)
                .setPositiveButton(R.string.yes){ _, _ ->
                    stopRun()
                }
                .setNegativeButton(R.string.no){ dialogInterface, i ->
                    dialogInterface.cancel()
                }.create().show()
    }

    private fun stopRun() {

        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    private fun addAllPolyline() {

        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1)
        {
            for(path in pathPoints){

                var polylineOptions = PolylineOptions().color(R.color.black).width(8f).addAll(path)
                map?.addPolyline(polylineOptions)
            }
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(pathPoints.last().last(), 15f))
        }
    }

    private fun toggleService() {

        if(!isRunStarted){
            isRunStarted = true
            menu?.getItem(0)?.isVisible = true
        }

        toggleButtons()

        if(isTracking)
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        else
            sendCommandToService(ACTION_PAUSE_SERVICE)
    }

    private fun subscribeToObservers() {

        TrackingService.isTracking.observe(viewLifecycleOwner, {
            isTracking = it
            toggleButtons()
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, {

            pathPoints = it

            if (isTracking && it.isNotEmpty() && it.last().isNotEmpty() && it.last().size > 1) {
                var firstPoint = it.last()[it.last().size - 2]
                var secondPoint = it.last()[it.last().size - 1]
                drawPolyline(firstPoint, secondPoint)
            }
        })

        TrackingService.runningTime.observe(viewLifecycleOwner, {
            runTime = it
            tvTimer.text = TrackingUtility().getFormattedTime(it)
        })
    }

    private fun toggleButtons() {

        if(isTracking){
            btnToggleRun.text = getString(R.string.stop)
            btnFinishRun.visibility = View.VISIBLE
        }
        else{
            btnToggleRun.text = getString(R.string.start)
            btnFinishRun.visibility = View.GONE
        }
    }

    private fun drawPolyline(firstPoint: LatLng, secondPoint: LatLng){

        var polylineOptions = PolylineOptions().color(R.color.black).width(8f).add(firstPoint).add(secondPoint)
        map?.addPolyline(polylineOptions)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(secondPoint, 15f))
    }

    private fun sendCommandToService(action: String){

        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }
    }

    private fun requestLocationPermissions() {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            requestLocationForLessThanAPI30()
        else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                createBackgroundPermissionDialog()
        }
    }

    private fun createBackgroundPermissionDialog(){
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.background_location_permission_title)
            .setMessage(R.string.background_location_permission_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                launcher?.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
                activity?.onBackPressed()
            }
            .create()
            .show()
    }

    private fun requestLocationForLessThanAPI30(){

        val permList = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
            permList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        if (!TrackingUtility().arePermissionsGranted(requireContext(), permList))
            multiplePermissionLauncher?.launch(permList.toTypedArray())
    }

    private fun openSettings(){
        Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${requireActivity().packageName}")).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
        }
    }

    private fun finishRunAndSaveInDB(){

        var totalDistanceOfRun = TrackingUtility().getTotalDistanceFromPolyline(pathPoints).toInt()
        var avgSpeed = round(totalDistanceOfRun / 1000f / (runTime / 1000f / 60 / 60))/10f
        var caloriesBurned = ((totalDistanceOfRun / 1000f) * weight).toInt()

        map?.snapshot {
            sendCommandToService(ACTION_STOP_SERVICE)
            var runToSave = Run(it, TrackingService.runStartTime, caloriesBurned, totalDistanceOfRun, runTime, avgSpeed)
            viewModel.insertRun(runToSave)
            findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
            Snackbar.make(requireActivity().findViewById(R.id.rootView), R.string.run_saved, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}