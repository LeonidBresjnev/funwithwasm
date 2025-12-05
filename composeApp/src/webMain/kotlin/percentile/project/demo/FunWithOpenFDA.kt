package percentile.project.demo


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.call.*
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
import percentile.project.demo.openFda.Feature
import kotlin.uuid.ExperimentalUuidApi


@OptIn(ExperimentalUuidApi::class)
@Composable
fun FunWithOpenFDA() {

    var generic by remember { mutableStateOf("Trilaciclib") }
    var brand by remember { mutableStateOf("Cosela") }
    var indication by remember { mutableStateOf("") }

    var response by remember { mutableStateOf<OpenFDAEntry?>(null) }
    var status by remember { mutableStateOf(0) }


    var showFeature by remember { mutableStateOf(false) }
    var featureIdx by remember { mutableIntStateOf(-1) }
    var shownFeature by remember { mutableStateOf<Pair<String,List<String>>>(Pair("",emptyList())) }

    var linkNext by remember { mutableStateOf("init") }
    val scope = rememberCoroutineScope()

    var htmlContent = false

    /*
    LaunchedEffect(key1 = Unit) {

        val client = HttpClient {

            install(HttpTimeout) {
                requestTimeoutMillis = 5000
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        val BASEURL = "https://api.fda.gov/drug/label.json?search=generic_name=Trilaciclib&limit=10"

        val resultDef = async(context = Dispatchers.Default) {
            val httpResponse: Result<HttpResponse> = runCatching {
                client.get(BASEURL)
            }
            println("inside async of FunWithOpenFDA")
            return@async httpResponse
        }
        val result = resultDef.await()
        result.onSuccess { action ->
            status = action.status.value
            response = if (action.status == HttpStatusCode.OK) {
                action.body<OpenFDAEntry>()
            } else {
                null
            }
        }
        result.onFailure { error ->
            response = null
        }
    }*/
    Row {
        TextField(
            value = generic,
            onValueChange = { generic = it },
            enabled = true,
            singleLine = true,
            label = { Text("generic name") },
            modifier = Modifier.padding(5.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = androidx.compose.ui.text.input.ImeAction.Next
            ),
        )
        TextField(
            value = brand,
            onValueChange = { brand = it },
            enabled = true,
            singleLine = true,
            label = { Text("brand name") },
            modifier = Modifier.padding(5.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = androidx.compose.ui.text.input.ImeAction.Next
            ),
        )
        TextField(
            value = indication,

            onValueChange = { indication = it },
            enabled = true,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            label = { Text("Indication") },
            modifier = Modifier.padding(5.dp)
        )
        Button(
            enabled = generic.length >= 3 || brand.length >= 3 || indication.length >= 3,
            onClick = {
                /*CoroutineScope(context= Dispatchers.Default).launch {*/
                scope.launch(context = Dispatchers.Default) {
                    val client = HttpClient {

                        install(HttpTimeout) {
                            requestTimeoutMillis = 5000
                        }

                        install(ContentNegotiation) {
                            json(
                                Json {
                                    ignoreUnknownKeys = true
                                    /*   isLenient = true*/
                                },
                                contentType = ContentType.Application.Json
                            )
                        }
                    }
                    println(client.engine.config.toString())

                    val genericQuery = if (generic.length >= 3) "+AND+openfda.generic_name:$generic*" else ""
                    val brandQuery = if (brand.length >= 3) "+AND+openfda.brand_name:$brand*" else ""
                    val indicationQuery =
                        if (indication.length >= 3) "+AND+_exists_:indications_and_usage+AND+indications_and_usage:$indication*" else ""
                    val BASEURL =
                        "https://api.fda.gov/drug/label.json?search=_exists_:openfda${genericQuery}${brandQuery}${indicationQuery}&limit=10"

                    val resultDef = async {
                        val httpResponse: Result<HttpResponse> = runCatching {
                            client.get(BASEURL)
                        }
                        println("inside async of FunWithOpenFDA - button click")
                        return@async httpResponse
                    }
                    val result = resultDef.await()
                    result.onSuccess { action ->
                        status = action.status.value
                        val headers = action.headers.entries()
                        println(BASEURL)
                        println("Headers:")
                        headers.forEach {
                            println("${it.key}: ${it.value}, ${it.value.joinToString(", ")}")
                        }
                        println("---")
                        linkNext = action.headers["Link"] ?: ""
                        response = if (action.status == HttpStatusCode.OK) {
                            action.body<OpenFDAEntry>()
                        } else {
                            null
                        }
                    }
                    result.onFailure { error ->
                        response = null
                    }
                    client.close()
                }
            },
            modifier = Modifier.padding(5.dp)
        ) {
            Text("Search")
        }

        IconButton(
            onClick = {
                /*CoroutineScope(context= Dispatchers.Default).launch {*/
                scope.launch(context = Dispatchers.Default) {
                    val client = HttpClient {

                        install(HttpTimeout) {
                            requestTimeoutMillis = 5000
                        }

                        install(ContentNegotiation) {
                            json(Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            })
                        }
                    }

                    val resultDef = async {
                        val httpResponse: Result<HttpResponse> = runCatching {
                            client.get(linkNext)
                        }
                        println("inside async of FunWithOpenFDA - button click")
                        return@async httpResponse
                    }
                    val result = resultDef.await()
                    result.onSuccess { action ->
                        status = action.status.value
                        linkNext = action.headers["LINK"] ?: ""
                        response = if (action.status == HttpStatusCode.OK) {
                            action.body<OpenFDAEntry>()
                        } else {
                            null
                        }
                    }
                    result.onFailure { error ->
                        response = null
                    }
                }
            },
            enabled = (linkNext.isNotEmpty()),
            content = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Page",
                )
            }
        )
    }

    Column {
        Text("Fun with OpenFDA")
        Text("status: $status")
        Text("Hits: ${response?.meta?.results?.total ?: 0} Results: ${response?.results?.size ?: 0}")
        Text("Next page link: $linkNext")

        response?.run {
            LazyColumn {
                items(items = this@run.results, key = { it.key }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        modifier = Modifier.padding(5.dp).sizeIn(maxWidth = 860.dp, maxHeight = 640.dp)
                    ) {
                        Row(modifier=Modifier.padding(5.dp).fillMaxSize()) {
                            Column(modifier = Modifier.weight(0.5f)) {
                                Text(text="General information", style= MaterialTheme.typography.headlineSmall)
                                Text("Generic Name(s): ${item.openfda.generic_name.joinToString(", ")}")
                                Text("Brand Name(s): ${item.openfda.brand_name.joinToString(", ")}")
                                Text("Substance Name(s): ${item.openfda.substance_name.joinToString(", ")}")
                                Text("Product Type(s): ${item.openfda.product_type.joinToString(", ")}")
                                Text("Manufacturer name(s): ${item.openfda.manufacturer_name.joinToString(", ")}")
                                Text("Route(s) of administration: ${item.openfda.route.joinToString(", ")}")
                            }
                            val features = listOf(
                                Pair("Indication and usage", item.indications_and_usage),
                                Pair("Dosage and administration", item.dosage_and_administration),
                                Pair("Dosage form and strength", item.dosage_forms_and_strengths),
                                Pair("Contraindications", item.contraindications),
                                Pair("Warnings and cautions", item.warnings_and_cautions),
                                Pair("Adverse reactions", item.adverse_reactions),
                                Pair("Drug interactions", item.drug_interactions),
                                Pair("Use in specific populations", item.use_in_specific_populations),
                                Pair("Pregnancy", item.pregnancy),
                                Pair("Pediatric use", item.pediatric_use),
                                Pair("Geriatric use", item.geriatric_use),
                                Pair("Overdosage", item.overdosage),
                                Pair("Description", item.description),
                                Pair("Clinical pharmacology", item.clinical_pharmacology),
                                Pair("Mechanism of action", item.mechanism_of_action),
                                Pair("Pharmacodynamics", item.pharmacodynamics),
                                Pair("Pharmacokinetics", item.pharmacokinetics),
                                Pair("Nonclinical toxicology", item.nonclinical_toxicology),
                                Pair("Carcinogenesis and mutagenesis and impairment of fertility",
                                    item.carcinogenesis_and_mutagenesis_and_impairment_of_fertility
                                ),
                                Pair("Animal pharmacology and/or toxicology",item.animal_pharmacology_and_or_toxicology),
                                Pair("Clinical studies", item.clinical_studies),
                                Pair("How supplied", item.how_supplied),
                                Pair("Storage and handling", item.storage_and_handling),
                                Pair("Information for patients", item.information_for_patients),
                                Pair("Med. guide", item.spl_medguide),
                                Pair("Package label principal display panel", item.package_label_principal_display_panel)
                            ).filter { it.second.isNotEmpty() }

                            Column(
                                modifier = Modifier.weight(0.5f)
                            ) {
                                Text(text="Details", style= MaterialTheme.typography.headlineSmall)
                                features.forEachIndexed{ idx,iu ->
                                    Text(
                                        modifier = Modifier.clickable(
                                        enabled = iu.second.isNotEmpty(),
                                        onClick = {
                                            htmlContent = false

                                            featureIdx = idx
                                            showFeature = true
                                            shownFeature = features[idx]
                                        }
                                    ),
                                        text = iu.first)
                                    HorizontalDivider(thickness = 1.dp)
                                }
                            }


                        }

                        /*
                        Row {
                            Button(
                                enabled = item.indications_and_usage.isNotEmpty(),
                                onClick = {
                                    htmlContent = false
                                    showFeature = true
                                    feature = item.indications_and_usage
                                }
                            ) {
                                Text("Indications and Usage")
                            }

                            Button(
                                enabled = item.dosage_and_administration.isNotEmpty(),
                                onClick = {
                                    htmlContent = false
                                    showFeature = true
                                    feature = item.dosage_and_administration
                                }
                            ) {
                                Text("Dosage and Administration")
                            }


                            Button(
                                enabled = item.dosage_and_administration_table.isNotEmpty(),
                                onClick = {
                                    htmlContent = true
                                    showFeature = true
                                    feature = item.dosage_and_administration_table
                                }
                            ) {
                                Text("Dosage and Administration")
                            }
                        }*/
                    }
                    /* if (item.adverse_reactions.isNotEmpty()) {
                        Column {
                            item.adverse_reactions.forEach { ae ->
                                Text(ae)
                            }
                            HorizontalDivider(thickness = 1.dp)

                        }
                    }*/
                }
            }
        }
    }
    if (showFeature) {
        println("featureIdx: $featureIdx")
        println("features: ${shownFeature.first}, ${shownFeature.second.joinToString(", ")}")
        Feature(
            feature=shownFeature,
            onDismissRequest = { showFeature = false }
        )
    }
}