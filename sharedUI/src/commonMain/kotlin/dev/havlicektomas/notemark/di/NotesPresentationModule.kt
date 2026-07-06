package dev.havlicektomas.notemark.di

import dev.havlicektomas.notemark.notes.presentation.detail.NoteDetailViewModel
import dev.havlicektomas.notemark.notes.presentation.list.NotesListViewModel
import dev.havlicektomas.notemark.notes.presentation.reader.ReaderViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val notesPresentationModule = module {
    viewModelOf(::NotesListViewModel)
    viewModelOf(::NoteDetailViewModel)
    viewModelOf(::ReaderViewModel)
}
