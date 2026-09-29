package com.example.myapplication


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

data class FormState(
    val nombre: String = "", val matricula: String = "",
    val asignatura: String? = null, val hora: String? = null, val fecha: String? = null,
    val errores: Map<String, String> = emptyMap()
)

private fun validar(f: FormState) = buildMap {
    when {
        f.nombre.isBlank() -> put("nombre", "El nombre es obligatorio")
        f.nombre.trim().length < 3 -> put("nombre", "Debe tener al menos 3 caracteres")
        !f.nombre.matches(Regex("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$")) -> put("nombre", "Solo se permiten letras y espacios")
    }
    when {
        f.matricula.isBlank() -> put("matricula", "La matrícula es obligatoria")
        !f.matricula.matches(Regex("^[0-9]+$")) -> put("matricula", "Solo se permiten números")
        f.matricula.length !in 4..10 -> put("matricula", "Debe tener entre 4 y 10 dígitos")
    }
    if (f.asignatura.isNullOrBlank()) put("asignatura", "Selecciona una asignatura")
    if (f.hora.isNullOrBlank()) put("hora", "Selecciona la hora")
    if (f.fecha.isNullOrBlank()) put("fecha", "Selecciona la fecha")
}

private fun formatoFecha(millis: Long): String {
    val z = millis / 86_400_000L + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
    return "${d.toString().padStart(2, '0')}/${m.toString().padStart(2, '0')}/${if (m <= 2) y + 1 else y}"
}

class RegistroViewModel : ViewModel() {
    var estado by mutableStateOf(FormState())
        private set

    // CORREGIDO: lista inmutable dentro de mutableStateOf (antes: mutableStateListOf)
    var registros by mutableStateOf(listOf<FormState>())
        private set

    fun update(field: String, value: String) {
        estado = when (field) {
            "nombre" -> estado.copy(nombre = value)
            "matricula" -> estado.copy(matricula = value)
            "asignatura" -> estado.copy(asignatura = value)
            "hora" -> estado.copy(hora = value)
            "fecha" -> estado.copy(fecha = value)
            else -> estado
        }.copy(errores = estado.errores - field)
    }

    fun guardar() {
        val err = validar(estado)
        estado = estado.copy(errores = err)
        if (err.isEmpty()) {
            // CORREGIDO: se crea una lista nueva con el registro al inicio
            registros = listOf(estado) + registros
            estado = FormState()
        }
    }
}

private val ASIGNATURAS = listOf("Programación Móvil", "Bases de Datos", "Redes de Computadoras", "Ingeniería de Software", "Sistemas Operativos")

@Composable
private fun CampoTexto(
    valor: String?, etiqueta: String, error: String?,
    icono: String? = null, readOnly: Boolean = false, onClick: (() -> Unit)? = null, onCambio: (String) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    if (onClick != null) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { if (it is PressInteraction.Release) onClick() }
        }
    }
    OutlinedTextField(
        value = valor ?: "", onValueChange = onCambio, label = { Text(etiqueta) }, singleLine = true,
        isError = error != null, supportingText = { error?.let { Text(it) } }, readOnly = readOnly,
        trailingIcon = icono?.let { { Text(it) } }, interactionSource = interactionSource, modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val vm: RegistroViewModel = viewModel { RegistroViewModel() }
            val st = vm.estado
            var showPicker by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Registro de práctica", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    CampoTexto(st.nombre, "Nombre", st.errores["nombre"]) { vm.update("nombre", it) }
                    CampoTexto(st.matricula, "Matrícula", st.errores["matricula"]) { vm.update("matricula", it) }

                    var exp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
                        OutlinedTextField(
                            value = st.asignatura ?: "", onValueChange = {}, readOnly = true, label = { Text("Asignatura") },
                            isError = st.errores["asignatura"] != null, supportingText = { st.errores["asignatura"]?.let { Text(it) } },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) }, modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                            ASIGNATURAS.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { vm.update("asignatura", it); exp = false }) }
                        }
                    }

                    CampoTexto(st.hora, "Hora de clase", st.errores["hora"], "🕒", readOnly = true, onClick = { showPicker = "hora" })
                    CampoTexto(st.fecha, "Fecha de entrega", st.errores["fecha"], "📅", readOnly = true, onClick = { showPicker = "fecha" })

                    Button(onClick = vm::guardar, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Guardar") }

                    if (vm.registros.isNotEmpty()) {
                        Text("Registros Guardados (${vm.registros.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        vm.registros.forEach { r ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("✓ Guardado correctamente", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(r.nombre.trim(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Matrícula: ${r.matricula.trim()}")
                                    Text("Asignatura: ${r.asignatura}")
                                    Text("Hora: ${r.hora} | Entrega: ${r.fecha}")
                                }
                            }
                        }
                    }
                }
            }

            if (showPicker == "hora") {
                val timeState = rememberTimePickerState(is24Hour = true)
                AlertDialog(
                    onDismissRequest = { showPicker = null },
                    confirmButton = {
                        TextButton(onClick = {
                            val h = timeState.hour.toString().padStart(2, '0')
                            val m = timeState.minute.toString().padStart(2, '0')
                            vm.update("hora", "$h:$m")
                            showPicker = null
                        }) { Text("Aceptar") }
                    },
                    dismissButton = { TextButton(onClick = { showPicker = null }) { Text("Cancelar") } },
                    text = { TimePicker(state = timeState) }
                )
            } else if (showPicker == "fecha") {
                val dateState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showPicker = null },
                    confirmButton = {
                        TextButton(onClick = {
                            dateState.selectedDateMillis?.let { vm.update("fecha", formatoFecha(it)) }
                            showPicker = null
                        }) { Text("Aceptar") }
                    },
                    dismissButton = { TextButton(onClick = { showPicker = null }) { Text("Cancelar") } }
                ) { DatePicker(state = dateState) }
            }
        }
    }
}