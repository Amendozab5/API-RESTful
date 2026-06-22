package com.example.consumoapirest

import android.os.Bundle
import android.text.Html
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var txtAlumnosList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtAlumnosList = findViewById(R.id.txtAlumnosList)

        // Verificar si las credenciales por defecto no han sido modificadas
        if (SupabaseConfig.URL == "YOUR_SUPABASE_URL" || SupabaseConfig.ANON_KEY == "YOUR_SUPABASE_ANON_KEY") {
            showConfigurationInstruction()
        } else {
            fetchAlumnosFromSupabase()
        }
    }

    private fun showConfigurationInstruction() {
        val htmlInstruction = """
            <h3>⚠️ Configuración Requerida</h3>
            <p>Por favor, configura las credenciales de tu proyecto de Supabase en el archivo:</p>
            <p><b>SupabaseConfig.kt</b></p>
            <br>
            <p><b>Paso 1:</b> Entra a tu panel de Supabase.</p>
            <p><b>Paso 2:</b> Ve a Settings -> API.</p>
            <p><b>Paso 3:</b> Copia la <i>Project URL</i> y el <i>anon public API Key</i>.</p>
            <p><b>Paso 4:</b> Pégalos en las constantes correspondientes en el archivo <code>SupabaseConfig.kt</code> de tu proyecto Android.</p>
        """.trimIndent()
        
        setFormattedText(htmlInstruction)
    }

    private fun fetchAlumnosFromSupabase() {
        val url = "${SupabaseConfig.URL}/rest/v1/${SupabaseConfig.TABLE_NAME}"
        
        val requestQueue = Volley.newRequestQueue(this)
        
        val jsonArrayRequest = object : JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            Response.Listener { response ->
                displayAlumnos(response)
            },
            Response.ErrorListener { error ->
                val errorMessage = error.networkResponse?.let {
                    "Código de estado: ${it.statusCode}\nDatos: ${String(it.data)}"
                } ?: error.message ?: "Error desconocido de red"
                
                val htmlError = """
                    <font color="#D32F2F"><b>Error al obtener datos:</b></font><br>
                    $errorMessage<br><br>
                    <i>Por favor verifica que la URL, la anon-key y el nombre de la tabla sean correctos y que tengas permisos de lectura en la tabla '${SupabaseConfig.TABLE_NAME}' de Supabase.</i>
                """.trimIndent()
                setFormattedText(htmlError)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["apikey"] = SupabaseConfig.ANON_KEY
                headers["Authorization"] = "Bearer ${SupabaseConfig.ANON_KEY}"
                return headers
            }
        }
        
        requestQueue.add(jsonArrayRequest)
    }

    private fun displayAlumnos(jsonArray: JSONArray) {
        if (jsonArray.length() == 0) {
            txtAlumnosList.text = "No se encontraron alumnos en la tabla '${SupabaseConfig.TABLE_NAME}'."
            return
        }

        val htmlBuilder = StringBuilder()

        for (i in 0 until jsonArray.length()) {
            val alumnoObj = jsonArray.getJSONObject(i)
            
            // Buscar campos de forma flexible para soportar diferentes nombres de columnas
            val nombre = getFieldAsString(alumnoObj, listOf("apellidos_nombres", "nombre", "nombre_completo", "nombres", "name", "full_name"))
            val paralelo = getFieldAsString(alumnoObj, listOf("paralelo", "curso", "aula", "seccion", "parallel")) ?: "A"
            val periodo = getFieldAsString(alumnoObj, listOf("periodo", "periodo_lectivo", "anio", "year", "period")) ?: "2026-2027"
            val email = getFieldAsString(alumnoObj, listOf("correo_institucional", "email", "correo", "correo_electronico", "correo_microsoft", "mail"))

            val index = i + 1
            htmlBuilder.append("<b>$index.- ${nombre?.uppercase() ?: "SIN NOMBRE"}</b><br>")
            htmlBuilder.append("Paralelo: ${paralelo ?: "N/A"}<br>")
            htmlBuilder.append("Periodo: ${periodo ?: "N/A"}<br>")
            htmlBuilder.append("Email: ${email ?: "N/A"}<br><br>")
        }

        setFormattedText(htmlBuilder.toString())
    }

    private fun getFieldAsString(jsonObj: JSONObject, keys: List<String>): String? {
        for (key in keys) {
            if (jsonObj.has(key) && !jsonObj.isNull(key)) {
                val value = jsonObj.optString(key)
                if (value.isNotBlank() && value != "null") {
                    return value
                }
            }
        }
        return null
    }

    private fun setFormattedText(htmlContent: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            txtAlumnosList.text = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            txtAlumnosList.text = Html.fromHtml(htmlContent)
        }
    }
}