package com.simplecityapps.shuttle.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NavigationRes
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.simplecityapps.adapter.RecyclerAdapter
import com.simplecityapps.shuttle.R
import com.simplecityapps.localmediaprovider.local.data.room.database.MediaDatabase
import com.simplecityapps.localmediaprovider.local.data.room.exportDatabase
import com.simplecityapps.localmediaprovider.local.data.room.restoreDatabase
import com.simplecityapps.shuttle.ui.common.autoCleared
import com.simplecityapps.shuttle.ui.common.error.userDescription
import com.simplecityapps.shuttle.ui.screens.sleeptimer.SleepTimerDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.exitProcess


@AndroidEntryPoint
class BottomDrawerSettingsFragment :
    BottomSheetDialogFragment(),
    BottomDrawerSettingsContract.View {
    // Lifecycle

    @Inject lateinit var presenter: BottomDrawerSettingsPresenter

    @Inject lateinit var database: MediaDatabase

    private var adapter: RecyclerAdapter by autoCleared()

    private val exportDatabaseLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) {
            dismiss()
        } else {
            exportDatabase(uri)
        }
    }

    private val restoreDatabaseLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            dismiss()
        } else {
            restoreDatabase(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_bottom_drawer, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RecyclerAdapter(viewLifecycleOwner.lifecycleScope)

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter

        presenter.bindView(this)
    }

    override fun onResume() {
        super.onResume()

        setSelectedItem(findNavController().currentDestination?.id)

        findNavController().addOnDestinationChangedListener(destinationChangedListener)

        presenter.loadData()
    }

    override fun onPause() {
        findNavController().removeOnDestinationChangedListener(destinationChangedListener)
        super.onPause()
    }

    override fun onDestroyView() {
        presenter.unbindView()

        super.onDestroyView()
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    // Private

    private val destinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ -> setSelectedItem(destination.id) }

    private fun setSelectedItem(
        @NavigationRes destinationIdRes: Int?
    ) {
        presenter.currentDestinationIdRes = destinationIdRes
    }

    // SettingsViewBinder.Listener Implementation

    private val settingsItemClickListener =
        object : SettingsViewBinder.Listener {
            override fun onMenuItemClicked(settingsItem: SettingsMenuItem) {
                when (settingsItem) {
                    SettingsMenuItem.ExportDatabase -> {
                        exportDatabaseLauncher.launch("song.db")
                    }
                    SettingsMenuItem.RestoreDatabase -> {
                        restoreDatabaseLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3", "*/*"))
                    }
                    else -> {
                        dismiss()
                        when (settingsItem) {
                            SettingsMenuItem.Shuffle -> presenter.shuffleAll()
                            SettingsMenuItem.SleepTimer -> SleepTimerDialogFragment.newInstance().show(requireFragmentManager())
                            SettingsMenuItem.Dsp -> findNavController().navigate(R.id.action_bottomSheetFragment_to_equalizerFragment)
                            SettingsMenuItem.Settings -> findNavController().navigate(R.id.action_bottomSheetFragment_to_settingsFragment)
                            else -> {}
                        }
                    }
                }
            }
        }

    private fun exportDatabase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                exportDatabase(database, requireContext(), uri)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Database exported successfully", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    private fun restoreDatabase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                restoreDatabase(database, requireContext(), uri)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Database restored successfully. Please restart the app.", Toast.LENGTH_LONG).show()
                    restartApp()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    fun restartApp() {
        val context = requireContext()
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        // Required for API 34 and later
        // https://developer.android.com/about/versions/14/behavior-changes-14#safer-intents
        mainIntent.setPackage(context.packageName)
        context.startActivity(mainIntent)
        exitProcess(0)
    }

    // BottomDrawerSettingsContract.View Implementation

    override fun setData(
        settingsItems: List<SettingsMenuItem>,
        currentDestination: Int?
    ) {
        adapter.update(settingsItems.map { settingsItem -> SettingsViewBinder(settingsItem, false, settingsItemClickListener) })
    }

    override fun showLoadError(error: Error) {
        Toast.makeText(context, error.userDescription(resources), Toast.LENGTH_LONG).show()
    }

    // Static

    companion object {
        const val TAG = "BottomDrawerSettingsFragment"

        fun newInstance() = BottomDrawerSettingsFragment()
    }
}
