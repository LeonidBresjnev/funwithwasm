package percentile.project.demo.tree

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.launch
import percentile.project.demo.TreeEntry

@Composable
fun ExpandableTree(items: List<TreeEntry>,
                   getIcd: (String) -> TreeEntry?,
                   modifier: Modifier = Modifier,
                   isSelected: (String) -> Boolean,
                   onSelectedChange: (String, Boolean) -> Unit,
                   useSearch: Boolean = false,
                   setUseSearch: (Boolean) -> Unit,
                   searchresult: (String)->Boolean,
                   searchterm: String = "") {

    val scrollState = rememberScrollState()


    val scope = rememberCoroutineScope()



    Box(modifier = modifier.onKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.DirectionUp -> {

                    scope.launch {
                        scrollState.animateScrollTo((scrollState.value - 40).coerceAtLeast(0))
                    }
                    //scrollState.scrollTo((scrollState.value - 40).coerceAtLeast(0))
                    true
                }
                Key.DirectionDown -> {
                    scope.launch {
                        scrollState.animateScrollTo((scrollState.value + 40).coerceAtMost(scrollState.maxValue))
                    }
                    true
                }
                else -> false
            }
        } else false
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize().verticalScroll(scrollState)
        ) {


                items.filter { it.parent == "None"}.forEach {
                    ExpandableItemWidget(
                        item = it,
                        getIcd = getIcd,
                        isSelected = isSelected,
                        onSelectedChange = onSelectedChange,
                        useSearch = useSearch,
                        searchresult = searchresult,
                        searchterm = searchterm,
                    )
                }
                /*
            items.filter { it.parent == "None" }.forEach { item ->
                ExpandableItemWidget(
                    item = item,
                    getIcd = getIcd,
                    isSelected = isSelected,
                    onSelectedChange = onSelectedChange,
                    useSearch = useSearch,
                    searchresult = searchresult,
                    searchterm = searchterm,
                )
            }*/
            }


            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd).fillMaxHeight()
            )
        }

    setUseSearch(false)
}


/*
@Composable
fun ExpandableTree(tree: List<MenuData>,onClick:() ->Unit={}) {

    if (tree.isEmpty()) {
        Row {
            Text("No items available")
        }
    }
    else {

        LazyColumn {
            items(
                count = tree.size,
                key = { index -> tree[index].id }
            ) { it ->
                Text(text=tree[it].displayName "$it",)
                if (tree[it].children.isNotEmpty()) {
                    ExpandableTree(tree=(tree[it].children))
                }
            }
        }

    }
}*/
