/*
 * LifeDots
 *
 * Copyright (C) 2018 Raphael Mack http://www.raphael-mack.de
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.FragmentDetailNoteBinding
import com.mdiqentw.lifedots.model.DetailViewModel

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
class DetailNoteFragment : Fragment() {
    private var viewModel: DetailViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val binding: FragmentDetailNoteBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_detail_note, container, false)
        //here data must be an instance of the class MarsDataProvider

        binding.note.setOnClickListener{
            if (viewModel!!.currentActivity().value != null) {
                val dialog = NoteEditDialog()
//                dialog.setStyle(DialogFragment.STYLE_NO_FRAME, 0)
                val noteText = viewModel!!.mNote.value
                if (noteText != null && noteText.isNotBlank())
                    dialog.inputText = noteText.toString()
                dialog.show(parentFragmentManager, "NoteEditDialogFragment")
            }
        }

        viewModel = ViewModelProvider(requireActivity()).get(DetailViewModel::class.java)
        binding.viewModel = viewModel
        // Specify the current activity as the lifecycle owner.
        binding.lifecycleOwner = this
        return binding.root
    }
}