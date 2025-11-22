package percentile.project.demo.tree

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import percentile.project.demo.TreeEntry


@Composable
fun ExpandableItemWidget(
    modifier : Modifier = Modifier,
    item: TreeEntry,
    level: Int = 0,  // Add level parameter,
    getIcd: (String) -> TreeEntry?,
    isSelected: (String) -> Boolean,
    onSelectedChange: (String, Boolean) -> Unit,
    useSearch: Boolean = false,
    searchresult: (String)->Boolean,
    searchterm: String = "",

    ) {
    var expanded by remember {
        mutableStateOf(false)
    }
    val indentationDp = (level * 16).dp  // 16dp per level


    val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

    if (useSearch && searchresult(item.value) ) {
        expanded=true
    }

        Column(
            modifier = modifier.fillMaxWidth()
                .background(Color(red = 255, green = 255, blue = 255, alpha = 255))

                 .animateContentSize(
                     animationSpec = tween(durationMillis = 200)
                 )
                .padding(start = indentationDp)
                /*  .height(if (expanded) 400.dp else 50.dp)*/
                .clickable(
                    enabled = item.children.isNotEmpty(),
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    expanded = !expanded
                }

        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {

                    Icon(
                        imageVector = if (item.children.isEmpty()) Icons.Filled.Circle
                            else if (!expanded) Icons.Filled.ExpandMore
                        else Icons.Filled.ExpandLess,
                        contentDescription = "Settings Icon",
                        tint= if (item.children.isEmpty()) Color.Black else
                            if (expanded) Color.Green else Color.Red
                    )
                    Checkbox(
                        checked=isSelected(item.value ),
                        onCheckedChange={ isChecked ->
                            println("Checkbox for ${item.value} changed to $isChecked")
                            onSelectedChange(item.value,isChecked)
                        }
                    )

                    val icdCode = buildAnnotatedString {
                        if (item.valueLabel.isNotEmpty()) {
                            withStyle(style = boldStyle) {
                                append(item.valueLabel)
                            }
                            append(": ")
                        }
                        if (searchterm.isEmpty()||searchterm.length<3) append(item.label)
                        else {

                            val idx = item.label.indexOf(string=searchterm, ignoreCase = true)
                            if (idx<0) append(item.label)
                            else if (idx<0) {
                                append(item.label+"x"+searchterm)
                            }
                            else {
                                append(item.label.take(idx))
                                withStyle(style = boldStyle) {
                                    append(item.label.substring(idx, idx + searchterm.length))
                                }
                                if ((idx + searchterm.length)<item.label.length) append(item.label.substring(startIndex = idx + searchterm.length))
                            }
                        }
                    }

                    Text(text = icdCode )
            }
/*
            if (expanded) {
                content()
            }*/

       // AnimatedVisibility(visible=expanded) {
            if (expanded) {
                /*
                LazyColumn {
                    items(
                        items= item.children.mapNotNull { getIcd(it) },
                    key= { it.value} ) { childItem ->
                        ExpandableItemWidget(
                            item = childItem,
                            level = level + 1,
                            getIcd = getIcd,
                            isSelected = isSelected,
                            onSelectedChange = onSelectedChange,
                            useSearch=useSearch,
                            searchresult = searchresult
                        )
                    }
                }*/

                if (item.children.isNotEmpty()) {
                    Column {

                        item.children.forEach { childstr ->
                            val child = getIcd(childstr)
                            child?.let {
                                ExpandableItemWidget(
                                    item = it,
                                    level = level + 1,
                                    getIcd = getIcd,
                                    isSelected = isSelected,
                                    onSelectedChange = onSelectedChange,
                                    useSearch = useSearch,
                                    searchresult = searchresult,
                                    searchterm = searchterm
                                )
                            }
                        }
                    }
                }
            }



    }
}