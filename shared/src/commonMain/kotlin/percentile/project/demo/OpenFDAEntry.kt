package percentile.project.demo

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class OpenFDAEntry(
    val meta: Meta,
    val results: List<OpenFDAResultEntry>
)

@Serializable
data class Meta(
    val disclaimer: String,
    val terms: String,
    val license: String,
    val last_updated: String,
    val results: MetaResults
)

@Serializable
data class MetaResults(
    val total: Int,
    val limit: Int,
    val skip: Int
)

@Serializable
data class OpenFDAResultEntry(
    val indications_and_usage: List<String> = emptyList(),
    val dosage_and_administration: List<String> = emptyList(),
    val dosage_forms_and_strengths: List<String> = emptyList(),
    val contraindications: List<String> = emptyList(),
    val warnings_and_cautions: List<String> = emptyList(),
    val adverse_reactions: List<String> = emptyList(),
    val drug_interactions: List<String> = emptyList(),
    val use_in_specific_populations: List<String> = emptyList(),
    val pregnancy: List<String> = emptyList(),
    val pediatric_use: List<String> = emptyList(),
    val geriatric_use: List<String> = emptyList(),
    val overdosage: List<String> = emptyList(),
    val description: List<String> = emptyList(),
    val clinical_pharmacology: List<String> = emptyList(),
    val mechanism_of_action: List<String> = emptyList(),
    val pharmacodynamics: List<String> = emptyList(),
    val pharmacokinetics: List<String> = emptyList(),
    val nonclinical_toxicology: List<String> = emptyList(),
    val carcinogenesis_and_mutagenesis_and_impairment_of_fertility: List<String> = emptyList(),
    val animal_pharmacology_and_or_toxicology: List<String> = emptyList(),
    val clinical_studies: List<String> = emptyList(),
    val how_supplied: List<String> = emptyList(),
    val storage_and_handling: List<String> = emptyList(),
    val information_for_patients: List<String> = emptyList(),
    val spl_medguide: List<String> = emptyList(),
    val package_label_principal_display_panel: List<String> = emptyList(),
    val dosage_and_administration_table: List<String> = emptyList(),
    val openfda: OpenFDA
) {
    @OptIn(ExperimentalUuidApi::class)
    @Transient val key= Uuid.random()
}

@Serializable
data class OpenFDA(
    val brand_name: List<String> = emptyList(),
    val generic_name: List<String> = emptyList(),
    val substance_name: List<String> = emptyList(),
)