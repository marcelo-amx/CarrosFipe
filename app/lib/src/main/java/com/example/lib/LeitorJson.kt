package com.example.lib

import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import java.io.File

object LeitorJson {
    fun carregarFiltrados(): List<CarroFiltrado> {
        return try {
            val jsonFile = File("C:\\Users\\User\\AndroidStudioProjects\\CarrosFIpe\\app\\src\\main\\res\\raw\\filtrada.json")
            val jsonString = jsonFile.readText()
            val tipoLista = object : TypeToken<List<CarroFiltrado>>() {}.type
            Gson().fromJson<List<CarroFiltrado>>(jsonString, tipoLista) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun carregarFipe(): List<CarroFipe> {
        return try {
            val jsonFile =
                File("C:\\Users\\User\\AndroidStudioProjects\\CarrosFIpe\\app\\src\\main\\res\\raw\\fipe.json")
            val jsonString = jsonFile.readText()
            val tipoLista = object : TypeToken<List<CarroFipe>>() {}.type
            Gson().fromJson<List<CarroFipe>>(jsonString, tipoLista) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}