package percentile.project.demo

import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


class IcdApi {
    private val client = HttpClient {

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

   // private val baseUrl = "http://10.11.12.120:$SERVER_PORT"
    companion object {
       private const val BASEURL ="http://127.0.0.1:$SERVER_PORT"
   }
/*
    suspend fun getIcd(code: String): Result<Icdentry> {
        return try {
            val response = client.get("$baseUrl/icd") {
                parameter("code", code)
            }

            if (response.status.value == 200) {
                Result.success(response.body<Icdentry>())
            } else {
                Result.failure(Exception("Not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/
/*
    suspend fun getIcdChildren(code: String): Result<List<Icdentry>> {
        return try {
            val response = client.get("$baseUrl/icdchildren") {
                parameter("code", code)
            }

            if (response.status.value == 200) {
                Result.success(response.body<List<Icdentry>>())
            } else {
                Result.failure(Exception("Not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

    suspend fun getIcdFull(request: String= "icdfulllist") : /*Result<Map<String,Icdentry>>*/ Result<HttpResponse> = runCatching {
        client.get("$BASEURL/$request")
    }

/*
    suspend fun getDensity(ns: Int, ps: Float,nt: Int, pt: Float, service:String = "npp",alphaprior: Float=0.5f, betaprior: Float=0.5f):
            Result<List<List<Double>>> {
        return try {
            val response = client.get("$baseUrl/$service") {
                parameter("ns", ns)
                parameter("ps", ps)
                parameter("nt", nt)
                parameter("pt", pt)
                parameter("alpha", alphaprior)
                parameter("beta", betaprior)
            }
            if (response.status.value == 200) {
                Result.success(response.body<List<List<Double>>>())
            } else {
                Result.failure(Exception("Not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

/*
    suspend fun robustMixture(ns: Int, ps: Float,nt: Int, pt: Float,w: Float, service:String = "pdf",alphaprior: Float=0.5f, betaprior: Float=0.5f): Result<Pair<Double,Pair<List<Double>,List<Double>>>> {
        return try {
            val response = client.get("$baseUrl/robustpdfposterior") {
                parameter("ns", ns)
                parameter("ps", ps)
                parameter("nt", nt)
                parameter("pt", pt)
                parameter("w", w)
                parameter("alpha", alphaprior)
                parameter("beta", betaprior)
                parameter("figure",service)
            }
            if (response.status.value == 200) {
                Result.success(response.body<Pair<Double,Pair<List<Double>,List<Double>>>>())
            } else {
                Result.failure(Exception("Not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/

/*
    suspend fun robustMixtureprior(ns: Int, ps: Float,alphaprior: Float=0.5f, betaprior: Float=0.5f): Result<Pair<List<Double>,List<Double>>> {
        return try {
            val response = client.get("$baseUrl/robustpdfprior") {
                parameter("ns", ns)
                parameter("ps", ps)
                parameter("alpha", alphaprior)
                parameter("beta", betaprior)
            }
            if (response.status.value == 200) {
                Result.success(response.body<Pair<List<Double>,List<Double>>>())
            } else {
                Result.failure(Exception("Not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/
    fun close() {
        client.close()
    }
}

