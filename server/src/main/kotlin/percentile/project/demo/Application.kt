package percentile.project.demo


import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import io.ktor.server.plugins.contentnegotiation.*
import org.apache.commons.math3.distribution.BetaDistribution
import org.apache.commons.math3.special.Beta
import kotlin.math.exp
import kotlin.math.pow

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.configureHTTP() {
    println("Configuring CORS...")
    install(plugin=CORS) {
        allowHeader("user_session")
        exposeHeader("user_session")
        //allowMethod(HttpMethod.Options)
        //allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        //allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Get)
        //allowHeader(HttpHeaders.ContentType)
        //allowHeader(HttpHeaders.Authorization)
        //allowHeader(header="MyCustomHeader")
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }


    install(plugin=ContentNegotiation) {
        json()
    }


    install(plugin=Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(minSize = 1024L) // condition
        }
    }
}



fun Application.module() {
    configureHTTP()
    var clickCounter = 0



    routing {
        get("/") {
            clickCounter++
            println("Click count (server): $clickCounter")
            val responseDef = call.async{
                //delay(1000)
                return@async clickCounter
            }
            val response = responseDef.await()
            call.respondText("Ktor: ${Greeting().greet()}, click count (response): $response")
            println("Response sent.")
        }

        get(path="/icd") {
            val code = call.request.queryParameters["code"] ?: "00000"
            println("Received request for ICD code: $code")
            println(Icd10.entries[code]?:"NotFound" )
            val entry = Icd10.entries[code]
            if (entry != null) {
                call.respond<TreeEntry>(
                    status = HttpStatusCode.OK,
                    message = entry
                )
            } else {
                call.respond<TreeEntry>(
                    status = HttpStatusCode.NotFound,
                    message =  TreeEntry(
                        value = "NotFound",
                        children = mutableListOf(),
                        label = "Not Found",
                        parent = "None",
                        type = "None"
                    )
                )
            }
        }



        get(path="/icdchildren") {
            val code = call.request.queryParameters["code"] ?: "00000"
            println("Received request for ICD code: $code")
            println(Icd10.entries[code]?:"NotFound" )
            val entry = Icd10.entries[code]

            entry?.let {
                call.respond<List<TreeEntry>>(
                    status = HttpStatusCode.OK,
                    message = it.children.map { it2->Icd10.entries[it2]!! }/*.filter { it2-> it2 != null } as List<Icdentry>*/
                )
            }?:run {
                call.respond<String>(
                    status = HttpStatusCode.NotFound,
                    message =  "Entry not found"
                )
            }
        }

        get(path="/icdfulllist") {
            println("full list requested")
            call.respond<Map<String,TreeEntry>>(
                status = HttpStatusCode.OK,
                message = Icd10.entries
            )
            println("full list sent")
        }


        get(path="/uscfulllist") {
            println("full list requested")
            call.respond<Map<String,TreeEntry>>(
                status = HttpStatusCode.OK,
                message = usc.entries
            )
            println("full list sent")
        }

        get("/npp") {
            val ns = (call.request.queryParameters["ns"] ?: "10").toDouble()
            val nt = (call.request.queryParameters["nt"] ?: "10").toDouble()
            val ps = (call.request.queryParameters["ps"] ?: "0.5").toDouble()
            val pt = (call.request.queryParameters["pt"] ?: "0.5").toDouble()
            val alpha= (call.request.queryParameters["alpha"] ?: "0.5").toDouble()
            val beta = (call.request.queryParameters["beta"] ?: "0.5").toDouble()


            val xs=ps*ns
            val xt=pt*nt
            val stepsize=1000.0
            val stepsizeinv=1/stepsize
            val grandintegral = (1..1000).sumOf {
                val w = it.toDouble() *stepsizeinv
                return@sumOf stepsizeinv * exp(
                    (Beta.logBeta((xt + w * xs + alpha), nt - xt + w * (ns - xs) + beta) -
                            Beta.logBeta(w * xs + alpha, w * (ns - xs) + beta))
                )
            }
            //println("$ns $xs $nt $xt $grandintegral")
            val result = (0..100).map {
                val p = it.toDouble() / 100.0
                val y = (1..1000).sumOf { widx ->
                    val w = widx.toDouble() / 1000.0

                    val c = exp(Beta.logBeta(w * xs + alpha, w * (ns - xs) + beta))
                    val y =
                        0.001 * p.pow(xt + w * xs +alpha - 1.0) * (1.0 - p).pow((nt - xt) + w * (ns - xs) +beta-1.0) / c
                    return@sumOf y
                } / grandintegral

                return@map y
            }

            val betadist0 = BetaDistribution(alpha+xt, beta+nt -xt)
            val result_w0 = (0..100).map {
                val p = it.toDouble() / 100.0
                val y = betadist0.density(p)
                return@map y
            }
            val betadist1 = BetaDistribution(alpha+xt+xs, beta+nt -xt +ns -xs)
            val result_w1 = (0..100).map {
                val p = it.toDouble() / 100.0
                val y = betadist1.density(p)
                return@map y
            }

            //val integral = result.sumOf { it * 0.01 }
            //println("integral check: $integral")
            val simpson = (result.first() + result.last() + 4.0 * result.filterIndexed { idx, _ -> idx % 2 == 1 }
                .sum() + 2.0 * result.filterIndexed { idx, _ -> idx % 2 == 0 && idx != 0 && idx != 100 }
                .sum()) * 0.01 / 3.0
            println("simpson integral check - p: $simpson")

            call.respond<List<List<Double>>>(
                status = HttpStatusCode.OK,
                message = listOf(result,result_w0,result_w1)
            )
        }


        get("/nppweight") {
            val ns = (call.request.queryParameters["ns"] ?: "10").toDouble()
            val nt = (call.request.queryParameters["nt"] ?: "10").toDouble()
            val ps = (call.request.queryParameters["ps"] ?: "0.5").toDouble()
            val pt = (call.request.queryParameters["pt"] ?: "0.5").toDouble()
            val alpha= (call.request.queryParameters["alpha"] ?: "0.5").toDouble()
            val beta = (call.request.queryParameters["beta"] ?: "0.5").toDouble()
            val xs=ps*ns
            val xt=pt*nt
            val stepsize=1000.0
            val stepsizeinv=1/stepsize
            val grandintegral = (1..1000).sumOf {
                val w = it.toDouble() *stepsizeinv
                return@sumOf stepsizeinv * exp(
                    (Beta.logBeta((xt + w * xs + alpha), nt - xt + w * (ns - xs) + beta) -
                            Beta.logBeta(w * xs + alpha, w * (ns - xs) + beta))
                )
            }
            //println("$ns $xs $nt $xt $grandintegral")

            val result = (1..1000).map { widx ->
                val w = widx.toDouble() / 1000.0
                val logf =Beta.logBeta(xt+xs*w+alpha,(nt-xt)+(ns-xs)*w+beta) -
                          Beta.logBeta(xs*w+alpha,(ns-xs)*w+beta)
                val pdf = exp(logf)/ grandintegral

                return@map pdf
            }

            val integral = result.sumOf { it * 0.001 }
            println("integral check: $integral")
            val simpson = (result.first() + result.last() + 4.0 * result.filterIndexed { idx, _ -> idx % 2 == 1 }
                .sum() + 2.0 * result.filterIndexed { idx, _ -> idx % 2 == 0 && idx != 0 && idx != 1000 }
                .sum()) * 0.001 / 3.0

            println("simpson integral check - w: $simpson")
            call.respond<List<List<Double>>>(
                status = HttpStatusCode.OK,
                message = listOf(result)
            )
        }

        get("/robustpdfposterior") {
            println("robust pdf requested")
            val ns = (call.request.queryParameters["ns"] ?: "10").toDouble()
            val nt = (call.request.queryParameters["nt"] ?: "10").toDouble()
            val ps = (call.request.queryParameters["ps"] ?: "0.5").toDouble()
            val pt = (call.request.queryParameters["pt"] ?: "0.5").toDouble()
            val alpha = (call.request.queryParameters["alpha"] ?: "0.5").toDouble()
            val beta = (call.request.queryParameters["beta"] ?: "0.5").toDouble()
            val w = (call.request.queryParameters["w"] ?: "0.5").toDouble()

            val xs = ps * ns
            val xt = pt * nt

            val betadist1 = BetaDistribution(xt + xs + alpha, nt - xt + (ns - xs) + beta)
            val betadist2 = BetaDistribution(xt + alpha, nt - xt + beta)

            val postwUnweighted1 = w * exp(
                Beta.logBeta(xt + xs + alpha, (nt - xt) + (ns - xs) + beta)
                        - Beta.logBeta(xs + alpha, (ns - xs) + beta)
            )
            val postwUnweighted2 = (1.0-w) * exp(
                Beta.logBeta(xt + alpha, nt - xt + beta)
                        - Beta.logBeta(alpha, beta)
            )
            val postw = postwUnweighted1 / (postwUnweighted1 + postwUnweighted2)

           // println("simpson integral check - w: $simpson")
            call.respond<Pair<Double,Pair<List<Double>,List<Double>>>>(
                status = HttpStatusCode.OK,
                message = Pair(postw,Pair(first=
                    (1..999).map {
                        val p = it.toDouble() / 1000.0
                        return@map betadist1.density(p)
                    },
                    second=
                    (1..999).map {
                        val p = it.toDouble() / 1000.0
                        return@map betadist2.density(p)
                    }
                ))
            )

        }


        get("/robustpdfprior") {
            println("prior robust pdf requested")
            val ns = (call.request.queryParameters["ns"] ?: "10").toDouble()
            val ps = (call.request.queryParameters["ps"] ?: "0.5").toDouble()
            val alpha = (call.request.queryParameters["alpha"] ?: "0.5").toDouble()
            val beta = (call.request.queryParameters["beta"] ?: "0.5").toDouble()

            val xs = ps * ns

            val betadist1 = BetaDistribution(xs + alpha, (ns - xs) + beta)
            val betadist2 = BetaDistribution(alpha, beta)

            val result1 = (1..999).map {
                val p = it.toDouble() / 1000.0
                return@map betadist1.density(p)
            }
            val result2 = (1..999).map {
                val p = it.toDouble() / 1000.0
                return@map betadist2.density(p)
            }

            call.respond<Pair<List<Double>,List<Double>>>(
                status = HttpStatusCode.OK,
                message = Pair(result1,result2)
            )

        }


    }
}