package percentile.project.demo


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import percentile.project.demo.openFda.Feature
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class, InternalAPI::class)
@Composable
fun FunWithOpenFDA() {

    var generic by remember { mutableStateOf("eptin") }
    var brand by remember { mutableStateOf("") }
    var indication by remember { mutableStateOf("migraine") }
    var maxHits by remember { mutableStateOf(20) }

    var response by remember { mutableStateOf<OpenFDAEntry?>(null) }
    var status by remember { mutableStateOf(0) }

    var extendedProductType by remember { mutableStateOf(false) }
    var selectedProductType by remember { mutableStateOf("Human OTC drug") }

    var showFeature by remember { mutableStateOf(false) }
    var featureIdx by remember { mutableIntStateOf(-1) }
    var shownFeature by remember { mutableStateOf<Pair<String, List<String>>>(Pair("", emptyList())) }

    var linkNext by remember { mutableStateOf("init") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var filter by remember { mutableStateOf<List<Boolean>> ( emptyList() ) }
    var applyFilter by remember { mutableStateOf(false) }

    var htmlContent = false

    val productTypes = listOf("Human OTC drug", "Human prescription drug", "CELLULAR THERAPY")

    Column {
        Row(modifier=Modifier.padding(15.dp)
            .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            TextField(
                value = generic,
                onValueChange = { generic = it },
                enabled = true,
                singleLine = true,
                label = { Text("generic name") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
            )
            TextField(
                value = brand,
                onValueChange = { brand = it },
                enabled = true,
                singleLine = true,
                label = { Text("brand name") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
            )
            TextField(
                value = indication,

                onValueChange = { indication = it },
                enabled = true,
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                label = { Text("Indication") }
            )

            TextField(
                modifier=Modifier.clickable(
                    enabled=true,
                    onClick = { extendedProductType = true }
                ),
                value = selectedProductType,
                onValueChange={},
                label = { Text("Product type") }
            )
            DropdownMenu(
                expanded = extendedProductType,
                onDismissRequest = { extendedProductType = false }
            ) {
                productTypes.forEach {
                    DropdownMenuItem(
                        onClick = {
                            selectedProductType = it
                            extendedProductType = false
                        },
                        text =  {Text(it)}
                    )
                }
            }

            TextField(
                value = maxHits.toString(),
                onValueChange = { maxHits = it.toIntOrNull() ?: maxHits },
                enabled = true,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                label = {Text("hits per page")}
            )

            Button(
                enabled = !isLoading && (generic.length >= 3 || brand.length >= 3 || indication.length >= 3),
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
                        //println(client.engine.config.toString())

                        val genericQuery = if (generic.length >= 3) "+AND+openfda.generic_name:$generic*" else ""
                        val brandQuery = if (brand.length >= 3) "+AND+openfda.brand_name:$brand*" else ""
                        val indicationQuery =
                            if (indication.length >= 3) "+AND+_exists_:indications_and_usage+AND+indications_and_usage:$indication*" else ""
                        val BASEURL = "https://api.fda.gov/drug/label.json?search=_exists_:openfda"

                        isLoading = true
                        val resultDef = async {
                            val httpResponse: Result<HttpResponse> = runCatching {
                                client.get(BASEURL+"${genericQuery}${brandQuery}${indicationQuery}&limit=$maxHits")
                            }
                            println("inside async of FunWithOpenFDA - button click")
                            return@async httpResponse
                        }
                        val result = resultDef.await()
                        isLoading = false
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
                            filter=emptyList()
                        }
                        result.onFailure { error ->
                            response = null
                        }



                        client.close()
                    }
                }
            ) {
                Text("Search")
            }
            Button(
                enabled = (!isLoading) && (response != null) && (response!!.results.any { it.indications_and_usage.isNotEmpty() }) && (indication.isNotEmpty()),
                onClick = {
                    isLoading = true
                    val client = HttpClient {
                        install(ContentNegotiation) {
                            json(
                                Json {
                                    ignoreUnknownKeys = true
                                },
                                contentType = ContentType.Application.Json
                            )
                        }
                    }
                    val baseurl =
                        "https://datascience-azure-openai-swedencentral.openai.azure.com/openai/responses?api-version=2025-04-01-preview"

                    scope.launch {
                        val resultDef = async {

                            val content = response!!
                                .results.joinToString(
                                    prefix = "\"",
                                    postfix = "\"",
                                    separator = ". £"
                                ) { it.indications_and_usage.joinToString(". ") }
                            // println(content)
                            //JsonObject()

                            val input = Json.encodeToJsonElement(value = listOf(
                                OpenAIInput(role="system", content = "identify labels for drugs that treats ${indication.removeSurrounding("\"")}, either in monotherapy or in combination with other drugs. each user specify one label. ignore that some texts are repetitive. use only information from the given labels, and not from anywhere else. Consider the ${response!!.results.size} labels indepently. Output an array of true or false corresponding to if the drug treats the indication. The output array should contain ${response!!.results.size} elements")) +
                                response!!.results.map { it2->
                                    OpenAIInput(
                                        role="user",
                                        content=it2.indications_and_usage.joinToString(". ").replace("\"", "'")
                                    )
                                }
                            )
                            val myBody0 ="""
{
    "model": "gpt-5-mini",
    "input": 
        $input
    ,
    "text": {
        "format": {
            "type": "json_schema",
            "name": "person",
            "strict": true,
            "schema": {
                "type": "object",
                "properties": {
                    "name": {
                        "type": "array",
                        "items": {
                            "type": "boolean"
                        }
                    }
                },
                "required": [
                    "name"
                ],
                "additionalProperties": false
            }
        }
    }
}""".trimIndent()
/*

                            val myBody =
                                """{
"model": "gpt-5-mini",
"input": [
{
"role": "system",
"content": "Identify labels for drugs that treats ${indication.removeSurrounding("\"")} either as monotherapy or in combination with other drugs. Use only information from label and not from anywhere else. The ${response!!.results.size} labels are separated by a £ sign. Ignore if labels repeat each other. Consider labels independently and do not combine information across labels. As output, I want a list of booleans in plain text, comma separated, one boolean for each of the ${response!!.results.size} label and in same order as the input"
},
{
"role": "user",
"content": $content
}
],
"max_output_tokens": 100000
}""".trimIndent()*/

                           // println(myBody)
                           // println("inside async - before post")
                            val httpResponse: Result<HttpResponse> = runCatching {
                                //println("inside runCatching")
                                client.post(baseurl) {
                                    contentType(ContentType.Application.Json)
                                    headers {
                                        append(HttpHeaders.Accept, value = "application/json")
                                    }
                                    bearerAuth(token = "CugGWRyhKKqyveq06MA4KzXYlXcyvHYm7jWBDSHKJ0P65JQwzrtsJQQJ99BDACfhMk5XJ3w3AAABACOGEJho")
                                    body = myBody0
                                    println(myBody0)
                                    setBody(myBody0)
                                }
                            }
                          //  println("inside async - after post")
                            return@async httpResponse
                        }
                        val result = resultDef.await()
                        result.onSuccess { action ->
                            println("success")
                            println(action.status.value)
                            val openAIresponse = if (action.status == HttpStatusCode.OK) {
                                action.body<OpenAIResponse>()
                            } else {
                                null
                            }
                            println(openAIresponse?.output?.first { it.type == "message" }?.content?.joinToString { it.text }
                                ?: "?")
                            filter = openAIresponse?.output?.first { it.type == "message" }?.content?.first()?.text?.split(",")
                                ?.map { it.contains(other="True", ignoreCase = true)} ?: emptyList()
                            println(filter.joinToString(", "))


                        }
                        result.onFailure { error ->
                            println(error.message)
                        }
                        isLoading = false
                    }
                }
            ) {
                Text("context filter")
            }
            Checkbox(
                  checked=applyFilter,
                onCheckedChange = { applyFilter = it }
            )
            /*
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
        )*/
        }

        Column {
            Text("Fun with OpenFDA")
            Text("status: $status")
            Text("Hits: ${response?.meta?.results?.total ?: 0} Results: ${response?.results?.size ?: 0}")
            Text("Next page link: $linkNext")
            Text("$applyFilter, ${filter.size}, ${response?.results?.size}")

            response?.run {
                if (!isLoading) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(860.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalItemSpacing = 16.dp,
                        contentPadding = PaddingValues(
                            start =  8.dp,
                            end =  8.dp,
                            top = 8.dp,
                            bottom =  8.dp,
                        )
                    ) {
                        itemsIndexed(
                            items = if (applyFilter && (filter.size == response?.results?.size )) {
                                response?.results?.filterIndexed { idx, _ -> filter[idx] } ?: emptyList()
                            }
                            else this@run.results,
                            key = { _, it -> it.key }
                        ) { idx,item ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                                modifier = Modifier.padding(5.dp).sizeIn(maxWidth = 860.dp, maxHeight = 300.dp)
                            ) {
                                Row(modifier = Modifier.padding(5.dp).fillMaxSize()) {
                                    Column(modifier = Modifier.weight(0.5f)) {
                                        Text(text = "General information", style = MaterialTheme.typography.headlineSmall)
                                        Text("Generic Name(s): ${item.openfda.generic_name.joinToString(", ")}")
                                        Text("Brand Name(s): ${item.openfda.brand_name.joinToString(", ")}")
                                        Text("Substance Name(s): ${item.openfda.substance_name.joinToString(", ")}")
                                        Text("Product Type(s): ${item.openfda.product_type.joinToString(", ")}")
                                        Text("Manufacturer name(s): ${item.openfda.manufacturer_name.joinToString(", ")}")
                                        Text("Route(s) of administration: ${item.openfda.route.joinToString(", ")}")
                                        if (filter.size == response?.results?.size ) {
                                            Text("Indication in right context: ${if (filter[idx]) "Yes" else "No"} ")
                                        }

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
                                        Pair(
                                            "Carcinogenesis and mutagenesis and impairment of fertility",
                                            item.carcinogenesis_and_mutagenesis_and_impairment_of_fertility
                                        ),
                                        Pair(
                                            "Animal pharmacology and/or toxicology",
                                            item.animal_pharmacology_and_or_toxicology
                                        ),
                                        Pair("Clinical studies", item.clinical_studies),
                                        Pair("How supplied", item.how_supplied),
                                        Pair("Storage and handling", item.storage_and_handling),
                                        Pair("Information for patients", item.information_for_patients),
                                        Pair("Med. guide", item.spl_medguide),
                                        Pair(
                                            "Package label principal display panel",
                                            item.package_label_principal_display_panel
                                        )
                                    ).filter { it.second.isNotEmpty() }


                                    Column(modifier = Modifier.weight(0.5f)) {
                                        Text(text = "Details", style = MaterialTheme.typography.headlineSmall)
                                        LazyColumn {
                                            itemsIndexed(items = features, key = { _, it -> it.first }) { idx, iu ->
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


                                }
                            }
                        }
                    }
                }
                else {
                    CircularProgressIndicator()
                }
            }
        }
        if (showFeature) {
            //println("featureIdx: $featureIdx")
            //println("features: ${shownFeature.first}, ${shownFeature.second.joinToString(", ")}")
            Feature(
                feature = shownFeature,
                onDismissRequest = { showFeature = false },
                searchStr = indication
            )
        }
    }
}

