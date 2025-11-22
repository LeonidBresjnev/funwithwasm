package percentile.project.demo


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi

/*
import percentile.project.demo.tree.MenuData
import percentile.project.demo.tree.MenuData_impl*/
@OptIn(ExperimentalMaterial3Api::class, ExperimentalKoalaPlotApi::class)
@Composable
fun App() {



    /*
    val tree: MenuData = MenuData_impl(
        id = 1,
        displayName = "Root",
        parentId = null,
        children = listOf(
            MenuData_impl(
                id = 2,
                displayName = "Child 1",
                parentId = 1,
                children = listOf(
                    MenuData_impl(
                        id = 3,
                        displayName = "Child 11",
                        parentId = 2,
                        children = listOf()
                    ),
                    MenuData_impl(
                        id = 4,
                        displayName = "Child 12",
                        parentId = 2,
                        children = listOf()
                    )
                )
            ),
            MenuData_impl(
                id = 5,
                displayName = "Child 2",
                parentId = 1,
                children = listOf(
                    MenuData_impl(
                        id = 6,
                        displayName = "Child 21",
                        parentId = 5,
                        children = listOf()
                    )
                )
            )
        )
    )*/

/*
    val icdApi = IcdApi()
    var root: Icdentry? = null

    scope.launch(Dispatchers.Default) {
        val result = async {
            val codeDef =  icdApi.getIcd("00000")
            return@async codeDef
        }

        root = result.await().getOrNull()
    }*/


    val tabRowItems = listOf(
        TabRowItem(
            title = "Fun with Normalized power prior",
            screen = {
                NormalizedPowerPrior()

            }
        ),

        TabRowItem(
            title = "Fun with Robust Mixture prior",
            screen = {
                RobustMixture()

            }
        ),

        TabRowItem(
            title = "Fun with P-value based power prior",
            screen = {
                PValueBased()
            }
        ),

        TabRowItem(
            title = "More Fun!",
            screen = {
                MoreFun()

            }
        )
    )


    val pagerState = rememberPagerState {
        tabRowItems.size
    }

    var selectedTabIndex by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(selectedTabIndex) {
        pagerState.animateScrollToPage(selectedTabIndex)
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            selectedTabIndex = pagerState.currentPage
        }
    }
    MaterialTheme {



        Scaffold(
            modifier=Modifier.fillMaxSize(),
            topBar = { TopAppBar(
                title = { Text("Compose Multiplatform Demo") },
                colors= TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
            )) },
            bottomBar = {  Text(text = getPlatform().name) }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabRowItems.forEachIndexed { index, item ->
                        Tab(
                            enabled = item.enabled,
                            selected = index == selectedTabIndex,
                            onClick = {
                                selectedTabIndex = index
                            },
                            //
                            text = {
                                Text(text = item.title,

                                    style=if (index==selectedTabIndex) MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold)
                                        else MaterialTheme.typography.bodyMedium
                                    )
                            }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.92f),
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = true
                ) {
                    tabRowItems[it].screen()
                }


            }

        }

    }
}
