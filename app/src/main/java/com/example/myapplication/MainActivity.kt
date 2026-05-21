package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.FuzzyMatcher.isAlikeTo

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lerCarros()
    }

    private fun lerCarros() {
        val carrosFipe = LeitorJson.carregarFipe(this)
        val carrosFiltrados = LeitorJson.carregarFiltrados(this)
        val brandsFipe = carrosFipe.map { it.brand }.distinct()
        val brandsFiltrados = carrosFiltrados.map { it.brand }.distinct()
        val brandsNaoEncontrados = carrosFiltrados
            .filter { filtrado -> brandsFipe.find { filtrado.brand.equals(it, true) } == null }
            .filter { it.model != "IMPLEMENTOS" }
            .sortedBy { it.brand }

        carrosFiltrados.forEach { filtrado ->
            val fipeCodes: MutableList<String> = mutableListOf()
            val fipe = carrosFipe.filter { it.brand.equals(filtrado.brand,true) }

            fipe.forEach { fipe ->
                if (fipe.model.isAlikeTo(filtrado.model))
                {
                    fipeCodes.add(fipe.id)
                }
            }
            filtrado.fipeCodes = fipeCodes
        }
        val x = 10
    }
}
