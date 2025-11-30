package percentile.project.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCohort(cohort: CohortCriteria,
               onOk: (CohortCriteria) -> Unit,
               onCancel: ()->Unit) {

    var name by remember {
        mutableStateOf(cohort.name)
    }
    var ageRange by remember {
        mutableStateOf(cohort.ageSpan.first.toFloat()..cohort.ageSpan.last.toFloat())
    }
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) ) {

        Column {
            Row() {
                Text(
                    text = "Cohort: ${cohort.id}"
                )
                TextField(
                    value=name,
                    onValueChange = { name=it },
                    label = { Text(text="Name") }
                )
            }
            Row {
                Text(text="Age:")
                RangeSlider(
                    startThumb = {Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = ageRange.start.roundToInt().toString(), modifier = Modifier.padding(bottom = 4.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 2.dp, height = 20.dp)
                                .background(Color.Black)
                        )
                    }},
                    endThumb = {Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = ageRange.endInclusive.roundToInt().toString(), modifier = Modifier.padding(bottom = 4.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 2.dp, height = 20.dp)
                                .background(Color.Black)
                        )
                    }},
                    valueRange = 0f..100f,
                    value=ageRange,
                    onValueChange = { ageRange = it},
                    steps = 99
                )
            }
            Row(modifier=Modifier.fillMaxSize().padding(5.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {

                DiagnosisDialog(buttonText = "Select diagnoses", title = "Select diagnoses", request = "icdfulllist", onSelectionConfirmed = {})
                DiagnosisDialog(buttonText = "Select drugs", title = "Select drugs", request = "uscfulllist", onSelectionConfirmed = {})
                Button(
                    modifier=Modifier.fillMaxWidth(0.4f),
                    onClick = {

                        onOk(
                            CohortCriteria(
                                id = cohort.id,
                                name=name,
                                ageSpan=ageRange.start.toInt()..ageRange.endInclusive.toInt()))
                    }) {
                    Text(text = "Ok")
                }
            }
        }
    }
}