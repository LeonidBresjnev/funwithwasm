package percentile.project.demo.openFda

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun Feature(feature: Pair<String,List<String>>, onDismissRequest: () -> Unit, searchStr: String="") {

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false // Allows custom sizing
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, androidx.compose.ui.graphics.Color.Black),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier= Modifier.padding(16.dp)) {
                Text(text=feature.first,style= MaterialTheme.typography.headlineSmall)
                LazyColumn {
                    itemsIndexed(items = feature.second) { idx,iu ->
                        val unquoted = searchStr.lowercase().removeSurrounding("\"")
                        if (unquoted.isNotEmpty() ) {
                            val rabinKarp = iu.lowercase().rabinKarp(unquoted)
                            val builder = AnnotatedString.Builder()
                            builder.append(iu)
                            rabinKarp.forEach {


                                builder.addStyle(
                                    style = SpanStyle(fontWeight = FontWeight.Bold),
                                    start = it,
                                    end = it+unquoted.length
                                )
                            }
                            Text(builder.toAnnotatedString())

                        }
                        else Text(iu)
                        /*if (true)*/
                       /* else {*/


                            //appendElementToDiv("root", iu)
                            //val media = MediaType(iu)

                            /*
                        renderComposable(rootElementId = "root") {
                            Div {
                                // Render raw HTML-like structure
                                P {
                                    Text("This is a paragraph with ")
                                    B { Text("bold text") }
                                }
                            }
                        }*/

                       /* }*/
                        if (idx != feature.second.lastIndex) HorizontalDivider(thickness = 1.dp)
                    }
                }
            }
        }
    }
}
