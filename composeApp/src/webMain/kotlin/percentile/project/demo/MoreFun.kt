package percentile.project.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import demo.composeapp.generated.resources.Res
import demo.composeapp.generated.resources.compose_multiplatform
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource

@Composable
fun MoreFun() {

    val scope = rememberCoroutineScope()
    var response by remember {
        mutableStateOf("startval")
    }
    var clickCounter by remember {
        mutableIntStateOf(0)
    }

    var status by remember {
        mutableIntStateOf(0)
    }

    val client=HttpClient {
        install(plugin=ContentNegotiation) {

            json(json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            }
            )


        }

        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }

    var showContent by remember { mutableStateOf(false) }
    var cohorts by remember { mutableStateOf(
        List(5) {
            CohortCriteria(
                id = it
            )
        })
    }
    var editCohort by remember { mutableIntStateOf(-1)}


    Column(modifier=Modifier.padding(10.dp).fillMaxSize()) {
        Button(onClick = {
            println("Button clicked - before state changes")
            showContent = !showContent
            clickCounter++

            println("Button clicked - after state changes, $clickCounter, $showContent")


            println("Button clicked - outside coroutine")
            scope.launch(context = Dispatchers.Default) {
                val httpResponseDef = async {
                    println("Button clicked - inside coroutine")

                    val httpResponse: Result<HttpResponse> =
                        runCatching {
                            client.get("http://10.11.12.120:$SERVER_PORT") {
                                timeout {
                                    requestTimeoutMillis = 8000
                                }
                            }
                        }
                    println("Button clicked - after http call")
                    return@async httpResponse
                }
                println("Before awaiting httpResponseDef")
                println(httpResponseDef.toString())
                val httpResponse = httpResponseDef.await()
                println("httpResponse.status.description")
                httpResponse.onSuccess { action ->
                    status = action.status.value
                    if (action.status == HttpStatusCode.OK) {
                        response = action.bodyAsText()
                    } else {
                        response = "Error: ${action.status.description}"
                        println("Error: ${action.status.description}")
                    }
                }.onFailure { error ->
                    response = "Exception occurred: ${error.message ?: ""}"
                    println(response)
                }
            }
        })
        {

            Text("Click me!")
        }

        AnimatedVisibility(visible = showContent) {
            val greeting = remember { Greeting().greet() }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(resource = Res.drawable.compose_multiplatform), contentDescription = null)
                Text("Compose: $greeting")
            }
        }


        Text(text = response)

        Text(text = "Click count: $clickCounter")
        Text(text = "status: $status")
        //Text("invchisq(0.95,10)=${invchisq_cdf(df=10.0,p=0.95)}")

        //Text("Beta(1/2,1/2)=${exp(lnBeta(0.5, 0.5))}")
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(350.dp),
        ) {
            items(
                items = cohorts,
                key = { cohort -> cohort.id }
            ) { cohort ->
                if (editCohort != cohort.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CardDefaults.elevatedCardColors().containerColor,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) ) {
                    Column {
                        Text(
                            text = "Cohort: ${cohort.id}"
                        )
                        Row {
                            Button(onClick = {
                                cohorts = cohorts.filter { it.id != cohort.id }
                            }) {
                                Text(text = "Delete")
                            }
                            Button(onClick = {
                                editCohort = cohort.id
                            }) {
                                Text(text = "Edit")
                            }
                        }
                    }
                }
                else Card(

                ) {
                    Column {
                        Text(
                            text = "Cohort: ${cohort.id}"
                        )
                        Row {
                            Button(onClick = {
                                cohorts = cohorts.filter { it.id != cohort.id }
                            }) {
                                Text(text = "Delete")
                            }

                            DiagnosisDialog(buttonText = "Select diagnoses", title = "Select diagnoses", request = "icdfulllist", onSelectionConfirmed = {})
                            DiagnosisDialog(buttonText = "Select drugs", title = "Select drugs", request = "uscfulllist", onSelectionConfirmed = {})
                            Button(onClick = {
                                editCohort = -1
                            }) {
                                Text(text = "Ok")
                            }
                        }
                    }
                }
            }


        }
    }

}