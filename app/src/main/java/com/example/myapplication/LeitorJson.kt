package com.example.myapplication

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LeitorJson {
    fun carregarFiltrados(context: Context): List<CarroFiltrado> {
        return try {
            val jsonString = context.resources.openRawResource(R.raw.filtrada).bufferedReader().use { it.readText() }
            val tipoLista = object : TypeToken<List<CarroFiltrado>>() {}.type
            Gson().fromJson(jsonString, tipoLista)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun carregarFipe(context: Context): List<CarroFipe> {
        return try {
            val jsonString = context.resources.openRawResource(R.raw.fipe).bufferedReader().use { it.readText() }
            val tipoLista = object : TypeToken<List<CarroFipe>>() {}.type
            Gson().fromJson(jsonString, tipoLista)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
