package com.smartfind.app.presentation.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartfind.app.R

class ModelSelectionDialog : DialogFragment() {

    interface ModelSelectionListener {
        fun onModelSelected(modelName: String)
    }

    private var listener: ModelSelectionListener? = null
    private var models: Array<String> = emptyArray()
    private var currentModel: String? = null
    private var selectedModelIndex: Int = -1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? ModelSelectionListener
        if (listener == null) {
            throw ClassCastException("$parentFragment must implement ModelSelectionListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            models = it.getStringArray(ARG_MODELS) ?: emptyArray()
            currentModel = it.getString(ARG_CURRENT_MODEL)
        }

        selectedModelIndex = models.indexOf(currentModel).coerceAtLeast(0)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_model_title))
            .setSingleChoiceItems(models, selectedModelIndex) { _, which ->
                selectedModelIndex = which
            }
            .setPositiveButton(getString(R.string.select)) { _, _ ->
                if (selectedModelIndex != -1 && selectedModelIndex < models.size) {
                    listener?.onModelSelected(models[selectedModelIndex])
                }
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                dismiss()
            }
            .create()
    }

    companion object {
        private const val ARG_MODELS = "models"
        private const val ARG_CURRENT_MODEL = "current_model"

        fun newInstance(models: Array<String>, currentModel: String): ModelSelectionDialog {
            val args = Bundle().apply {
                putStringArray(ARG_MODELS, models)
                putString(ARG_CURRENT_MODEL, currentModel)
            }
            return ModelSelectionDialog().apply {
                arguments = args
            }
        }
    }
}
