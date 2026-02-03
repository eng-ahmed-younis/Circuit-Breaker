package com.example.circuitbreaker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.circuitbreaker.data.circuitbreaker.CircuitBreaker
import com.example.circuitbreaker.data.circuitbreaker.CircuitBreakerStats
import com.example.circuitbreaker.data.dto.PostDto
import com.example.circuitbreaker.data.dto.UserDto
import com.example.circuitbreaker.ui.theme.CircuitBreakerTheme
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    viewModel: UserViewModel = koinViewModel()
) {
    val usersState by viewModel.usersState.collectAsState()
    val postsState by viewModel.postsState.collectAsState()
    val ktorStats by viewModel.ktorStatsState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Users", "Posts", "Stats")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Circuit Breaker Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> UsersTab(usersState, viewModel)
                1 -> PostsTab(postsState, viewModel)
                2 -> StatsTab(ktorStats, viewModel)
            }
        }
    }
}

@Composable
fun UsersTab(
    usersState: UiState<List<UserDto>>,
    viewModel: UserViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { viewModel.fetchUsers() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch Users")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (usersState) {
            is UiState.Idle -> EmptyState("Click the button to fetch users")
            is UiState.Loading -> LoadingState()
            is UiState.Success -> UsersList(users = usersState.data)
            is UiState.Error -> ErrorState(
                message = usersState.message,
                isCircuitBreakerOpen = usersState.isCircuitBreakerOpen
            )
        }
    }
}

@Composable
fun PostsTab(
    postsState: UiState<List<PostDto>>,
    viewModel: UserViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { viewModel.fetchPosts() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch Posts")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (postsState) {
            is UiState.Idle -> EmptyState("Click the button to fetch posts")
            is UiState.Loading -> LoadingState()
            is UiState.Success -> PostsList(posts = postsState.data)
            is UiState.Error -> ErrorState(
                message = postsState.message,
                isCircuitBreakerOpen = postsState.isCircuitBreakerOpen
            )
        }
    }
}

@Composable
fun StatsTab(
    stats: CircuitBreakerStats?,
    viewModel: UserViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { viewModel.refreshStats() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Stats")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (stats != null) {
            CircuitBreakerStatsCard(stats)
        } else {
            EmptyState("No statistics available yet")
        }
    }
}

@Composable
fun CircuitBreakerStatsCard(stats: CircuitBreakerStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Circuit Breaker: ${stats.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Divider()

            StatRow("State", getStateText(stats.state), getStateColor(stats.state))
            StatRow("Failure Count", "${stats.failureCount} / ${stats.failureThreshold}", Color.Unspecified)
            StatRow("Half-Open Attempts", stats.halfOpenAttempts.toString(), Color.Unspecified)

            if (stats.lastFailureTime > 0) {
                val timeSinceFailure = (System.currentTimeMillis() - stats.lastFailureTime) / 1000
                StatRow("Time Since Last Failure", "${timeSinceFailure}s ago", Color.Unspecified)
            }
        }
    }
}

fun getStateText(state: CircuitBreaker.State): String {
    return when (state) {
        CircuitBreaker.State.CLOSED -> "CLOSED (Healthy)"
        CircuitBreaker.State.OPEN -> "OPEN (Failing)"
        CircuitBreaker.State.HALF_OPEN -> "HALF-OPEN (Testing)"
    }
}

fun getStateColor(state: CircuitBreaker.State): Color {
    return when (state) {
        CircuitBreaker.State.CLOSED -> Color(0xFF4CAF50)
        CircuitBreaker.State.OPEN -> Color(0xFFF44336)
        CircuitBreaker.State.HALF_OPEN -> Color(0xFFFF9800)
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun UsersList(users: List<UserDto>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Total Users: ${users.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(users) { user ->
            UserCard(user)
        }
    }
}

@Composable
fun UserCard(user: UserDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            user.phone?.let {
                Text(
                    text = "Phone: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PostsList(posts: List<PostDto>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Total Posts: ${posts.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(posts) { post ->
            PostCard(post)
        }
    }
}

@Composable
fun PostCard(post: PostDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(
    message: String,
    isCircuitBreakerOpen: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isCircuitBreakerOpen) {
                    Color(0xFFFFEBEE)
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isCircuitBreakerOpen) "⚠️ Circuit Breaker Open" else "❌ Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isCircuitBreakerOpen) Color(0xFFC62828) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}





















// ============================================
// PREVIEWS
// ============================================

@Preview(showBackground = true)
@Composable
fun PreviewUserCard() {
    CircuitBreakerTheme {
        UserCard(
            user = UserDto(
                id = 1,
                name = "John Doe",
                email = "john.doe@example.com",
                phone = "+1-555-0101"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPostCard() {
    CircuitBreakerTheme {
        PostCard(
            post = PostDto(
                id = 1,
                userId = 1,
                title = "Introduction to Android Development",
                body = "Android development has become one of the most popular platforms for mobile app creation. In this post, we'll explore the fundamentals of building Android applications."
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUsersList() {
    CircuitBreakerTheme {
        UsersList(
            users = listOf(
                UserDto(1, "John Doe", "john@example.com", "+1-555-0101"),
                UserDto(2, "Jane Smith", "jane@example.com", "+1-555-0102"),
                UserDto(3, "Bob Johnson", "bob@example.com", "+1-555-0103")
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPostsList() {
    CircuitBreakerTheme {
        PostsList(
            posts = listOf(
                PostDto(1, 1, "First Post", "This is the first post content"),
                PostDto(2, 1, "Second Post", "This is the second post content"),
                PostDto(3, 2, "Third Post", "This is the third post content")
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEmptyState() {
    CircuitBreakerTheme {
        EmptyState("Click the button to fetch users")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoadingState() {
    CircuitBreakerTheme {
        LoadingState()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewErrorState() {
    CircuitBreakerTheme {
        ErrorState(
            message = "Network connection failed",
            isCircuitBreakerOpen = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCircuitBreakerOpenError() {
    CircuitBreakerTheme {
        ErrorState(
            message = "Circuit breaker is OPEN. Service unavailable. Retry in 25 seconds.",
            isCircuitBreakerOpen = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCircuitBreakerStatsCardClosed() {
    CircuitBreakerTheme {
        CircuitBreakerStatsCard(
            stats = CircuitBreakerStats(
                name = "KtorCircuitBreaker",
                state = CircuitBreaker.State.CLOSED,
                failureCount = 0,
                failureThreshold = 3,
                lastFailureTime = 0,
                halfOpenAttempts = 0
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCircuitBreakerStatsCardOpen() {
    CircuitBreakerTheme {
        CircuitBreakerStatsCard(
            stats = CircuitBreakerStats(
                name = "KtorCircuitBreaker",
                state = CircuitBreaker.State.OPEN,
                failureCount = 3,
                failureThreshold = 3,
                lastFailureTime = System.currentTimeMillis() - 15000,
                halfOpenAttempts = 0
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCircuitBreakerStatsCardHalfOpen() {
    CircuitBreakerTheme {
        CircuitBreakerStatsCard(
            stats = CircuitBreakerStats(
                name = "KtorCircuitBreaker",
                state = CircuitBreaker.State.HALF_OPEN,
                failureCount = 3,
                failureThreshold = 3,
                lastFailureTime = System.currentTimeMillis() - 30000,
                halfOpenAttempts = 1
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStatRow() {
    CircuitBreakerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StatRow("State", "CLOSED (Healthy)", Color(0xFF4CAF50))
            StatRow("Failure Count", "0 / 3", Color.Unspecified)
            StatRow("Half-Open Attempts", "0", Color.Unspecified)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewUsersTabIdle() {
    CircuitBreakerTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Fetch Users")
                }
                EmptyState("Click the button to fetch users")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewUsersTabLoading() {
    CircuitBreakerTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Fetch Users")
                }
                LoadingState()
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewUsersTabSuccess() {
    CircuitBreakerTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fetch Users")
                }
                Spacer(modifier = Modifier.height(16.dp))
                UsersList(
                    users = listOf(
                        UserDto(1, "John Doe", "john@example.com", "+1-555-0101"),
                        UserDto(2, "Jane Smith", "jane@example.com", "+1-555-0102")
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewPostsTabSuccess() {
    CircuitBreakerTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fetch Posts")
                }
                Spacer(modifier = Modifier.height(16.dp))
                PostsList(
                    posts = listOf(
                        PostDto(1, 1, "Introduction to Android", "Learn about Android development..."),
                        PostDto(2, 1, "Kotlin Coroutines", "Understanding async programming...")
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewStatsTab() {
    CircuitBreakerTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh Stats")
                }
                Spacer(modifier = Modifier.height(16.dp))
                CircuitBreakerStatsCard(
                    stats = CircuitBreakerStats(
                        name = "KtorCircuitBreaker",
                        state = CircuitBreaker.State.CLOSED,
                        failureCount = 1,
                        failureThreshold = 3,
                        lastFailureTime = System.currentTimeMillis() - 5000,
                        halfOpenAttempts = 0
                    )
                )
            }
        }
    }
}