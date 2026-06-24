package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.HostRepository
import com.example.data.repository.PresetAgentRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HostsScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.TermiAgentViewModel
import com.example.ui.viewmodel.TermiAgentViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup local database & repository layers
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val database = AppDatabase.getDatabase(this, applicationScope)
        val hostRepository = HostRepository(database.hostDao())
        val chatRepository = ChatRepository(database.chatMessageDao())
        val presetAgentRepository = PresetAgentRepository(database.presetAgentDao())

        val factory = TermiAgentViewModelFactory(
            application = application,
            hostRepository = hostRepository,
            chatRepository = chatRepository,
            presetAgentRepository = presetAgentRepository
        )
        val viewModel = ViewModelProvider(this, factory)[TermiAgentViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppLayout(viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Nodes : Screen("nodes", "SSH Nodes", Icons.Default.Dns)
    object Chat : Screen("chat", "AI Agent", Icons.Default.Forum)
    object Presets : Screen("presets", "Presets", Icons.Default.AutoAwesome)
}

@Composable
fun MainAppLayout(viewModel: TermiAgentViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationItems = listOf(
        Screen.Dashboard,
        Screen.Nodes,
        Screen.Chat,
        Screen.Presets
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val hosts by viewModel.hosts.collectAsState()
    val selectedHost by viewModel.selectedHost.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SophisticatedSurface,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SophisticatedSurface)
                        .padding(20.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Server Network",
                            tint = SophisticatedAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SSH & MESH NODES",
                            color = SophisticatedText,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Machines managed securely, including low-latency Tailscale tunnels.",
                        color = SophisticatedTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Host Toggle / List of hosts
                    if (hosts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active hosts. Link a machine in the SSH Nodes section.",
                                color = SophisticatedTextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Scrolling list of hosts
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(hosts) { host ->
                                val isSelected = host.id == selectedHost?.id
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) SophisticatedSurfaceVariant else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) SophisticatedAccent else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.selectHost(host.id)
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Active dot or state icon
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (host.isActive) SophisticatedAccent else SophisticatedTextMuted)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = host.name,
                                                    color = SophisticatedText,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "${host.username}@${host.ipOrHostname}",
                                                    color = SophisticatedTextMuted,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        
                                        // Tailscale tag badge
                                        if (host.useTailscale) {
                                            Surface(
                                                color = SophisticatedPrimary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Tailscale",
                                                    color = SophisticatedPrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Row: quick link to add hosts
                    HorizontalDivider(color = SophisticatedBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            navController.navigate(Screen.Nodes.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedSurfaceVariant,
                            contentColor = SophisticatedText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Server",
                            tint = SophisticatedAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add SSH Node",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = SophisticatedSurface,
                    modifier = Modifier.testTag("app_navigation_bar")
                ) {
                    navigationItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) SophisticatedAccent else SophisticatedTextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    color = if (isSelected) SophisticatedText else SophisticatedTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = SophisticatedSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToChat = {
                            navController.navigate(Screen.Chat.route) {
                                popUpTo(Screen.Dashboard.route)
                                launchSingleTop = true
                            }
                        },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Nodes.route) {
                    HostsScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Chat.route) {
                    ChatScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Presets.route) {
                    PresetsScreen(
                        viewModel = viewModel,
                        onNavigateToChat = {
                            navController.navigate(Screen.Chat.route) {
                                popUpTo(Screen.Dashboard.route)
                                launchSingleTop = true
                            }
                        },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}
