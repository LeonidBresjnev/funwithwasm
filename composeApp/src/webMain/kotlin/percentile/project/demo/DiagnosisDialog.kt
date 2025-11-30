package percentile.project.demo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import percentile.project.demo.tree.ExpandableTree


//external fun alert(message: String)

@Composable
fun DiagnosisDialog(
    buttonText: String,
    title: String="",
    request: String,
    onSelectionConfirmed: (Map<String,TreeEntry>) -> Unit
) {

    var icdCodes by remember { mutableStateOf<Map<String,TreeEntry>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }
    val expandedCodes = remember { emptySet<String>().toMutableStateList() }
    val selectedCodes = remember { emptySet<String>().toMutableStateList() }
    var usesearch by remember { mutableStateOf(false) }


    val getIcd = { code: String ->
        icdCodes[code]
    }

    val isSelected = { code: String ->
        selectedCodes.contains(code)
    }


    val onSelectedChange: (String, Boolean) -> Unit = { code: String, selected: Boolean ->
        if (selected) {
            selectedCodes.add(element=code)
        } else {
            selectedCodes.remove(element=code)
        }
    }

    fun addParentsToExpanded(code: String) {
        if (!expandedCodes.contains(code)) {
            expandedCodes.add(code)
            icdCodes[code]?.let {
                addParentsToExpanded(it.parent)
            }
        }
    }

    LaunchedEffect(show) {
        if (!show) return@LaunchedEffect
        if (icdCodes.isNotEmpty()) return@LaunchedEffect
        println("Launching effect to load ICD codes")
        isLoading=true
        val icdApi = IcdApi()
        try {
            val resultdeffered =
                async(context= Dispatchers.Default) {
                    icdApi.getIcdFull(request=request)
                }

            val result = resultdeffered.await()
            result.fold(
                onSuccess = { codes ->
                    if (codes.status== HttpStatusCode.OK) {
                        icdCodes = codes.body<Map<String,TreeEntry>>()
                        println("Loaded ${icdCodes.size} codes")
                    }
                    else println(codes.status.description)
                    isLoading = false
                },
                onFailure = { error ->
                    println("Error loading ICD codes: ${error.message}")
                    isLoading = false
                }
            )
        } finally {
            icdApi.close()
        }
    }

    var searchstr by remember { mutableStateOf("")}
    Button(onClick = { show = true }) {
        Text(buttonText)
    }

    val searchresult: (String)->Boolean = { code ->
        expandedCodes.contains(code)
    }

    val onsearch = {
        usesearch=true
        expandedCodes.clear()
        val code = searchstr.trim()
        if (code.length>=3) {
            icdCodes.values.filter {
                it.value.contains(code, ignoreCase = true) ||
                        it.label.contains(code, ignoreCase = true)
            }.forEach {
                addParentsToExpanded(it.value)
            }
        } else {
            expandedCodes.clear()
        }
    }

    if (show) {
        Dialog(
            onDismissRequest = {
                icdCodes = emptyMap()
                show=false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false // Allows custom sizing
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TextField(
                        onValueChange = { searchstr=it },
                        value=searchstr,
                        label={ Text("Search code or label")},
                        singleLine=true,

                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Search // or ImeAction.Send, Search, etc.
                        ),


                        keyboardActions = KeyboardActions(
                            onSearch = {
                                onsearch()
                            }
                        ),

                        leadingIcon = {
                            IconButton(onClick = {
                                searchstr = ""
                                usesearch = false
                                expandedCodes.clear()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )

                            }
                        },

                        trailingIcon = {
                            IconButton(onClick=onsearch,
                                enabled=true,
                            ) {
                                Icon(imageVector = Icons.Default.Search,
                                    contentDescription = "Clear search")
                            }
                        }
                    )

                    // Content area for tree/list
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                        if (isLoading) {
                            Text("Loading ICD codes...")
                        } else if (icdCodes.isEmpty()) {
                            Text("No ICD codes available.")
                        } else {
                            println("root:" + icdCodes["00000"])
                            ExpandableTree(modifier = Modifier.weight(0.75f),
                                items = listOf(icdCodes["00000"]!!),
                                isSelected = isSelected,
                                onSelectedChange = onSelectedChange,
                                getIcd = getIcd,
                                setUseSearch = {usesearch=it },
                                useSearch = usesearch,
                                searchresult = searchresult,
                                searchterm = searchstr
                            )
                        }
/*
                            LazyColumn(modifier=Modifier.weight(0.25f)) {
                                items(items=expandedCodes.toList(),
                                    key= { it } ) {
                                    Row {
                                        Text(text = "$it, ${icdCodes[it]?.label?:""}")
                                    }
                                }
                            }*/

                            LazyColumn(modifier=Modifier.weight(0.25f)) {
                                items(items=selectedCodes.toList(),
                                    key= { it } ) {
                                    Row {
                                        IconButton(onClick = {
                                            selectedCodes.remove(element=it)
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove"
                                            )
                                        }
                                        Text(text = "icd: $it, ${icdCodes[it]?.label?:""}")
                                    }
                                }
                            }
                        }
                    }
                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { show=false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                              /*  val selected = icdCodes.filter { selectedCodes.contains(it.value) }*/
                                onSelectionConfirmed(selectedCodes.toList().associateWith { icdCodes[it]!! })


                                icdCodes = emptyMap()
                                show = false
                            },
                            //enabled = selectedCodes.isNotEmpty()
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }

}

