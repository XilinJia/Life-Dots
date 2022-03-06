/*
 * LifeDots
 *
 * Copyright (C) 2017 Raphael Mack http://www.raphael-mack.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.mdiqentw.lifedots.ui.main

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager.LayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.mdiqentw.lifedots.R


/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
class NoteEditDialog : DialogFragment() {

    private var note: String? = null
    private lateinit var input: EditText
    private var mListener: NoteEditDialogListener? = null
    var diaryId: Long = 0
    var inputText: String = ""

    lateinit var result: Dialog

    interface NoteEditDialogListener {
        fun onNoteEditPositiveClick(str: String?, dialog: DialogFragment?)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let { super.onSaveInstanceState(it) }
        val builder = AlertDialog.Builder(requireActivity())

        val inflater = requireActivity().layoutInflater
        val dlgView = inflater.inflate(R.layout.dialog_note_editor, null)

        input = dlgView.findViewById(R.id.note_text)
        if (savedInstanceState != null) {
            input.setText(savedInstanceState.getString("Note"))
        }
        input.setText(inputText)
        note = inputText
        input.setSelection(input.text.length)
        input.requestFocus()
        val inputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)

        builder.setView(dlgView) // Add action buttons
                .setPositiveButton(R.string.dlg_ok) { _: DialogInterface?, _: Int ->
                    mListener!!.onNoteEditPositiveClick(input.text.toString(), this@NoteEditDialog) }
                .setNegativeButton(R.string.dlg_cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        result = builder.create()

        result.window!!.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return result
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        mListener = try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            context as NoteEditDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement NoteEditDialogListener")
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString("Note", input.text.toString())
    }
}